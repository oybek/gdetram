package io.github.oybek.gdetram.service

trait UserService[F[_]] {
  def refreshUserInfo: F[Unit]
}
