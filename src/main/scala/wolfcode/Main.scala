package wolfcode

import cats.effect._
import org.http4s.blaze.client.BlazeClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import telegramium.bots.high._

object Main extends IOApp {
  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      config <- Config.create.load
      _ <- logger.info(s"loaded config: $config")
      _ <- BlazeClientBuilder[IO].resource.use { httpClient =>
        implicit val api: Api[IO] = BotApi(httpClient, baseUrl = s"https://api.telegram.org/bot${config.token}")
        new LongPollGate().start()
      }
    } yield ExitCode.Success
  }
}