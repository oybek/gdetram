package io.github.oybek.gdetram.domain

import io.github.oybek.gdetram.model.{Button, Platform}

package object handler {
  type Reply = (String, List[List[Button]])
  type UserId = (Platform, Long)
}
