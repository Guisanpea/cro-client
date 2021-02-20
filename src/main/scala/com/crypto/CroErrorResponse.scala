package com.crypto

import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}

case class CroErrorResponse(code: Int, msg: String)

object CroErrorResponse {
  implicit val encoder: Encoder[CroErrorResponse] = deriveConfiguredEncoder
  implicit val decoder: Decoder[CroErrorResponse] = deriveConfiguredDecoder
}
