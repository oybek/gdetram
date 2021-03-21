package io.github.oybek.gdetram.dao.impl

import doobie.ConnectionIO
import doobie.implicits._
import doobie.util.query.Query0
import io.github.oybek.gdetram.dao.CityRepo
import io.github.oybek.gdetram.model.City
import io.github.oybek.gdetram.service.model.Message.Geo

object CityRepoImpl extends CityRepo[ConnectionIO] {
  def findByName(name: String): ConnectionIO[(City, Int)] =
    selectMostMatched(name).unique

  def get(cityId: Int): ConnectionIO[City] =
    select(cityId).unique

  def getAll: ConnectionIO[List[City]] =
    selectAll.to[List]

  def getNearest(geo: Geo): ConnectionIO[City] =
    selectNearest(geo).unique

  // Queries
  def select(cityId: Int): Query0[City] =
    sql"select id, name, latitude, longitude from city where id = $cityId".query[City]

  val selectAll: Query0[City] =
    sql"select id, name, latitude, longitude from city".query[City]

  def selectMostMatched(name: String): Query0[(City, Int)] =
    sql"select *, levenshtein(lower(name), lower($name)) from city order by levenshtein limit 1".query[(City, Int)]

  def selectNearest(geo: Geo): Query0[City] =
    sql"""select id, name, latitude, longitude from city
          order by (latitude - ${geo.latitude})^2 + (longitude - ${geo.longitude})^2 limit 1""".query[City]
}
