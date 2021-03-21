package io.github.oybek.gdetram.service

import org.jsoup.nodes.Document

trait DocumentService[F[_]] {
  def fetchCached(url: String): F[Option[Document]]
}
