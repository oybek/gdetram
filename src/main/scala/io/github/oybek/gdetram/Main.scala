package io.github.oybek.gdetram

import java.util.concurrent.TimeUnit
import cats.syntax.all._
import cats.instances.list._
import cats.instances.option._
import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource, Sync, Timer}
import cats.~>
import doobie.hikari.HikariTransactor
import io.github.oybek.gdetram.config.Config
import io.github.oybek.gdetram.model.Platform.{Tg, Vk}
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
import doobie.{ConnectionIO, ExecutionContexts}
import io.github.oybek.gdetram.dao.impl.{CityRepoImpl, JournalRepoImpl, MessageRepoImpl, StopRepoImpl, UserRepoImpl}
import io.github.oybek.gdetram.dao.{CityRepo, JournalRepo, MessageRepo, StopRepo, UserRepo}
import io.github.oybek.gdetram.service.impl.{CityService, LogicImpl, MetricServiceImpl, RegistrationService, StartService, StatusService, StopService, TabloidServiceImpl, UserServiceImpl}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

object Main extends IOApp {
  type F[+T] = IO[T]
  type G[T] = ConnectionIO[T]

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
          case (transactor, httpClient, blocker) =>
            implicit val transaction: ~>[G, F] = new ~>[G, F] {
              override def apply[A](cio: G[A]): F[A] =
                cio.transact(transactor)
            }

            implicit val client : Client[F] = Logger(logHeaders = false, logBody = false)(httpClient)

            implicit val cityRepo    : CityRepo[G]    = CityRepoImpl
            implicit val stopRepo    : StopRepo[G]    = StopRepoImpl
            implicit val userRepo    : UserRepo[G]    = UserRepoImpl
            implicit val messageRepo : MessageRepo[G] = MessageRepoImpl
            implicit val journalRepo : JournalRepo[G] = JournalRepoImpl

            implicit val tabloidService : TabloidService[F] = new TabloidServiceImpl[F]

            implicit val startService        : StartService[F]           = new StartService[F]
            implicit val registrationService : RegistrationService[F, G] = new RegistrationService[F, G]
            implicit val cityService         : CityService[F, G]         = new CityService[F, G]
            implicit val stopService         : StopService[F, G]         = new StopService[F, G]
            implicit val statusService       : StatusService[F, G]       = new StatusService[F, G]

            implicit val core : Logic[F] = new LogicImpl[F, G]

            implicit val metricService : MetricService[F, G] = new MetricServiceImpl[F, G]

            implicit val vkBotApi : VkApi[F] = new VkApiHttp4s[F](client)
            implicit val tgBotApi : Api[F]   = new BotApi[F](client, s"https://api.telegram.org/bot${config.tgBotApiToken}", blocker)

            implicit val vkBot: VkBot[F] = new VkBot[F](config.getLongPollServerReq)
            implicit val tgBot: TgBot[F, G] = new TgBot[F, G](config.adminTgIds.split(",").map(_.trim).toList)

            for {
              _ <- DB.initialize(transactor)
              f1 <- vkBot.start.start
              f2 <- tgBot.start.start
              _ <- tgBot.dailyReports("Ну что уебаны?! Готовы к метрикам?".some).everyDayAt(8, 0).start
              _ <- spamTg(messageRepo).start.void
              _ <- spamVk(messageRepo).start.void
              _ <- vkRevoke(vkBotApi, vkBot, config.getLongPollServerReq).every(10.seconds, (9, 24)).start.void
              _ <- transaction(UserServiceImpl.refreshUserInfo).attempt.void.everyDayAt(0, 0).start
              _ <- f1.join
              _ <- f2.join
            } yield ()
        }
    } yield ExitCode.Success

  private def spamTg(messageRepo: MessageRepo[G])(implicit
                                                  tgBot: TgBot[F, G],
                                                  transaction: G ~> F): F[Unit] = (
    for {
      messages <- transaction(messageRepo.pollSyncMessage(Tg))
      _ <- Sync[F].delay(log.info(s"sync_message tg: $messages"))
      _ <- messages.toNel.traverse(_.head match {
        case (Tg, id, text) => tgBot.send(id.toInt, text)
        case _ => ().pure[F]
      })
    } yield ()
  ).every(10.seconds, (9, 20))

  private def spamVk(messageRepo: MessageRepo[G])(implicit
                                                  vkBot: VkBot[F],
                                                  transaction: G ~> F): F[Unit] = (
    for {
      messages <- transaction(messageRepo.pollSyncMessage(Vk, 100))
      _ <- Sync[F].delay(log.info(s"sync_message vk: $messages"))
      _ <- messages.toNel.traverse(messages1 =>
        vkBot.sendMessage(
          Right(messages1.map(_._2).some),
          messages1.head._3,
        )
      )
    } yield ()
  ).every(10.seconds, (9, 20))

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
