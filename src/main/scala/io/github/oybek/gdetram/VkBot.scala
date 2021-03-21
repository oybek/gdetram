package io.github.oybek.gdetram

import cats.data.NonEmptyList
import cats.effect.concurrent.Ref
import cats.effect.syntax.all._
import cats.effect.{Async, Clock, Concurrent, Sync, Timer}
import cats.syntax.all._
import io.github.oybek.gdetram.dao.JournalRepo
import io.github.oybek.gdetram.model.Platform.Vk
import io.github.oybek.gdetram.service.Logic
import io.github.oybek.gdetram.service.model.Message.{Geo, Text}
import io.github.oybek.gdetram.util.Formatting._
import io.github.oybek.vk4s.api._
import io.github.oybek.vk4s.domain.{AudioMessage, LongPollBot, MessageNew, WallPostNew, WallReplyNew}
import io.github.oybek.vk4s.api.{GetLongPollServerReq, Keyboard, SendMessageReq}
import org.http4s.client.Client
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._

class VkBot[F[_]: Async: Timer: Concurrent](getLongPollServerReq: GetLongPollServerReq)(implicit httpClient: Client[F],
                                                                                        core: Logic[F],
                                                                                        vkApi: VkApi[F])
    extends LongPollBot[F](httpClient, vkApi, getLongPollServerReq) {

  implicit val log: Logger = LoggerFactory.getLogger("VkGate")

  override def onMessageNew(message: MessageNew): F[Unit] = (
    Sync[F].delay { log.info(s"got message $message") } >> (
      message match {
        case MessageNew(_, _, peerId, _, _, Some(geo), _) =>
          for {
            answer <- core.handle(Vk -> peerId, Geo(geo.coordinates.latitude, geo.coordinates.longitude))
            _ <- sendMessage(Left(peerId), text = answer._1, keyboard = answer._2.toVk.some)
          } yield ()

        case MessageNew(messageId, _, peerId, _, _, _, List(AudioMessage(_, _, _, _, _, _))) =>
          for {
            _ <- Timer[F].sleep(1500 millis)
            getMessageByIdRes <- vkApi.getMessageById(
              GetMessageByIdReq(
                messageIds = List(messageId),
                version = getLongPollServerReq.version,
                accessToken = getLongPollServerReq.accessToken
              )
            )
            text = getMessageByIdRes.response.items.headOption.flatMap {
              _.attachments.collectFirst { case x: AudioMessage => x }
            } match {
              case Some(AudioMessage(_, _, _, _, _, Some(text))) => text
              case _ => "Cannot recognize speech"
            }
            answer <- core.handle(Vk -> peerId, Text(text))
            _ <- sendMessage(Left(peerId), text = answer._1, keyboard = answer._2.toVk.some)
          } yield ()

        case MessageNew(_, _, peerId, _, text, _, _) =>
          for {
            answer <- core.handle(Vk -> peerId, Text(text))
            _ <- sendMessage(
              Left(peerId),
              text = answer._1,
              keyboard = answer._2.toVk.some
            )
          } yield ()
      }
  )).start.void

  override def onWallPostNew(wallPostNew: WallPostNew): F[Unit] = Sync[F].unit

  override def onWallReplyNew(wallReplyNew: WallReplyNew): F[Unit] =
    Sync[F].unit

  def sendMessage(to: Either[Long, Option[NonEmptyList[Long]]],
                  text: String,
                  attachment: Option[String] = None,
                  keyboard: Option[Keyboard] = None): F[Unit] = {
    val sendMessageReq = SendMessageReq(
      peerId = to.fold(_.some, _ => None),
      message = text,
      userIds = to.fold(_ => None, identity),
      version = getLongPollServerReq.version,
      randomId = 0,
      accessToken = getLongPollServerReq.accessToken,
      attachment = attachment,
      keyboard = keyboard
    )
    for {
      time <- Clock[F].realTime(MILLISECONDS)
      resp <- vkApi.sendMessage(sendMessageReq.copy(randomId = time))
      _ <- Sync[F].delay { log.info(s"send message: $sendMessageReq, got resp $resp") }
    } yield ()
  }
}
