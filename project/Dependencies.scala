import sbt._

object Dependencies {

  object V {
    val catsCore = "2.1.1"
    val catsEffect = "2.1.4"
    val circe = "0.13.0"
    val scalaTest = "3.2.0"
    val http4s = "0.21.7"
    val slf4j = "1.7.26"
    val logback = "1.2.3"
    val pureConfig = "0.13.0"
    val doobie = "0.8.8"
    val jsoup = "1.7.2"
    val telegramium = "2.49.0"
    val mock = "4.4.0"
    val mockTest = "3.1.0"
    val dbrush = "0.1"
  }

  val catsCore = "org.typelevel" %% "cats-core" % V.catsCore
  val catsEffect = "org.typelevel" %% "cats-effect" % V.catsEffect
  val scalaTest = "org.scalatest" %% "scalatest" % V.scalaTest % Test
  val pureConfig = "com.github.pureconfig" %% "pureconfig" % V.pureConfig
  val dbrush = "io.github.oybek" % "dbrush" % V.dbrush
  val jsoup = "org.jsoup" % "jsoup" % V.jsoup

  val doobie = Seq(
    "org.tpolecat" %% "doobie-core" % V.doobie,
    "org.tpolecat" %% "doobie-postgres" % V.doobie,
    "org.tpolecat" %% "doobie-hikari" % V.doobie,
    "org.tpolecat" %% "doobie-h2" % V.doobie,
    "org.tpolecat" %% "doobie-scalatest" % V.doobie % Test
  )

  val circe = Seq(
    "io.circe" %% "circe-core" % V.circe,
    "io.circe" %% "circe-parser" % V.circe,
    "io.circe" %% "circe-generic" % V.circe,
    "io.circe" %% "circe-generic-extras" % V.circe
  )

  val http4s = Seq(
    "org.http4s" %% "http4s-dsl" % V.http4s,
    "org.http4s" %% "http4s-circe" % V.http4s,
    "org.http4s" %% "http4s-blaze-client" % V.http4s
  )

  val telegramium = Seq(
    "io.github.apimorphism" %% "telegramium-core" % V.telegramium,
    "io.github.apimorphism" %% "telegramium-high" % V.telegramium
  )

  val mock = Seq(
    "org.scalamock" %% "scalamock" % V.mock % Test,
    "org.scalatest" %% "scalatest" % V.mockTest % Test
  )

  val testContainers = Seq(
    "com.dimafeng" %% "testcontainers-scala-core" % "0.39.0" % "test",
    "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.39.0" % "test",
    "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.39.0" % "test"
  )

  val common = Seq(catsCore, catsEffect, scalaTest, pureConfig, jsoup, dbrush) ++
    telegramium ++
    circe ++
    http4s ++
    doobie ++
    mock ++
    testContainers
}
