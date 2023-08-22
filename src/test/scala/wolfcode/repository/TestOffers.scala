package wolfcode.repository

import cats.data.NonEmptyList
import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import wolfcode.model.{Offer, PhotoIds}

import java.time.OffsetDateTime

trait TestOffers {
  this: PostgresSetup =>

  def loadTestOffers: IO[Unit] =
    Update[Offer](
      """
      INSERT INTO offers (id, owner_id, description, photo_ids, publish_time, brand, model, yearr, price)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """
    ).updateMany(testOffers).transact(transactor).void

  val testOffers: NonEmptyList[Offer] =
    NonEmptyList.of(
      Offer(0, 1L, "", PhotoIds("" :: Nil), OffsetDateTime.now(), "kia", "k5", 2019, 15200)
    )
}
