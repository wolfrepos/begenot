package wolfcode.repository

import cats.data.NonEmptyList
import cats.effect.unsafe.implicits.global
import doobie.postgres.implicits._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

class OfferRepositoryTest extends AnyFunSuite with IOChecker with PostgresSetup with TestOffers {
  private lazy val offerRepository = OfferRepository.create(transactor)

  test("putQuery") {
    check(OfferRepository.putQuery(testOffers.head))
  }

  test("ftSearchQuery") {
    check(OfferRepository.ftSearchQuery(NonEmptyList.of("Процессор", "intel", "i3")))
  }

  test("try ftSearch") {
    (for {
      _ <- loadTestOffers

      offers <- offerRepository.ftSearch(NonEmptyList.of("ryzen", "5"))
      _ = offers.map(_.id) shouldBe List(3, 4)

      offers <- offerRepository.ftSearch(NonEmptyList.of("процессор", "intel", "i5"))
      _ = offers.map(_.id) shouldBe List(2, 1, 3, 4)

      offers <- offerRepository.ftSearch(NonEmptyList.of("intel", "core", "i3", "12100"))
      _ = offers.map(_.id) shouldBe List(1, 2)
    } yield ()).unsafeRunSync()
  }
}
