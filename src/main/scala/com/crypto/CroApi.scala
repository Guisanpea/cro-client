package com.crypto

import cats.effect.IO
import cats.implicits._
import com.crypto.CroApi._
import com.crypto.request.CroPrivateRequest.getAccountSummaryRequest
import com.crypto.request.Json._
import com.crypto.request.{CroPrivateRequest, CroPublicRequest, CroSignedRequest}
import com.crypto.response.Candlestick.getGrowth
import com.crypto.response.Json._
import com.crypto.response._
import io.circe.Decoder
import org.typelevel.log4cats.Logger
import scalable.market.common.Instrument
import scalable.market.common.syntax.NestedFunctorSyntax.NestedFunctorOps
import sttp.client3._
import sttp.client3.circe._
import sttp.model.Uri

import java.net.URI
import scala.concurrent.duration.DurationInt

final class CroApi(httpClient: HttpClient, logger: Logger[IO], sigProvider: SigProvider) extends CryptoComApi {

  // When you buy a currency pair from a broker, you buy the base currency and sell the quote currency.
  // for example if you buy instrument BTC_USDT with amountToSpend 10.0 you buy BTC by selling 10 USDT
  def requestBuyOrder(instrument: Instrument, amountToSpend: Double): IO[CroResponse[GetOrderDetailResult]] =
    for {
      request <-
        CroPrivateRequest.createBuyRequest(instrument, amountToSpend = amountToSpend).map(sigProvider.signRequest)
      sttpRequest = createPostRequest[CreateOrderResult](method = "private/create-order", body = request)
      _           <- logger.info(s"Sending buy request: ${pprint.apply(sttpRequest)}")
      response    <- httpClient.send(sttpRequest)
      _           <- IO.sleep(1.second)
      orderDetail <- getOrderDetail(response.result.orderId)
    } yield orderDetail

  // When you sell a currency pair from a broker, you sell the base currency and receive the quote currency.
  // for example if you sell instrument CRO_USDT with amountToSell 10.0 you get USDT by spending 10 CRO
  def requestSellOrder(instrument: Instrument, amountToSell: Double): IO[CroResponse[GetOrderDetailResult]] = {
    for {
      request <-
        CroPrivateRequest.createSellRequest(instrument, amountToSell = amountToSell).map(sigProvider.signRequest)
      sttpRequest = createPostRequest[CreateOrderResult](method = "private/create-order", body = request)
      _           <- logger.info(s"Sending buy request: ${pprint.apply(sttpRequest)}")
      response    <- httpClient.send(sttpRequest)
      _           <- IO.sleep(1.second)
      orderDetail <- getOrderDetail(response.result.orderId)
    } yield orderDetail
  }

  def getOrderDetail(orderId: String): IO[CroResponse[GetOrderDetailResult]] =
    for {
      request <- CroPrivateRequest.createGetOrderDetails(orderId).map(sigProvider.signRequest)
      sttpRequest = createPostRequest[GetOrderDetailResult](method = "private/get-order-detail", body = request)
      _        <- logger.info(s"Sending getOrder request: ${pprint.apply(sttpRequest)}")
      response <- httpClient.send(sttpRequest)
    } yield response

  val getAccountSummary: IO[List[Balance]] = {
    for {
      request <- getAccountSummaryRequest.map(sigProvider.signRequest)
      sttpRequest = createPostRequest[GetAccountSummaryResult]("private/get-account-summary", request)
      _        <- logger.info(s"Sending get-account-summary request: ${pprint.apply(sttpRequest)}")
      response <- httpClient.send(sttpRequest)
    } yield response.result.account
  }

  def getTickerForInstrument(instrument: Instrument) =
    for {
      _        <- logger.info(s"Sending getTicker for instrument ${pprint.apply(instrument)}")
      response <- httpClient.send(getInstrumentTickerRequest(instrument))
    } yield response.result.toOptionalTicker

  def getExchangeRate(instrument: Instrument): IO[Option[Double]] =
    getTickerForInstrument(instrument)
      .flatMap {
        case None         => getTickerForInstrument(instrument.opposite).map(_.map(_.inverse))
        case Some(ticker) => IO.pure(Some(ticker))
      }
      .map(_.map(_.averagePrice))

  def get24hCandlesticks(instrument: Instrument): IO[Vector[Candlestick]] =
    for {
      _        <- logger.info(s"Sending getTicker for instrument ${pprint.apply(instrument)}")
      response <- httpClient.send(getCandlestickRequest(instrument))
    } yield response.result.data

  val minutesInADay = 24 * 60 /* hours in a day */ /*minutes in an hour */
  def getTop24Instrument = {
    for {
      instruments <- getInstruments.mapEvery(_.instrumentName)
      growths     <- instruments.traverse(get24hCandlesticks).mapEvery(_.takeRight(minutesInADay)).mapEvery(getGrowth)
    } yield instruments
      .zip(growths)
      .sortBy(_._2)
      .take(24)
      .map(_._1)
  }

  val getAllTickers: IO[Vector[Ticker]] =
    for {
      _        <- logger.info(s"Sending getAllTickers request")
      response <- httpClient.send(getAllTickersRequest)
    } yield response.result.data

  val getInstruments: IO[List[InstrumentResult]] = {
    type GetInstrumentResponse = CroPublicResponse[GetInstrumentsResult]
    for {
      request <- CroPublicRequest.getInstrumentsRequest
      sttpRequest =
        createGetRequest[CroPublicRequest, GetInstrumentResponse](method = "public/get-instruments", body = request)
      _        <- logger.info(s"Sending getInstruments request: ${pprint.apply(sttpRequest)}")
      response <- httpClient.send(sttpRequest)
    } yield response.result.instruments
  }
}

object CroApi {
  val minutesInADay   = 24 * 60
  private val baseUrl = "https://api.crypto.com/v2"

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

}
