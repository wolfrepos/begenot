package wolfcode.repository

import cats.data.OptionT
import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import mouse.all.anySyntaxMouse
import wolfcode.model.Offer

import java.time.OffsetDateTime

trait PendingOfferRepository {
  def put(offer: Offer): IO[Unit]
  def get(id: Int): IO[Option[Offer]]
  def getOldestForPublish: IO[Option[Offer]]
  def delete(id: Int): IO[Unit]
  def publish(id: Int): IO[Option[Offer]]
  def decline(id: Int): IO[Option[Offer]]
}

object PendingOfferRepository {
  def create(tx: Transactor[IO]): PendingOfferRepository =
    new PendingOfferRepository {
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
          offer <- PendingOfferRepository.getQuery(id).option |> (OptionT(_))
          _ <- OfferRepository.putQuery(offer).run |> (OptionT.liftF(_))
          _ <- PendingOfferRepository.deleteQuery(id).run |> (OptionT.liftF(_))
        } yield offer
      }.value.transact(tx)

      override def decline(id: Int): IO[Option[Offer]] = {
        for {
          offer <- PendingOfferRepository.getQuery(id).option |> (OptionT(_))
          _ <- PendingOfferRepository.deleteQuery(id).run |> (OptionT.liftF(_))
        } yield offer
      }.value.transact(tx)
    }

  def putQuery(offer: Offer): Update0 = {
    import offer._
    sql"""
       INSERT INTO pending_offers (description, photo_ids, publish_time, owner_id)
       VALUES ($description, ${photoIds.mkString(sep)}, $publishTime, $ownerId)
       """.update
  }

  def getQuery(id: Int): Query0[Offer] =
    sql"""
     SELECT id, description, photo_ids, publish_time, owner_id
     FROM pending_offers WHERE id = $id
     """
      .query[(Int, String, String, OffsetDateTime, Long)]
      .map {
        case (id, description, photoIds, createTime, ownerId) =>
          Offer(id, description, photoIds.split(sep).toList, createTime, ownerId)
      }

  val getOldestForPublishQuery: Query0[Offer] =
    sql"""
       SELECT t1.id, t1.description, t1.photo_ids, t1.publish_time, t1.owner_id
       FROM pending_offers t1
       INNER JOIN users t2 ON t1.owner_id = t2.id
       ORDER BY publish_time ASC LIMIT 1
       """
      .query[(Int, String, String, OffsetDateTime, Long)]
      .map {
        case (id, description, photoIds, createTime, ownerId) =>
          Offer(id, description, photoIds.split(sep).toList, createTime, ownerId)
      }

  def deleteQuery(id: Int): Update0 =
    sql"DELETE FROM pending_offers where id = $id".update

  val sep = "&"
}
