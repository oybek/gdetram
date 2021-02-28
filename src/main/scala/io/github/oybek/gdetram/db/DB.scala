package io.github.oybek.gdetram.db

import cats.effect.{Async, Blocker, ContextShift, Resource, Sync}
import doobie.hikari.HikariTransactor
import io.github.oybek.gdetram.config.DatabaseConfig
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

object DB {
  def transactor[F[_]: Sync: Async: ContextShift](
    config: DatabaseConfig,
    ec: ExecutionContext,
    blocker: Blocker
  ): Resource[F, HikariTransactor[F]] =
    HikariTransactor.newHikariTransactor[F](
      config.driver,
      config.url,
      config.user,
      config.password,
      ec,
      blocker
    )

  def initialize[F[_]: Sync](transactor: HikariTransactor[F]): F[Unit] = transactor.configure(
    dataSource =>
      Sync[F].delay {
        Flyway
          .configure()
          .dataSource(dataSource)
          .load()
          .migrate()
        ()
      }
  )
}
