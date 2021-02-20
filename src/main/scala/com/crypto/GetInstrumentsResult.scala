package com.crypto

import com.crypto.GetInstrumentsResult.Instruments
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}

case class GetInstrumentsResult(
    instruments: List[Instruments]
)

object GetInstrumentsResult {
  implicit val encoder: Encoder[GetInstrumentsResult] = deriveConfiguredEncoder
  implicit val decoder: Decoder[GetInstrumentsResult] = deriveConfiguredDecoder
  implicit val instrumentsEncoder: Encoder[Instruments] = deriveConfiguredEncoder
  implicit val instrumentsDecoder: Decoder[Instruments] = deriveConfiguredDecoder

  case class Instruments(
      instrumentName: String,
      quoteCurrency: String,
      baseCurrency: String,
      priceDecimals: Double,
      quantityDecimals: Double,
      marginTradingEnabled: Boolean
  )
}
