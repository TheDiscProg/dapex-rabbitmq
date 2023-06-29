import sbt._

object Dependencies {

  lazy val all = Seq(
    "DAPEX" % "dapex-messaging_2.13" % "0.1.5",
    "Shareprice" %% "shareprice-config" % "0.1.2",
    "dev.profunktor" %% "fs2-rabbit" % "5.0.0",
    "dev.profunktor" %% "fs2-rabbit-circe" % "5.0.0",
    "com.beachape" %% "enumeratum" % "1.7.2",
    "com.beachape" %% "enumeratum-circe" % "1.7.2",
    "io.circe" %% "circe-core" % "0.14.5",
    "io.circe" %% "circe-generic" % "0.14.5",
    "io.circe" %% "circe-parser" % "0.14.5",
    "io.circe" %% "circe-refined" % "0.14.5",
    "io.circe" %% "circe-generic-extras" % "0.14.3",
    "org.typelevel" %% "cats-effect" % "3.4.8",
    "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
    "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test
  )
}
