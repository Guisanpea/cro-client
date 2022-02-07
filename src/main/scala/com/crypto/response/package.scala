package com.crypto

import scalable.market.common.Instrument
import squants.market.Currency

package object response {
  case class CreateOrderResult(orderId: String, clientOid: Option[String])
  case class CroErrorResponse(code: Int, msg: String)
  case class CroPublicResponse[Result](method: String, result: Result)
  case class CroResponse[Result](id: Long, method: String, code: Int, result: Result)
  case class GetAccountSummaryResult(account: List[Balance])
  case class GetAllTickersResult(data: Vector[Ticker])
  case class GetCandlestickResult(instrumentName: Instrument, interval: String, data: Vector[Candlestick])
  case class GetInstrumentsResult(instruments: List[InstrumentResult])
  case class GetOrderDetailResult(tradeList: List[TradeList], orderInfo: OrderInfo)

  case class Balance(
    balance: BigDecimal,
    available: BigDecimal,
    order: BigDecimal,
    stake: BigDecimal,
    currency: Currency
  )

  case class InstrumentResult(
    instrumentName: Instrument,
    baseCurrency: Currency,
    quoteCurrency: Currency,
    // number of decimal places the price can have
    priceDecimals: Int,
    // number of decimal places the quantity can have
    quantityDecimals: Int,
    marginTradingEnabled: Boolean
  )

  case class TradeList(
    side: String,
    instrumentName: String,
    fee: Double,
    tradeId: String,
    createTime: Long,
    tradedPrice: Double,
    tradedQuantity: Double,
    feeCurrency: String,
    orderId: String
  )

  case class OrderInfo(
    status: String,
    reason: Option[Int],
    side: String,
    price: Option[Double],
    quantity: Option[Double],
    orderId: String,
    clientOid: Option[String],
    createTime: Long,
    updateTime: Long,
    `type`: String,
    instrumentName: String,
    cumulativeQuantity: Option[Double],
    cumulativeValue: Option[Double],
    avgPrice: Double,
    feeCurrency: String,
    timeInForce: String,
    execInst: String
  )
}
