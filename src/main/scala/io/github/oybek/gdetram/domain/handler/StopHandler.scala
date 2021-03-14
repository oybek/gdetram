package io.github.oybek.gdetram.domain.handler

import cats.data.EitherT
import cats.{Applicative, Monad}
import cats.implicits._
import cats.effect._
import io.github.oybek.gdetram.db.repository._
import io.github.oybek.gdetram.domain.Text
import io.github.oybek.gdetram.model._
import io.github.oybek.gdetram.service.TabloidService
import io.github.oybek.gdetram.util.Formatting

import java.sql.Timestamp

class StopHandler[F[_] : Applicative: Monad: Timer](implicit
                                                    stopRepo: StopRepoAlg[F],
                                                    journalRepo: JournalRepoAlg[F],
                                                    tabloid: TabloidService[F]) extends Handler[F, (UserId, City, Text), Reply] {

  override def handle(input: (UserId, City, Text)): EitherT[F, Reply, Reply] = input match {
    case (userId, city, Text(userText)) =>
      EitherT.right(stopRepo.selectMostMatched(userText, city.id)).flatMap {
        case Some((stop, mistakeNum)) if mistakeNum < (stop.name.length / 2).max(4) =>
          nextF(replyTabloid(userId, city, userText, stop))

        case Some(_) =>
          reply(
            """
              |Не знаю такую остановку 😟
              |
              |Отправьте геопозицию - я подскажу названия ближайших остановок
              |""".stripMargin,
            defaultKbrd(TextButton("город " + city.name)))

        case None =>
          reply(
            s"""
               |Для города ${city.name}
               |пока не загружена база остановок
               |""".stripMargin,
            defaultKbrd(TextButton("город " + city.name)))
      }
  }

  private def replyTabloid(userId: UserId,
                           city: City,
                           userText: String,
                           stop: Stop) =
    for {
      tabloidText <- getTabloid(stop)
      currMillis <- Timer[F].clock.realTime(scala.concurrent.duration.MILLISECONDS)
      _ <- journalRepo.insert(Record(stop.id, new Timestamp(currMillis), userId._2.toString, userText, userId._1))
    } yield (
      tabloidText,
      defaultKbrd(
        TextButton("город " + city.name),
        TextButton(stop.name)
      )
    )

  private def getTabloid(stop: Stop) =
    tabloid
      .getArrivals(stop)
      .map {
        case Nil => s"На остановку ${stop.name} сейчас ничего не едет"
        case l =>
          l.map {
            case (dir, arrivals) =>
              Formatting.toChatText(stop, dir, arrivals)
          }.mkString
      }
}
