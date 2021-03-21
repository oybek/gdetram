package io.github.oybek.gdetram.dao

import io.github.oybek.gdetram.model.Stop
import io.github.oybek.gdetram.service.model.Message.Geo

trait StopRepo[F[_]] {
  def findByName(query: String, cityId: Int): F[Option[(Stop, Int)]]
  def getNearest(geo: Geo): F[List[Stop]]
}
