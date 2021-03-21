package io.github.oybek.gdetram.db

import cats.effect.IO
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.scalatest.IOChecker
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import io.github.oybek.gdetram.dao.impl.{CityRepoImpl, JournalRepoImpl, MessageRepoImpl, StopRepoImpl, UserRepoImpl}
import io.github.oybek.gdetram.service.impl.UserServiceImpl
import io.github.oybek.gdetram.model.Platform.Tg
import io.github.oybek.gdetram.model.{Record, User}
import io.github.oybek.gdetram.service.model.Message.Geo
import org.flywaydb.core.Flyway
import org.scalatest.funsuite.AnyFunSuite

import java.sql.Timestamp
import java.time.LocalDateTime

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

  test("queries checks") {
    check(StopRepoImpl.selectMostMatched("123", 1))
    check(StopRepoImpl.selectNearest(Geo(0.0f, 0.0f)))

    check(CityRepoImpl.selectNearest(Geo(0.0f, 0.0f)))
    check(CityRepoImpl.select(1))
    check(CityRepoImpl.selectAll)
    check(CityRepoImpl.selectMostMatched("name"))

    check(UserRepoImpl.insert(User((Tg, 1), 1, None, 0)))
    check(UserRepoImpl.selectAll)
    check(UserRepoImpl.updateq(User((Tg, 1), 1, None, 0)))
    check(UserRepoImpl.select((Tg, 1)))

    check(JournalRepoImpl.insertRecordSql(Record(1, new Timestamp(System.currentTimeMillis()), "", "", Tg)))

    check(MessageRepoImpl.delAsyncMessageFor((Tg, 1), ""))
    check(MessageRepoImpl.getAsyncMessageFor((Tg, 1)))
    check(MessageRepoImpl.getSyncMessage(Tg, 1))
    check(MessageRepoImpl.delSyncMessageFor((Tg, 1), ""))

    check(UserServiceImpl.refreshUserInfoQ)
  }
}
