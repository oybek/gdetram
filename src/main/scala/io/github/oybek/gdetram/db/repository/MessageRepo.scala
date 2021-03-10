package io.github.oybek.gdetram.db.repository

import cats.effect.Sync
import cats.instances.list._
import cats.instances.option._
import cats.syntax.all._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.github.oybek.gdetram.model.Platform

trait MessageRepoAlg[F[_]] {
  def pollAsyncMessage(user: (Platform, Long)): F[Option[String]]
  def pollSyncMessage(platform: Platform, limit: Long = 1): F[List[(Platform, Long, String)]]
}

class MessageRepo[F[_]: Sync](tx: Transactor[F]) extends MessageRepoAlg[F] {
  override def pollAsyncMessage(user: (Platform, Long)): F[Option[String]] = (
    for {
      msgOpt <- Queries.getAsyncMessageFor(user).option
      _ <- msgOpt.traverse(text => Queries.delAsyncMessageFor(user, text).run)
    } yield msgOpt
  ).transact(tx)

  override def pollSyncMessage(platform: Platform, limit: Long = 1): F[List[(Platform, Long, String)]] = (
    for {
      msgList <- Queries.getSyncMessage(platform, limit).to[List]
      _ <- msgList.traverse { case (platform, id, text) =>
        Queries.delSyncMessageFor((platform, id), text).run
      }
    } yield msgList
  ).transact(tx)
}
