package io.github.oybek.gdetram.domain.chain

import cats.{Applicative, Monad}
import cats.implicits._
import cats.effect._
import io.github.oybek.gdetram.db.repository._
import io.github.oybek.gdetram.domain.chain.model._
import io.github.oybek.gdetram.domain.model._
import io.github.oybek.gdetram.service.TabloidAlg
import io.github.oybek.gdetram.util.Formatting

import java.sql.Timestamp

class StopHandler[F[_] : Applicative: Monad: Timer](implicit
                                                    stopRepo: StopRepoAlg[F],
                                                    journalRepo: JournalRepoAlg[F],
                                                    tabloid: TabloidAlg[F]) extends Handler[F, (UserId, City, Text), Reply] {

  val handle: ((UserId, City, Text)) => F[Either[Reply, Reply]] = {
    case (userId, city, Text(userText)) =>
      stopRepo.selectMostMatched(userText, city.id).flatMap {
        case Some((stop, mistakeNum)) if mistakeNum < (stop.name.length / 2).max(4) =>
          for {
            tabloidText <- getTabloid(stop)
            currMillis  <- Timer[F].clock.realTime(scala.concurrent.duration.MILLISECONDS)
            _           <- journalRepo.insert(Record(stop.id, new Timestamp(currMillis), userId._2.toString, userText, userId._1))
            replyKbrd = defaultKbrd(
              TextButton("город " + city.name),
              TextButton(stop.name)
            )
          } yield (tabloidText, replyKbrd).asRight[Reply]
        case Some(_) =>
          ("""|Не знаю такую остановку 😟
              |
              |Отправьте геопозицию - я подскажу названия ближайших остановок
              |""".stripMargin,
            defaultKbrd(TextButton("город " + city.name))).asLeft[Reply].pure[F]
        case None =>
          (s"""|Для города ${city.name}
               |пока не загружена база остановок
               |""".stripMargin,
            defaultKbrd(TextButton("город " + city.name))).asLeft[Reply].pure[F]
      }
  }

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
