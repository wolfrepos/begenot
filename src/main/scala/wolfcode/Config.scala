package wolfcode

import cats.effect.IO
import ciris._

case class Config(token: String)

object Config {
  def create: ConfigValue[IO, Config] =
    prop("tg.token").as[String].map(Config(_))
}
