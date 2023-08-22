package wolfcode.model

import doobie.Meta

case class PhotoIds(ids: List[String])

object PhotoIds {
  implicit val meta: Meta[PhotoIds] = Meta[String].imap { s =>
    PhotoIds(s.split(sep).toList)
  } { photoIds =>
    photoIds.ids.mkString(sep)
  }
  val sep = "&"
}
