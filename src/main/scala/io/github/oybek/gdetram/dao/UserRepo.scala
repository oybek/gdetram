package io.github.oybek.gdetram.dao

import io.github.oybek.gdetram.model.User
import io.github.oybek.gdetram.service.UserId

trait UserRepo[F[_]] {
  def get(userId: UserId): F[Option[User]]
  def update(user: User): F[Unit]
  def add(user: User): F[Unit]
  def getAll: F[List[User]]
}
