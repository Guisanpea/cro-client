package com.crypto.response

import com.crypto.response.GetInstrumentsResult.Instruments
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}

case class GetInstrumentsResult(instruments: List[Instruments])

object GetInstrumentsResult {
  case class Instruments(
    instrumentName: String,
    quoteCurrency: String,
    baseCurrency: String,
    priceDecimals: Double,
    quantityDecimals: Double,
    marginTradingEnabled: Boolean
  )
}
