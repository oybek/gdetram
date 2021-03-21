package io.github.oybek.gdetram.service.impl

import cats.effect.Sync
import cats.syntax.all._
import cats.{Monad, ~>}
import io.github.oybek.gdetram.dao.{CityRepo, UserRepo}
import io.github.oybek.gdetram.model.Platform.{Tg, Vk}
import io.github.oybek.gdetram.model.{City, User}
import io.github.oybek.gdetram.service.MetricService

import java.sql.Timestamp
import java.time.LocalDateTime

class MetricServiceImpl[F[_]: Sync, G[_]: Monad](implicit
                                                 userRepo: UserRepo[G],
                                                 cityRepo: CityRepo[G],
                                                 transaction: G ~> F) extends MetricService[F, G] {

  def userStats: F[String] =
    transaction {
      for {
        usersInfo <- userRepo.getAll
        cities <- cityRepo.getAll
        caption = usersInfo
          .groupBy(_.userId._1)
          .view.mapValues(cityReport(_, cities))
          .map {
            case (Vk, info) => s"ВК\n$info"
            case (Tg, info) => s"Телега\n$info"
          }
          .mkString("\n\n")
      } yield caption
    }

  implicit def ordered: Ordering[Timestamp] = (x: Timestamp, y: Timestamp) => x compareTo y

  private def cityReport(usersInfo: List[User], cities: List[City]): String =
    usersInfo
      .groupBy(_.cityId)
      .map {
        case (cityId, usersInfo) =>
          val (active, passive) = usersInfo.foldLeft((0, 0)) {
            case ((a, p), user) =>
              if (user.lastMonthActiveDays > 0) (a + 1, p)
              else (a, p + 1)
          }
          val cityName = cities.find(_.id == cityId).fold("Хуйгород")(_.name)
          s"#$cityName - Активных: $active, Пассивных: $passive"
      }.mkString("\n")

  implicit class IntOps(n: Int) {
    def weeksAgo: Timestamp =
      Timestamp.valueOf(LocalDateTime.now().minusWeeks(n))
  }
}
