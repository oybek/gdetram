package io.github.oybek.gdetram.service

import cats.effect.IO
import cats.implicits._
import cats.instances.list._
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.ConnectionIO
import doobie.implicits._
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import io.github.oybek.gdetram.db.repository.{CityRepo, CityRepoAlg, UserRepo, UserRepoImpl}
import io.github.oybek.gdetram.model.Platform.{Tg, Vk}
import io.github.oybek.gdetram.model.{City, User}
import io.github.oybek.gdetram.donnars.StopDonnar
import org.flywaydb.core.Flyway
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.sql.Timestamp
import java.time.LocalDateTime

class MetricsServiceSpec extends AnyFlatSpec
                         with Matchers
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

  override def afterStart(): Unit = {
    val flyway = Flyway
      .configure()
      .dataSource(container.jdbcUrl, container.username, container.password)
      .load()
    flyway.clean()
    flyway.migrate()

    (
      userRepo.upsert(User(Vk, 123, 1, None, 1)) >>
      userRepo.upsert(User(Vk, 124, 1, None, 0)) >>
      userRepo.upsert(User(Vk, 125, 2, None, 0)) >>
      userRepo.upsert(User(Tg, 123, 1, None, 1)) >>
      userRepo.upsert(User(Tg, 124, 1, None, 0)) >>
      userRepo.upsert(User(Tg, 125, 2, None, 0)) >>
      userRepo.upsert(User(Tg, 126, 2, None, 0))
    ).transact(transactor).void.unsafeRunSync()
  }

  implicit lazy val userRepo = new UserRepoImpl
  implicit lazy val cityRepo = stub[CityRepoAlg[IO]]
  implicit lazy val metricService = new MetricService[IO]

  private val yekb = City(1, "Екатеринбург", 0.0f, 0.0f)
  private val perm = City(2, "Пермь", 0.0f, 0.0f)

  "userStats method" must "evaluate correct stats" in {
    (() => cityRepo.selectAll)
      .when()
      .returns(IO.pure(List(yekb, perm)))

    metricService.userStats.unsafeRunSync() shouldBe (
      """ВК
        |#Екатеринбург - Активных: 1, Пассивных: 1
        |#Пермь - Активных: 0, Пассивных: 1
        |
        |Телега
        |#Екатеринбург - Активных: 1, Пассивных: 1
        |#Пермь - Активных: 0, Пассивных: 2""".stripMargin
    )
  }

  implicit class IntOps(n: Int) {
    def weeksAgo: Timestamp =
      Timestamp.valueOf(LocalDateTime.now().minusWeeks(n))
  }
}
