package wolfcode.repo

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragments._
import wolfcode.model.Offer

package object sql {

  private val offerFieldsWithoutId: Fragment =
    fr"""owner_id,
         description,
         photo_ids,
         publish_time,
         brand,
         model,
         yearr,
         price"""

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

  val getPendingOfferToPublish: Query0[Offer] =
    sql"""
       SELECT t1.id, t1.owner_id, t1.description, t1.photo_ids, t1.publish_time, t1.brand, t1.model, t1.yearr, t1.price
       FROM pending_offers t1
       INNER JOIN users t2 ON t1.owner_id = t2.id
       ORDER BY publish_time ASC LIMIT 1
       """.query[Offer]

  private def putOfferInto(table: Fragment)(offer: Offer): Update0 = {
    import offer._
    fr"INSERT INTO" ++ table ++ fr"(" ++ offerFieldsWithoutId ++ fr")" ++
    fr"VALUES ($ownerId, $description, $photoIds, $publishTime, $brand, $model, $year, $price)"
  }.update

  val putOffer: Offer => Update0 = putOfferInto(fr"offers")
  val putPendingOffer: Offer => Update0 = putOfferInto(fr"pending_offers")

  def deletePendingOffer(id: Int): Update0 =
    sql"DELETE FROM pending_offers where id = $id".update
}
