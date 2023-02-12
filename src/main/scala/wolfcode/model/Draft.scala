package wolfcode.model

import java.time.OffsetDateTime

case class Draft(ownerId: Long,
                 description: Option[String],
                 photoIds: List[String],
                 createTime: OffsetDateTime)
