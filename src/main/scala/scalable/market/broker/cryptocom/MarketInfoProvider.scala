package scalable.market.broker.cryptocom

import cats.effect.IO
import scalable.market.common.Instrument
import squants.market.Currency

trait MarketInfoProvider {
  val instruments: IO[Set[Instrument]]

  def isTradingInstrument(currencyPair: Instrument): IO[Boolean]

  def getInstrumentFor(instrumentName: String): IO[Option[Instrument]]

  def round(instrument: Instrument, amount: BigDecimal): IO[BigDecimal]

  def findIntermediaryStableCurrency(baseCurrency: Currency, quoteCurrency: Currency): IO[Option[Currency]]
}
