package io.github.oybek.gdetram.service.impl

import cats.effect.Sync
import cats.implicits._
import cats.{Monad, ~>}
import io.github.oybek.gdetram.dao.{CityRepo, UserRepo}
import io.github.oybek.gdetram.service.Replies._
import io.github.oybek.gdetram.model._
import io.github.oybek.gdetram.service.model.Message
import io.github.oybek.gdetram.service.model.Message.{Geo, Text}
import io.github.oybek.gdetram.service.{Handler, Reply, UserId}

class RegistrationService[F[_]: Sync, G[_]: Monad](implicit
                                                   cityRepo: CityRepo[G],
                                                   userRepo: UserRepo[G],
                                                   transaction: G ~> F) extends Handler[F, User] {
  override def handle(userId: UserId, message: Message): F[Either[Reply, User]] =
    transaction {
      userRepo.get(userId).flatMap {
        case Some(user) => user.asRight[Reply].pure[G]
        case None => message match {
          case geo: Geo =>
            for {
              city <- cityRepo.getNearest(geo)
              user = User(userId, city.id)
              _    <- userRepo.add(User(userId, city.id))
            } yield user.asRight[Reply]

          case Text(text) =>
            for {
              (city, mistakeNum) <- cityRepo.findByName(text)
              cities <- cityRepo.getAll
              replyOrUser <- if (mistakeNum > 4) {
                cantFindCity(cities.map(_.name)).asLeft[User].pure[G]
              } else {
                userRepo.add(User(userId, city.id)).as(
                  cityChosen(city.name, cities.map(_.name)).asLeft[User]
                )
              }
            } yield replyOrUser
        }
      }
    }
}
