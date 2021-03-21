package io.github.oybek.gdetram.service

import io.github.oybek.gdetram.model.User
import io.github.oybek.gdetram.service.model.Message

trait AuthorizedHandler[F[_], B] {
  def handle(user: User, message: Message): F[Either[Reply, B]]
}
