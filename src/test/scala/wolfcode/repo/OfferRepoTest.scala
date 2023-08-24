package wolfcode.repo

import cats.data.NonEmptyList
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxOptionId
import doobie.postgres.implicits._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite

class OfferRepoTest extends AnyFunSuite with IOChecker with PostgresSetup with TestOffers {
  private lazy val offerRepository = OfferRepo.create(transactor)

  val query = OfferRepo.Query(
    brand = "kia".some,
    model = "k5".some,
    year = 2019.some,
    transmission = "automatic".some,
    steering = "left".some,
    mileage = 170000.some,
    priceMin = 16000.some,
    priceMax = 19000.some,
    city = "bishkek".some
  )
  test("queries") {
    check(sql.putOffer(testOffers.head))
    check(sql.queryOffer(query))
  }
}
