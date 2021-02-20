package com.crypto

import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}

case class CroResponse[Result](id: Long, method: String, code: Int, result: Result)

object CroResponse {
  implicit def encoder[R: Encoder]: Encoder[CroResponse[R]] = deriveConfiguredEncoder
  implicit def decoder[R: Decoder]: Decoder[CroResponse[R]] = deriveConfiguredDecoder
}
