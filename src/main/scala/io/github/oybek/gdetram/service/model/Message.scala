package io.github.oybek.gdetram.service.model

sealed trait Message

object Message {
  case class Text(text: String) extends Message
  case class Geo(latitude: Float, longitude: Float) extends Message
}
