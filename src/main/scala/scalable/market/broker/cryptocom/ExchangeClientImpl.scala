package scalable.market.broker.cryptocom

import cats.data.OptionT
import cats.effect.IO
import com.crypto.CryptoComApi
import guru.broken.models
import scalable.market.broker.cryptocom.CroError.{
  AssetNotFound,
  InstrumentNotExchangeableWithIntermediary,
  NonProcessedOrder
}
import scalable.market.common.Instrument
import scalable.market.common.syntax.ChainingSyntax.scalaUtilChainingOps
import scalable.market.common.syntax.NestedFunctorSyntax.NestedFunctorOps
import squants.market.{Currency, Money}

final case class ExchangeClientImpl(croApi: CryptoComApi, marketInfoProvider: MarketInfoProvider, croConfig: CroConfig)
    extends ExchangeClient {

  override val balances: IO[List[Money]] =
    croApi.getAccountSummary
      .mapEvery(balance => Money(balance.balance, balance.currency))

  override def exchange(baseAsset: String, quoteAsset: String): IO[Money] = {
    val i = Instrument.from(baseAsset, quoteAsset)
    println(i)
    for {
      isInstrumentValid <- marketInfoProvider.isTradingInstrument(i)
      isOppositeValid   <- marketInfoProvider.isTradingInstrument(i.opposite)
      res <- (isInstrumentValid, isOppositeValid) match {
        case (_, true) => buyAll(i.opposite)
        case (true, _) => sellAll(i)
        case _         => exchangeWithIntermediary(i)
      }
    } yield res
  }

  private def sellAll(i: Instrument): IO[Money] =
    for {
      baseBalance <- getAssetBalance(i.base)
      order       <- croApi.requestSellOrder(i, baseBalance.available.toDouble)
      optionalQuantity = order.result.orderInfo.quantity
      quantity <- optionalQuantity match {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(NonProcessedOrder)
      }
    } yield Money(quantity, i.quote)

  private def buyAll(i: Instrument): IO[Money] =
    for {
      quote <- getAssetBalance(i.quote)
      order <- croApi.requestBuyOrder(i, quote.available.toDouble)
      optionalQuantity = order.result.orderInfo.quantity
      quantity <- optionalQuantity match {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(NonProcessedOrder)
      }
    } yield Money(quantity, i.base)

  private def exchangeWithIntermediary(instrument: Instrument): IO[Money] = {
    val Instrument(from, to) = instrument
    (for {
      intermediary <- marketInfoProvider.findIntermediaryStableCurrency(from, to) |> OptionT.apply
      _            <- OptionT.liftF(exchange(from.code, intermediary.code))
      res          <- OptionT.liftF(exchange(intermediary.code, to.code))
    } yield res)
      .foldF(IO.raiseError(InstrumentNotExchangeableWithIntermediary))(IO.pure)
  }

  private def getAssetBalance(asset: Currency) =
    croApi.getAccountSummary.map(_.find(_.currency == asset)).flatMap {
      case Some(balance) => IO.pure(balance)
      case None          => IO.raiseError(AssetNotFound)
    }

  private def getExchangeRate(i: Instrument): IO[Double] =
    croApi
      .getExchangeRate(i)
      .|>(OptionT.apply)
      .orElse(
        marketInfoProvider
          .findIntermediaryStableCurrency(i.base, i.quote)
          .|>(OptionT.apply)
          .semiflatMap(intermediary => getExchangeRateWithIntermediary(i, intermediary))
      )
      .getOrElseF(IO.raiseError(InstrumentNotExchangeableWithIntermediary))

  private def getExchangeRateWithIntermediary(i: Instrument, intermediary: Currency): IO[Double] = {
    type GetRateResult = IO[Double]
    for {
      baseRateOption  <- croApi.getExchangeRate(Instrument(i.base, intermediary))
      baseRate        <- baseRateOption.fold[GetRateResult](IO.raiseError(InstrumentNotExchangeableWithIntermediary))(IO.pure)
      quoteRateOption <- croApi.getExchangeRate(Instrument(intermediary, i.quote))
      quoteRate <-
        quoteRateOption.fold[GetRateResult](IO.raiseError(InstrumentNotExchangeableWithIntermediary))(IO.pure)
    } yield baseRate * quoteRate
  }

  val getTop24: IO[List[models.Symbol]] = IO.pure(List.empty)
}
