package wolfcode.repo

import doobie.postgres.implicits._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite
import wolfcode.model.{Offer, PhotoIds}

import java.time.OffsetDateTime

class PendingOfferRepoTest extends AnyFunSuite with IOChecker with PostgresSetup {
  test("queries") {
    check(PendingOfferRepo.putQuery(Offer(0, 1L, "", PhotoIds("" :: Nil), OffsetDateTime.now(), "kia", "k5", 2019, 15200)))
    check(PendingOfferRepo.getQuery(0))
    check(PendingOfferRepo.getOldestForPublishQuery)
    check(PendingOfferRepo.deleteQuery(0))
  }
}
