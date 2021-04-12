package faith.knowledge.croclient

import com.crypto.{CroApi, SigProvider}
import faith.knowledge.common.Money.USDT
import faith.knowledge.common.Money.USDT
import squants.market.{BTC, USD}
import sttp.client3.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import zio.clock.Clock
import zio.console.Console
import zio.logging.Logging
import zio.{ZIO, ZLayer, system}

object App extends zio.App {
  val appLayer: ZLayer[Console with Clock with Any, Throwable, Logging with SttpClient] = (
    Logging.console()
      ++ AsyncHttpClientZioBackend.layer()
  )

  override def run(args: List[String]) = {
    (for {
      api      <- produceApi
      response <- api.requestBuyOrder(from = USDT, to = BTC, amount = 100)
    } yield response)
      .provideCustomLayer(appLayer)
      .exitCode
  }

  val produceApi: ZIO[system.System, SecurityException, CroApi] = (for {
    apiKey    <- system.env("CRO_APIKEY")
    apiSecret <- system.env("CRO_APISECRET")
  } yield new SigProvider(apiKey.get, apiSecret.get))
    .map(new CroApi(_))
}
