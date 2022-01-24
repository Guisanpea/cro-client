package faith.knowledge.croclient

import cats.data.OptionT
import cats.effect.{Concurrent, IO}
import cats.implicits._
import com.crypto.CroApi
import com.crypto.response.InstrumentResult
import faith.knowledge.common.Instrument
import faith.knowledge.common.syntax.BooleanSyntax._
import faith.knowledge.common.syntax.ChainingSyntax.scalaUtilChainingOps
import squants.market.Currency

import scala.math.BigDecimal.RoundingMode.FLOOR

private[knowledge] class MarketInfoProvider(
  instrumentInfoCached: IO[Map[Instrument, InstrumentResult]],
  config: CroConfig
) {
  val instruments: IO[Set[Instrument]] = instrumentInfoCached.map(_.keySet)

  def isValidMarket(instrument: Instrument): IO[Boolean] =
    instrumentInfoCached.map { map =>
      map.keySet.contains(instrument) or map.keySet.contains(instrument.opposite)
    }
  def isValidInstrument(instrument: Instrument): IO[Boolean] =
    instrumentInfoCached.map { map =>
      map.keySet.contains(instrument)
    }
  def getMarketFor(instrumentName: String): IO[Option[Instrument]] =
    instrumentInfoCached.map { map =>
      map.keySet.find(instrument => instrument.instrumentName == instrumentName)
    }

  def round(instrument: Instrument, amount: BigDecimal): IO[BigDecimal] = {
    val instrumentNotFoundError =
      IO.raiseError(new IllegalArgumentException("Trying to get the market info for an instrument that doesn't exist"))
    getInstrumentInfo(instrument).flatMap {
      case Some(value) => IO.pure(amount.setScale(value.quantityDecimals, FLOOR))
      case None        => instrumentNotFoundError
    }
  }

  private def getCurrenciesFromInstruments: IO[Set[Currency]] =
    instruments.map { instruments =>
      instruments.flatMap(_.currencies)
    }

  private def getInstrumentInfo(instrument: Instrument): IO[Option[InstrumentResult]] =
    instrumentInfoCached.map(_.get(instrument))

  def findIntermediaryCurrency(baseCurrency: Currency, quoteCurrency: Currency): IO[Option[Currency]] =
    findValidIntermediary(config.stableAssets, baseCurrency, quoteCurrency)
      .|>(OptionT.apply)
      .orElseF(findValidIntermediaryFromInstruments(baseCurrency, quoteCurrency))
      .value

  private def findValidIntermediaryFromInstruments(base: Currency, quote: Currency): IO[Option[Currency]] = {
    for {
      currencies        <- getCurrenciesFromInstruments.map(_.toList)
      foundIntermediary <- findValidIntermediary(currencies, base, quote)
    } yield foundIntermediary
  }

  private def findValidIntermediary(currencies: List[Currency], base: Currency, quote: Currency): IO[Option[Currency]] =
    for {
      currencies <- currencies.filterA(c => isValidInstrument(Instrument(quote, c)))
      currencies <- currencies.filterA(c => isValidInstrument(Instrument(base, c)))
    } yield currencies.headOption
}
