package com.crypto.response

import com.crypto.response.GetOrderDetailResult.{OrderInfo, TradeList}

case class GetOrderDetailResult(tradeList: List[TradeList], orderInfo: OrderInfo)

object GetOrderDetailResult {
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
