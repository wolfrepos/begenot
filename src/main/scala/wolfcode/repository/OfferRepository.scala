package wolfcode.repository

import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import wolfcode.model.{Offer, PhotoIds}

trait OfferRepository {
  def put(offer: Offer): IO[Unit]
}

object OfferRepository {
  def create(tx: Transactor[IO]): OfferRepository =
    new OfferRepository {
      override def put(offer: Offer): IO[Unit] =
        putQuery(offer)
          .run
          .transact(tx)
          .void
    }

  def putQuery(offer: Offer): Update0 = {
    import offer._
    sql"""
       INSERT INTO offers (description, photo_ids, publish_time, owner_id, brand, model, yearr, price)
       VALUES ($description, $photoIds, $publishTime, $ownerId, $brand, $model, $year, $price)
       """.update
  }

  val sep = "&"
}
