package wolfcode.repo

import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragments.whereAndOpt
import doobie.util.transactor.Transactor
import wolfcode.model.Offer

trait OfferRepo {
  def put(offer: Offer): IO[Unit]
  def query(q: OfferRepo.Query): IO[List[Offer]]
}

object OfferRepo {
  def create(tx: Transactor[IO]): OfferRepo =
    new OfferRepo {
      def put(offer: Offer): IO[Unit] =
        SQL.put(offer)
          .run
          .transact(tx)
          .void

      def query(q: Query): IO[List[Offer]] =
        SQL.query(q)
          .to[List]
          .transact(tx)
    }

  object SQL {
    def put(offer: Offer): Update0 = {
      import offer._
      sql"""
         INSERT INTO offers (description, photo_ids, publish_time, owner_id, brand, model, yearr, price)
         VALUES ($description, $photoIds, $publishTime, $ownerId, $brand, $model, $year, $price)
         """.update
    }

    def query(q: OfferRepo.Query): Query0[Offer] = {
      sql"""
         SELECT id, owner_id, description, photo_ids, publish_time, brand, model, yearr, price
         FROM offers
         """ ++ whereAndOpt(
        q.brand.map(x => fr"brand = $x"),
        q.model.map(x => fr"model = $x"),
        q.year.map(x => fr"yearr = $x"),
        q.minPrice.map(x => fr"price >= $x"),
        q.maxPrice.map(x => fr"price <= $x"),
      )
    }.query[Offer]
  }

  case class Query(brand: Option[String],
                   model: Option[String],
                   year: Option[Int],
                   minPrice: Option[Int],
                   maxPrice: Option[Int])
}
