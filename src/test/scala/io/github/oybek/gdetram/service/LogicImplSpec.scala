package io.github.oybek.gdetram.service

import cats.effect.IO
import doobie.util.ExecutionContexts
import io.github.oybek.gdetram.db.repository._
import io.github.oybek.gdetram.donnars.StopDonnar
import io.github.oybek.gdetram.model.Platform.Vk
import io.github.oybek.gdetram.model.{City, Stop, User}
import io.github.oybek.plato.model.TransportT._
import io.github.oybek.plato.model.{Arrival, TransportT}
import io.github.oybek.gdetram.domain.{LogicImpl, Text}
import io.github.oybek.gdetram.domain.handler.{CityHandler, FirstHandler, PsHandler, StopHandler}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class LogicImplSpec extends AnyFlatSpec with Matchers with MockFactory with StopDonnar {
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)
  implicit val tm = IO.timer(ExecutionContexts.synchronous)

  implicit val journalRepo = stub[JournalRepoAlg[IO]]
  implicit val PSService = stub[MessageRepoAlg[IO]]
  implicit val stopRepo = stub[StopRepoAlg[IO]]
  implicit val extractor = stub[TabloidService[IO]]
  implicit val cityRepo = stub[CityRepoAlg[IO]]
  implicit val userRepo = stub[UserRepo[IO]]

  implicit val firstHandler = new FirstHandler[IO]
  implicit val cityHandler = new CityHandler[IO]
  implicit val stopHandler = new StopHandler[IO]
  implicit val psHandler = new PsHandler[IO]

  private val rightArrow = "➡️"

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

    (userRepo.select _)
      .when(*, *)
      .returns(IO { Some(User(Vk, 123, 1, None, 0)) })

    (cityRepo.select _)
      .when(*)
      .returns(IO { City(1, "city", 0.0f, 0.0f) })

    (extractor.getArrivals _)
      .when(stop)
      .returns(IO { List("Гагарина" -> List(Arrival("25", 5 minutes, Bus))) })

    (journalRepo.insert _)
      .when(*)
      .returns(IO { 1 })

    (PSService.pollAsyncMessage _)
      .when(*)
      .returns(IO { None })

    // action
    val core = new LogicImpl[IO]
    val result = core.handle(Vk -> 123)(Text("Дом кино"))

    // check
    result.unsafeRunSync()._1 shouldBe
      s"""
         |Дом кино $rightArrow Гагарина
         |${TransportT.emoji(TransportT.Bus)} 25 - 5 мин.
         |""".stripMargin
  }

  "when geo is got" must "find nearest stops and update cache" in {
    val user = Vk -> 123L

    (PSService.pollAsyncMessage _)
      .when(*)
      .returns(IO { None })

    val core = new LogicImpl[IO]

    // TODO: complete the test
  }
}
