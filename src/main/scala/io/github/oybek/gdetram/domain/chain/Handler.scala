package io.github.oybek.gdetram.domain.chain

import io.github.oybek.gdetram.domain.model.{Button, GeoButton}

trait Handler[F[_], A, B] {
  val handle: A => F[Either[Reply, B]]

  protected def defaultKbrd(topButton: Button*): List[List[Button]] =
    List(topButton.toList, List(GeoButton))
}
