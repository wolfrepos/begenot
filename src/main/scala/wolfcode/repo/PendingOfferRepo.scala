package wolfcode.repo

import cats.data.OptionT
import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import mouse.all.anySyntaxMouse
import wolfcode.model.Offer

trait PendingOfferRepo {
  def put(offer: Offer): IO[Unit]
  def get(id: Int): IO[Option[Offer]]
  def getOldestForPublish: IO[Option[Offer]]
  def delete(id: Int): IO[Unit]
  def publish(id: Int): IO[Option[Offer]]
  def decline(id: Int): IO[Option[Offer]]
}

object PendingOfferRepo {
  def create(tx: Transactor[IO]): PendingOfferRepo =
    new PendingOfferRepo {
      override def put(offer: Offer): IO[Unit] =
        putQuery(offer).run.transact(tx).void

      override def get(id: Int): IO[Option[Offer]] =
        getQuery(id).option.transact(tx)

      override def getOldestForPublish: IO[Option[Offer]] =
        getOldestForPublishQuery.option.transact(tx)

      override def delete(id: Int): IO[Unit] =
        deleteQuery(id).run.transact(tx).void

      override def publish(id: Int): IO[Option[Offer]] = {
        for {
          offer <- PendingOfferRepo.getQuery(id).option |> (OptionT(_))
          _ <- OfferRepo.SQL.put(offer).run |> (OptionT.liftF(_))
          _ <- PendingOfferRepo.deleteQuery(id).run |> (OptionT.liftF(_))
        } yield offer
      }.value.transact(tx)

      override def decline(id: Int): IO[Option[Offer]] = {
        for {
          offer <- PendingOfferRepo.getQuery(id).option |> (OptionT(_))
          _ <- PendingOfferRepo.deleteQuery(id).run |> (OptionT.liftF(_))
        } yield offer
      }.value.transact(tx)
    }

  def putQuery(offer: Offer): Update0 = {
    import offer._
    sql"""
       INSERT INTO pending_offers (description, photo_ids, publish_time, owner_id, brand, model, yearr, price)
       VALUES ($description, $photoIds, $publishTime, $ownerId, $brand, $model, $year, $price)
       """.update
  }

  def getQuery(id: Int): Query0[Offer] =
    sql"""
       SELECT id, owner_id, description, photo_ids, publish_time, brand, model, yearr, price
       FROM pending_offers WHERE id = $id
       """.query[Offer]

  val getOldestForPublishQuery: Query0[Offer] =
    sql"""
       SELECT t1.id, t1.owner_id, t1.description, t1.photo_ids, t1.publish_time, t1.brand, t1.model, t1.yearr, t1.price
       FROM pending_offers t1
       INNER JOIN users t2 ON t1.owner_id = t2.id
       ORDER BY publish_time ASC LIMIT 1
       """.query[Offer]

  def deleteQuery(id: Int): Update0 =
    sql"DELETE FROM pending_offers where id = $id".update
}
