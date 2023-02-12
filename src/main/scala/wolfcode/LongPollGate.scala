package wolfcode

import cats.data.NonEmptyList
import cats.effect._
import cats.implicits.catsSyntaxOptionId
import io.circe.syntax.EncoderOps
import org.typelevel.log4cats.slf4j.Slf4jLogger
import telegramium.bots.CirceImplicits.messageEncoder
import telegramium.bots._
import telegramium.bots.high._
import telegramium.bots.high.implicits._
import telegramium.bots.high.keyboards.ReplyKeyboardMarkups.singleButton
import wolfcode.model.{Offer, State}
import wolfcode.model.State.{Idle, Viewing}

import java.time.OffsetDateTime

class LongPollGate(state: Ref[IO, Map[Long, State]])(implicit api: Api[IO]) extends LongPollBot[IO](api) {
  private val logger = Slf4jLogger.getLogger[IO]

  override def onMessage(msg: Message): IO[Unit] = {
    implicit val chatId: Long = msg.chat.id

    for {
      _ <- logger.info(s"got message: ${msg.asJson}")
      stateOpt <- state.get.map(_.get(msg.chat.id))
      _ <- (stateOpt, msg.text, msg.photo) match {
        case (_, Some("/start"), _) => onStart
        case (Some(Idle | Viewing(_)), Some(text), _) => onSearch(text)
        case (Some(Viewing(offers)), Some(text), _) if text.startsWith("еще") => onMore(offers)
      }
    } yield ()
  }

  private def onStart(implicit chatId: Long): IO[Unit] =
    state.update(_.updated(chatId, State.Idle)) >>
      Methods.sendMessage(
        chatId = ChatIntId(chatId),
        text = "Здесь будет инструкция как пользоваться ботом"
      ).exec.void

  private def onSearch(text: String)(implicit chatId: Long): IO[Unit] =
    state.update(_.updated(chatId, Viewing(NonEmptyList.of(offer)))) >>
      Methods.sendMessage(
        chatId = ChatIntId(chatId),
        text = s"Здесь будет результат поиска по тексту ${text}"
      ).exec.void

  private def onMore(offers: NonEmptyList[Offer])(implicit chatId: Long): IO[Unit] =
    NonEmptyList.fromList(offers.tail) match {
      case None =>
        state.update(_.updated(chatId, Idle)) >>
          Methods.sendMessage(
            chatId = ChatIntId(chatId),
            text = s"Это было последнее предложение"
          ).exec.void
      case Some(tail) =>
        state.update(_.updated(chatId, Viewing(tail))) >>
          Methods.sendMessage(
            chatId = ChatIntId(chatId),
            text = s"Есть еще предложения"
          ).exec.void
    }

  private val offer = Offer(1, "", List(""), OffsetDateTime.now(), 1)
}
