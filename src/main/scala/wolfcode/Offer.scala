package wolfcode

import java.time.OffsetDateTime

case class Offer(id: Int = 0,
                 description: String,
                 photoIds: List[String],
                 publishTime: OffsetDateTime,
                 ownerId: Long)
