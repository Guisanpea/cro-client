package com.crypto

import io.circe
import sttp.client3._
import sttp.client3.asynchttpclient.zio._
import sttp.client3.circe._
import zio.logging.log

object Api {
  private val baseUrl = "https://uat-api.3ona.co/v2/"

  val getInstruments = {
    type GetInstrumentsResponse = CroResponse[GetInstrumentsResult]

    def instrumentsRequest(request: CroPublicRequest) =
      basicRequest
        .get(uri"$baseUrl/public/get-instruments")
        .body(request)
        .response(asJsonEither[CroErrorResponse, GetInstrumentsResponse])

    for {
      request <- CroPublicRequest.getInstrumentsRequest
      sttpRequest = instrumentsRequest(request)
      _        <- log.info(s"Sending getInstruments request: $sttpRequest")
      response <- send(sttpRequest)
      _        <- logResponse(response)
    } yield response
  }

  def logResponse[R](response: Response[Either[ResponseException[CroErrorResponse, circe.Error], R]]) =
    response.body match {
      case Left(HttpError(body, statusCode)) =>
        log.error(s"received http error with status code ${statusCode.code} response was $response")
      case Left(DeserializationException(body, error)) =>
        log.error(s"failed to serialize response $body with error $error, response was $response")
      case Right(value) =>
        log.info(s"received response from CRO $value, response was $response")
    }
}
