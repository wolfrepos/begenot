package wolfcode.repo

import cats.data.OptionT
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import mouse.all.anySyntaxMouse
import wolfcode.model.Offer

trait PendingOfferRepo {
  def put(offer: Offer): IO[Unit]
  def getToPublish: IO[Option[Offer]]
  def publish(id: Int): IO[Option[Offer]]
  def decline(id: Int): IO[Option[Offer]]
}

object PendingOfferRepo {
  def create(tx: Transactor[IO]): PendingOfferRepo =
    new PendingOfferRepo {
      override def put(offer: Offer): IO[Unit] =
        sql.putPendingOffer(offer).run.transact(tx).void

      override def getToPublish: IO[Option[Offer]] =
        sql.getPendingOfferToPublish.option.transact(tx)

      override def publish(id: Int): IO[Option[Offer]] = {
        for {
          offer <- sql.getPendingOffer(id).option |> (OptionT(_))
          _ <- sql.putOffer(offer).run |> (OptionT.liftF(_))
          _ <- sql.deletePendingOffer(id).run |> (OptionT.liftF(_))
        } yield offer
      }.value.transact(tx)

      override def decline(id: Int): IO[Option[Offer]] = {
        for {
          offer <- sql.getPendingOffer(id).option |> (OptionT(_))
          _ <- sql.deletePendingOffer(id).run |> (OptionT.liftF(_))
        } yield offer
      }.value.transact(tx)
    }
}
