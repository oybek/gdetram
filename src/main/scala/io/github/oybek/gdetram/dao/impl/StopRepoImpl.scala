package io.github.oybek.gdetram.dao.impl

import doobie.implicits._
import doobie.ConnectionIO
import doobie.util.query.Query0
import io.github.oybek.gdetram.dao.StopRepo
import io.github.oybek.gdetram.model.Stop
import io.github.oybek.gdetram.service.model.Message.Geo

object StopRepoImpl extends StopRepo[ConnectionIO] {

  def findByName(query: String, cityId: Int): ConnectionIO[Option[(Stop, Int)]] =
    selectMostMatched(query, cityId).option

  def getNearest(geo: Geo): ConnectionIO[List[Stop]] =
    selectNearest(geo).to[List]

  //
  def selectMostMatched(query: String, cityId: Int): Query0[(Stop, Int)] =
    sql"""select id, name, latitude, longitude, url, city_id, levenshtein(LOWER(stop.name), LOWER($query))
          from stop where stop.city_id = $cityId order by levenshtein limit 1""".query[(Stop, Int)]

  def selectNearest(geo: Geo): Query0[Stop] =
    sql"""select id, name, latitude, longitude, url, city_id from stop
          order by (stop.latitude - ${geo.latitude})^2 + (stop.longitude - ${geo.longitude})^2
          limit 3""".query[Stop]

}
