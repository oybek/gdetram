package io.github.oybek.gdetram.service

import io.github.oybek.gdetram.model.Stop
import io.github.oybek.plato.model.Arrival

trait TabloidService[F[_]] {
  def getArrivals(stop: Stop): F[List[(String, List[Arrival])]]
}
