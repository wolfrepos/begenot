package wolfcode.repository

import doobie.postgres.implicits._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite
import wolfcode.model.User

class UserRepositoryTest extends AnyFunSuite with IOChecker with PostgresSetup {
  test("queries") {
    check(UserRepository.upsertQuery(User(0L, "", "")))
    check(UserRepository.getQuery(0L))
  }
}
