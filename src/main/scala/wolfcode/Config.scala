package wolfcode

import cats.effect.IO
import cats.implicits.catsSyntaxTuple4Semigroupal
import ciris._

case class Config(token: String,
                  dbUrl: String,
                  dbUser: String,
                  dbPass: String)

object Config {
  def create: ConfigValue[IO, Config] = (
    prop("tg.token").as[String],
    prop("db.url").as[String],
    prop("db.user").as[String],
    prop("db.pass").as[String],
  ).mapN(Config(_, _, _, _))
}
