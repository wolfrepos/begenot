package wolfcode.model

import java.time.OffsetDateTime

case class Offer(id: Int,
                 description: String,
                 photoIds: List[String],
                 publishTime: OffsetDateTime,
                 ownerId: Long)
