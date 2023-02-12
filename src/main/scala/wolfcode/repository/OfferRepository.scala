package wolfcode.repository

import cats.data.NonEmptyList
import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import wolfcode.Offer

import java.time.OffsetDateTime

trait OfferRepository {
  def put(offer: Offer): IO[Unit]
  def ftSearch(words: NonEmptyList[String]): IO[List[Offer]]
}

object OfferRepository {
  def create(tx: Transactor[IO]): OfferRepository =
    new OfferRepository {
      override def put(offer: Offer): IO[Unit] = ???

      override def ftSearch(words: NonEmptyList[String]): IO[List[Offer]] =
        ftSearchQuery(words)
          .to[List]
          .transact(tx)
    }

  def ftSearchQuery(words: NonEmptyList[String]): Query0[Offer] =
    sql"""
       SELECT id, description, photo_ids, publish_time, owner_id
       FROM offers, to_tsquery('russian', ${words.toList.mkString("|")}) as q
       WHERE to_tsvector('russian', description) @@ q
       ORDER BY ts_rank(to_tsvector('russian', description), q) DESC;
       """
      .query[(Int, String, String, OffsetDateTime, Long)]
      .map {
        case (id, description, photoIds, publishTime, ownerId) =>
          Offer(id, description, photoIds.split("&").toList, publishTime, ownerId)
      }
}
