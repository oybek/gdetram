package io.github.oybek.gdetram.domain.chain

import cats.Applicative
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId}
import io.github.oybek.gdetram.domain.Phrases.cityAsk
import io.github.oybek.gdetram.domain.chain.model.{Input, Text}
import io.github.oybek.gdetram.domain.model.{Button, Platform}

class FirstHandler[F[_]: Applicative] extends Handler[F, Input, Unit] {
  val handle: Input => F[Either[Reply, Unit]] = {
    case Text("начать" | "/start") =>
      (cityAsk, defaultKbrd()).asLeft[Unit].pure[F]
    case _ =>
      ().asRight[Reply].pure[F]
  }
}
