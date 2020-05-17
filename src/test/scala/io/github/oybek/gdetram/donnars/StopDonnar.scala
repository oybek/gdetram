package io.github.oybek.gdetram.donnars

import io.github.oybek.gdetram.model.{City, Stop}

trait StopDonnar {
  val stop =
    Stop(
      id = 1,
      url = "test_url",
      name = "test_names",
      latitude = 0.0f,
      longitude = 0.0f,
      city = City(1, "city", 0.0f, 0.0f)
    )
}