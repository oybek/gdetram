package io.github.oybek.gdetram.domain

import cats.data.EitherT

import cats.effect._
import cats.implicits._
import io.github.oybek.gdetram.domain.chain.model.Input
import io.github.oybek.gdetram.domain.chain._
import io.github.oybek.gdetram.domain.model.Button

trait CoreAlg[F[_]] {
  def handle(userId: UserId)(input: Input): F[Reply]
}

class Core[F[_]: Sync: Concurrent: Timer](implicit
                                          firstHandler: FirstHandler[F],
                                          cityHandler: CityHandler[F],
                                          stopHandler: StopHandler[F],
                                          psHandler: PsHandler[F])
    extends CoreAlg[F] {

  def handle(userId: UserId)(input: Input): F[Reply] = (
    for {
      _            <- EitherT(firstHandler.handle(input))
      (city, text) <- EitherT(cityHandler.handle(userId, input))
      reply        <- EitherT(stopHandler.handle((userId, city, text)))
      _            <- EitherT(psHandler.handle((userId, reply)))
    } yield ("", List.empty[List[Button]])
  ).value.map(_.fold(identity, identity))
}
