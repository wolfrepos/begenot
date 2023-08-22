package wolfcode

import telegramium.bots.{Contact, Message, WebAppData}

object CustomExtractors {

  object PhotoId {
    def unapply(message: Message): Option[String] =
      message.photo.maxByOption(_.width).map(_.fileId)
  }

  object Text {
    def unapply(message: Message): Option[String] =
      message.text
  }

  object Contactt {
    def unapply(message: Message): Option[Contact] =
      message.contact
  }

  object WebApp {
    def unapply(message: Message): Option[WebAppData] =
      message.webAppData
  }
}
