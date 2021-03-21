package io.github.oybek.gdetram.service.impl

import cats.Applicative
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId}
import io.github.oybek.gdetram.service.Replies.cityAsk
import io.github.oybek.gdetram.service.model.Message
import io.github.oybek.gdetram.service.model.Message.Text
import io.github.oybek.gdetram.service.{Handler, Reply, UserId}

class StartService[F[_]: Applicative] extends Handler[F, Unit] {

  def handle(userId: UserId, message: Message): F[Either[Reply, Unit]] =
    message match {
      case Text("начать" | "/start") =>
        cityAsk(withGreeting = true).asLeft[Unit].pure[F]

      case _ =>
        ().asRight[Reply].pure[F]
    }
}
