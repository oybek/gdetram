package io.github.oybek.gdetram.service.impl

import cats.Applicative.ops.toAllApplicativeOps
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import doobie.util.update.Update0
import io.github.oybek.gdetram.service.UserService

object UserServiceImpl extends UserService[ConnectionIO] {
  override def refreshUserInfo: ConnectionIO[Unit] =
    refreshUserInfoQ.run.void

  val refreshUserInfoQ: Update0 =
    sql"""update user_info
          set last_month_active_days = q.last_month_active_days
          from (
            select platform, user_id::bigint, count(distinct(date(time))) as last_month_active_days
            from journal
            where time >= now() - interval '30 days'
            group by (user_id, platform)
          ) as q
          where user_info.platform = q.platform and user_info.id = q.user_id""".update
}
