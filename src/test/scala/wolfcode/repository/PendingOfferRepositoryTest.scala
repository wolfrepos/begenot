package wolfcode.repository

import doobie.postgres.implicits._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite
import wolfcode.model.{Offer, PhotoIds}

import java.time.OffsetDateTime

class PendingOfferRepositoryTest extends AnyFunSuite with IOChecker with PostgresSetup {
  test("queries") {
    check(PendingOfferRepository.putQuery(Offer(0, 1L, "", PhotoIds("" :: Nil), OffsetDateTime.now(), "kia", "k5", 2019, 15200)))
    check(PendingOfferRepository.getQuery(0))
    check(PendingOfferRepository.getOldestForPublishQuery)
    check(PendingOfferRepository.deleteQuery(0))
  }
}
