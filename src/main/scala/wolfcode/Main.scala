package wolfcode

import cats.effect._
import org.flywaydb.core.Flyway
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import telegramium.bots.high._
import wolfcode.model.State

object Main extends IOApp {
  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      config <- Config.create.load
      _ <- logger.info(s"loaded config: $config")
      _ <- flywayMigrate(config)
      ref <- Ref.of[IO, Map[Long, State]](Map.empty)
      _ <- BlazeClientBuilder[IO].resource.use { httpClient =>
        implicit val api: Api[IO] = BotApi(httpClient, baseUrl = s"https://api.telegram.org/bot${config.token}")
        new LongPollGate(ref).start()
      }
    } yield ExitCode.Success
  }

  def flywayMigrate(config: Config): IO[Unit] =
    IO {
      import config._
      Flyway
        .configure()
        .dataSource(dbUrl, dbUser, dbPass)
        .baselineOnMigrate(true)
        .load()
        .migrate()
    }
}