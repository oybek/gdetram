package io.github.oybek.gdetram.service

import cats.effect.IO
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.implicits._
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import io.github.oybek.gdetram.dao.impl.UserRepoImpl
import io.github.oybek.gdetram.dao.{CityRepo, JournalRepo, StopRepo}
import io.github.oybek.gdetram.service.impl.{RegistrationService, StartService}
import io.github.oybek.gdetram.donnars.StopDonnar
import io.github.oybek.gdetram.model.Platform.Vk
import io.github.oybek.gdetram.model.{City, Stop, User}
import io.github.oybek.plato.model.TransportT._
import io.github.oybek.plato.model.{Arrival, TransportT}
import org.flywaydb.core.Flyway
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

/*
class LogicImplSpec extends AnyFlatSpec with Matchers
                                        with MockFactory
                                        with StopDonnar
                                        with ForAllTestContainer {
  override lazy val container = PostgreSQLContainer()
  implicit lazy val cs = IO.contextShift(ExecutionContexts.synchronous)
  implicit lazy val tm = IO.timer(ExecutionContexts.synchronous)
  implicit lazy val transactor =
    Transactor
      .fromDriverManager[IO](
        container.driverClassName,
        container.jdbcUrl,
        container.username,
        container.password
      )

  implicit lazy val journalRepo = stub[JournalRepoAlg[IO]]
  implicit lazy val messageRepo = new MessageRepoImpl
  implicit lazy val stopRepo = stub[StopRepoAlg[IO]]
  implicit lazy val extractor = stub[TabloidService[IO]]
  implicit lazy val cityRepo = stub[CityRepo[IO]]
  implicit lazy val userRepo = new UserRepoImpl

  implicit lazy val firstHandler = new StartService[IO]
  implicit lazy val cityHandler = new RegistrationService[IO]
  implicit lazy val stopHandler = new StopHandler[IO]
  implicit lazy val psHandler = new StatusFormer[IO]

  override def afterStart(): Unit = {
    val flyway = Flyway
      .configure()
      .dataSource(container.jdbcUrl, container.username, container.password)
      .load()
    flyway.clean()
    flyway.migrate()

    userRepo.upsert(User(Vk, 123, 1, None, 0)).transact(transactor).unsafeRunSync()
  }

  "simple query" must "work" in {
    // test datas
    val stop = Stop(0, "Дом кино", 0.0f, 0.0f, "http://foo.bar", City(1, "city", 0.0f, 0.0f))

    // stubs
    (stopRepo.selectMostMatched _)
      .when("Дом кино", 1)
      .returns(IO { Some(stop -> 0) })

    (() => cityRepo.selectAll)
      .when()
      .returns(IO { List(City(1, "city", 0.0f, 0.0f)) })

    (cityRepo.select _)
      .when(*)
      .returns(IO { City(1, "city", 0.0f, 0.0f) })

    (extractor.getArrivals _)
      .when(stop)
      .returns(IO { List("Гагарина" -> List(Arrival("25", 5 minutes, Bus))) })

    (journalRepo.insert _)
      .when(*)
      .returns(IO { 1 })

    // action
    val core = new LogicImpl[IO]
    val result = core.handle(Vk -> 123)(Text("Дом кино"))

    // check
    result.unsafeRunSync()._1 shouldBe
      s"""
         |Дом кино $rightArrow Гагарина
         |${TransportT.emoji(TransportT.Bus)} 25 - 5 мин.
         |
         |city
         |""".stripMargin
  }

  "when geo is got" must "find nearest stops and update cache" in {
    // TODO: complete the test
  }

  private val rightArrow = "➡️"
}


 */