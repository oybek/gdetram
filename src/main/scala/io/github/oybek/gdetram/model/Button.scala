package io.github.oybek.gdetram.model

import io.github.oybek.vk4s.api.{Action, Keyboard, Button => VkButton}
import telegramium.bots.{InlineKeyboardButton, InlineKeyboardMarkup}

sealed trait Button
case class TextButton(text: String) extends Button
case class LinkButton(text: String, url: String) extends Button
case object GeoButton extends Button

object Button {

  implicit class VkButtonConverter(val button: Button) extends AnyVal {
    def toVk: VkButton = button match {
      case TextButton(text) =>
        VkButton(Action(`type` = "text", label = Some(text.take(40))))
      case LinkButton(text, link) =>
        VkButton(Action("open_link", Some(link), Some(text)))
      case GeoButton => VkButton(Action("location"))
    }
  }

  implicit class TgButtonConverter(val button: Button) extends AnyVal {
    def toTg: Option[InlineKeyboardButton] = button match {
      case TextButton(text) =>
        Some(InlineKeyboardButton(text = text, callbackData = Some(text)))
      case LinkButton(text, link) =>
        Some(InlineKeyboardButton(text = text, url = Some(link)))
      case _ => None
    }
  }

  implicit class KeyboardConverter(val button: List[List[Button]]) extends AnyVal {
    def toVk: Keyboard =
      Keyboard(
        oneTime = Some(false),
        inline = false,
        button.filter(_.nonEmpty).map(_.map(_.toVk))
      )

    def toTg: InlineKeyboardMarkup =
      InlineKeyboardMarkup(button.map(_.flatMap(_.toTg)))
  }

}
