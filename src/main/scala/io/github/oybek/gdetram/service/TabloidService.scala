package io.github.oybek.gdetram.service

import cats.effect.{Clock, Sync}
import cats.syntax.all._
import io.github.oybek.gdetram.model.Stop
import io.github.oybek.plato.model.Arrival
import io.github.oybek.plato.parser.BustimeParser

trait TabloidService[F[_]] {
  def getArrivals(stop: Stop): F[List[(String, List[Arrival])]]
}

class TabloidServiceBustimeImpl[F[_]: Sync: Clock](implicit documentFetcher: DocFetcherAlg[F]) extends TabloidService[F] {

  override def getArrivals(stop: Stop): F[List[(String, List[Arrival])]] =
    documentFetcher.fetchCached(stop.url).map { doc =>
      doc.map(BustimeParser.parse).getOrElse(List())
    }
}
