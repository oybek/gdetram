package io.github.oybek.gdetram.domain.chain

import cats.effect.Sync
import cats.implicits._
import io.github.oybek.gdetram.db.repository._
import io.github.oybek.gdetram.domain.Phrases._
import io.github.oybek.gdetram.domain.chain.model._
import io.github.oybek.gdetram.domain.model._

class CityHandler[F[_]: Sync](implicit
                              cityRepo: CityRepoAlg[F],
                              userRepo: UserRepoAlg[F],
                              stopRepo: StopRepoAlg[F]) extends Handler[F, (UserId, Input), (City, Text)] {
  val handle: ((UserId, Input)) => F[Either[Reply, (City, Text)]] = {
    case ((platform, userId), Geo(latitude, longitude)) =>
      for {
        nearestStops <- stopRepo.selectNearest(latitude, longitude)
        _ <- nearestStops.headOption.traverse(
          stop => userRepo.upsert(User(platform, userId.toInt, stop.city))
        ).void
        res = (
          s"""
             |Город ${nearestStops.headOption.map(_.city.name).getOrElse("Не определен")}
             |
             |3 ближайшие остановки:
             |${nearestStops.map(x => "- " + x.name).mkString("\n")}
             |""".stripMargin,
          nearestStops.map(stop => List(TextButton(stop.name)))
        ).asLeft
      } yield res

    case ((platform, userId), Text(text)) =>
      userRepo.selectUser(platform, userId.toInt).flatMap {
        case Some(user) if !text.trim.toLowerCase.startsWith("город") =>
          (user.city, Text(text))
            .asRight[Reply]
            .pure[F]

        case _ =>
          val cityName = if (text.trim.toLowerCase.startsWith("город")) text.trim.drop(5).trim else text
          findCity(cityName).flatMap {
            case Some(city) => gotCity((platform, userId), city)
            case None => (cantFindCity, defaultKbrd())
              .asLeft[(City, Text)]
              .pure[F]
          }
      }
  }

  private def gotCity(user: (Platform, Long), city: City): F[Either[Reply, (City, Text)]] = {
    for {
      _ <- userRepo.upsert(User(user._1, user._2.toInt, city))
      cityNames <- cityRepo.selectAllCitiesNames
      text = cityChosen(city.name, cityNames)
      kbrd = defaultKbrd(TextButton("город " + city.name))
    } yield (text, kbrd).asLeft
  }

  private def findCity(text: String): F[Option[City]] =
    for {
      cityAndMistakeNum <- cityRepo.selectCity(text.trim)
      (city, mistakeNum) = cityAndMistakeNum
      res = if (mistakeNum > 4) { None } else { city.some }
    } yield res
}
