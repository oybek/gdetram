package io.github.oybek.gdetram.domain.handler

import cats.Applicative
import cats.data.EitherT
import io.github.oybek.gdetram.domain.Phrases.cityAsk
import io.github.oybek.gdetram.domain.{Input, Text}

class FirstHandler[F[_]: Applicative] extends Handler[F, Input, Unit] {

  def handle(input: Input): EitherT[F, Reply, Unit] = input match {
    case Text("начать" | "/start") =>
      EitherT.leftT[F, Unit]((cityAsk, defaultKbrd))
    case _ =>
      EitherT.rightT[F, Reply](())
  }
}
