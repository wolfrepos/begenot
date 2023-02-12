ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.10"
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", "versions", "9", xs@_*) => MergeStrategy.first
  case x => (ThisBuild / assemblyMergeStrategy).value(x)
}

lazy val root = (project in file("."))
  .settings(
    name := "begenot",
    libraryDependencies ++= List(
      "com.dimafeng" %% "testcontainers-scala-core" % "0.40.12" % Test,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.40.12" % Test,
      "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.40.12" % Test,
      "io.github.apimorphism" %% "telegramium-core" % "7.65.0",
      "io.github.apimorphism" %% "telegramium-high" % "7.65.0",
      "is.cir" %% "ciris" % "3.1.0",
      "org.flywaydb" % "flyway-core" % "9.14.1",
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC2",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC2",
      "org.tpolecat" %% "doobie-scalatest" % "1.0.0-RC2" % Test,
      "org.typelevel" %% "mouse" % "1.2.1"
    ),
    assembly / assemblyJarName := "app.jar"
  )
