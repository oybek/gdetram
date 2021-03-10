package io.github.oybek.gdetram.db.repository

import cats.syntax.all._
import cats.effect.Sync
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import doobie.implicits._
import doobie.util.query.Query0
import io.github.oybek.gdetram.model._

trait UserRepo[F[_]] {
  def upsert(user: User): F[Int]
  def select(platform: Platform, userId: Int): F[Option[User]]
  def selectAll: F[List[User]]
}

class UserRepoImpl[F[_]: Sync](transactor: Transactor[F]) extends UserRepo[F] {
  def upsert(user: User): F[Int] =
    Queries
      .upsertUserQuery(user)
      .run
      .transact(transactor)

  def select(platform: Platform, userId: Int): F[Option[User]] =
    Queries.selectUserQuery(platform, userId).option.transact(transactor)

  def selectAll: F[List[User]] =
    Queries
      .selectAllUsersQuery
      .to[List]
      .transact(transactor)
}