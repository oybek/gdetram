package io.github.oybek.gdetram.dao.impl

import doobie.scalatest.IOChecker
import io.github.oybek.gdetram.model.Platform.Tg
import io.github.oybek.gdetram.samples.TestInstances.randomUserId
import org.scalatest.funsuite.AnyFunSuite

class MessageRepoQueriesSpec extends AnyFunSuite with IOChecker with PostgresSetup {
  test("check MessageRepo queries") {
    check(MessageRepoImpl.delAsyncMessageFor(randomUserId, ""))
    check(MessageRepoImpl.getAsyncMessageFor(randomUserId))
    check(MessageRepoImpl.getSyncMessage(Tg, 1))
    check(MessageRepoImpl.delSyncMessageFor(randomUserId, ""))
  }
}
