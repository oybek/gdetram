package io.github.oybek.gdetram.dao.impl

import cats.Applicative.ops.toAllApplicativeOps
import doobie.implicits._
import doobie.implicits.javasql._
import doobie.ConnectionIO
import doobie.util.update.Update0
import io.github.oybek.gdetram.dao.JournalRepo
import io.github.oybek.gdetram.model.Record


object JournalRepoImpl extends JournalRepo[ConnectionIO] {
  def insert(record: Record): ConnectionIO[Unit] =
    insertRecordSql(record).run.void

  def insertRecordSql(record: Record): Update0 = {
    val Record(stopId, time, userId, text, platform) = record
    sql"insert into journal values ($stopId, $time, $userId, $text, $platform)".update
  }
}
