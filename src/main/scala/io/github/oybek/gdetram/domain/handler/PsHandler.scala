package io.github.oybek.gdetram.domain.handler

import cats.{Applicative, Monad}
import cats.implicits._
import io.github.oybek.gdetram.db.repository.MessageRepoAlg

class PsHandler[F[_]: Applicative: Monad](implicit
                                          messageRepo: MessageRepoAlg[F]) extends Handler[F, UserId, Option[String]] {

  val handle: UserId => F[Either[Reply, Option[String]]] =
    messageRepo.pollAsyncMessage(_).map(_.asRight[Reply])
}
