package wolfcode.repository

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import wolfcode.model.Draft

import java.time.OffsetDateTime

trait DraftRepository {
  def put(offer: Draft): IO[Unit]
  def getOldest: IO[Option[Draft]]
}

object DraftRepository {
  def create(tx: Transactor[IO]): DraftRepository =
    new DraftRepository {
      override def put(offer: Draft): IO[Unit] =
        putQuery(offer)
          .run
          .transact(tx)
          .void

      override def getOldest: IO[Option[Draft]] =
        getOldestQuery
          .option
          .transact(tx)
    }

  def putQuery(offer: Draft): Update0 = {
    import offer._
    sql"""
       INSERT INTO drafts (description, photo_ids, create_time, owner_id)
       VALUES ($description, ${photoIds.mkString(sep)}, $createTime, $ownerId)
       """.update
  }

  val getOldestQuery: Query0[Draft] =
    sql"""
       SELECT id, description, photo_ids, create_time, owner_id
       FROM drafts ORDER BY create_time ASC LIMIT 1
       """
      .query[(Int, String, String, OffsetDateTime, Long)]
      .map {
        case (id, description, photoIds, createTime, ownerId) =>
          Draft(id, ownerId, description.some, photoIds.split(sep).toList, createTime)
      }

  val sep = "&"
}
