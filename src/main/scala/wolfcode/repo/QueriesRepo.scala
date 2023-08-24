package wolfcode.repo

import cats.effect.IO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor

import java.time.OffsetDateTime

trait QueriesRepo {
  def put(userId: Long, queryTime: OffsetDateTime, query: OfferRepo.Query): IO[Unit]
}

object QueriesRepo {
  def create(tx: Transactor[IO]): QueriesRepo =
    new QueriesRepo {
      override def put(userId: Long, queryTime: OffsetDateTime, query: OfferRepo.Query): IO[Unit] =
        sql.putQuery(userId, queryTime, query).run
          .transact(tx)
          .void
    }
}
