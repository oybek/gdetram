package io.github.oybek.gdetram

import java.util.concurrent.TimeUnit
import cats.data.NonEmptyList
import cats.syntax.all._
import cats.instances.list._
import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource, Sync, Timer}
import doobie.hikari.HikariTransactor
import io.github.oybek.gdetram.config.Config
import io.github.oybek.gdetram.db.DB
import io.github.oybek.gdetram.db.repository._
import io.github.oybek.gdetram.model.Platform.{Tg, Vk}
import io.github.oybek.gdetram.domain.{Logic, LogicImpl}
import io.github.oybek.gdetram.service.{DocFetcherAlg, TabloidServiceBustimeImpl}
import io.github.oybek.gdetram.service._
import io.github.oybek.gdetram.util.TimeTools._
import io.github.oybek.vk4s.api.{GetConversationsReq, GetLongPollServerReq, Unanswered, VkApi, VkApiHttp4s}
import io.github.oybek.vk4s.domain.{Conversation, Peer}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import org.slf4j.LoggerFactory
import telegramium.bots.high.{Api, BotApi}

import doobie.implicits._
import doobie.{ExecutionContexts, Transactor}
import io.github.oybek.gdetram.domain.handler.{CityHandler, FirstHandler, StatusFormer, StopHandler}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

object Main extends IOApp {
  type F[+T] = IO[T]

  private val log = LoggerFactory.getLogger("Main")

  override def run(args: List[String]): IO[ExitCode] =
    for {
      configFile <- Sync[F].delay {
        Option(System.getProperty("application.conf"))
      }
      config <- Config.load[F](configFile)
      _ <- Sync[F].delay { log.info(s"loaded config: $config") }
      _ <- resources(config)
        .use {
          case (hikariTransactor, httpClient, blocker) =>
            implicit val transactor      : Transactor[F]       = hikariTransactor
            implicit val client          : Client[F]           = Logger(logHeaders = false, logBody = false)(httpClient)

            implicit val cityRepo        : CityRepoAlg[F]      = new CityRepo[F](transactor)
            implicit val journalRepo     : JournalRepoAlg[F]   = new JournalRepo(transactor)
            implicit val stopRepo        : StopRepoAlg[F]      = new StopRepo(transactor)
            implicit val userRepo        : UserRepo            = new UserRepoImpl
            implicit val messageRepo     : MessageRepo         = new MessageRepoImpl

            implicit val documentFetcher : DocFetcherAlg[F]    = new DocFetcher[F]
            implicit val source1         : TabloidService[F]   = new TabloidServiceBustimeImpl[F]

            implicit val firstHandler    : FirstHandler[F]     = new FirstHandler[F]
            implicit val cityHandler     : CityHandler[F]      = new CityHandler[F]
            implicit val stopHandler     : StopHandler[F]      = new StopHandler[F]
            implicit val statusFormer    : StatusFormer[F]     = new StatusFormer[F]

            implicit val core            : Logic[F]            = new LogicImpl[F]
            implicit val metricService   : MetricServiceAlg[F] = new MetricService[F]

            implicit val vkBotApi        : VkApi[F]            = new VkApiHttp4s[F](client)
            implicit val tgBotApi        : Api[F]              = new BotApi[F](client, s"https://api.telegram.org/bot${config.tgBotApiToken}", blocker)

            implicit val vkBot: VkBot[F] = new VkBot[F](config.getLongPollServerReq)
            implicit val tgBot: TgBot[F] = new TgBot[F](config.adminTgIds.split(",").map(_.trim).toList)

            for {
              _ <- DB.initialize(hikariTransactor)
              f1 <- vkBot.start.start
              f2 <- tgBot.start.start

              _ <- tgBot.dailyReports("Ну что уебаны?! Готовы к метрикам?".some).everyDayAt(8, 0).start

              _ <- spamTg(messageRepo).start.void
              _ <- spamVk(messageRepo).start.void

              _ <- vkRevoke(vkBotApi, vkBot, config.getLongPollServerReq)
                .every(10.seconds, (9, 24)).start.void

              _ <- f1.join
              _ <- f2.join
            } yield ()
        }
    } yield ExitCode.Success

  private def spamTg(messageRepo: MessageRepo)(implicit
                                               tgBot: TgBot[F],
                                               transactor: Transactor[F]): F[Unit] =
    messageRepo
      .pollSyncMessage(Tg)
      .transact(transactor)
      .flatTap(x => Sync[F].delay(log.info(s"sync_message tg: $x")))
      .flatMap {
        case List((Tg, id, text)) => tgBot.send(id.toInt, text)
        case _ => Sync[F].unit
      }
      .attempt.flatMap {
        case Left(ex) =>
          Sync[F].delay { log.error(s"tg sync_message send error ${ex.getLocalizedMessage}") } >>
            spamTg(messageRepo)
        case Right(_) =>
          ().pure[F]
      }
      .every(10.seconds, (9, 20))

  private def spamVk(messageRepo: MessageRepo)(implicit
                                               vkBot: VkBot[F],
                                               transactor: Transactor[F]): F[Unit] =
    messageRepo
      .pollSyncMessage(Vk, 100)
      .transact(transactor)
      .flatTap(x => Sync[F].delay(log.info(s"sync_message vk: $x")))
      .flatMap {
        case (Vk, id, text)::xs =>
          vkBot.sendMessage(
            Right(NonEmptyList.fromList(id::xs.map(_._2))),
            text,
          )
        case _ => Sync[F].unit
      }
      .attempt.flatMap {
        case Left(ex) =>
          Sync[F].delay { log.error(s"vk sync_message send error ${ex.getLocalizedMessage}") } >>
            spamVk(messageRepo)
        case Right(_) =>
          ().pure[F]
      }
      .every(10.seconds, (9, 20))

  private def vkRevoke(vkApi: VkApi[F],
                       vkBot: VkBot[F],
                       getLongPollServerReq: GetLongPollServerReq,
                       offset: Int = 0,
                       count: Int = 100): F[Unit] =
    for {
      getConversationsRes <- vkApi.getConversations(
        GetConversationsReq(
          filter = Unanswered,
          offset = offset,
          count = count,
          version = getLongPollServerReq.version,
          accessToken = getLongPollServerReq.accessToken
        )
      )
      _ <- getConversationsRes.response.items.map(_.conversation).traverse {
        case Conversation(Peer(peerId, _, _), _) =>
          vkBot.sendMessage(Left(peerId), "Прошу прощения за заминки, сейчас я снова работаю") >>
            Timer[F].sleep(2 seconds)
      }
    } yield ()

  private def resources(
    config: Config
  ): Resource[F, (HikariTransactor[F], Client[F], Blocker)] = {
    for {
      httpCp <- ExecutionContexts.cachedThreadPool[F]
      connEc <- ExecutionContexts.fixedThreadPool[F](10)
      tranEc <- ExecutionContexts.cachedThreadPool[F]
      transactor <- DB.transactor[F](config.database, connEc, Blocker.liftExecutionContext(tranEc))
      httpClient <- BlazeClientBuilder[F](httpCp)
        .withResponseHeaderTimeout(FiniteDuration(60, TimeUnit.SECONDS))
        .resource
      blocker <- Blocker[F]
    } yield (transactor, httpClient, blocker)
  }
}
