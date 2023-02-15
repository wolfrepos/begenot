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
      sendText("Привет!") >>
      sendText("Здесь будет инструкция как пользоваться ботом")
  }

  private def searchOffers(text: String)(implicit chatId: Long): IO[Unit] = {
    val foundOffers = NonEmptyList.of(offer, offer, offer)
    sendOffers(foundOffers.toList)
  }

  private def sendOffers(offers: List[Offer])(implicit chatId: Long): IO[Unit] =
    offers match {
      case Nil =>
        states.update(_.updated(chatId, Idle)) >>
          sendText("По Вашему запросу не нашлось предложений(")
      case offer :: Nil =>
        states.update(_.updated(chatId, Idle)) >>
          sendOffer(offer)
      case offer :: next :: rest =>
        states.update(_.updated(chatId, Viewing(NonEmptyList.of(next, rest: _*)))) >>
          sendOffer(offer, rest.length + 1)
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
        sendText("Ваше объявление будет опубликовано после проверки")
    else
      states.update(_.updated(chatId, Drafting(newDraft))) >>
        sendText(
          """
            |Добавьте описание товара/услуги
            |укажите стоимость и другие важные детали
            |чтобы на него откликнулось больше людей
            |""".stripMargin
        ).whenA(newDraft.description.isEmpty && newDraft.photoIds.length == 1)
  }

  private def sendText(text: String)(implicit chatId: Long): IO[Unit] =
    Methods.sendMessage(
      chatId = ChatIntId(chatId),
      text = text
    ).exec.void

  private def sendOffer(offer: Offer, left: Int = 0)(implicit chatId: Long): IO[Unit] =
    Methods.sendMediaGroup(
      chatId = ChatIntId(chatId),
      media = offer.photoIds.map(InputMediaPhoto(_))
    ).exec.void >>
      Methods.sendMessage(
        chatId = ChatIntId(chatId),
        text = offer.description,
        replyMarkup = singleButton(
          KeyboardButton(s"еще $left"),
          resizeKeyboard = true.some
        ).some
      ).exec.void

  private val offer = Offer(
    id = 1,
    description =
      """
        |Продам процессор intel core i3 12100f
        |Процессор со встроенной графикой
        |""".stripMargin,
    photoIds = List("AgACAgIAAxkBAAIJ42PsqrAgEFe5rdyW1jNXtHrbYtSfAAKpwzEbCDlgS_4PrjvMgqsHAQADAgADeQADLgQ"),
    publishTime = OffsetDateTime.now(),
    ownerId = 1
  )
}
