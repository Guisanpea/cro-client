package com.crypto

import scala.collection.SortedMap

trait Jsonable {
  def toJsonMapSorted: Map[String, String]
  def toCroParamsString: String =
    toJsonMapSorted
      .map(kv => kv._1 + kv._2)
      .foldLeft("") { (acc, curr) => acc + curr }
}
