package io.github.oybek.gdetram.dao

import io.github.oybek.gdetram.model.Platform
import io.github.oybek.gdetram.service.UserId

trait MessageRepo[F[_]] {
  def pollAsyncMessage(user: UserId): F[Option[String]]
  def pollSyncMessage(platform: Platform, limit: Long = 1): F[List[(Platform, Long, String)]]
}

