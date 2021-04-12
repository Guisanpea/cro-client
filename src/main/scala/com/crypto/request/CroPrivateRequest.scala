package com.crypto.request

import cats.implicits._
import com.crypto.Jsonable
import faith.knowledge.common.CurrencyPair
import squants.market.Currency
import zio.{ZIO, clock, random}
import zio.clock.Clock
import zio.random.Random

import scala.collection.immutable.SortedMap

case class CroPrivateRequest[Params <: Jsonable](id: Long, method: String, nonce: Long, params: Option[Params]) {
  lazy val paramsMap       = params.fold(Map.empty[String, String])(_.toJsonMapSorted.toMap)
  lazy val croParamsString = params.fold("")(_.toCroParamsString)
}

object CroPrivateRequest {
  def createBuyRequest(pair: CurrencyPair, amount: BigDecimal) =
    for {
      id    <- random.nextInt.map(_.abs)
      nonce <- clock.instant.map(_.toEpochMilli)
    } yield CroPrivateRequest(
      id = id,
      method = "private/create-order",
      nonce = nonce,
      params = CreateOrderRequestParams(
        instrumentName = s"${pair.instrumentName}",
        `type` = "MARKET",
        side = "BUY",
        notional = amount.some
      ).some
    )

  def createGetOrderDetails(orderId: String) =
    for {
      id    <- random.nextInt.map(_.abs)
      nonce <- clock.instant.map(_.toEpochMilli)
    } yield CroPrivateRequest(
      id = id,
      method = "private/get-order-detail",
      nonce = nonce,
      params = GetOrderDetailParams(orderId).some
    )
}
