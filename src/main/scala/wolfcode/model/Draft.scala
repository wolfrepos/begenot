package wolfcode.model

import cats.data.NonEmptyList
import cats.implicits.catsSyntaxTuple2Semigroupal

import java.time.OffsetDateTime

case class Draft(id: Int,
                 ownerId: Long,
                 description: Option[String],
                 photoIds: List[String],
                 createTime: OffsetDateTime) {
  def toOffer: Option[Offer] =
    (description, NonEmptyList.fromList(photoIds)).mapN {
      case (desc, pIds) =>
        Offer(
          id = id,
          ownerId = ownerId,
          description = desc,
          photoIds = pIds.toList,
          publishTime = createTime
        )
    }
}

object Draft {
  def create(ownerId: Long, createTime: OffsetDateTime): Draft =
    Draft(id = 0, ownerId = ownerId, None, List(), createTime = createTime)
}
