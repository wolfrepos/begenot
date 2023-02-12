ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "begenot",
    libraryDependencies ++= List(
      "io.github.apimorphism" %% "telegramium-core" % "7.65.0",
      "io.github.apimorphism" %% "telegramium-high" % "7.65.0",
      "is.cir" %% "ciris" % "3.1.0",
      "org.typelevel" %% "log4cats-slf4j" % "2.5.0"
    )
  )
