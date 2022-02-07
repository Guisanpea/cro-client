package com.crypto.response

import scalable.market.common.Instrument

case class Ticker(
  instrument: Instrument,
  bid: Double,
  ask: Double,
  lastTrade: Double,
  timestampMillis: Long,
  volume24h: Double,
  highest24h: Double,
  lowest24h: Double,
  change24h: Double
) {
  def inverse =
    Ticker(
      instrument = instrument.opposite,
      bid = 1.0 / bid,
      ask = 1.0 / ask,
      lastTrade = 1.0 / lastTrade,
      timestampMillis = timestampMillis,
      volume24h = volume24h * (1 / averagePrice), // this is an approximation
      lowest24h = 1.0 / highest24h,
      highest24h = 1.0 / lowest24h,
      change24h = (-change24h) * (1 / averagePrice) // this is an approximation
    )

  def averagePrice = (bid + ask) / 2
}
