package wolfcode.repo

import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import wolfcode.model.User

trait UserRepo {
  def upsert(user: User): IO[Unit]
  def get(id: Long): IO[Option[User]]
}

object UserRepo {
  def create(tx: Transactor[IO]): UserRepo =
    new UserRepo {
      override def upsert(user: User): IO[Unit] =
        upsertQuery(user).run.transact(tx).void

      override def get(id: Long): IO[Option[User]] =
        getQuery(id).option.transact(tx)
    }

  def upsertQuery(user: User): Update0 = {
    import user._
    sql"""
       INSERT INTO users (id, phone_number, first_name)
       VALUES ($id, $phoneNumber, $firstName)
       ON CONFLICT (id) DO UPDATE
       SET phone_number = $phoneNumber, first_name = $firstName
       """.update
  }

  def getQuery(id: Long): Query0[User] =
    sql"""
     SELECT id, phone_number, first_name FROM users
     WHERE id = $id
     """.query[User]
}
