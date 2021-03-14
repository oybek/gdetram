package io.github.oybek.gdetram.domain.handler

import cats.data.EitherT
import cats.{Applicative, Monad}
import cats.implicits._
import io.github.oybek.gdetram.db.repository.MessageRepoAlg
import io.github.oybek.gdetram.model.Button

class PsHandler[F[_]: Applicative: Monad](implicit
                                          messageRepo: MessageRepoAlg[F]) extends Handler[F, UserId, Option[String]] {

  override def handle(userId: UserId): EitherT[F, (String, List[List[Button]]), Option[String]] =
    nextF(messageRepo.pollAsyncMessage(userId))
}
