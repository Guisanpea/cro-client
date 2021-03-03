package com.crypto.request

import com.crypto.Jsonable

import scala.collection.immutable.SortedMap

case class GetOrderDetailParams(orderId: String) extends Jsonable {
  override def toJsonMapSorted: SortedMap[String, String] =
    convertToJsonMapSorted(this)
}

