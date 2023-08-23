package wolfcode.repo

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragments._
import wolfcode.model.Offer

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
      q.minPrice.map(x => fr"price >= $x"),
      q.maxPrice.map(x => fr"price <= $x"),
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
}
