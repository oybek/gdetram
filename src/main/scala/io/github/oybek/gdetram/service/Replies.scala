package io.github.oybek.gdetram.service

import io.github.oybek.gdetram.model.Stop
import io.github.oybek.gdetram.service.model.Button.{GeoButton, TextButton}

object Replies {

  def cityChosen(cityName: String, cityNames: List[String]): Reply = (
    s"🏙️ Выбран город $cityName\n" +
      "\n" +
      "Для смены города напиши 'город', а потом название города, например:\n" +
      "город Казань\n" +
      "Или просто отправь геопозицию\n" +
      "\n" +
      "Доступные города:\n" +
      s"${cityNames.sorted.mkString(", ")}",
    List(List(GeoButton))
  )

  def cityAsk(withGreeting: Boolean = false): Reply = (
    (if (withGreeting) "Привет!\n" else "") +
      "Подскажи в каком ты городе?" + "\n" +
      "Или просто отправь геопозицию",
    List(List(GeoButton))
  )

  def cantFindCity(cityNames: List[String]): Reply = (
    "Не нашел такой город 😟\n" +
      "Попробуй еще раз\n" +
      "Или просто отправь геопозицию\n" +
      "\n" +
      "Доступные города: " + s"${cityNames.sorted.mkString(", ")}",
    List(List(GeoButton))
  )

  def cantFindStop: Reply = (
    "Не знаю такую остановку 😟\n" +
      "\n" +
      "Отправьте геопозицию - я подскажу названия ближайших остановок\n",
    List(List(GeoButton))
  )

  val noCityBase: Reply = (
    "Не загружена база остановок",
    List(List(GeoButton))
  )

  def nearestStops(stops: List[Stop], cityName: String): Reply = (
    "Ближайшие остановки:\n" +
    stops.map("\uD83D\uDE8F " + _.name).mkString("\n") + "\n" +
    s"$cityName",
    stops.map(stop => List(TextButton(stop.name)))
  )
}
