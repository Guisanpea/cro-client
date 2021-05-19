package com.crypto

import com.crypto.CroApi._
import com.crypto.CroSttp.send
import com.crypto.request.CroPrivateRequest.getAccountSummaryRequest
import com.crypto.request.{CroPrivateRequest, CroPublicRequest, CroSignedRequest}
import com.crypto.response.Candlestick.getGrowth
import com.crypto.response.{
  Candlestick,
  CreateOrderResult,
  CroErrorResponse,
  CroPublicResponse,
  CroResponse,
  GetAccountSummaryResult,
  GetAllTickersResult,
  GetCandlestickResult,
  GetInstrumentsResult,
  GetOrderDetailResult,
  GetTickerResult,
  Ticker
}
import faith.knowledge.common.syntax.ZioSyntax._
import faith.knowledge.common.{CroError, IncongruentStateException, Instrument}
import io.circe.Decoder
import sttp.client3._
import sttp.client3.asynchttpclient.zio.SttpClient
import sttp.client3.circe._
import sttp.model.Uri
import zio._
import zio.clock.Clock
import zio.duration._
import zio.logging.{Logging, log}
import zio.random.Random
import zio.stm.{STM, TMap}

import java.net.URI

class CroApi(
  sigProvider: SigProvider,
  cachedGetInstruments: IO[CroError, List[GetInstrumentsResult.InstrumentResult]],
  cachedGetAllTickers: IO[CroError, Vector[Ticker]],
  candlesticksCaches: TMap[Instrument, IO[CroError, Vector[Candlestick]]]
) {

  val getInstruments = cachedGetInstruments
  val getAllTickers  = cachedGetAllTickers
  val getCandlestickCacheMap: STM[Nothing, TMap[Instrument, IO[Throwable, Vector[Candlestick]]]] =
    TMap.empty[Instrument, IO[Throwable, Vector[Candlestick]]]

  // When you buy a currency pair from a broker, you buy the base currency and sell the quote currency.
  // for example if you buy instrument BTC_USDT with amountToSpend 10.0 you buy BTC by selling 10 USDT
  def requestBuyOrder(
    instrument: Instrument,
    amountToSpend: Double
  ): ZIO[HttpEnv, CroError, CroResponse[GetOrderDetailResult]] =
    for {
      request <-
        CroPrivateRequest.createBuyRequest(instrument, amountToSpend = amountToSpend).map(sigProvider.signRequest)
      sttpRequest = createPostRequest[CreateOrderResult](method = "private/create-order", body = request)
      _           <- log.info(s"Sending buy request: ${pprint.apply(sttpRequest)}")
      response    <- send(sttpRequest)
      _           <- ZIO.sleep(1.second)
      orderDetail <- getOrderDetail(response.result.orderId)
    } yield orderDetail

  // When you sell a currency pair from a broker, you sell the base currency and receive the quote currency.
  // for example if you sell instrument CRO_USDT with amountToSell 10.0 you get USDT by spending 10 CRO
  def requestSellOrder(instrument: Instrument, amountToSell: Double) = {
    for {
      request <-
        CroPrivateRequest.createSellRequest(instrument, amountToSell = amountToSell).map(sigProvider.signRequest)
      sttpRequest = createPostRequest[CreateOrderResult](method = "private/create-order", body = request)
      _           <- log.info(s"Sending buy request: ${pprint.apply(sttpRequest)}")
      response    <- send(sttpRequest)
      _           <- ZIO.sleep(1.second)
      orderDetail <- getOrderDetail(response.result.orderId)
    } yield orderDetail
  }

  def getOrderDetail(orderId: String): ZIO[HttpEnv, CroError, CroResponse[GetOrderDetailResult]] =
    for {
      request <- CroPrivateRequest.createGetOrderDetails(orderId).map(sigProvider.signRequest)
      sttpRequest = createPostRequest[GetOrderDetailResult](method = "private/get-order-detail", body = request)
      _        <- log.info(s"Sending getOrder request: ${pprint.apply(sttpRequest)}")
      response <- send(sttpRequest)
    } yield response

  val getAccountSummary = {
    for {
      request <- getAccountSummaryRequest.map(sigProvider.signRequest)
      sttpRequest = createPostRequest[GetAccountSummaryResult]("private/get-account-summary", request)
      _        <- log.info(s"Sending get-account-summary request: ${pprint.apply(sttpRequest)}")
      response <- send(sttpRequest)
    } yield response.result.account
  }

  def getTickerForInstrument(instrument: Instrument) =
    for {
      _        <- log.info(s"Sending getTicker for instrument ${pprint.apply(instrument)}")
      response <- send(getInstrumentTickerRequest(instrument))
    } yield response.result.toOptionalTicker

  def getExchangeRate(instrument: Instrument): ZIO[HttpEnv, CroError, Option[Double]] =
    getTickerForInstrument(instrument)
      .flatMap {
        case None         => getTickerForInstrument(instrument.opposite).mapEvery(_.inverse)
        case Some(ticker) => ZIO.succeed(Some(ticker))
      }
      .mapEvery(_.averagePrice)

  def get24hCandlesticks(instrument: Instrument): ZIO[HttpEnv, CroError, Vector[Candlestick]] = {
    def get24hCandlestickCall(instrument: Instrument): ZIO[Logging with SttpClient, CroError, Vector[Candlestick]] =
      for {
        _        <- log.info(s"Sending getTicker for instrument ${pprint.apply(instrument)}")
        response <- send(getCandlestickRequest(instrument))
      } yield response.result.data

    def getCachedCandlestickFromMap(
      cachedF: IO[CroError, Vector[Candlestick]]
    ): UIO[IO[CroError, Vector[Candlestick]]] = {
      (for {
        _ <- candlesticksCaches.putIfAbsent(instrument, cachedF)
        f <- candlesticksCaches.getOrElse(instrument, throw IncongruentStateException)
      } yield f).commit
    }

    for {
      cachedFunction  <- get24hCandlestickCall(instrument).cached(1.minute)
      functionFromMap <- getCachedCandlestickFromMap(cachedFunction)
      function        <- functionFromMap
    } yield function

  }

  def getTop24Instrument = {
    for {
      instruments <- getInstruments.mapEvery(_.instrumentName)
      growths     <- ZIO.foreach(instruments)(get24hCandlesticks).mapEvery(_.takeRight(minInADay)).mapEvery(getGrowth)
    } yield instruments
      .zip(growths)
      .sortBy(_._2)
      .take(24)
      .map(_._1)
  }

}

