package io.github.oybek.gdetram.service

trait MetricService[F[_], G[_]] {
  def userStats: F[String]
}
