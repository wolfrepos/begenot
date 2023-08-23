package wolfcode.repo

import cats.effect.IO
import doobie.implicits._
import doobie.postgres.implicits._
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
        sql.putOffer(offer)
          .run
          .transact(tx)
          .void

      def query(q: Query): IO[List[Offer]] =
        sql.queryOffer(q)
          .to[List]
          .transact(tx)
    }

  case class Query(brand: Option[String],
                   model: Option[String],
                   year: Option[Int],
                   minPrice: Option[Int],
                   maxPrice: Option[Int])
}
