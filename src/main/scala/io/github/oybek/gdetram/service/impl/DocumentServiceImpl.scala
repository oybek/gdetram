package io.github.oybek.gdetram.service.impl

import cats.effect.{Clock, Sync}
import cats.implicits._
import io.github.oybek.gdetram.service.DocumentService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import scala.jdk.CollectionConverters._

class DocumentServiceImpl[F[_]: Sync: Clock] extends DocumentService[F] {
  private val log = LoggerFactory.getLogger("DocumentFetcher")
  private val cache: concurrent.Map[String, (Long, Document)] =
    new ConcurrentHashMap[String, (Long, Document)](1 << 12).asScala

  def fetchCached(url: String): F[Option[Document]] =
    cache.get(url) match {
      case None                                           => fetchUncached(url)
      case Some((cachedTime, _)) if now - cachedTime > 10 => fetchUncached(url)
      case Some((_, document))                            =>
        Sync[F]
          .delay { log.info(s"$url is cached") }
          .as(Some(document))
    }

  private def fetchUncached(url: String): F[Option[Document]] =
    for {
      fetchAttempt <- Sync[F].delay { Jsoup.connect(url).get() }.attempt
      document <- fetchAttempt match {
        case Left(throwable) =>
          Sync[F]
            .delay(log.warn(s"error fetching document from $url: ${throwable.getMessage}"))
            .as(Option.empty[Document])
        case Right(doc) =>
          Sync[F]
            .delay(log.info(s"fetched document from $url"))
            .map(_ => cache.update(url, now -> doc))
            .as(Option(doc))
      }
    } yield document

  private def now = java.time.Instant.now().getEpochSecond
}
