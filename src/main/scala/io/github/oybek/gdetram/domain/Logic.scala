package io.github.oybek.gdetram.domain

import cats.data.EitherT
import cats.effect._
import cats.implicits._
import io.github.oybek.gdetram.domain.handler._

trait Logic[F[_]] {
  def handle(userId: UserId)(input: Input): F[Reply]
}

class LogicImpl[F[_]: Sync: Concurrent: Timer](implicit
                                               firstHandler: FirstHandler[F],
                                               cityHandler: CityHandler[F],
                                               stopHandler: StopHandler[F],
                                               psHandler: PsHandler[F])
    extends Logic[F] {

  def handle(userId: UserId)(input: Input): F[Reply] = (
    for {
      _            <- EitherT(firstHandler.handle(input))
      city         <- EitherT(cityHandler.handle(userId, input))
      (text, kbrd) <- EitherT(stopHandler.handle(userId, city, input))
      psText       <- EitherT(psHandler.handle(userId))
      result = (text + psText.fold("")("\n" + _), kbrd)
    } yield result
  ).value.map(_.fold(identity, identity))
}
