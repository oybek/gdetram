package io.github.oybek.gdetram.dao.impl

import cats.Applicative.ops.toAllApplicativeOps
import doobie.implicits._
import doobie.ConnectionIO
import doobie.util.query.Query0
import doobie.util.update.Update0
import io.github.oybek.gdetram.dao.UserRepo
import io.github.oybek.gdetram.model._
import io.github.oybek.gdetram.service.UserId

object UserRepoImpl extends UserRepo[ConnectionIO] {
  def get(userId: UserId): ConnectionIO[Option[User]] =
    select(userId).option

  def getAll: ConnectionIO[List[User]] =
    selectAll.to[List]

  def update(user: User): ConnectionIO[Unit] =
    updateq(user).run.void

  def add(user: User): ConnectionIO[Unit] =
    insert(user).run.void

  //
  def select(userId: UserId): Query0[User] =
    sql"""select platform, id, city_id, last_stop_id, last_month_active_days
          from user_info
          where platform = ${userId._1} and id = ${userId._2}""".query[User]

  val selectAll: Query0[User] =
    sql"select platform, id, city_id, last_stop_id, last_month_active_days from user_info".query[User]

  def updateq(user: User): Update0 = {
    import user._
    sql"""update user_info
          set city_id = $cityId,
              last_stop_id = $lastStopId,
              last_month_active_days = $lastMonthActiveDays
          where platform = ${userId._1} and id = ${userId._2}""".update
  }

  def insert(user: User): Update0 = {
    import user._
    sql"""insert into user_info (platform, id, city_id, last_stop_id, last_month_active_days)
          values (${userId._1}, ${userId._2}, $cityId, $lastStopId, $lastMonthActiveDays)""".update
  }
}
