package io.github.oybek.gdetram.db

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.scalatest.IOChecker
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import io.github.oybek.gdetram.db.repository.Queries
import io.github.oybek.gdetram.model
import io.github.oybek.gdetram.model.Platform.{Tg, Vk}
import io.github.oybek.gdetram.model.{Record, User}
import org.flywaydb.core.Flyway
import org.scalatest.funsuite.AnyFunSuite

import java.sql.Timestamp

class QueriesSpec extends AnyFunSuite with IOChecker with ForAllTestContainer {

  override val container = PostgreSQLContainer()
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

  lazy val transactor =
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
  }

  test("user repo queries") {
    check(Queries.selectAllUsersQuery)
  }

  test("city select") {
    check(Queries.selectMostMatchedCity("Екатеринург"))
  }

  test("all city select") {
    check(Queries.selectAllCitites)
  }

  test("user_info upsert/select queries") {
    check(Queries.upsertUserQuery(User(Vk, 1, 1, 1.some, 1)))
    check(Queries.selectUserQuery(Vk, 1))
  }

  test("Journal insert request") {
    check(
      Queries.insertRecordSql(Record(0, new Timestamp(3L), "0", "test", Vk))
    )
    check(
      Queries.insertRecordSql(model.Record(0, new Timestamp(3L), "0", "test", Tg))
    )
  }

  test("Select most matched stops") {
    check(Queries.selectMostMatched("Ельцина", 1))
    check(Queries.selectNearest(0.0f, 0.0f))
  }

  test("Message queries") {
    check(Queries.getAsyncMessageFor((Vk, 123)))
    check(Queries.getSyncMessage(Vk, 100))
    check(Queries.delSyncMessageFor((Vk, 123), "hello"))
    check(Queries.delAsyncMessageFor((Vk, 123), "hello"))
  }
}
