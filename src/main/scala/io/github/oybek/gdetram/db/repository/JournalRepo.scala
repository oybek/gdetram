package io.github.oybek.gdetram.db.repository

import java.sql.Timestamp

import cats.effect.Sync
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import doobie._
import doobie.implicits._
import doobie.implicits.javasql._
import doobie.util.query.Query0
import io.github.oybek.gdetram.model.Record

case class DailyMetric(dateWhen: Timestamp, cityName: String, active: Int, passive: Int)

trait JournalRepoAlg[F[_]] {
  def insert(record: Record): F[Int]
}

class JournalRepo[F[_]: Sync](transactor: Transactor[F]) extends JournalRepoAlg[F] {
  def insert(record: Record): F[Int] =
    Queries.insertRecordSql(record).run.transact(transactor)
}
