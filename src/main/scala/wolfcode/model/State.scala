package wolfcode.model

import cats.data.NonEmptyList

sealed trait State

object State {
  case object Idle extends State
  case object Drafting extends State
  case class Viewing(offers: NonEmptyList[Offer]) extends State
}
