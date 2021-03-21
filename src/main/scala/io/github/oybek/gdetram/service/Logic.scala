package io.github.oybek.gdetram.service

import io.github.oybek.gdetram.service.model.Message

trait Logic[F[_]] {
  def handle(userId: UserId, message: Message): F[Reply]
}
