package wolfcode.model

import java.time.OffsetDateTime

case class Offer(id: Int,
                 ownerId: Long,
                 description: String,
                 photoIds: PhotoIds,
                 publishTime: OffsetDateTime,
                 brand: String,
                 model: String,
                 year: Int,
                 price: Int)
