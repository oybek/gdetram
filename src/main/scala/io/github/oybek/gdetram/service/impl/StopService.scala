package io.github.oybek.gdetram.service.impl

import cats.effect._
import cats.implicits._
import cats.{Monad, ~>}
import io.github.oybek.gdetram.dao.{CityRepo, JournalRepo, StopRepo, UserRepo}
import io.github.oybek.gdetram.model._
import io.github.oybek.gdetram.service.Replies.{cantFindStop, nearestStops, noCityBase}
import io.github.oybek.gdetram.service._
import io.github.oybek.gdetram.service.model.Button.TextButton
import io.github.oybek.gdetram.service.model.Message
import io.github.oybek.gdetram.service.model.Message.{Geo, Text}
import io.github.oybek.gdetram.util.Formatting
import io.github.oybek.plato.model.Arrival

import java.sql.Timestamp

class StopService[F[_] : Monad: Timer, G[_]: Monad](implicit
                                                    cityRepo: CityRepo[G],
                                                    stopRepo: StopRepo[G],
                                                    userRepo: UserRepo[G],
                                                    journalRepo: JournalRepo[G],
                                                    tabloidService: TabloidService[F],
                                                    transaction: G ~> F) extends AuthorizedHandler[F, Reply] {

  override def handle(user: User, message: Message): F[Either[Reply, Reply]] = message match {
    case geo: Geo =>
      transaction {
        for {
          city <- cityRepo.get(user.cityId)
          stops <- stopRepo.getNearest(geo)
        } yield nearestStops(stops, city.name).asLeft[Reply]
      }

    case Text(text) =>
      transaction {
        stopRepo.findByName(text, user.cityId)
      } flatMap {
        case Some((stop, mistakeNum)) if mistakeNum < (stop.name.length / 2).max(4) =>
          replyTabloid(user, text, stop).map(_.asRight[Reply])

        case Some(_) => cantFindStop.asLeft[Reply].pure[F]
        case None => noCityBase.asLeft[Reply].pure[F]
      }
  }

  private def replyTabloid(user: User,
                           userText: String,
                           stop: Stop) =
    for {
      tabloidText <- getTabloid(stop)
      currMillis <- Timer[F].clock.realTime(scala.concurrent.duration.MILLISECONDS)
      _ <- transaction(
        journalRepo.insert(
          Record(
            stop.id,
            new Timestamp(currMillis),
            user.userId._2.toString,
            userText,
            user.userId._1)
        ) >> userRepo.update(user.copy(lastStopId = stop.id.some))
      )
    } yield (
      tabloidText,
      List(List(TextButton(stop.name)))
    )

  private def getTabloid(stop: Stop) =
    tabloidService
      .getArrivals(stop)
      .map {
        case Nil => s"На остановку ${stop.name} сейчас ничего не едет"
        case arrivals =>
          val (nonEmptyArrivals, emptyArrivals) = arrivals.partition(_._2.nonEmpty)
          (nonEmptyArrivals.sortBy(_._1) ++ emptyArrivals.sortBy(_._1)).map {
            case (dir, arrivals) => Formatting.toChatText(stop, dir, arrivals)
          }.mkString
      }
}
