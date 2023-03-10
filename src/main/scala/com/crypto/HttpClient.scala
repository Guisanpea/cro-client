package com.crypto

import cats.effect.IO
import com.crypto.response.CroErrorResponse
import io.circe.{Error => SerializationError}
import scalable.market.broker.cryptocom.CroError
import scalable.market.common.SttpLogger
import scalable.market.common.syntax.ChainingSyntax.scalaUtilChainingOps
import sttp.capabilities.Effect
import sttp.client3.{Request, ResponseException, SttpBackend}

class HttpClient(sttpClient: SttpBackend[IO, Any], sttpLogger: SttpLogger) {
  type SttpError            = ResponseException[CroErrorResponse, SerializationError]
  type SttpResponse[Result] = Either[SttpError, Result]

  def send[Result](request: Request[SttpResponse[Result], Effect[IO]]): IO[Result] =
    for {
      sttpResponse <- sttpClient.send(request)
      _            <- sttpLogger.logCirceResponse(sttpResponse)
      response <- sttpResponse.body match {
        case Left(error) =>
          CroError.fromResponseError(error) |> IO.raiseError
        case Right(value) => IO.pure(value)
      }
    } yield response
}
