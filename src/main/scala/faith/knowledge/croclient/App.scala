package faith.knowledge.croclient

import com.crypto.{CroApi, CroFactory, SigProvider}
import faith.knowledge.common.Currencies.{CRO, USDT}
import faith.knowledge.common.Instrument
import squants.market.{BTC, USD}
import sttp.client3.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import zio.clock.Clock
import zio.console.Console
import zio.logging.{Logging, log}
import zio._
import zio.duration._

import java.awt.AttributeValue

object App extends zio.App {
  val appLayer: ZLayer[Console with Clock with Any, Throwable, Logging with SttpClient] = (
    Logging.console()
      ++ AsyncHttpClientZioBackend.layer()
  )

  override def run(args: List[String]) = {
    (for {
      api      <- produceApi
      response <- api.get24hCandlesticks(Instrument(BTC, USDT))
      _        <- console.putStrLn(response.last.toString)
    } yield response.last)
      .provideCustomLayer(appLayer)
      .exitCode
  }

  val produceApi = (for {
    apiKey    <- system.env("CRO_APIKEY")
    apiSecret <- system.env("CRO_APISECRET")
  } yield new SigProvider(apiKey.get, apiSecret.get))
    .flatMap(CroFactory.createCroApi)
}
