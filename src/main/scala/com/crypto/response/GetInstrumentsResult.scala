package com.crypto.response

import com.crypto.response.GetInstrumentsResult.InstrumentResult
import faith.knowledge.common.Instrument
import squants.market.Currency

case class GetInstrumentsResult(instruments: List[InstrumentResult])

object GetInstrumentsResult {
  case class InstrumentResult(
    instrumentName: Instrument,
    baseCurrency: Currency,
    quoteCurrency: Currency,
    priceDecimals: Double,
    quantityDecimals: Double,
    marginTradingEnabled: Boolean
  )
}
