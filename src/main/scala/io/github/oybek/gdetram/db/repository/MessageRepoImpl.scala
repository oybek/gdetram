package io.github.oybek.gdetram.db.repository

import cats.instances.list._
import cats.instances.option._
import cats.syntax.all._
import doobie.ConnectionIO
import doobie.implicits._
import io.github.oybek.gdetram.model.Platform

trait MessageRepo {
  def pollAsyncMessage(user: (Platform, Long)): ConnectionIO[Option[String]]
  def pollSyncMessage(platform: Platform, limit: Long = 1): ConnectionIO[List[(Platform, Long, String)]]
}

class MessageRepoImpl extends MessageRepo {
  override def pollAsyncMessage(user: (Platform, Long)): ConnectionIO[Option[String]] =
    for {
      msgOpt <- Queries.getAsyncMessageFor(user).option
      _ <- msgOpt.traverse(text => Queries.delAsyncMessageFor(user, text).run)
    } yield msgOpt

  override def pollSyncMessage(platform: Platform, limit: Long = 1): ConnectionIO[List[(Platform, Long, String)]] =
    for {
      msgList <- Queries.getSyncMessage(platform, limit).to[List]
      _ <- msgList.traverse { case (platform, id, text) =>
        Queries.delSyncMessageFor((platform, id), text).run
      }
    } yield msgList
}
