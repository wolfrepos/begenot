package wolfcode.repository

import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import wolfcode.model.Offer

import java.time.OffsetDateTime

trait SpiritOfferRepository {
  def put(offer: Offer): IO[Unit]
  def get(id: Int): IO[Option[Offer]]
}

object SpiritOfferRepository {
  def create(tx: Transactor[IO]): SpiritOfferRepository =
    new SpiritOfferRepository {
      override def put(offer: Offer): IO[Unit] =
        putQuery(offer)
          .run
          .transact(tx)
          .void

      override def get(id: Int): IO[Option[Offer]] =
        getQuery(id)
          .option
          .transact(tx)
    }

  def putQuery(offer: Offer): Update0 = {
    import offer._
    sql"""
       INSERT INTO offer_spirits (description, photo_ids, publish_time, owner_id)
       VALUES ($description, ${photoIds.mkString(sep)}, $publishTime, $ownerId)
       """.update
  }

  def getQuery(id: Int): Query0[Offer] =
    sql"""
       SELECT id, description, photo_ids, publish_time, owner_id
       FROM offer_spirits WHERE id = $id
       """
      .query[(Int, String, String, OffsetDateTime, Long)]
      .map {
        case (id, description, photoIds, publishTime, ownerId) =>
          Offer(id, description, photoIds.split(sep).toList, publishTime, ownerId)
      }

  val sep = "&"
}
