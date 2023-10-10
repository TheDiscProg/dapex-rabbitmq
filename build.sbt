ThisBuild / organization := "DAPEX"

ThisBuild / version := "0.1.5"

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

lazy val root = project.in(file("."))
  .configs(IntegrationTest)
  .enablePlugins(
    ScalafmtPlugin
  )
  .settings(
    commonSettings,
    name := "dapex-rabbitmq",
    scalacOptions ++= Scalac.options,
    Defaults.itSettings
  )

githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")

addCommandAlias("formatAll", ";scalafmt;test:scalafmt;it:scalafmt")
addCommandAlias("testAll", ";clean;test;it:test")



