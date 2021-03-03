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
import io.circe
import io.circe.Decoder
import squants.market.Currency
import sttp.client3.Response.ExampleGet.uri
import sttp.client3._
import sttp.client3.asynchttpclient.zio._
import sttp.client3.circe._
import sttp.model.Uri
import zio.{ZIO, clock}
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
      _        <- log.info(s"Sending getInstruments request: $sttpRequest")
      response <- send(sttpRequest)
      _        <- logResponse(response)
    } yield response

  def requestBuyOrder(from: Currency, to: Currency, amount: BigDecimal) =
    for {
      request <- CroPrivateRequest.createBuyRequest(from, to, amount).map(sigProvider.signRequest)
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
