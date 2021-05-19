package com.crypto.response

import faith.knowledge.common.Instrument

case class GetCandlestickResult(instrumentName: Instrument, interval: String, data: Vector[Candlestick])
