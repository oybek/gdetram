package io.github.oybek.gdetram

import cats.effect.{Blocker, IO}
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.hikari.HikariTransactor
import doobie.util
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import io.github.oybek.gdetram.db.DB
import io.github.oybek.gdetram.db.repository.{CityRepo, CityRepoAlg, JournalRepo, MessageRepo, MessageRepoAlg, StopRepo, StopRepoAlg, UserRepoImpl, UserRepo}
import io.github.oybek.gdetram.domain.LogicImpl
import io.github.oybek.gdetram.donnars.StopDonnar
import io.github.oybek.gdetram.service.{MetricService, TabloidService}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import telegramium.bots.high.Api

class TgBotSpec extends AnyFlatSpec with Matchers with MockFactory with StopDonnar with ForAllTestContainer {

  override val container = PostgreSQLContainer()
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)
  implicit val tm = IO.timer(ExecutionContexts.synchronous)
  lazy val transactor =
    Transactor
      .fromDriverManager[IO](
        container.driverClassName,
        container.jdbcUrl,
        container.username,
        container.password,
      )

/*
  implicit val api: Api[IO] = mock[Api[IO]]

  implicit val cityRepo: CityRepoAlg[IO] = new CityRepo[IO](transactor)
  implicit val journalRepo: JournalRepo[IO] = new JournalRepo[IO](transactor)
  implicit val messageRepo: MessageRepoAlg[IO] = new MessageRepo[IO](transactor)
  implicit val stopRepo: StopRepoAlg[IO] = new StopRepo[IO](transactor)
  implicit val userRepo: UserRepoAlg[IO] = new UserRepo[IO](transactor)

  implicit val metricsService: MetricService[IO] = new MetricService[IO]
  implicit val tabloid: TabloidAlg[IO] = stub[TabloidAlg[IO]]

  implicit val brain: Brain[IO] = new Brain[IO]

  implicit val tgBot: TgBot[IO] = new TgBot[IO](List.empty[String])
*/
}
