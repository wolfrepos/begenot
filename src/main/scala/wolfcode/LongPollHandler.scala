package wolfcode

import cats.data.NonEmptyList
import cats.effect._
import cats.implicits.{catsSyntaxApplicativeByName, catsSyntaxOptionId}
import io.circe.syntax.EncoderOps
import org.typelevel.log4cats.slf4j.Slf4jLogger
import telegramium.bots.CirceImplicits.messageEncoder
import telegramium.bots._
import telegramium.bots.high.implicits._
import telegramium.bots.high.keyboards.ReplyKeyboardMarkups._
import telegramium.bots.high.{Api, Methods}
import wolfcode.model.State.{Drafting, Idle, Viewing}
import wolfcode.model.{Draft, Offer, State}

import java.time.OffsetDateTime
import scala.concurrent.duration.DurationInt

class LongPollHandler(states: Ref[IO, Map[Long, State]])(implicit api: Api[IO]) extends LongPoll[IO](api) {
  private val logger = Slf4jLogger.getLogger[IO]

  override def onMessage(msg: Message): IO[Unit] = {
    implicit val chatId: Long = msg.chat.id
    for {
      _ <- logger.info(s"got message: ${msg.asJson}")
      state <- states.get.map(_.get(msg.chat.id))
      _ <- (msg.text, state, msg.photo) match {
        case (Some("/start"), _, _) => sendInstructions(greet = true)
        case (Some(text), Some(Drafting(draft)), _) => updateDraft(draft, text = text.some)
        case (_, state, photoSizes) if photoSizes.nonEmpty =>
          logger.info(s"xxx ${(msg.text, state, NonEmptyList.fromList(msg.photo))}") >>
            updateDraft(
              draft = state match {
                case Some(Drafting(draft)) => draft
                case _ => Draft.create(chatId, OffsetDateTime.now)
              },
              photo = photoSizes.maxByOption(_.width).map(_.fileId)
            )
        case (Some(text), Some(Viewing(offers)), _) if text.startsWith("еще") => sendOffers(offers.toList)
        case (Some(text), None | Some(Idle | Viewing(_)), _) => searchOffers(text)
        case _ => sendInstructions()
      }
    } yield ()
  }

  private def sendInstructions(greet: Boolean = false)(implicit chatId: Long): IO[Unit] = {
    states.update(_.updated(chatId, State.Idle)) >>
      Methods.sendMessage(
        chatId = ChatIntId(chatId),
        text = "Привет!"
      ).exec.whenA(greet) >>
      Methods.sendMessage(
        chatId = ChatIntId(chatId),
        text = "Здесь будет инструкция как пользоваться ботом"
      ).exec.void
  }

  private def searchOffers(text: String)(implicit chatId: Long): IO[Unit] = {
    val foundOffers = NonEmptyList.of(offer, offer, offer)
    sendOffers(foundOffers.toList)
  }

  private def sendOffers(offers: List[Offer])(implicit chatId: Long): IO[Unit] =
    offers match {
      case Nil =>
        states.update(_.updated(chatId, Idle)) >>
          Methods.sendMessage(
            chatId = ChatIntId(chatId),
            text = s"Для вашего запроса не нашлось предложений"
          ).exec.void
      case offer :: Nil =>
        states.update(_.updated(chatId, Idle)) >>
          Methods.sendMessage(
            chatId = ChatIntId(chatId),
            text = s"Последнее предложение",
            replyMarkup = ReplyKeyboardRemove(true).some
          ).exec.void
      case offer :: next :: rest =>
        states.update(_.updated(chatId, Viewing(NonEmptyList.of(next, rest: _*)))) >>
          Methods.sendMessage(
            chatId = ChatIntId(chatId),
            text = s"Предложение $offer",
            replyMarkup = singleButton(
              KeyboardButton(s"еще ${rest.length + 1}"),
              resizeKeyboard = true.some
            ).some
          ).exec.void
    }

  private def updateDraft(draft: Draft,
                          text: Option[String] = None,
                          photo: Option[String] = None)(implicit chatId: Long): IO[Unit] = {
    val newDraft = draft.copy(
      description = text,
      photoIds = photo.fold(draft.photoIds)(_ :: draft.photoIds)
    )
    if (newDraft.isReady)
      states.update(_.updated(chatId, Idle)) >>
        Methods.sendMessage(
          chatId = ChatIntId(chatId),
          text = s"Ваше объявление будет опубликовано после проверки"
        ).exec.void
    else {
      states.update(_.updated(chatId, Drafting(newDraft))) >>
        Methods.sendMessage(
          chatId = ChatIntId(chatId),
          text = s"Добавьте описание товара/услуги, укажите стоимость"
            + " и другие важные детали чтобы на него откликнулось больше людей"
        ).exec.void.whenA(newDraft.description.isEmpty && newDraft.photoIds.length == 1) >>
        IO.sleep(2.seconds)
    }
  }

  private val offer = Offer(1, "", List(""), OffsetDateTime.now(), 1)
}
