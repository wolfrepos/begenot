package wolfcode

import cats.effect._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.typelevel.log4cats.slf4j.Slf4jLogger
import telegramium.bots.high._
import wolfcode.model.State
import wolfcode.repo.{OfferRepo, PendingOfferRepo, UserRepo}
import wolfcode.tg.EventHandler

object Main extends IOApp {
  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      config <- Config.create.load
      _ <- logger.info(s"loaded config: $config")
      _ <- flywayMigrate(config)
      _ <- resources(config).use {
        case (client, tx, ref) =>
          implicit val api: Api[IO] = BotApi(client, baseUrl = s"https://api.telegram.org/bot${config.token}")
          val pendingOfferRepository = PendingOfferRepo.create(tx)
          val offerRepository = OfferRepo.create(tx)
          val userRepository = UserRepo.create(tx)
          val longPollBot = new EventHandler(
            ref,
            userRepository,
            offerRepository,
            pendingOfferRepository
          )
          longPollBot.job.start >>
            longPollBot.start()
      }
    } yield ExitCode.Success
  }

  private def resources(config: Config): Resource[IO,
    (Client[IO], HikariTransactor[IO], Ref[IO, Map[Long, State]])
  ] =
    for {
      tranEc <- ExecutionContexts.cachedThreadPool[IO]
      tx <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        config.dbUrl,
        config.dbUser,
        config.dbPass,
        tranEc
      )
      // TODO:
      // on acquire load state of the draft from the db
      // on release save the draft to the db
      ref <- Resource.make(Ref.of[IO, Map[Long, State]](Map.empty)) {
        _ => IO.unit
      }
      client <- BlazeClientBuilder[IO].resource
    } yield (client, tx, ref)

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