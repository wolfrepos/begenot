package wolfcode.model

import java.time.OffsetDateTime

case class Draft(id: Int,
                 ownerId: Long,
                 description: Option[String],
                 photoIds: List[String],
                 createTime: OffsetDateTime) {
  def isReady: Boolean =
    photoIds.nonEmpty && description.nonEmpty
}

object Draft {
  def create(ownerId: Long, createTime: OffsetDateTime): Draft =
    Draft(id = 0, ownerId = ownerId, None, List(), createTime = createTime)
}
