package io.github.oybek.gdetram.service.impl

import cats.Monad
import cats.data.EitherT
import cats.effect.{Concurrent, Sync, Timer}
import io.github.oybek.gdetram.service.model.Message
import io.github.oybek.gdetram.service.{Logic, Reply, UserId}

class LogicImpl[F[_]: Sync: Concurrent: Timer, G[_]: Monad](implicit
                                                            startService: StartService[F],
                                                            registrationService: RegistrationService[F, G],
                                                            cityService: CityService[F, G],
                                                            stopService: StopService[F, G],
                                                            statusService: StatusService[F, G])
  extends Logic[F] {

  def handle(userId: UserId, message: Message): F[Reply] = (
    for {
      _                   <- EitherT(startService.handle(userId, message))
      user                <- EitherT(registrationService.handle(userId, message))
      _                   <- EitherT(cityService.handle(user, message))
      (tabloid, keyboard) <- EitherT(stopService.handle(user, message))
      status              <- EitherT(statusService.handle(user, message))
    } yield (
      tabloid + "\n" +
        status,
      keyboard
    )
  ).fold(identity, identity)
}