object CroApi {
  val minInADay       = 24 * 60
  private val baseUrl = "https://api.crypto.com/v2"
  type HttpEnv = Clock with Logging with SttpClient with Random

  def createGetRequest[Request: BodySerializer, Response: Decoder](method: String, body: Request) =
    basicRequest
      .get(Uri(URI.create(s"$baseUrl/$method")))
      .body(body)
      .response(asJsonEither[CroErrorResponse, Response])

  def createPostRequest[Result: Decoder](method: String, body: CroSignedRequest) =
    basicRequest
      .post(Uri(URI.create(s"$baseUrl/$method")))
      .body(body)
      .response(asJsonEither[CroErrorResponse, CroResponse[Result]])

  val getAllTickersRequest =
    basicRequest
      .get(Uri(URI.create(s"$baseUrl/public/get-ticker")))
      .response(asJsonEither[CroErrorResponse, CroPublicResponse[GetAllTickersResult]])

  def getInstrumentTickerRequest(instrument: Instrument) =
    basicRequest
      .get(Uri(URI.create(s"$baseUrl/public/get-ticker?instrument_name=${instrument.instrumentName}")))
      .response(asJsonEither[CroErrorResponse, CroPublicResponse[GetTickerResult]])

  def getCandlestickRequest(instrument: Instrument) = {
    val uri = URI.create(s"$baseUrl/public/get-candlestick?instrument_name=${instrument.instrumentName}&timeframe=1D")
    basicRequest
      .get(Uri(uri))
      .response(asJsonEither[CroErrorResponse, CroPublicResponse[GetCandlestickResult]])
  }

  val getAllTickers: ZIO[HttpEnv, CroError, Vector[Ticker]] =
    for {
      _        <- log.info(s"Sending getAllTickers request")
      response <- send(getAllTickersRequest)
    } yield response.result.data

  val getInstruments: ZIO[HttpEnv, CroError, List[GetInstrumentsResult.InstrumentResult]] = {
    type GetInstrumentResponse = CroPublicResponse[GetInstrumentsResult]
    for {
      request <- CroPublicRequest.getInstrumentsRequest
      sttpRequest =
        createGetRequest[CroPublicRequest, GetInstrumentResponse](method = "public/get-instruments", body = request)
      _        <- log.info(s"Sending getInstruments request: ${pprint.apply(sttpRequest)}")
      response <- send(sttpRequest)
    } yield response.result.instruments
  }

}
