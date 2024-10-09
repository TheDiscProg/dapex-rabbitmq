import sbt.librarymanagement.CrossVersion

lazy val scala2 = "2.13.14"
lazy val scala3 = "3.5.1"
lazy val supportedScalaVersions = List(scala2, scala3)


ThisBuild / organization := "simex"

ThisBuild / version := "0.9.0"

lazy val commonSettings = Seq(
  scalaVersion := scala3,
  libraryDependencies ++= Dependencies.all,
  githubOwner := "TheDiscProg",
  githubRepository := "simex-rabbitmq"
)

lazy val root = project.in(file("."))
  .enablePlugins(
    ScalafmtPlugin
  )
  .settings(
    commonSettings,
    name := "simex-rabbitmq",
    scalacOptions ++=Scalac.options,
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2,13)) => Seq("-Ytasty-reader")
        case _ => Seq("-Yretain-trees")
      }
    },
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq(
      ("com.github.pureconfig" %% "pureconfig" % "0.17.7").cross(CrossVersion.for3Use2_13)
    )
  )

lazy val integrationTest = (project in file("it"))
  .enablePlugins(ScalafmtPlugin)
  .settings(
    commonSettings,
    name := "simex-rabbitmq-integration-test",
    publish / skip := true,
    libraryDependencies ++= Dependencies.it,
    libraryDependencies ++= Seq(
      ("com.dimafeng" %% "testcontainers-scala-scalatest" % "0.41.4").cross(CrossVersion.for3Use2_13)
    ),
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("2"))
        Seq(
          compilerPlugin(("org.typelevel" %% "kind-projector" % "0.13.3").cross(CrossVersion.full)),
          compilerPlugin(("com.olegpy" %% "better-monadic-for" % "0.3.1")),
        )
      else
        Seq()
    },
    parallelExecution := false,
    coverageFailOnMinimum := true,
    coverageMinimumStmtTotal := 85,
    coverageMinimumBranchTotal := 80,
  )
  .dependsOn(root % "test->test; compile->compile")
  .aggregate(root)

githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")

addCommandAlias("formatAll", ";scalafmt;test:scalafmt;integrationTest/test:scalafmt;")
addCommandAlias("cleanAll", ";clean;integrationTest/clean")
addCommandAlias("itTest", ";integrationTest/clean;integrationTest/test:scalafmt;integrationTest/test")
addCommandAlias("cleanTest", ";clean;scalafmt;test:scalafmt;test;")
addCommandAlias("testAll", ";cleanAll;formatAll;test;itTest;")
addCommandAlias("cleanCoverage", ";cleanAll;formatAll;coverage;testAll;coverageReport;")
