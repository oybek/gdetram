package io.github.oybek.gdetram.service

import java.io.File
import java.sql.Timestamp
import cats.syntax.all._
import cats.effect.Sync
import io.github.oybek.gdetram.db.repository.{CityRepo, CityRepoAlg, UserRepo}
import io.github.oybek.gdetram.model.Platform._
import io.github.oybek.gdetram.model.{City, User}

import java.time.LocalDateTime

trait MetricServiceAlg[F[_]] {
  def userStats: F[String]
}

class MetricService[F[_]: Sync](implicit userRepo: UserRepo[F],
                                         cityRepo: CityRepoAlg[F]) extends MetricServiceAlg[F] {

  def userStats: F[String] =
    for {
      usersInfo <- userRepo.selectAll
      cities <- cityRepo.selectAll
      caption = usersInfo
        .groupBy(_.platform)
        .view.mapValues(cityReport(_, cities))
        .map {
          case (Vk, info) => s"ВК\n$info"
          case (Tg, info) => s"Телега\n$info"
        }
        .mkString("\n\n")
    } yield caption

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
