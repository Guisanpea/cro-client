import Dependencies._

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "Cro client",
    libraryDependencies ++= Seq(
      cats,
      circe,
      circeGenericExtras,
      commonsCodec,
      scalaTest % Test,
      squants,
      zio,
      zioLogging
    ) ++ macwireAll ++ sttpAll
  )
