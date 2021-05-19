package com.crypto.response

import cats.implicits.catsSyntaxOptionId
import com.crypto.response.GetTickerResult.{InstrumentFoundResult, InstrumentNotFoundResult}

sealed trait GetTickerResult {
  def toOptionalTicker: Option[Ticker] =
    this match {
      case InstrumentNotFoundResult(_) => None
      case InstrumentFoundResult(data) => data.some
    }
}

object GetTickerResult {
  case class InstrumentNotFoundResult(data: List[Unit]) extends GetTickerResult
  case class InstrumentFoundResult(data: Ticker)        extends GetTickerResult
}
