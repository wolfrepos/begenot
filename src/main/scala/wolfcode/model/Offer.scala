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
  case class Car(description: Option[String],
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
         |Коробка: ${showTransmission(transmission)}
         |Пробег: $mileage
         |Цена: $price $$
         |Руль: ${showSteering(steering)}
         |Год: $year
         |${description.fold("")(_ + "\n")}
         |${Emoji.phone} $phone
         |${Emoji.city} ${showCity(city)}
         |""".stripMargin

    def showTransmission(s: String): String =
      s match {
        case "automatic" => "Автомат"
        case "manual" => "Механика"
        case _ => s
      }

    def showSteering(s: String): String =
      s match {
        case "left" => "Левый"
        case "right" => "Правый"
        case _ => s
      }

    def showCity(s: String): String =
      s match {
        case "bishkek" => "Бишкек"
        case "osh" => "Ош"
        case _ => s
      }
  }
}