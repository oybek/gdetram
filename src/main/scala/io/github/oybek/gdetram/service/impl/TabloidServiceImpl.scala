package io.github.oybek.gdetram.service.impl

import cats.effect.{Clock, Sync}
import cats.syntax.all._
import io.github.oybek.gdetram.model.Stop
import io.github.oybek.gdetram.service.{DocumentService, TabloidService}
import io.github.oybek.plato.model.Arrival
import io.github.oybek.plato.parser.BustimeParser

class TabloidServiceImpl[F[_]: Sync: Clock] extends TabloidService[F] {
  private val documentFetcher : DocumentService[F] = new DocumentServiceImpl[F]
  override def getArrivals(stop: Stop): F[List[(String, List[Arrival])]] =
    documentFetcher.fetchCached(stop.url).map { doc =>
      doc.map(BustimeParser.parse).getOrElse(List())
    }
}
