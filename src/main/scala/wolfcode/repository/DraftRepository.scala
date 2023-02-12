package wolfcode.repository

import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import mouse.all._
import wolfcode.model.Draft

import java.time.OffsetDateTime

trait DraftRepository {
  def put(draft: Draft): IO[Unit]
  def get(id: Int): IO[Option[Draft]]
}

object DraftRepository {
  def create(tx: Transactor[IO]): DraftRepository =
    new DraftRepository {
      override def put(draft: Draft): IO[Unit] =
        putQuery(draft)
          .run
          .transact(tx)
          .void

      override def get(ownerId: Int): IO[Option[Draft]] =
        getQuery(ownerId)
          .option
          .transact(tx)
    }

  def putQuery(draft: Draft): Update0 = {
    import draft._
    val photoIdsOpt = photoIds.nonEmpty.option(photoIds.mkString(sep))
    sql"""
       INSERT INTO drafts (owner_id, description, photo_ids, create_time)
       VALUES ($ownerId, $description, $photoIdsOpt, $createTime)
       """.update
  }

  def getQuery(ownerId: Long): Query0[Draft] =
    sql"""
       SELECT owner_id, description, photo_ids, create_time
       FROM drafts WHERE owner_id = $ownerId
       """
      .query[(Long, Option[String], Option[String], OffsetDateTime)]
      .map {
        case (ownerId, description, photoIds, createTime) =>
          Draft(ownerId, description, photoIds.fold(List.empty[String])(_.split(sep).toList), createTime)
      }

  val sep = "&"
}
