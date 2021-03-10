package io.github.oybek.gdetram.db.repository

import cats.effect.Sync
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.github.oybek.gdetram.model.City

trait CityRepoAlg[F[_]] {
  def select(cityId: Int): F[City]
  def selectAll: F[List[City]]

  def find(query: String): F[(City, Int)]
}

class CityRepo[F[_]: Sync](transactor: Transactor[F]) extends CityRepoAlg[F] {
  def select(cityId: Int): F[City] =
    Queries
      .selectCityQuery(cityId)
      .unique
      .transact(transactor)

  def selectAll: F[List[City]] =
    Queries.selectAllCitites.to[List].transact(transactor)


  def find(query: String): F[(City, Int)] =
    Queries
      .selectMostMatchedCity(query)
      .unique
      .transact(transactor)
}
