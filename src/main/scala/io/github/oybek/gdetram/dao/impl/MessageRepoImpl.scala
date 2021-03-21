package io.github.oybek.gdetram.dao.impl

import cats.implicits._
import doobie.ConnectionIO
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.update.Update0
import io.github.oybek.gdetram.dao.MessageRepo
import io.github.oybek.gdetram.model.Platform
import io.github.oybek.gdetram.service.UserId

object MessageRepoImpl extends MessageRepo[ConnectionIO] {
  override def pollAsyncMessage(user: UserId): ConnectionIO[Option[String]] =
    for {
      msgOpt <- getAsyncMessageFor(user).option
      _ <- msgOpt.fold(().pure[ConnectionIO]) {
        text => delAsyncMessageFor(user, text).run.void
      }
    } yield msgOpt

  override def pollSyncMessage(platform: Platform, limit: Long = 1): ConnectionIO[List[(Platform, Long, String)]] =
    for {
      msgList <- getSyncMessage(platform, limit).to[List]
      _ <- msgList.traverse { case (platform, id, text) =>
        delSyncMessageFor((platform, id), text).run
      }
    } yield msgList

  //
  def getAsyncMessageFor(user: UserId): Query0[String] =
    sql"select text from async_message where platform = ${user._1} and id = ${user._2} limit 1".query[String]

  def delAsyncMessageFor(user: UserId, text: String): Update0 =
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
