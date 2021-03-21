package io.github.oybek.gdetram

import io.github.oybek.gdetram.model.Platform
import io.github.oybek.gdetram.service.model.Button

package object service {
  type Reply = (String, List[List[Button]])
  type UserId = (Platform, Long)
}
