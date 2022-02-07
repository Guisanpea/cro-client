package scalable.market.broker.cryptocom

import cats.data.OptionT
import cats.effect.IO
import cats.implicits._
import com.crypto.response.InstrumentResult
import scalable.market.common.Instrument
import scalable.market.common.syntax.BooleanSyntax.TextualBoolean
import squants.market.Currency

import scala.math.BigDecimal.RoundingMode.FLOOR

private[cryptocom] class MarketInfoProviderImpl(
  instrumentInfoCached: IO[Map[Instrument, InstrumentResult]],
  config: CroConfig
) extends MarketInfoProvider {
  override val instruments: IO[Set[Instrument]] = instrumentInfoCached.map(_.keySet)

  override def isTradingInstrument(instrument: Instrument): IO[Boolean] =
    instrumentInfoCached.map { map =>
      map.keySet.contains(instrument) or map.keySet.contains(instrument.opposite)
    }

  override def getInstrumentFor(instrumentName: String): IO[Option[Instrument]] =
    instrumentInfoCached.map { map =>
      map.keySet.find(instrument => instrument.instrumentName == instrumentName)
    }

  override def round(instrument: Instrument, amount: BigDecimal): IO[BigDecimal] = {
    val instrumentNotFoundError =
      IO.raiseError(new IllegalArgumentException("Trying to get the market info for an instrument that doesn't exist"))
    getInstrumentInfo(instrument).flatMap {
      case Some(value) => IO.pure(amount.setScale(value.quantityDecimals, FLOOR))
      case None        => instrumentNotFoundError
    }
  }

  private def getInstrumentInfo(instrument: Instrument): IO[Option[InstrumentResult]] =
    instrumentInfoCached.map(_.get(instrument))

  override def findIntermediaryStableCurrency(baseCurrency: Currency, quoteCurrency: Currency): IO[Option[Currency]] =
    OptionT
      .apply(findBuyBuyIntermediaryStableCurrency(baseCurrency, quoteCurrency))
      .orElseF(findBuySellIntermediaryStableCurrency(baseCurrency, quoteCurrency))
      .orElseF(findSellBuyIntermediaryStableCurrency(baseCurrency, quoteCurrency))
      .orElseF(findSellSellIntermediaryStableCurrency(baseCurrency, quoteCurrency))
      .value

  private def findBuyBuyIntermediaryStableCurrency(from: Currency, to: Currency): IO[Option[Currency]] = {
    val intermediaries = config.stableAssets
    for {
      intermediaries <- intermediaries.filterA(i => isTradingInstrument(Instrument(i, from)))
      intermediaries <- intermediaries.filterA(i => isTradingInstrument(Instrument(to, i)))
    } yield intermediaries.headOption
  }

  private def findBuySellIntermediaryStableCurrency(
    baseCurrency: Currency,
    quoteCurrency: Currency
  ): IO[Option[Currency]] = {
    val currencies = config.stableAssets
    for {
      currencies <- currencies.filterA(c => isTradingInstrument(Instrument(c, baseCurrency)))
      currencies <- currencies.filterA(c => isTradingInstrument(Instrument(c, quoteCurrency)))
    } yield currencies.headOption
  }

  private def findSellBuyIntermediaryStableCurrency(
    baseCurrency: Currency,
    quoteCurrency: Currency
  ): IO[Option[Currency]] = {
    val currencies = config.stableAssets
    for {
      currencies <- currencies.filterA(c => isTradingInstrument(Instrument(baseCurrency, c)))
      currencies <- currencies.filterA(c => isTradingInstrument(Instrument(quoteCurrency, c)))
    } yield currencies.headOption
  }

  private def findSellSellIntermediaryStableCurrency(
    baseCurrency: Currency,
    quoteCurrency: Currency
  ): IO[Option[Currency]] = {
    val currencies = config.stableAssets
    for {
      currencies <- currencies.filterA(c => isTradingInstrument(Instrument(baseCurrency, c)))
      currencies <- currencies.filterA(c => isTradingInstrument(Instrument(c, quoteCurrency)))
    } yield currencies.headOption
  }
}
