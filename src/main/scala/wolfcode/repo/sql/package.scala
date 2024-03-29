package wolfcode.repo

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragments._
import wolfcode.model.Offer

import java.time.OffsetDateTime

package object sql {

  private val offerFieldsWithoutId: Fragment =
    fr"""owner_id,
         photo_ids,
         publish_time,
         description,
         brand,
         model,
         yearr,
         price,
         transmission,
         steering,
         mileage,
         phone,
         city"""

  private val offerFields: Fragment =
    fr"id," ++ offerFieldsWithoutId

  def queryOffer(q: OfferRepo.Query): Query0[Offer] = {
    fr"SELECT" ++ offerFields ++ fr"FROM offers" ++ whereAndOpt(
      q.brand.map(x => fr"brand = $x"),
      q.model.map(x => fr"model = $x"),
      q.year.map(x => fr"yearr = $x"),
      q.transmission.map(x => fr"transmission = $x"),
      q.steering.map(x => fr"steering = $x"),
      q.mileage.map(x => fr"mileage <= $x"),
      q.priceMin.map(x => fr"price >= $x"),
      q.priceMax.map(x => fr"price <= $x"),
      q.city.map(x => fr"city = $x"),
    )
  }.query[Offer]

  def getPendingOffer(id: Int): Query0[Offer] = {
    fr"SELECT" ++ offerFields ++ fr"FROM pending_offers" ++ fr"WHERE id = $id"
  }.query[Offer]

  val getPendingOfferToPublish: Query0[Offer] = {
    fr"SELECT" ++ offerFields ++ fr"FROM pending_offers" ++ fr"ORDER BY publish_time ASC LIMIT 1"
  }.query[Offer]

  private def putOfferInto(table: Fragment)(offer: Offer): Update0 = {
    import offer._
    fr"INSERT INTO" ++ table ++ fr"(" ++ offerFieldsWithoutId ++ fr")" ++
      fr"""VALUES ($ownerId,
                   $photoIds,
                   $publishTime,
                   ${car.description},
                   ${car.brand},
                   ${car.model},
                   ${car.year},
                   ${car.price},
                   ${car.transmission},
                   ${car.steering},
                   ${car.mileage},
                   ${car.phone},
                   ${car.city})"""
  }.update

  val putOffer: Offer => Update0 = putOfferInto(fr"offers")
  val putPendingOffer: Offer => Update0 = putOfferInto(fr"pending_offers")

  def deletePendingOffer(id: Int): Update0 =
    sql"DELETE FROM pending_offers where id = $id".update

  def putQuery(userId: Long, queryTime: OffsetDateTime, query: OfferRepo.Query): Update0 = {
    import query._
    sql"""
       INSERT INTO QUERIES (user_id, query_time, brand, model, yearr, transmission, steering, mileage, price_min, price_max, city)
       VALUES ($userId, $queryTime, $brand, $model, $year, $transmission, $steering, $mileage, $priceMin, $priceMax, $city)
       """.update
  }
}
