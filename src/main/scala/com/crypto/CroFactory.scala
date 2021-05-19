package com.crypto

import com.crypto.CroApi.{getAllTickers, getAllTickersRequest, getInstruments}
import com.crypto.response.{Candlestick, GetInstrumentsResult}
import faith.knowledge.common.{CroError, Instrument}
import faith.knowledge.common.SttpLogger.logCirceResponse
import sttp.client3.asynchttpclient.zio.send
import zio.{Task, _}
import zio.duration._
import zio.logging.log
import zio.stm.TMap

object CroFactory {
  def createCroApi(sigProvider: SigProvider) =
    for {
      cachedTickers     <- getAllTickers.cached(2.seconds)
      cachedInstruments <- getInstruments.cached(24.hours)
      candlestickCaches <- TMap.empty[Instrument, IO[CroError, Vector[Candlestick]]].commit
    } yield new CroApi(sigProvider, cachedInstruments, cachedTickers, candlestickCaches)
}
