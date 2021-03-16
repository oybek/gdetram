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
                                               statusFormer: StatusFormer[F])
    extends Logic[F] {

  def handle(userId: UserId)(input: Input): F[Reply] = (
    for {
      _            <- firstHandler.handle(input)
      (city, text) <- cityHandler.handle(userId, input)
      (text, kbrd) <- stopHandler.handle(userId, city, text)
      status       <- statusFormer.handle(userId, city)
    } yield (
      text + status.fold("")("\n" + _),
      kbrd
    )
  ).fold(identity, identity)
}
