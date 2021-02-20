package com.crypto

import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}

case class CroPrivateRequest[Params](
  id: Long,
  apiKey: String,
  method: String,
  sig: String,
  nonce: Long,
  params: Option[Params]
)

object CroPrivateRequest {
  implicit def privateEncoder[Params: Encoder]: Encoder[CroPrivateRequest[Params]] = deriveConfiguredEncoder
  implicit def privateDecoder[Params: Decoder]: Decoder[CroPrivateRequest[Params]] = deriveConfiguredDecoder
}
