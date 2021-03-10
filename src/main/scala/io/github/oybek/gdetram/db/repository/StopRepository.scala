package io.github.oybek.gdetram.db.repository

import cats.effect.Sync
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.github.oybek.gdetram.model.Stop

trait StopRepoAlg[F[_]] {
  def selectMostMatched(query: String, cityId: Int): F[Option[(Stop, Int)]]
  def selectNearest(latitude: Float, longitude: Float): F[List[Stop]]
}

class StopRepo[F[_]: Sync](transactor: Transactor[F])
    extends StopRepoAlg[F] {

  def selectMostMatched(query: String, cityId: Int): F[Option[(Stop, Int)]] =
    Queries
      .selectMostMatched(query, cityId)
      .option
      .transact(transactor)

  def selectNearest(latitude: Float, longitude: Float): F[List[Stop]] =
    Queries
      .selectNearest(latitude, longitude)
      .to[List]
      .transact(transactor)
}
