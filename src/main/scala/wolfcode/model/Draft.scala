package wolfcode.model

import java.time.OffsetDateTime

case class Draft(ownerId: Long,
                 description: Option[String],
                 photoIds: List[String],
                 createTime: OffsetDateTime) {
  def isReady: Boolean =
    photoIds.nonEmpty && description.nonEmpty
}

object Draft {
  def create(ownerId: Long, createTime: OffsetDateTime): Draft =
    Draft(ownerId = ownerId, None, List(), createTime = createTime)
}
