package io.github.oybek.gdetram.domain.chain

import cats.{Applicative, Monad}
import cats.implicits._
import io.github.oybek.gdetram.db.repository.MessageRepoAlg

class PsHandler[F[_]: Applicative: Monad](implicit
                                          messageRepo: MessageRepoAlg[F]) extends Handler[F, (UserId, Reply), Unit] {

  val handle: ((UserId, Reply)) => F[Either[Reply, Unit]] = {
    case (userId, (replyText, replyKbrd)) =>
      messageRepo.pollAsyncMessage(userId).map(psText =>
        (replyText + psText.fold("")("\n" + _), replyKbrd).asLeft[Unit]
      )
  }
}
