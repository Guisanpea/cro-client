package com.crypto.request

import com.crypto.Jsonable

import scala.collection.immutable.SortedMap

case class CreateOrderRequestParams(
  instrumentName: String,
  side: String,
  `type`: String,
  notional: Option[BigDecimal] = None
) extends Jsonable {
  override def toJsonMapSorted: SortedMap[String, String] =
    convertToJsonMapSorted(this)
}
