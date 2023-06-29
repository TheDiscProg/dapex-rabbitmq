ThisBuild / organization := "DAPEX"

ThisBuild / version := "0.1.0"

lazy val commonSettings = Seq(
  scalaVersion := "2.13.10",
  libraryDependencies ++= Dependencies.all,
  resolvers += Resolver.githubPackages("TheDiscProg"),
  githubOwner := "TheDiscProg",
  githubRepository := "dapex-rabbitmq",
  githubTokenSource := TokenSource.Environment("REPO_READ"),
  addCompilerPlugin(
    ("org.typelevel" %% "kind-projector" % "0.13.2").cross(CrossVersion.full)
  ),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
)

lazy val base = (project in file("base"))
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
  .dependsOn(base % "test->test; compile->compile")

addCommandAlias("clntst", ";clean;scalafmt;test:scalafmt;test;")
addCommandAlias("cvrtst", ";clean;scalafmt;test:scalafmt;coverage;test;coverageReport;")


