package wolfcode.repo

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import doobie.util.transactor.Transactor
import org.scalatest.funsuite.AnyFunSuite
import org.testcontainers.utility.DockerImageName
import wolfcode.{Config, Main}

trait PostgresSetup extends ForAllTestContainer {
  this: AnyFunSuite =>

  override val container: PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:10.10"))

  lazy val transactor: Transactor.Aux[IO, Unit] =
    Transactor
      .fromDriverManager[IO](
        container.driverClassName,
        container.jdbcUrl,
        container.username,
        container.password
      )

  override def afterStart(): Unit =
    Main.flywayMigrate(
      Config("", container.jdbcUrl, container.username, container.password)
    ).unsafeRunSync()
}
