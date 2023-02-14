package wolfcode.model

import cats.data.NonEmptyList

sealed trait State

object State {
  case object Idle extends State
  case class Drafting(draft: Draft) extends State
  case class Viewing(offers: NonEmptyList[Offer]) extends State
}
