package io.github.oybek.gdetram.domain.handler

import cats.Applicative
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId}
import io.github.oybek.gdetram.domain.{Input, Text}
import io.github.oybek.gdetram.domain.Phrases.cityAsk
import io.github.oybek.gdetram.model.{Button, Platform}

class FirstHandler[F[_]: Applicative] extends Handler[F, Input, Unit] {
  val handle: Input => F[Either[Reply, Unit]] = {
    case Text("начать" | "/start") =>
      (cityAsk, defaultKbrd()).asLeft[Unit].pure[F]
    case _ =>
      ().asRight[Reply].pure[F]
  }
}
