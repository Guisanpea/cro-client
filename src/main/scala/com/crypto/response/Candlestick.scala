package com.crypto.response

case class Candlestick(
  endTimestamp: Long,
  openPrice: Double,
  closePrice: Double,
  lowestPrice: Double,
  highestPrice: Double,
  volume: Double
)

object Candlestick {
  def getGrowth(candlesticks: Vector[Candlestick]): Double = {
    val head = candlesticks.head
    val last = candlesticks.last
    last.closePrice / head.openPrice
  }
}
