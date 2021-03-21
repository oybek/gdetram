package io.github.oybek.gdetram.service.impl

import cats.effect.Sync
import cats.implicits._
import cats.{Monad, ~>}
import io.github.oybek.gdetram.dao.{CityRepo, UserRepo}
import io.github.oybek.gdetram.service.Replies._
import io.github.oybek.gdetram.model._
import io.github.oybek.gdetram.service.model.Message
import io.github.oybek.gdetram.service.model.Message.{Geo, Text}
import io.github.oybek.gdetram.service.{AuthorizedHandler, Reply}

class CityService[F[_]: Sync, G[_]: Monad](implicit
                                           cityRepo: CityRepo[G],
                                           userRepo: UserRepo[G],
                                           transaction: G ~> F) extends AuthorizedHandler[F, Unit] {
  override def handle(user: User, message: Message): F[Either[Reply, Unit]] =
    transaction {
      message match {
        case geo: Geo =>
          for {
            city <- cityRepo.getNearest(geo)
            _ <- userRepo.update(user.copy(cityId = city.id))
          } yield ().asRight[Reply]

        case Text(text) if text.startsWith("город") =>
          for {
            (city, mistakeNum) <- cityRepo.findByName(text.drop(5).trim)
            cities <- cityRepo.getAll
            replyOrUser <- if (mistakeNum > 4) {
              cantFindCity(cities.map(_.name)).asLeft[Unit].pure[G]
            } else {
              userRepo.update(user.copy(cityId = city.id)).as(
                cityChosen(city.name, cities.map(_.name)).asLeft[Unit]
              )
            }
          } yield replyOrUser

        case _ => ().asRight[Reply].pure[G]
      }
    }
}
