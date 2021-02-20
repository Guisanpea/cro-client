package knowledge.faith.croclient

import com.crypto.Api
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.logging.Logging

object App extends zio.App {
  val appLayer = (
    Logging.console()
      ++ AsyncHttpClientZioBackend.layer()
  )

  override def run(args: List[String]) =
    (for {
      response <- Api.getInstruments
    } yield response)
      .provideCustomLayer(appLayer)
      .exitCode
}
