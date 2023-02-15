package wolfcode.repository

import cats.implicits.catsSyntaxOptionId
import doobie.postgres.implicits._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite
import wolfcode.model.Draft

import java.time.OffsetDateTime

class DraftRepositoryTest extends AnyFunSuite with IOChecker with PostgresSetup {
  test("putQuery") {
    check(DraftRepository.putQuery(Draft(0, 1L, "".some, ""::Nil, OffsetDateTime.now())))
  }

  test("getQuery") {
    check(DraftRepository.getOldestQuery)
  }
}
