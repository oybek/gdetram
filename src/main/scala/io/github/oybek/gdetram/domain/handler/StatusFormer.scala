package io.github.oybek.gdetram.domain.handler

import cats.data.EitherT
import cats.effect.Bracket
import cats.implicits._
import doobie.Transactor
import doobie.implicits._
import io.github.oybek.gdetram.db.repository.{MessageRepo, UserRepo}
import io.github.oybek.gdetram.model.{Button, City, Platform}

class StatusFormer[F[_]: Bracket[*[_], Throwable]](implicit
                                                   messageRepo: MessageRepo,
                                                   userRepo: UserRepo,
                                                   transactor: Transactor[F]) extends Handler[F, (UserId, City), Option[String]] {

  override def handle(input: (UserId, City)): EitherT[F, (String, List[List[Button]]), Option[String]] = input match {
    case (userId: UserId, city: City) =>
      nextF(
        getMessageAndUserInfo(userId).map {
          case (messageOpt, Some(userInfo)) =>
            val isVip = userInfo.lastMonthActiveDays >= 10
            s"""${if (isVip) "⭐ " else ""}${city.name}
               |${messageOpt.getOrElse("")}""".stripMargin.some
          case _ => None
        }
      )
  }

  private def getMessageAndUserInfo(userId: (Platform, Long)) = (
    for {
      message <- messageRepo.pollAsyncMessage(userId)
      userInfo <- userRepo.select(userId._1, userId._2.toInt)
    } yield (message, userInfo)
  ).transact(transactor)
}
