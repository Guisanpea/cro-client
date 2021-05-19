package faith.knowledge.common

import io.circe
import cats.implicits._
import sttp.client3.{DeserializationException, HttpError, Response, ResponseException}
import zio.logging.log

object SttpLogger {
  // format: off
  def logCirceResponse[Error, Result](response: Response[Either[
    ResponseException[Error, circe.Error],
    Result
  ]]) =
  // format: on
  response.body match {
    case Left(HttpError(body, statusCode)) =>
      log.error(s"""received http error with status code ${statusCode.code},
                   |body ${pprint.apply(body)}""".stripMargin)
    case Left(DeserializationException(body, error)) =>
      log.error(s"""failed to serialize response ${body.replace("\\\"", "\"")},
                   |with error ${pprint.apply(error.show)}""".stripMargin)
    case Right(value) =>
      log.info(s"received success response from CRO ${pprint.apply(value)}")
  }
}
