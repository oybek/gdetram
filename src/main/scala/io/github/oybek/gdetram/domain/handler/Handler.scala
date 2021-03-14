package io.github.oybek.gdetram.domain.handler

import cats.Applicative
import cats.data.EitherT
import io.github.oybek.gdetram.model.{Button, GeoButton}

trait Handler[F[_], A, B] {
  def handle(a: A): EitherT[F, Reply, B]

  def reply(reply: Reply)(implicit applicative: Applicative[F]): EitherT[F, Reply, B] =
    EitherT.leftT[F, B](reply)

  def next(b: B)(implicit applicative: Applicative[F]): EitherT[F, Reply, B] =
    EitherT.rightT[F, Reply](b)

  def replyF(reply: F[Reply])(implicit applicative: Applicative[F]): EitherT[F, Reply, B] =
    EitherT.left[B](reply)

  def nextF(b: F[B])(implicit applicative: Applicative[F]): EitherT[F, Reply, B] =
    EitherT.right[Reply](b)

  protected def defaultKbrd: List[List[Button]] =
    List(List(GeoButton))

  protected def defaultKbrd(topButton: Button*): List[List[Button]] =
    List(topButton.toList, List(GeoButton))
}
