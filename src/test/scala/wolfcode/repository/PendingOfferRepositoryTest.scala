package wolfcode.repository

import doobie.postgres.implicits._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite
import wolfcode.model.Offer

import java.time.OffsetDateTime

class PendingOfferRepositoryTest extends AnyFunSuite with IOChecker with PostgresSetup {
  test("putQuery") {
    check(PendingOfferRepository.putQuery(Offer(0, "", "" :: Nil, OffsetDateTime.now(), 1L)))
  }

  test("getQuery") {
    check(PendingOfferRepository.getOldestQuery)
  }
}
