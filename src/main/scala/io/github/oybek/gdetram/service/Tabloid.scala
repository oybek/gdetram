package io.github.oybek.gdetram.service

import cats.effect.{Clock, Sync}
import cats.syntax.all._
import io.github.oybek.gdetram.domain.model.Stop
import io.github.oybek.plato.model.Arrival
import io.github.oybek.plato.parser.ParserA

trait TabloidAlg[F[_]] {
  def extractInfo(stop: Stop): F[List[(String, List[Arrival])]]
}

class TabloidA[F[_]: Sync: Clock](implicit documentFetcher: DocFetcherAlg[F]) extends TabloidAlg[F] {

  override def extractInfo(stop: Stop): F[List[(String, List[Arrival])]] =
    documentFetcher.fetchCached(stop.url).map { doc =>
      doc.map(ParserA.parse).getOrElse(List())
    }
}