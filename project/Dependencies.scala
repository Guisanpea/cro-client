import sbt._

object Dependencies {
  // Private dependencies
  //lazy val brokenCommons = "guru.broken" % "common" % "1.0.0"

  // Public dependencies
  lazy val cats               = "org.typelevel" %% "cats-core"            % "2.3.0"
  lazy val circe              = "io.circe"      %% "circe-generic"        % "0.13.0"
  lazy val circeGenericExtras = "io.circe"      %% "circe-generic-extras" % "0.13.0"
  lazy val commonsCodec       = "commons-codec"  % "commons-codec"        % "1.15"

  val macwireVersion = "2.3.7"
  lazy val macwireAll = Seq(
    "com.softwaremill.macwire" %% "macros" % macwireVersion % "provided",
    "com.softwaremill.macwire" %% "proxy"  % macwireVersion
  )

  lazy val prettyPrint = "com.lihaoyi" %% "pprint" % "0.6.4"

  lazy val reactor = "io.projectreactor" %% "reactor-scala-extensions" % "0.8.0"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2"
  lazy val squants   = "org.typelevel" %% "squants"   % "1.7.0"

  val sttpVersion = "3.1.2"
  lazy val sttpAll = Seq(
    "com.softwaremill.sttp.client3" %% "core"                          % sttpVersion,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion,
    "com.softwaremill.sttp.client3" %% "circe"                         % sttpVersion
  )

  lazy val zio        = "dev.zio" %% "zio"         % "1.0.4-2"
  lazy val zioLogging = "dev.zio" %% "zio-logging" % "0.5.6"
}
