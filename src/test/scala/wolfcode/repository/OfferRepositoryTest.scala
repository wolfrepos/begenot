package wolfcode.repository

import cats.data.NonEmptyList
import cats.effect.unsafe.implicits.global
import doobie.postgres.implicits._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite

class OfferRepositoryTest extends AnyFunSuite with IOChecker with PostgresSetup with TestOffers {
  private lazy val offerRepository = OfferRepository.create(transactor)

  test("queries") {
    check(OfferRepository.putQuery(testOffers.head))
  }

  test("ftSearchQuery") {
  }

  test("try ftSearch") {
    (for {
      _ <- loadTestOffers
    } yield ()).unsafeRunSync()
  }
}
