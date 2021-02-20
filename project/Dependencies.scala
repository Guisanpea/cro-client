import sbt._

object Dependencies {

  lazy val circe = "io.circe" %% "circe-generic" % "0.13.0"
  lazy val circeGenericExtras = "io.circe" %% "circe-generic-extras" % "0.13.0"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"

  val sttpVersion = "3.1.2"
  lazy val sttp = "com.softwaremill.sttp.client3" %% "core" % sttpVersion
  lazy val sttpZio = "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion
  lazy val sttpCirce = "com.softwaremill.sttp.client3" %% "circe" % sttpVersion

  lazy val zio = "dev.zio" %% "zio" % "1.0.4-2"
  lazy val zioLogging = "dev.zio" %% "zio-logging" % "0.5.6"
}
