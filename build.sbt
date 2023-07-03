ThisBuild / organization := "DAPEX"

ThisBuild / version := "0.1.1"

lazy val commonSettings = Seq(
  scalaVersion := "2.13.10",
  libraryDependencies ++= Dependencies.all,
  resolvers += Resolver.githubPackages("TheDiscProg"),
  githubOwner := "TheDiscProg",
  githubRepository := "dapex-rabbitmq",
  addCompilerPlugin(
    ("org.typelevel" %% "kind-projector" % "0.13.2").cross(CrossVersion.full)
  ),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
)

lazy val rabbitmq_base = (project in file("rabbitmq_base"))
  .settings(
    commonSettings,
    name := "base",
    scalacOptions ++= Scalac.options,
    coverageExcludedPackages := Seq(
      "<empty>",
      ".*.entities.*"
    ).mkString(";")
  )

lazy val root = (project in file("."))
  .enablePlugins(
    ScalafmtPlugin
  )
  .settings(
    commonSettings,
    name := "dapex-rabbitmq",
    scalacOptions ++= Scalac.options
  )
  .aggregate(rabbitmq_base)
  .dependsOn(rabbitmq_base % "test->test; compile->compile")

githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")

addCommandAlias("clntst", ";clean;scalafmt;test:scalafmt;test;")
addCommandAlias("cvrtst", ";clean;scalafmt;test:scalafmt;coverage;test;coverageReport;")


