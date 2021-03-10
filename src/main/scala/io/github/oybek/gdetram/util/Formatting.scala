package io.github.oybek.gdetram.util

import io.github.oybek.gdetram.model.Stop
import io.github.oybek.plato.model.TransportT.{Bus, Tram, Troll}
import io.github.oybek.plato.model.{Arrival, TransportT}

import scala.concurrent.duration._

object Formatting {

  val emojis = List(Bus, Tram, Troll).map(TransportT.emoji)

  val rightArrow = "➡️"

  def toChatText(stop: Stop, dir: String, reaches: List[Arrival]): String = {
    s"""
       |${stop.name} $rightArrow $dir
       |${reachesToText(reaches)}
       |""".stripMargin
  }

  private def reachesToText(reaches: List[Arrival]): String = {
    if (reaches.isEmpty)
      "Ничего не едет"
    else {
      reachToChatText(reaches)
    }
  }

  private def reachToChatText(arrivals: List[Arrival]): String =
    arrivals
      .groupBy(arrival => arrival.transportT -> arrival.time)
      .toList
      .sortBy { case ((_, time), _) => time }
      .map {
        case ((ttype, time), arrivals) =>
          List(
            TransportT.emoji(ttype),
            arrivals.map(_.route).mkString(", "),
            if (time < 1.minutes)
              s"подъезжа${if (arrivals.length > 1) "ют" else "ет"}"
            else
              s"- ${time.length} мин."
          ).mkString(" ")
      }
      .mkString("\n")
}
