package wolfcode.repository

import cats.data.NonEmptyList
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite

class OfferRepositoryTest extends AnyFunSuite with IOChecker with PostgresSetup {
  test("ftSearchQuery") {
    check(OfferRepository.ftSearchQuery(NonEmptyList.of("Процессор", "intel", "i3")))
  }
}
