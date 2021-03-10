package io.github.oybek.gdetram.db.repository

import doobie.util.query.Query0
import doobie.util.update.Update0
import doobie.implicits._
import doobie.implicits.javasql._
import io.github.oybek.gdetram.model.{City, Platform, PsMessage, Record, Stop, User}

object Queries {

  def selectCityQuery(cityId: Int): Query0[City] =
    sql"select id, name, latitude, longitude from city where id = $cityId".query[City]

  def selectAllCitites: Query0[City] =
    sql"select id, name, latitude, longitude from city".query[City]

  def selectMostMatchedCity(query: String): Query0[(City, Int)] =
    sql"""
         |SELECT *, levenshtein(LOWER(name), LOWER($query)) from city order by levenshtein limit 1
         |""".stripMargin.query[(City, Int)]

  def upsertUserQuery(user: User): Update0 = {
    import user._
    sql"""
         |insert into user_info (platform, id, city_id, last_stop_id, last_month_active_days)
         |values ($platform, $id, $cityId, $lastStopId, $lastMonthActiveDays)
         |on conflict (platform, id)
         |do update set city_id = $cityId, last_stop_id = $lastStopId, last_month_active_days = $lastMonthActiveDays
         |""".stripMargin.update
  }

  def selectUserQuery(platform: Platform, userId: Int): Query0[User] =
    sql"""
         |select platform, id, city_id, last_stop_id, last_month_active_days
         |from user_info
         |where platform = $platform and id = $userId
         |""".stripMargin.query[User]

  def selectAllUsersQuery: Query0[User] =
    sql"select platform, id, city_id, last_stop_id, last_month_active_days from user_info".query[User]

  def selectMostMatched(query: String, cityId: Int): Query0[(Stop, Int)] =
    sql"""
         |select stop.id,
         |       stop.name,
         |       stop.latitude,
         |       stop.longitude,
         |       stop.url,
         |       city.id,
         |       city.name,
         |       city.latitude,
         |       city.longitude,
         |       levenshtein(LOWER(stop.name), LOWER($query))
         |  from stop
         |  left join city on stop.city_id = city.id
         |  where city_id = $cityId
         |  order by levenshtein
         |  limit 1
         |""".stripMargin.query[(Stop, Int)]

  def selectNearest(latitude: Float,
                    longitude: Float): Query0[Stop] =
    sql"""
         |select stop.id,
         |       stop.name,
         |       stop.latitude,
         |       stop.longitude,
         |       stop.url,
         |       city.id,
         |       city.name,
         |       city.latitude,
         |       city.longitude
         |  from stop
         |  left join city on stop.city_id = city.id
         |  order by (stop.latitude - $latitude)^2 + (stop.longitude - $longitude)^2 limit 3;
         |""".stripMargin.query[Stop]

  def insertRecordSql(record: Record): Update0 = {
    val Record(stopId, time, userId, text, platform) = record
    sql"insert into journal values ($stopId, $time, $userId, $text, $platform)".update
  }

  def getAsyncMessageFor(user: (Platform, Long)): Query0[String] =
    sql"select text from async_message where platform = ${user._1} and id = ${user._2} limit 1".query[String]

  def delAsyncMessageFor(user: (Platform, Long), text: String): Update0 =
    sql"delete from async_message where platform = ${user._1} and id = ${user._2} and text = $text".update

  def getSyncMessage(platform: Platform, limit: Long): Query0[(Platform, Long, String)] =
    sql"""
         |select platform,
         |       id,
         |       text
         |  from sync_message
         |  where platform = $platform
         |  limit $limit
         |""".stripMargin.query[(Platform, Long, String)]

  def delSyncMessageFor(user: (Platform, Long), text: String): Update0 =
    sql"delete from sync_message where platform = ${user._1} and id = ${user._2} and text = $text".update

}
