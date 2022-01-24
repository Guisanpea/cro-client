package com.crypto.request

import cats.effect.std.Random
import cats.effect.{Clock, IO}
import cats.implicits._
import com.crypto.request.Json._
import faith.knowledge.common.Instrument
import io.circe.Encoder
import io.circe.Encoder.encodeNone

case class CroPrivateRequest[Params: Encoder](id: Long, method: String, nonce: Long, params: Option[Params]) {
  lazy val paramsMap       = params.fold(Map.empty[String, String])(convertToJsonMapSorted)
  lazy val croParamsString = params.fold("")(toCroParamsString)
}

object CroPrivateRequest {
  def createBuyRequest(pair: Instrument, amountToSpend: Double): IO[CroPrivateRequest[CreateOrderRequestParams]] =
    for {
      id    <- Random.scalaUtilRandom[IO].map(_.betweenLong(0, Long.MaxValue)).flatten
      nonce <- Clock[IO].realTime.map(_.toMillis)
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

  def createSellRequest(pair: Instrument, amountToSell: Double): IO[CroPrivateRequest[CreateOrderRequestParams]] =
    for {
      id    <- Random.scalaUtilRandom[IO].map(_.betweenLong(0, Long.MaxValue)).flatten
      nonce <- Clock[IO].realTime.map(_.toMillis)
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
      id    <- Random.scalaUtilRandom[IO].map(_.betweenLong(0, Long.MaxValue)).flatten
      nonce <- Clock[IO].realTime.map(_.toMillis)
    } yield CroPrivateRequest(
      id = id,
      method = "private/get-order-detail",
      nonce = nonce,
      params = GetOrderDetailParams(orderId).some
    )

  val getAccountSummaryRequest =
    for {
      id    <- Random.scalaUtilRandom[IO].map(_.betweenLong(0, Long.MaxValue)).flatten
      nonce <- Clock[IO].realTime.map(_.toMillis)
    } yield CroPrivateRequest(id = id, method = "private/get-account-summary", nonce = nonce, params = None)(encodeNone)
}
