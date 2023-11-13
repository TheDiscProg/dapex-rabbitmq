import sbt._

object Dependencies {

  lazy val all = Seq(
    "simex" %% "simex-messaging" % "0.6.0",
    "dev.profunktor" %% "fs2-rabbit" % "5.0.0",
    "dev.profunktor" %% "fs2-rabbit-circe" % "5.0.0",
    "com.beachape" %% "enumeratum" % "1.7.2",
    "com.beachape" %% "enumeratum-circe" % "1.7.2",
    "io.circe" %% "circe-core" % "0.14.5",
    "io.circe" %% "circe-generic" % "0.14.5",
    "io.circe" %% "circe-parser" % "0.14.5",
    "io.circe" %% "circe-refined" % "0.14.5",
    "io.circe" %% "circe-generic-extras" % "0.14.3",
    "io.circe" %% "circe-config" % "0.10.0",
    "org.typelevel" %% "cats-effect" % "3.4.8",
    "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
    "eu.timepit" %% "refined" % "0.11.0"
  )

  lazy val it = Seq(
    "org.scalatest" %% "scalatest" % "3.2.15" % Test,
    "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test,
    "ch.qos.logback" % "logback-classic" % "1.4.11" % Test,
    "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.41.0" % Test,
    "com.github.blemale" %% "scaffeine" % "5.2.1" % Test,
    "org.testcontainers" % "rabbitmq" % "1.19.1" % Test
  )
}
