package io.github.oybek.gdetram.dao

import io.github.oybek.gdetram.model.City
import io.github.oybek.gdetram.service.model.Message.Geo

trait CityRepo[F[_]] {
  def findByName(name: String): F[(City, Int)]

  def get(cityId: Int): F[City]
  def getAll: F[List[City]]
  def getNearest(geo: Geo): F[City]
}
