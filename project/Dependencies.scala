import sbt._

object Dependencies {

  private lazy val simexVersion = "0.9.3"
  private lazy val circeVersion = "0.14.10"
  private lazy val fs2Version = "5.2.0"
  private lazy val enumeratumVersion = "1.7.5"
  private lazy val catsEffectVersion = "3.5.4"

  lazy val all = Seq(
    "io.github.thediscprog" %% "simex-messaging" % simexVersion,
    "io.github.thediscprog" %% "slogic" % "0.3.1",
    "dev.profunktor" %% "fs2-rabbit" % fs2Version,
    "dev.profunktor" %% "fs2-rabbit-circe" % fs2Version,
    "com.beachape" %% "enumeratum" % enumeratumVersion,
    "com.beachape" %% "enumeratum-circe" % enumeratumVersion,
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-config" % "0.10.1",
    "org.typelevel" %% "cats-effect" % catsEffectVersion,
    "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
    "eu.timepit" %% "refined" % "0.11.2"
  )

  lazy val it = Seq(
    "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test,
    "ch.qos.logback" % "logback-classic" % "1.5.8" % Test,
    "com.github.blemale" %% "scaffeine" % "5.3.0" % Test,
    "org.testcontainers" % "rabbitmq" % "1.20.2" % Test
  )
}
