import sbt._

object Dependencies {
  // Private dependencies
  //lazy val brokenCommons = "guru.broken" % "common" % "1.0.0"

  // Public dependencies
  lazy val cats               = "org.typelevel" %% "cats-core"            % "2.7.0"
  lazy val catsEffect         = "org.typelevel" %% "cats-effect"          % "3.3.4"
  lazy val catsTest           = "org.typelevel" %% "munit-cats-effect-3"  % "1.0.7" % Test
  lazy val circe              = "io.circe"      %% "circe-generic"        % "0.14.1"
  lazy val circeGenericExtras = "io.circe"      %% "circe-generic-extras" % "0.14.1"
  lazy val commonsCodec       = "commons-codec"  % "commons-codec"        % "1.15"

  lazy val fs2 = "co.fs2" %% "fs2-core" % "3.2.4"

  lazy val log4cats = "org.typelevel" %% "log4cats-slf4j" % "2.2.0"

  val macwireVersion = "2.5.2"
  lazy val macwireAll = Seq(
    "com.softwaremill.macwire" %% "macros" % macwireVersion % "provided",
    "com.softwaremill.macwire" %% "proxy"  % macwireVersion
  )

  lazy val mockito = "org.mockito" %% "mockito-scala" % "1.17.0"

  lazy val munit    = "org.scalameta"      %% "munit"     % "0.7.29" % Test
  lazy val mules    = "io.chrisdavenport"  %% "mules"     % "0.5.0"
  lazy val munitZio = "com.github.poslegm" %% "munit-zio" % "0.0.3"  % Test

  lazy val prettyPrint = "com.lihaoyi" %% "pprint" % "0.7.1"

  lazy val reactor = "io.projectreactor" %% "reactor-scala-extensions" % "0.8.0"

  lazy val squants   = "org.typelevel" %% "squants"   % "1.8.3"
  lazy val scalaMock = "org.scalamock" %% "scalamock" % "5.2.0" % Test
  val sttpVersion    = "3.3.17"
  lazy val sttpAll = Seq(
    "com.softwaremill.sttp.client3" %% "core"                           % sttpVersion,
    "com.softwaremill.sttp.client3" %% "circe"                          % sttpVersion,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % sttpVersion
  )
}
