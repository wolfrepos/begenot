package wolfcode.repository

import cats.data.NonEmptyList
import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import wolfcode.Offer

import java.time.OffsetDateTime

trait TestOffers {
  this: PostgresSetup =>

  def loadTestOffers: IO[Unit] =
    Update[(Int, String, String, OffsetDateTime, Long)](
      """
      INSERT INTO offers (id, description, photo_ids, publish_time, owner_id)
      VALUES (?, ?, ?, ?, ?)
      """
    ).updateMany(testOffers.map {
      case Offer(id, description, photoIds, publishTime, ownerId) =>
        (id, description, photoIds.mkString(OfferRepository.sep), publishTime, ownerId)
    }).transact(transactor).void

  val testOffers: NonEmptyList[Offer] =
    NonEmptyList.of(
      1 -> "Продаю процессор intel core i3 12100",
      2 -> "Продам процессор intel core i5 12600",
      3 -> "Продам процессор amd ryzen 5 2600x",
      4 -> "Продам процессор amd ryzen 3 2200g"
    ).map((offer _).tupled)

  private def offer(id: Int, description: String): Offer =
    Offer(id, description, List(""), OffsetDateTime.now(), 0)
}
