package wolfcode.repo

import cats.data.NonEmptyList
import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import wolfcode.model.{Offer, PhotoIds}

import java.time.OffsetDateTime

trait TestOffers {
  this: PostgresSetup =>

  val car = Offer.Car(
    description = None,
    brand = "kia",
    model = "k5",
    year = 2019,
    price = 17000,
    transmission = "automatic",
    steering = "left",
    mileage = 170000,
    phone = "123123",
    city = "bishkek"
  )
  val offer = Offer(
    id = 0,
    ownerId = 1L,
    photoIds = PhotoIds("" :: Nil), publishTime = OffsetDateTime.now(), car = car
  )

  val testOffers: NonEmptyList[Offer] =
    NonEmptyList.of(offer)
}
