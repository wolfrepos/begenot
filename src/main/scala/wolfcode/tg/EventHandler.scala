package wolfcode.tg

import cats.data.NonEmptyList
import cats.effect._
import cats.implicits.{catsSyntaxApplicativeByName, catsSyntaxOptionId}
import io.circe.generic.auto._
import io.circe.parser
import io.circe.syntax.EncoderOps
import org.typelevel.log4cats.slf4j.Slf4jLogger
import telegramium.bots.CirceImplicits.messageEncoder
import telegramium.bots._
import telegramium.bots.high.implicits._
import telegramium.bots.high.keyboards.{InlineKeyboardMarkups, ReplyKeyboardMarkups}
import telegramium.bots.high.{Api, Methods}
import wolfcode.model.State.{Drafting, Idle, Viewing}
import wolfcode.model.{Draft, Offer, State, User => User0}
import wolfcode.repo.{OfferRepo, PendingOfferRepo, QueriesRepo, UserRepo}
import wolfcode.tg.CustomExtractors._
import wolfcode.tg.Emoji

import java.time.OffsetDateTime
import scala.concurrent.duration.DurationInt
import scala.util.Random

class EventHandler(states: Ref[IO, Map[Long, State]],
                   userRepo: UserRepo,
                   offerRepo: OfferRepo,
                   pendingOfferRepo: PendingOfferRepo,
                   queriesRepo: QueriesRepo)(implicit api: Api[IO]) extends LongPoll[IO](api) {
  private val logger = Slf4jLogger.getLogger[IO]
  private val admins = List(108683062L)

  override def onMessage(message: Message): IO[Unit] = {
    implicit val chatId: Long = message.chat.id
    for {
      _ <- logger.info(s"got message: ${message.asJson}")
      state <- states.get.map(_.getOrElse(message.chat.id, State.Idle))
      _ <- (state, message) match {
        case (_, Contactt(c)) => userRepo.upsert(User0(chatId, c.phoneNumber, c.firstName))
        case (_, Text("/start")) => sendInstructions
        case (_, WebApp(WebAppData(data, buttonText))) if buttonText == searchButton.text =>
          parser.parse(data).flatMap(_.as[OfferRepo.Query]) match {
            case Left(error) => logger.info(s"Error parsing json: $error") >> sendInstructions
            case Right(q) =>
              for {
                _ <- logger.info(s"Got data from webApp: $q")
                offers <- offerRepo.query(q)
                _ <- sendOffers(offers)
                _ <- queriesRepo.put(chatId, OffsetDateTime.now, q)
              } yield ()
          }
        case (state, PhotoId(photoId)) =>
          updateDraft(
            draft = state match {
              case Drafting(draft) => draft
              case _ => Draft.create(chatId, OffsetDateTime.now)
            },
            photoId = photoId.some
          )
        case (Drafting(_), Text(text)) if text.toLowerCase.contains("отмена") => sendInstructions
        case (Drafting(draft), WebApp(WebAppData(data, buttonText))) if buttonText == createButton.text =>
          parser.parse(data).flatMap(_.as[Offer.Car]) match {
            case Left(error) =>
              logger.info(s"Error parsing json: $error") >>
                sendInstructions
            case Right(car) =>
              logger.info(s"Got data from webApp: $car") >>
                updateDraft(draft, car = car.some)
          }
        case _ => sendInstructions
      }
    } yield ()
  }

  override def onCallbackQuery(query: CallbackQuery): IO[Unit] = {
    implicit val chatId: Long = query.from.id
    query.data match {
      case Some(x) if x.startsWith("publish") && admins.contains(query.from.id) =>
        publishOffer(x.filter(_.isDigit).toInt) >>
          Methods.editMessageText(
            text = s"${Emoji.check} Опубликовано",
            chatId = query.message.map(_.chat.id).map(ChatIntId),
            messageId = query.message.map(_.messageId)
          ).exec.void
      case Some(x) if x.startsWith("decline") && admins.contains(query.from.id) =>
        declineOffer(x.filter(_.isDigit).toInt) >>
          Methods.editMessageText(
            text = s"${Emoji.cross} Отклонено",
            chatId = query.message.map(_.chat.id).map(ChatIntId),
            messageId = query.message.map(_.messageId)
          ).exec.void
      case Some("next") =>
        states.get.map(_.getOrElse(chatId, State.Idle)).flatMap {
          case Viewing(offers) =>
            sendOffers(offers.toList) >>
              Methods.editMessageReplyMarkup(
                chatId = query.message.map(_.chat.id).map(ChatIntId),
                messageId = query.message.map(_.messageId),
                replyMarkup = None
              ).exec.void
          case _ => IO.unit
        }
      case Some(x) if x.startsWith("contact") =>
        val userId = x.filter(_.isDigit).toInt
        userRepo.get(userId).flatMap {
          case Some(user) =>
            Methods.sendContact(
              chatId = ChatIntId(chatId),
              firstName = user.firstName,
              phoneNumber = user.phoneNumber,
              replyToMessageId = query.message.map(_.messageId)
            ).exec.void
          case None =>
            IO.unit
        }
      case _ => IO.unit
    }
  }

  def job: IO[Unit] = {
    val admin = Random.shuffle(admins).head
    pendingOfferRepo
      .getToPublish.attempt
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
    pendingOfferRepo.publish(offerId).flatMap {
      case Some(o) =>
        sendText(
          s"""
             |Ваше объявление:
             |${o.car.show}
             |${Emoji.check} Опубликовано
             |""".stripMargin
        )(o.ownerId)
      case None => IO.unit
    }

  private def declineOffer(offerId: Int) =
    pendingOfferRepo.decline(offerId).flatMap {
      case Some(o) =>
        sendText(
          s"""
             |Ваше объявление:
             |${o.car.show}
             |${Emoji.cross} Отклонено
             |""".stripMargin
        )(o.ownerId)
      case None => IO.unit
    }

  private def sendInstructions(implicit chatId: Long): IO[Unit] =
    states.update(_.updated(chatId, State.Idle)) >>
      sendText(
        s"""
           |${Emoji.car} Если Вы ищете машину - нажмите на кнопку поиска
           |
           |${Emoji.car} Если хотите продать машину - отправьте фотографии
           |""".stripMargin
      ) >>
      Methods.sendAnimation(
        chatId = ChatIntId(chatId),
        animation = InputLinkFile(gifId)
      ).exec.attempt.void

  private def sendOffers(offers: List[Offer])(implicit chatId: Long): IO[Unit] =
    offers match {
      case Nil =>
        states.update(_.updated(chatId, Idle)) >>
          sendText(s"${Emoji.penciveFace} по Вашему запросу не нашлось предложений, попробуйте другие критерии поиска")
      case offer :: Nil =>
        states.update(_.updated(chatId, Idle)) >>
          sendOffer(offer)
      case offer :: next :: rest =>
        states.update(_.updated(chatId, Viewing(NonEmptyList.of(next, rest: _*)))) >>
          sendOffer(offer, rest.length + 1)
    }

  private def updateDraft(draft: Draft,
                          photoId: Option[String] = None,
                          car: Option[Offer.Car] = None)(implicit chatId: Long): IO[Unit] = {
    val newDraft = draft.copy(
      car = car.orElse(draft.car),
      photoIds = photoId.fold(draft.photoIds)(_ :: draft.photoIds)
    )
    newDraft.toOffer match {
      case Some(offer) if admins.contains(chatId) =>
        states.update(_.updated(chatId, Idle)) >>
          offerRepo.put(offer) >>
          sendText(s"${Emoji.check} Объявление опубликовано")
      case Some(offer) =>
        states.update(_.updated(chatId, Idle)) >>
          pendingOfferRepo.put(offer) >>
          sendText(s"${Emoji.check} Ваше объявление будет опубликовано после проверки")
      case None =>
        states.update(_.updated(chatId, Drafting(newDraft))) >>
          sendText(
            s"""
               |${Emoji.smilingFace} Отлично - фотографии есть!
               |
               |Осталось заполнить простую форму
               |""".stripMargin,
            keyboard = ReplyKeyboardMarkups.singleColumn(List(
              createButton, KeyboardButton(s"${Emoji.cross} Отмена")
            ), resizeKeyboard = true.some)
          ).whenA(newDraft.car.isEmpty && newDraft.photoIds.length == 1)
    }
  }

  private def sendText(text: String,
                       keyboard: KeyboardMarkup = defaultKeyboard)(implicit chatId: Long): IO[Unit] =
    Methods.sendMessage(
      chatId = ChatIntId(chatId),
      text = text,
      replyMarkup = keyboard.some
    ).exec.attempt.void

  private def sendOffer(offer: Offer, left: Int = 0)(implicit chatId: Long): IO[Unit] =
    Methods.sendMediaGroup(
      chatId = ChatIntId(chatId),
      media = offer.photoIds.ids.map(InputMediaPhoto(_))
    ).exec.attempt.void >>
      Methods.sendMessage(
        chatId = ChatIntId(chatId),
        text = offer.car.show,
        replyMarkup =
          if (left == 0)
            defaultKeyboard.some
          else
            InlineKeyboardMarkups.singleRow(
              InlineKeyboardButton(s"${Emoji.car} $left", callbackData = "next".some) :: Nil
            ).some
      ).exec.void

  private val searchWebApp = WebAppInfo("https://wolfrepos.github.io/avtokg/search")
  private val createWebApp = WebAppInfo("https://wolfrepos.github.io/avtokg/create")
  private val createButton = KeyboardButton(s"Заполнить форму", webApp = createWebApp.some)
  private val searchButton = KeyboardButton(s"Поиск", webApp = searchWebApp.some)
  private val defaultKeyboard = ReplyKeyboardMarkups.singleButton(searchButton, resizeKeyboard = true.some)
  private val gifId = "CgACAgIAAxkBAAILwmTnZTtEDzkvQmCd-g8OMFu6bvG4AAKyNQAC_WdBS8_oAAEoCXo1gzAE"
}
