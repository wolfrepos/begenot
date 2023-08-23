package wolfcode.model

import cats.data.NonEmptyList
import cats.implicits.catsSyntaxTuple2Semigroupal
import wolfcode.model.Offer.Car

import java.time.OffsetDateTime

case class Draft(id: Int,
                 ownerId: Long,
                 createTime: OffsetDateTime,
                 photoIds: List[String],
                 car: Option[Car]) {
  def toOffer: Option[Offer] =
    (NonEmptyList.fromList(photoIds), car).mapN {
      case (pIds, car) =>
        Offer(
          id = id,
          ownerId = ownerId,
          photoIds = PhotoIds(pIds.toList),
          publishTime = createTime,
          car = car
        )
    }
}

object Draft {
  def create(ownerId: Long, createTime: OffsetDateTime): Draft =
    Draft(
      ownerId = ownerId,
      createTime = createTime,
      id = 0,
      photoIds = List.empty,
      car = None
    )
}
