package com.crypto.request

import cats.implicits._
import com.crypto.Jsonable
import faith.knowledge.common.Instrument
import squants.market.Currency
import zio.{RIO, URIO, ZIO, clock, random}
import zio.clock.Clock
import zio.random.Random

import scala.collection.immutable.SortedMap

case class CroPrivateRequest[Params <: Jsonable](id: Long, method: String, nonce: Long, params: Option[Params]) {
  lazy val paramsMap       = params.fold(Map.empty[String, String])(_.toJsonMapSorted.toMap)
  lazy val croParamsString = params.fold("")(_.toCroParamsString)
}

object CroPrivateRequest {
  def createBuyRequest(
    pair: Instrument,
    amountToSpend: Double
  ): URIO[Clock with Random, CroPrivateRequest[CreateOrderRequestParams]] =
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
        notional = amountToSpend.some
      ).some
    )

  def createSellRequest(
    pair: Instrument,
    amountToSell: Double
  ): URIO[Clock with Random, CroPrivateRequest[CreateOrderRequestParams]] =
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
        side = "SELL",
        quantity = amountToSell.some
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

  val getAccountSummaryRequest =
    for {
      id    <- random.nextInt.map(_.abs)
      nonce <- clock.instant.map(_.toEpochMilli)
    } yield CroPrivateRequest(id = id, method = "private/get-account-summary", nonce = nonce, params = None)
}
