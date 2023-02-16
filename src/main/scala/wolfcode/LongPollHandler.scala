package wolfcode

import cats.data.NonEmptyList
import cats.effect._
import cats.implicits.{catsSyntaxApplicativeByName, catsSyntaxOptionId}
import io.circe.syntax.EncoderOps
import org.typelevel.log4cats.slf4j.Slf4jLogger
import telegramium.bots.CirceImplicits.messageEncoder
import telegramium.bots._
import telegramium.bots.high.implicits._
import telegramium.bots.high.keyboards.InlineKeyboardMarkups
import telegramium.bots.high.keyboards.ReplyKeyboardMarkups._
import telegramium.bots.high.{Api, Methods}
import wolfcode.model.State.{Drafting, Idle, Viewing}
import wolfcode.model.{Draft, Offer, State}
import wolfcode.repository.{OfferRepository, PendingOfferRepository}

import java.time.OffsetDateTime
import scala.concurrent.duration.DurationInt
import scala.util.Random

class LongPollHandler(states: Ref[IO, Map[Long, State]],
                      pendingOfferRepository: PendingOfferRepository,
                      offerRepository: OfferRepository)(implicit api: Api[IO]) extends LongPoll[IO](api) {
  private val logger = Slf4jLogger.getLogger[IO]
  private val admins = List(108683062L)

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
            photo = photoSizes.maxByOption(_.width).map(_.fileId),
            text = msg.caption
          )
        case (Some(text), Some(Viewing(offers)), _) if text.startsWith("еще") => sendOffers(offers.toList)
        case (Some(text), None | Some(Idle | Viewing(_)), _) => searchOffers(text)
        case _ => sendInstructions()
      }
    } yield ()
  }

  override def onCallbackQuery(query: CallbackQuery): IO[Unit] =
    if (!admins.contains(query.from.id)) IO.unit else {
      query.data match {
        case Some(x) if x.startsWith("publish") => publishOffer(x.filter(_.isDigit).toInt)
        case Some(x) if x.startsWith("decline") => declineOffer(x.filter(_.isDigit).toInt)
        case _ => IO.unit
      }
    }

  def job: IO[Unit] = {
    val admin = Random.shuffle(admins).head
    pendingOfferRepository
      .getOldest.attempt
      .flatMap {
        case Right(Some(offer)) =>
          sendOffer(offer)(admin) >>
            Methods.sendMessage(
              chatId = ChatIntId(admin),
              text = "Ваше решение?",
              replyMarkup = InlineKeyboardMarkups.singleRow(
                List(
                  InlineKeyboardButton(Emoji.check, callbackData = s"publish${offer.id}".some),
                  InlineKeyboardButton(Emoji.cross, callbackData = s"decline${offer.id}".some),
                )
              ).some
            ).exec.void
        case Right(None) => IO.unit
        case Left(th) => sendText(th.toString)(admin)
      }
      .attempt >> IO.sleep(1.minute) >> job
  }

  private def publishOffer(offerId: Int) =
    pendingOfferRepository.publish(offerId).flatMap {
      case Some(o) =>
        sendText(
          s"""
             |Ваше объявление:
             |
             |${o.description}
             |
             |${Emoji.check} Опубликовано
             |""".stripMargin
        )(o.ownerId)
      case None => IO.unit
    }

  private def declineOffer(offerId: Int) =
    pendingOfferRepository.decline(offerId).flatMap {
      case Some(o) =>
        sendText(
          s"""
             |Ваше объявление:
             |
             |${o.description}
             |
             |${Emoji.cross} Отклонено
             |""".stripMargin
        )(o.ownerId)
      case None => IO.unit
    }

  private def sendInstructions(greet: Boolean = false)(implicit chatId: Long): IO[Unit] = {
    states.update(_.updated(chatId, State.Idle)) >>
      sendText("Привет!") >>
      sendText("Здесь будет инструкция как пользоваться ботом")
  }

  private def searchOffers(text: String)(implicit chatId: Long): IO[Unit] =
    NonEmptyList.fromList {
      text
        .filter(c => c.isLetterOrDigit || c.isWhitespace)
        .split("\\s+")
        .map(_.trim)
        .filterNot(_.isEmpty)
        .toList
    } match {
      case Some(words) =>
        offerRepository
          .ftSearch(words)
          .flatMap(sendOffers)
      case None =>
        IO.unit
    }

  private def sendOffers(offers: List[Offer])(implicit chatId: Long): IO[Unit] =
    offers match {
      case Nil =>
        states.update(_.updated(chatId, Idle)) >>
          sendText(s"${Emoji.penciveFace} по Вашему запросу не нашлось предложений")
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
    newDraft.toOffer match {
      case Some(offer) =>
        states.update(_.updated(chatId, Idle)) >>
          pendingOfferRepository.put(offer) >>
          sendText(s"${Emoji.check} Ваше объявление будет опубликовано после проверки")
      case None =>
        states.update(_.updated(chatId, Drafting(newDraft))) >>
          sendText(
            s"""
               |${Emoji.smilingFace} Отлично!
               |
               |Теперь опишите Ваш товар или услугу
               |Укажите стоимость и другие важные детали чтобы на него откликнулось больше людей
               |""".stripMargin
          ).whenA(newDraft.description.isEmpty && newDraft.photoIds.length == 1)
    }
  }

  private def sendText(text: String)(implicit chatId: Long): IO[Unit] =
    Methods.sendMessage(
      chatId = ChatIntId(chatId),
      text = text,
      replyMarkup = ReplyKeyboardRemove(removeKeyboard = true).some
    ).exec.attempt.void

  private def sendOffer(offer: Offer, left: Int = 0)(implicit chatId: Long): IO[Unit] =
    Methods.sendMediaGroup(
      chatId = ChatIntId(chatId),
      media = offer.photoIds.map(InputMediaPhoto(_))
    ).exec.void >>
      Methods.sendMessage(
        chatId = ChatIntId(chatId),
        text = offer.description,
        replyMarkup =
          if (left == 0)
            ReplyKeyboardRemove(removeKeyboard = true).some
          else
            singleButton(
              KeyboardButton(s"еще $left"),
              resizeKeyboard = true.some
            ).some
      ).exec.void

  object Emoji {
    val check = "✅"
    val cross = "❌"
    val penciveFace = "\uD83D\uDE14"
    val smilingFace = "☺️"
  }
}
