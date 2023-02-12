package wolfcode

import cats.effect._
import telegramium.bots._
import telegramium.bots.high._
import telegramium.bots.high.implicits._

class LongPollGate(implicit api: Api[IO]) extends LongPollBot[IO](api) {
  override def onMessage(msg: Message): IO[Unit] =
    Methods.sendMessage(chatId = ChatIntId(msg.chat.id), text = "Hello, world!").exec.void
}
