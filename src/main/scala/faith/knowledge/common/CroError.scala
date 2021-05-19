package faith.knowledge.common

import com.crypto.response.CroErrorResponse
import io.circe
import sttp.client3.{DeserializationException, HttpError, ResponseException}

sealed trait CroError extends Exception

object CroError {
  case object AssetNotFound
      extends IllegalArgumentException("The asset for the given operation is not available")
      with CroError

  case object NonProcessedOrder
      extends IllegalStateException("The order for the given operation hasnt been fulfilled")
      with CroError

  case object InstrumentNotExchangeableWithIntermediary
      extends IllegalArgumentException("The instrument cannot be bought or sold even with intermediary asset")
      with CroError

  case class ClientError(cause: Throwable) extends Exception(cause) with CroError

  case class ResponseSerializationError(error: circe.Error) extends Exception(error.fillInStackTrace()) with CroError

  case class CroExternalError(croError: CroErrorResponse) extends CroErrorException(croError.msg) with CroError

  def fromResponseError(response: ResponseException[CroErrorResponse, circe.Error]): CroError =
    response match {
      case httpError: HttpError[CroErrorResponse] => CroExternalError(httpError.body)
      case deserializationException: DeserializationException[circe.Error] =>
        ResponseSerializationError(deserializationException.error)
    }
}
