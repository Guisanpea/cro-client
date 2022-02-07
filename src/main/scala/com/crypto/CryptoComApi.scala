package com.crypto

import cats.effect.IO
import com.crypto.response._
import scalable.market.common.Instrument

trait CryptoComApi {
  // When you buy a currency pair from a broker, you buy the base currency and sell the quote currency.
  // for example if you buy instrument BTC_USDT with amountToSpend 10.0 you buy BTC by selling 10 USDT
  def requestBuyOrder(instrument: Instrument, amountToSpend: Double): IO[CroResponse[GetOrderDetailResult]]

  // When you sell a currency pair from a broker, you sell the base currency and receive the quote currency.
  // for example if you sell instrument CRO_USDT with amountToSell 10.0 you get USDT by spending 10 CRO
  def requestSellOrder(instrument: Instrument, amountToSell: Double): IO[CroResponse[GetOrderDetailResult]]

  def getOrderDetail(orderId: String): IO[CroResponse[GetOrderDetailResult]]

  val getAccountSummary: IO[List[Balance]]

  def getTickerForInstrument(instrument: Instrument): IO[Option[Ticker]]

  def getExchangeRate(instrument: Instrument): IO[Option[Double]]
}
