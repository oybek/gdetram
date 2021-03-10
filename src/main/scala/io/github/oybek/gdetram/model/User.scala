package io.github.oybek.gdetram.model

case class User(platform: Platform,
                id: Int,
                cityId: Int,
                lastStopId: Option[Int],
                lastMonthActiveDays: Int)
