package com.crypto

import com.crypto.CroApi.{createGetRequest, createPostRequest}
import com.crypto.request.{CroPrivateRequest, CroPublicRequest, CroSignedRequest}
import com.crypto.response.{
  CreateOrderResult,
  CroErrorResponse,
  CroResponse,
  GetInstrumentsResult,
  GetOrderDetailResult
}
import faith.knowledge.common.CurrencyPair
import io.circe
import io.circe.Decoder
import squants.market.Currency
import sttp.client3._
import sttp.client3.asynchttpclient.zio._
import sttp.client3.circe._
import sttp.model.Uri
import zio.logging.log
import zio._
import zio.duration._

import java.net.URI

class CroApi(sigProvider: SigProvider) {

  val getInstruments =
    for {
      request <- CroPublicRequest.getInstrumentsRequest
      sttpRequest =
        createGetRequest[CroPublicRequest, GetInstrumentsResult](method = "public/get-instruments", body = request)
      _            <- log.info(s"Sending getInstruments request: $sttpRequest")
      sttpResponse <- send(sttpRequest)
      _            <- logResponse(sttpResponse)
      response     <- ZIO.fromEither(sttpResponse.body)
    } yield response

  // When you buy a currency pair from a broker, you buy the base currency and sell the quote currency.
  // for example if you buy BTC_USD you but BTC by selling USD
  def requestBuyOrder(pair: CurrencyPair, amount: BigDecimal) =
    for {
      request <- CroPrivateRequest.createBuyRequest(pair, amount).map(sigProvider.signRequest)
      sttpRequest =
        createPostRequest[CroSignedRequest, CreateOrderResult](method = "private/create-order", body = request)
      _            <- log.info(s"Sending buy request: $sttpRequest")
      sttpResponse <- send(sttpRequest)
      _            <- logResponse(sttpResponse)
      response     <- ZIO.fromEither(sttpResponse.body)
      _            <- ZIO.sleep(1.second)
      orderDetail  <- getOrderDetail(response.result.orderId)
    } yield orderDetail

  def getOrderDetail(orderId: String) =
    for {
      request <- CroPrivateRequest.createGetOrderDetails(orderId).map(sigProvider.signRequest)
      sttpRequest =
        createPostRequest[CroSignedRequest, GetOrderDetailResult](method = "private/get-order-detail", body = request)
      _            <- log.info(s"Sending getOrder request: $sttpRequest")
      sttpResponse <- send(sttpRequest)
      _            <- logResponse(sttpResponse)
      response     <- ZIO.fromEither(sttpResponse.body)
    } yield response

  def logResponse[R](response: Response[Either[ResponseException[CroErrorResponse, circe.Error], R]]) =
    response.body match {
      case Left(HttpError(_, statusCode)) =>
        log.error(s"received http error with status code ${statusCode.code} response was $response")
      case Left(DeserializationException(body, error)) =>
        log.error(s"failed to serialize response $body with error $error, response was $response")
      case Right(value) =>
        log.info(s"received response from CRO $value, response was $response")
    }

  def getExchangeRate(pair: CurrencyPair) = {
    def findOppositePairPrice(instruments: List[GetInstrumentsResult.Instruments]) = {
      instruments
        .find(_.representsPair(pair.oppositePair))
        .map(_.priceDecimals)
        .map(1 / _)
    }

    getInstruments.map(_.instruments) map { instruments =>
      instruments
        .find(_.representsPair(pair))
        .map(_.priceDecimals)
        .orElse(findOppositePairPrice(instruments))
    } cached ()
  }
}

object CroApi {
  private val baseUrl = "https://api.crypto.com/v2"
  def createGetRequest[Request: BodySerializer, Response: Decoder](method: String, body: Request) =
    basicRequest
      .get(Uri(URI.create(s"$baseUrl/$method")))
      .body(body)
      .response(asJsonEither[CroErrorResponse, Response])

  def createPostRequest[Request: BodySerializer, Result: Decoder](method: String, body: Request) =
    basicRequest
      .post(Uri(URI.create(s"$baseUrl/$method")))
      .body(body)
      .response(asJsonEither[CroErrorResponse, CroResponse[Result]])
}
