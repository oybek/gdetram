package io.github.oybek.gdetram.domain.handler

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import doobie.implicits._
import doobie.Transactor
import io.github.oybek.gdetram.db.repository._
import io.github.oybek.gdetram.domain.{Geo, Input, Text}
import io.github.oybek.gdetram.domain.Phrases._
import io.github.oybek.gdetram.model._

class CityHandler[F[_]: Sync](implicit
                              cityRepo: CityRepoAlg[F],
                              userRepo: UserRepo,
                              stopRepo: StopRepoAlg[F],
                              transactor: Transactor[F]) extends Handler[F, (UserId, Input), (City, Text)] {

  override def handle(input: (UserId, Input)): EitherT[F, Reply, (City, Text)] = input match {
    case ((platform, userId), Geo(latitude, longitude)) =>
      replyF(nearestCityInfo(platform, userId.toInt, latitude, longitude))

    case ((platform, userId), Text(text)) =>
      EitherT.right(
        userRepo
          .select(platform, userId.toInt)
          .transact(transactor)
      ).flatMap {
        case Some(user) if !text.trim.toLowerCase.startsWith("город") =>
          nextF(cityRepo.select(user.cityId).map((_, Text(text))))
        case _ =>
          val cityName = if (text.trim.toLowerCase.startsWith("город")) text.trim.drop(5).trim else text
          replyF(findCity(cityName).flatMap {
            case Some(city) => gotCity((platform, userId), city)
            case None => (cantFindCity, defaultKbrd()).pure[F]
          })
      }
  }

  private def nearestCityInfo(platform: Platform,
                              userId: Int,
                              latitude: Float,
                              longitude: Float): F[Reply] =
    for {
      nearestStops <- stopRepo.selectNearest(latitude, longitude)
      _ <- nearestStops.headOption.traverse(
        stop => userRepo.upsert(
          User(platform, userId.toInt, stop.city.id, None, 0)
        ).transact(transactor)
      ).void
    } yield (
      s"""
         |Город ${nearestStops.headOption.map(_.city.name).getOrElse("Не определен")}
         |
         |3 ближайшие остановки:
         |${nearestStops.map(x => "- " + x.name).mkString("\n")}
         |""".stripMargin,
      nearestStops.map(stop => List(TextButton(stop.name)))
    )

  private def gotCity(user: (Platform, Long), city: City): F[Reply] =
    for {
      _ <- userRepo.upsert(User(user._1, user._2.toInt, city.id, None, 0)).transact(transactor)
      cities <- cityRepo.selectAll
      text = cityChosen(city.name, cities.map(_.name))
      kbrd = defaultKbrd(TextButton("город " + city.name))
    } yield (text, kbrd)

  private def findCity(text: String): F[Option[City]] =
    for {
      cityAndMistakeNum <- cityRepo.find(text.trim)
      (city, mistakeNum) = cityAndMistakeNum
      res = if (mistakeNum > 4) { None } else { city.some }
    } yield res
}
