package wolfcode.repository

import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import wolfcode.model.Offer

import java.time.OffsetDateTime

trait PendingOfferRepository {
  def put(offer: Offer): IO[Unit]
  def getOldest: IO[Option[Offer]]
}

object PendingOfferRepository {
  def create(tx: Transactor[IO]): PendingOfferRepository =
    new PendingOfferRepository {
      override def put(offer: Offer): IO[Unit] =
        putQuery(offer)
          .run
          .transact(tx)
          .void

      override def getOldest: IO[Option[Offer]] =
        getOldestQuery
          .option
          .transact(tx)
    }

  def putQuery(offer: Offer): Update0 = {
    import offer._
    sql"""
       INSERT INTO pending_offers (description, photo_ids, publish_time, owner_id)
       VALUES ($description, ${photoIds.mkString(sep)}, $publishTime, $ownerId)
       """.update
  }

  val getOldestQuery: Query0[Offer] =
    sql"""
       SELECT id, description, photo_ids, publish_time, owner_id
       FROM pending_offers ORDER BY publish_time ASC LIMIT 1
       """
      .query[(Int, String, String, OffsetDateTime, Long)]
      .map {
        case (id, description, photoIds, createTime, ownerId) =>
          Offer(id, description, photoIds.split(sep).toList, createTime, ownerId)
      }

  val sep = "&"
}
