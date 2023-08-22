package wolfcode.model

import cats.data.NonEmptyList
import cats.implicits.catsSyntaxTuple6Semigroupal

import java.time.OffsetDateTime

case class Draft(id: Int,
                 ownerId: Long,
                 description: Option[String],
                 photoIds: List[String],
                 createTime: OffsetDateTime,
                 brand: Option[String],
                 model: Option[String],
                 year: Option[Int],
                 price: Option[Int]) {
  def toOffer: Option[Offer] =
    (description, NonEmptyList.fromList(photoIds), brand, model, year, price).mapN {
      case (desc, pIds, brand, model, year, price) =>
        Offer(
          id = id,
          ownerId = ownerId,
          description = desc,
          photoIds = PhotoIds(pIds.toList),
          publishTime = createTime,
          brand = brand,
          model = model,
          year = year,
          price = price
        )
    }
}

object Draft {
  def create(ownerId: Long, createTime: OffsetDateTime): Draft =
    Draft(
      ownerId = ownerId,
      createTime = createTime,

      id = 0,
      description = None,
      photoIds = List.empty,
      brand = None,
      model = None,
      year = None,
      price = None
    )
}
