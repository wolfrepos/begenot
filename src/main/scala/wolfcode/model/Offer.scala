package wolfcode.model

import wolfcode.model.Offer.Car
import wolfcode.tg.Emoji

import java.time.OffsetDateTime

case class Offer(id: Int,
                 ownerId: Long,
                 photoIds: PhotoIds,
                 publishTime: OffsetDateTime,
                 car: Car)

object Offer {
  case class Car(description: String,
                 brand: String,
                 model: String,
                 year: Int,
                 price: Int,
                 transmission: String,
                 steering: String,
                 mileage: Int,
                 phone: String,
                 city: String) {
    def show: String =
      s"""
         |${Emoji.car} ${brand.capitalize} ${model.capitalize}
         |
         |Год: $year
         |Цена: $price $$
         |Коробка: $transmission
         |Руль: $steering
         |Пробег: $mileage
         |${if (description.isEmpty) "" else description + "\n"}
         |${Emoji.phone} $phone
         |${Emoji.city} $city
         |""".stripMargin
  }
}