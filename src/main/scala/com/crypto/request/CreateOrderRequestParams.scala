package com.crypto.request

case class CreateOrderRequestParams(
  instrumentName: String,
  side: String,
  `type`: String,
  notional: Option[Double] = None,
  quantity: Option[Double] = None
)
