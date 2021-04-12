package com.crypto.response

import com.crypto.response.GetInstrumentsResult.Instruments
import faith.knowledge.common.CurrencyPair
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}

case class GetInstrumentsResult(instruments: List[Instruments])

object GetInstrumentsResult {
  case class Instruments(
    instrumentName: String,
    baseCurrency: String,
    quoteCurrency: String,
    priceDecimals: BigDecimal,
    quantityDecimals: BigDecimal,
    marginTradingEnabled: Boolean
  ) {
    def representsPair(pair: CurrencyPair) =
      this.baseCurrency == pair.base.code && this.quoteCurrency == pair.quote.code
  }
}
