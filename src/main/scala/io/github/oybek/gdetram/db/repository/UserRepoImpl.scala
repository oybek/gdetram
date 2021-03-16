package io.github.oybek.gdetram.db.repository

import doobie.ConnectionIO
import io.github.oybek.gdetram.model._

trait UserRepo {
  def upsert(user: User): ConnectionIO[Int]
  def select(platform: Platform, userId: Int): ConnectionIO[Option[User]]
  def selectAll: ConnectionIO[List[User]]
}

class UserRepoImpl extends UserRepo {
  def upsert(user: User): ConnectionIO[Int] =
    Queries
      .upsertUserQuery(user)
      .run

  def select(platform: Platform, userId: Int): ConnectionIO[Option[User]] =
    Queries
      .selectUserQuery(platform, userId)
      .option

  def selectAll: ConnectionIO[List[User]] =
    Queries
      .selectAllUsersQuery
      .to[List]
}