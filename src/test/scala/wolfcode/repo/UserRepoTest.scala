package wolfcode.repo

import doobie.postgres.implicits._
import doobie.scalatest.IOChecker
import org.scalatest.funsuite.AnyFunSuite
import wolfcode.model.User

class UserRepoTest extends AnyFunSuite with IOChecker with PostgresSetup {
  test("queries") {
    check(UserRepo.upsertQuery(User(0L, "", "")))
    check(UserRepo.getQuery(0L))
  }
}
