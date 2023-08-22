package wolfcode.tg

import cats.effect.{Async, Ref}
import cats.syntax.all._
import cats.{Monad, Parallel}
import telegramium.bots._
import telegramium.bots.client._
import telegramium.bots.high.Api

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.util.control.NonFatal

abstract class LongPoll[F[_]: Parallel: Async](bot: Api[F]) extends Methods {

  import LongPoll.OffsetKeeper

  private def noop[A](a: A) = Monad[F].pure(a).void

  def onMessage(msg: Message): F[Unit]                                = noop(msg)
  def onEditedMessage(msg: Message): F[Unit]                          = noop(msg)
  def onChannelPost(msg: Message): F[Unit]                            = noop(msg)
  def onEditedChannelPost(msg: Message): F[Unit]                      = noop(msg)
  def onInlineQuery(query: InlineQuery): F[Unit]                      = noop(query)
  def onCallbackQuery(query: CallbackQuery): F[Unit]                  = noop(query)
  def onChosenInlineResult(inlineResult: ChosenInlineResult): F[Unit] = noop(inlineResult)
  def onShippingQuery(query: ShippingQuery): F[Unit]                  = noop(query)
  def onPreCheckoutQuery(query: PreCheckoutQuery): F[Unit]            = noop(query)
  def onPoll(poll: Poll): F[Unit]                                     = noop(poll)
  def onPollAnswer(pollAnswer: PollAnswer): F[Unit]                   = noop(pollAnswer)
  def onMyChatMember(myChatMember: ChatMemberUpdated): F[Unit]        = noop(myChatMember)
  def onChatMember(chatMember: ChatMemberUpdated): F[Unit]            = noop(chatMember)
  def onChatJoinRequest(request: ChatJoinRequest): F[Unit]            = noop(request)

  def onUpdate(update: Update): F[Unit] =
    for {
      _ <- update.message.fold(Monad[F].unit)(onMessage)
      _ <- update.editedMessage.fold(Monad[F].unit)(onEditedMessage)
      _ <- update.channelPost.fold(Monad[F].unit)(onChannelPost)
      _ <- update.editedChannelPost.fold(Monad[F].unit)(onEditedChannelPost)
      _ <- update.inlineQuery.fold(Monad[F].unit)(onInlineQuery)
      _ <- update.callbackQuery.fold(Monad[F].unit)(onCallbackQuery)
      _ <- update.chosenInlineResult.fold(Monad[F].unit)(onChosenInlineResult)
      _ <- update.shippingQuery.fold(Monad[F].unit)(onShippingQuery)
      _ <- update.preCheckoutQuery.fold(Monad[F].unit)(onPreCheckoutQuery)
      _ <- update.poll.fold(Monad[F].unit)(onPoll)
      _ <- update.pollAnswer.fold(Monad[F].unit)(onPollAnswer)
      _ <- update.myChatMember.fold(Monad[F].unit)(onMyChatMember)
      _ <- update.chatMember.fold(Monad[F].unit)(onChatMember)
      _ <- update.chatJoinRequest.fold(Monad[F].unit)(onChatJoinRequest)
    } yield ()

  def onError(e: Throwable): F[Unit] = {
    Async[F].delay(e.printStackTrace())
  }

  def poll(offsetKeeper: OffsetKeeper[F]): F[Unit] = {
    for {
      offset <- offsetKeeper.getOffset
      seconds = pollInterval.toSeconds.toInt
      updates <- bot
        .execute(getUpdates(offset = Some(offset), timeout = Some(seconds)))
        .onError {
          case _: java.util.concurrent.TimeoutException => poll(offsetKeeper)
          case NonFatal(e) =>
            for {
              _     <- onError(e)
              delay <- onErrorDelay
              _     <- Async[F].sleep(delay)
              _     <- poll(offsetKeeper)
            } yield ()
        }
      (messages, otherUpdates) = updates.foldLeft((List.empty[Message], List.empty[Update])) {
        case ((messages, updates), update) =>
          update.message match {
            case Some(x) => (x::messages, updates)
            case None => (messages, update::updates)
          }
      }
      _ <- messages
        .groupBy(_.chat.id)
        .values
        .toList.parTraverse { messages =>
          messages.traverse(onMessage).void
        }.void
      _ <- otherUpdates.filter(_.message.isEmpty).parTraverse {
        onUpdate(_).recoverWith { case NonFatal(e) =>
          onError(e)
        }
      }
      _    <- updates.map(_.updateId).maximumOption.traverse(max => offsetKeeper.setOffset(max + 1))
      next <- poll(offsetKeeper)
    } yield {
      next
    }
  }

  def start(): F[Unit] = {
    for {
      _          <- bot.execute(deleteWebhook())
      refCounter <- Ref.of[F, Int](0)
      offsetKeeper = new OffsetKeeper[F] {
        def getOffset: F[Int]               = refCounter.get
        def setOffset(offset: Int): F[Unit] = refCounter.set(offset)
      }
      _ <- poll(offsetKeeper)
    } yield {
      ()
    }
  }

  def pollInterval: Duration = 10.seconds

  /* Use effectful override to implement different backoff strategies */
  def onErrorDelay: F[FiniteDuration] = {
    Async[F].delay(5.seconds)
  }

}

object LongPoll {

  trait OffsetKeeper[F[_]] {
    def getOffset: F[Int]
    def setOffset(offset: Int): F[Unit]
  }

}
