package com.crypto

import com.crypto.response.CroErrorResponse
import faith.knowledge.common.CroError
import faith.knowledge.common.SttpLogger.logCirceResponse
import io.circe
import sttp.capabilities.{Effect, WebSockets}
import sttp.capabilities.zio.ZioStreams
import sttp.client3.{Request, Response, ResponseException}
import sttp.client3.asynchttpclient.zio.{SttpClient, send => sttpSend}
import zio.logging.Logging
import zio.{Task, ZIO, logging}

object CroSttp {
  type SttpResponse[Result] = Either[ResponseException[CroErrorResponse, circe.Error], Result]

  def send[Result](
    request: Request[SttpResponse[Result], Effect[Task] with ZioStreams with WebSockets]
  ): ZIO[Logging with SttpClient, CroError, Result] =
    for {
      sttpResponse <- sttpSend(request).mapError[CroError](CroError.ClientError.apply)
      _            <- logCirceResponse(sttpResponse)
      response     <- ZIO.fromEither(sttpResponse.body).mapError(CroError.fromResponseError)
    } yield response
}
