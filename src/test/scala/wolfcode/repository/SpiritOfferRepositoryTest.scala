package wolfcode.repository

import doobie.postgres.implicits._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite

class SpiritOfferRepositoryTest extends AnyFunSuite with IOChecker with PostgresSetup with TestOffers {
  test("putQuery") {
    check(SpiritOfferRepository.putQuery(testOffers.head))
  }

  test("getQuery") {
    check(SpiritOfferRepository.getQuery(1))
  }
}
