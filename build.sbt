import Dependencies._
import lmcoursier.definitions.Authentication

ThisBuild / scalaVersion := "2.13.5"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "faith.knowledge"

val glGroup       = 8582200
val glGrpRegistry = s"https://gitlab.com/api/v4/groups/$glGroup/-/packages/maven"

resolvers += "gitlab" at glGrpRegistry

val auth: Authentication =
  if (sys.env.contains("CI")) Authentication(Seq(("Job-Token", sys.env("CI_JOB_TOKEN"))))
  else Authentication(Seq(("Private-Token", sys.env.getOrElse("GITLAB_TOKEN", ""))))

csrConfiguration ~= (_.addRepositoryAuthentication("gitlab", auth))
updateClassifiers / csrConfiguration ~= (_.addRepositoryAuthentication("gitlab", auth))

lazy val root = (project in file("."))
  .settings(
    name := "Cro client",
    libraryDependencies ++= Seq(
//      brokenCommons,
      cats,
      catsEffect,
      catsTest,
      circe,
      circeGenericExtras,
      commonsCodec,
      fs2,
      log4cats,
      mules,
      munit,
      prettyPrint,
      reactor,
      scalaMock,
      squants
    ) ++ macwireAll ++ sttpAll
  )
