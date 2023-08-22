package wolfcode.repository

import cats.data.NonEmptyList
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxOptionId
import doobie.postgres.implicits._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite

class OfferRepoTest extends AnyFunSuite with IOChecker with PostgresSetup with TestOffers {
  private lazy val offerRepository = OfferRepo.create(transactor)

  test("queries") {
    check(OfferRepo.SQL.put(testOffers.head))
    check(OfferRepo.SQL.query(OfferRepo.Query("kia".some, "k5".some, 2019.some, 14000.some, 16000.some)))
  }

  test("execution") {
    (for {
      _ <- loadTestOffers
    } yield ()).unsafeRunSync()
  }
}
