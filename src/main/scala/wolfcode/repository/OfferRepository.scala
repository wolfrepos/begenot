package wolfcode.repository

import cats.data.{NonEmptyList, OptionT}
import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import mouse.all.anySyntaxMouse
import wolfcode.model.Offer

import java.time.OffsetDateTime
import scala.util.Random

trait OfferRepository {
  def put(offer: Offer): IO[Unit]

  def ftSearch(words: NonEmptyList[String]): IO[List[Offer]]

  def getRandomOffer: IO[Option[Offer]]
}

object OfferRepository {
  def create(tx: Transactor[IO]): OfferRepository =
    new OfferRepository {
      override def put(offer: Offer): IO[Unit] =
        putQuery(offer)
          .run
          .transact(tx)
          .void

      override def ftSearch(words: NonEmptyList[String]): IO[List[Offer]] =
        ftSearchQuery(words)
          .to[List]
          .transact(tx)

      override def getRandomOffer: IO[Option[Offer]] =
        (for {
          (minId, maxId) <- getMinMaxIdsQuery.option |> (OptionT(_))
          randId = Random.nextInt(maxId - minId + 1) + minId
          offer <- selectEqOrGrIdQuery(randId).option |> (OptionT(_))
        } yield offer).value.transact(tx)
    }

  def selectEqOrGrIdQuery(id: Int) =
    sql"""
       SELECT id, description, photo_ids, publish_time, owner_id
       FROM offers WHERE id >= $id
       """
      .query[(Int, String, String, OffsetDateTime, Long)]
      .map {
        case (id, description, photoIds, publishTime, ownerId) =>
          Offer(id, description, photoIds.split(sep).toList, publishTime, ownerId)
      }

  val getMinMaxIdsQuery =
    sql"select min(id), max(id) from offers".query[(Int, Int)]

  def putQuery(offer: Offer): Update0 = {
    import offer._
    sql"""
       INSERT INTO offers (description, photo_ids, publish_time, owner_id)
       VALUES ($description, ${photoIds.mkString(sep)}, $publishTime, $ownerId)
       """.update
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
          Offer(id, description, photoIds.split(sep).toList, publishTime, ownerId)
      }

  val sep = "&"
}
