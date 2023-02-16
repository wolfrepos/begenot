package wolfcode

import cats.data.NonEmptyList
import cats.effect._
import cats.implicits.{catsSyntaxApplicativeByName, catsSyntaxOptionId}
import io.circe.syntax.EncoderOps
import org.typelevel.log4cats.slf4j.Slf4jLogger
import telegramium.bots.CirceImplicits.messageEncoder
import telegramium.bots._
import telegramium.bots.high.implicits._
import telegramium.bots.high.keyboards.{InlineKeyboardMarkups, ReplyKeyboardMarkups}
import telegramium.bots.high.{Api, Methods}
import wolfcode.CustomExtractors._
import wolfcode.model.State.{Drafting, Idle, Viewing}
import wolfcode.model.{Draft, Offer, State, User => User0}
import wolfcode.repository.{OfferRepository, PendingOfferRepository, UserRepository}

import java.time.OffsetDateTime
import scala.concurrent.duration.DurationInt
import scala.util.Random

class LongPollHandler(states: Ref[IO, Map[Long, State]],
                      userRepository: UserRepository,
                      offerRepository: OfferRepository,
                      pendingOfferRepository: PendingOfferRepository)(implicit api: Api[IO]) extends LongPoll[IO](api) {
  private val logger = Slf4jLogger.getLogger[IO]
  private val admins = List(108683062L)

  override def onMessage(message: Message): IO[Unit] = {
    implicit val chatId: Long = message.chat.id
    for {
      _ <- logger.info(s"got message: ${message.asJson}")
      state <- states.get.map(_.getOrElse(message.chat.id, State.Idle))
      _ <- (message, state) match {
        case (Contactt(c), _) => userRepository.upsert(User0(chatId, c.phoneNumber, c.firstName))
        case (Text("/start"), _) => sendInstructions(greet = true)
        case (PhotoId(photoId), state) =>
          updateDraft(
            draft = state match {
              case Drafting(draft) => draft
              case _ => Draft.create(chatId, OffsetDateTime.now)
            },
            photoId = photoId.some,
            text = message.caption
          )
        case (Text(text), Drafting(draft)) => updateDraft(draft, text = text.some)
        case (Text(text), Idle | Viewing(_)) => searchOffers(text)
        case _ => sendInstructions()
      }
    } yield ()
  }

  override def onCallbackQuery(query: CallbackQuery): IO[Unit] = {
    implicit val chatId: Long = query.from.id
    query.data match {
      case Some(x) if x.startsWith("publish") && admins.contains(query.from.id) => publishOffer(x.filter(_.isDigit).toInt)
      case Some(x) if x.startsWith("decline") && admins.contains(query.from.id) => declineOffer(x.filter(_.isDigit).toInt)
      case Some("help") => sendInstructions()(query.from.id)
      case Some("next") =>
        states.get.map(_.getOrElse(chatId, State.Idle)).flatMap {
          case Viewing(offers) => sendOffers(offers.toList)
          case _ => IO.unit
        }
      case Some(x) if x.startsWith("contact") =>
        val userId = x.filter(_.isDigit).toInt
        userRepository.get(userId).flatMap {
          case Some(user) =>
            Methods.sendContact(
              chatId = ChatIntId(user.id),
              firstName = user.firstName,
              phoneNumber = user.phoneNumber
            ).exec.void
          case None =>
            IO.unit
        }
      case _ => IO.unit
    }
  }

  def job: IO[Unit] = {
    val admin = Random.shuffle(admins).head
    pendingOfferRepository
      .getOldestForPublish.attempt
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
      sendText("Привет!").whenA(greet) >>
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
          sendText(
            s"${Emoji.penciveFace} по Вашему запросу не нашлось предложений",
            keyboard = InlineKeyboardMarkups.singleButton(
              InlineKeyboardButton("Помощь", callbackData = "help".some)
            )
          )
      case offer :: Nil =>
        states.update(_.updated(chatId, Idle)) >>
          sendOffer(offer)
      case offer :: next :: rest =>
        states.update(_.updated(chatId, Viewing(NonEmptyList.of(next, rest: _*)))) >>
          sendOffer(offer, rest.length + 1)
    }

  private def updateDraft(draft: Draft,
                          text: Option[String] = None,
                          photoId: Option[String] = None)(implicit chatId: Long): IO[Unit] = {
    val newDraft = draft.copy(
      description = text.orElse(draft.description),
      photoIds = photoId.fold(draft.photoIds)(_ :: draft.photoIds)
    )
    newDraft.toOffer match {
      case Some(offer) =>
        states.update(_.updated(chatId, Idle)) >>
          pendingOfferRepository.put(offer) >>
          userRepository.get(chatId).flatMap {
            case Some(_) =>
              sendText(s"${Emoji.check} Ваше объявление будет опубликовано после проверки")
            case _ =>
              sendText(
                s"""
                   |${Emoji.check} Ваше объявление будет опубликовано после проверки
                   |
                   |Чтобы пользователи знали как с Вами связаться, поделитесь Вашим контактом нажав кнопку ниже
                   |""".stripMargin,
                ReplyKeyboardMarkups.singleButton(
                  KeyboardButton("Поделиться своим контактом", requestContact = true.some),
                  resizeKeyboard = true.some
                )
              )
          }
      case None =>
        states.update(_.updated(chatId, Drafting(newDraft))) >>
          sendText(
            s"""
               |${Emoji.smilingFace} Отлично - фотографии есть!
               |
               |Теперь опишите Ваш товар или услугу
               |Укажите стоимость и другие важные детали чтобы на него откликнулось больше людей
               |""".stripMargin
          ).whenA(newDraft.description.isEmpty && newDraft.photoIds.length == 1)
    }
  }

  private def sendText(text: String, keyboard: KeyboardMarkup = ReplyKeyboardRemove(removeKeyboard = true))(implicit chatId: Long): IO[Unit] =
    Methods.sendMessage(
      chatId = ChatIntId(chatId),
      text = text,
      replyMarkup = keyboard.some
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
          InlineKeyboardMarkups.singleRow(
            InlineKeyboardButton(s"Показать контакт", callbackData = s"contact${offer.ownerId}".some) :: {
              if (left == 0) Nil else InlineKeyboardButton(s"${Emoji.downArrow} еще $left", callbackData = "next".some) :: Nil
            }
          ).some
      ).exec.void

  object Emoji {
    val check = "✅"
    val cross = "❌"
    val downArrow = "⬇️"
    val penciveFace = "\uD83D\uDE14"
    val smilingFace = "☺️"
  }
}
