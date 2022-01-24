package faith.knowledge.common

import cats.effect.IO
import cats.implicits._
import io.circe.{Error => SerializationError}
import org.typelevel.log4cats.Logger
import sttp.client3.{DeserializationException, HttpError, Response, ResponseException}

class SttpLogger(logger: Logger[IO]) {
  // format: off
  def logCirceResponse[HttpError, Result](response: Response[Either[
    ResponseException[HttpError, SerializationError],
    Result
  ]]) =
  // format: on
  response.body match {
    case Left(HttpError(body, statusCode)) =>
      logger.error(s"""received http error with status code ${statusCode.code},
                      |body ${pprint.apply(body)}""".stripMargin)
    case Left(DeserializationException(body, error)) =>
      logger.error(s"""failed to serialize response ${body.replace("\\\"", "\"")},
                      |with error ${pprint.apply(error.show)}""".stripMargin)
    case Right(value) =>
      logger.info(s"received success response from CRO ${pprint.apply(value)}")
  }
}
