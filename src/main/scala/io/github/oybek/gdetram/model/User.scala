package io.github.oybek.gdetram.model

import io.github.oybek.gdetram.service.UserId

case class User(userId: UserId,
                cityId: Int,
                lastStopId: Option[Int] = None,
                lastMonthActiveDays: Int = 0)
