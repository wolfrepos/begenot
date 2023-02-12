ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "begenot",
    libraryDependencies ++= List(
      "io.github.apimorphism" %% "telegramium-core" % "7.65.0",
      "io.github.apimorphism" %% "telegramium-high" % "7.65.0",
      "is.cir" %% "ciris" % "3.1.0"
    ),
    assembly / assemblyJarName := "app.jar"
  )

ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", "versions", "9", xs @ _*) => MergeStrategy.first
  case x => (ThisBuild / assemblyMergeStrategy).value(x)
}
