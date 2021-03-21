package io.github.oybek.gdetram.service

import io.github.oybek.gdetram.service.model.Message

trait Handler[F[_], B] {
  def handle(userId: UserId, message: Message): F[Either[Reply, B]]
}
