package faith.knowledge.croclient

import cats.data.OptionT
import cats.effect.IO
import cats.implicits._
import com.crypto.{CroApi, SigProvider}
import faith.knowledge.common.CroError.{AssetNotFound, InstrumentNotExchangeableWithIntermediary, NonProcessedOrder}
import faith.knowledge.common.Instrument
import faith.knowledge.common.syntax.ChainingSyntax.scalaUtilChainingOps
import faith.knowledge.common.syntax.NestedFunctorSyntax._
import guru.broken.models
import guru.broken.models.AssetAmount
import io.chrisdavenport.mules.Cache
import squants.market.{Currency, Money}

class ExchangeClient(
  croApi: CroApi,
  croConfig: CroConfig,
  marketInfoProvider: MarketInfoProvider,
  cache: Cache[IO, String, String]
) {

  def getBalances: IO[List[Money]] =
    croApi.getAccountSummary
      .mapEvery(balance => Money(balance.balance, balance.currency))

  def exchange(baseAsset: String, quoteAsset: String): IO[Money] = {
    val i = Instrument.from(baseAsset, quoteAsset)
    for {
      isInstrumentValid <- marketInfoProvider.isValidInstrument(i)
      isOppositeValid   <- marketInfoProvider.isValidInstrument(i.opposite)
      res <- (isInstrumentValid, isOppositeValid) match {
        case (true, _) => sellAll(i)
        case (_, true) => buyAll(i)
        case _         => exchangeWithIntermediary(i)
      }
    } yield res
  }

  private def sellAll(i: Instrument): IO[Money] =
    for {
      baseBalance <- getAssetBalance(i.base)
      quantity <-
        croApi
          .requestSellOrder(i, baseBalance.available.toDouble)
          .map(_.result.orderInfo.quantity)
          .flatMap {
            case Some(value) => IO.pure(value)
            case None        => IO.raiseError(NonProcessedOrder)
          }
    } yield Money(quantity, i.quote)

  private def buyAll(i: Instrument): IO[Money] =
    for {
      baseBalance <- getAssetBalance(i.base)
      quantity <-
        croApi
          .requestBuyOrder(i, baseBalance.available.toDouble)
          .map(_.result.orderInfo.quantity)
          .flatMap {
            case Some(value) => IO.pure(value)
            case None        => IO.raiseError(NonProcessedOrder)
          }
    } yield Money(quantity, i.quote)

  private def exchangeWithIntermediary(instrument: Instrument): IO[Money] =
    sellViaIntermediary(instrument)
      .orElse(buyViaIntermediary(instrument))
      .getOrElseF(IO.raiseError(InstrumentNotExchangeableWithIntermediary))

  private def sellViaIntermediary(instrument: Instrument): OptionT[IO, Money] =
    for {
      intermediary <- OptionT(marketInfoProvider.findIntermediaryCurrency(instrument.base, instrument.quote))
      _            <- OptionT.liftF(sellAll(Instrument(instrument.base, intermediary)))
      amount       <- OptionT.liftF(sellAll(Instrument(intermediary, instrument.quote)))
    } yield amount

  private def buyViaIntermediary(instrument: Instrument): OptionT[IO, Money] =
    for {
      intermediary <- OptionT(marketInfoProvider.findIntermediaryCurrency(instrument.base, instrument.quote))
      _            <- OptionT.liftF(buyAll(Instrument(intermediary, instrument.base)))
      amount       <- OptionT.liftF(buyAll(Instrument(instrument.quote, intermediary)))
    } yield amount

  private def getAssetBalance(asset: Currency) =
    croApi.getAccountSummary.map(_.find(_.currency == asset)).flatMap {
      case Some(balance) => IO.pure(balance)
      case None          => IO.raiseError(AssetNotFound)
    }

  def convert(baseAsset: String, quoteAsset: String, amount: java.math.BigDecimal) = {
    val instrument = Instrument.from(baseAsset, quoteAsset)
    getExchangeRate(instrument)
      .map(BigDecimal(_) * amount)
      .map(_.underlying)
  }

  private def getExchangeRate(i: Instrument): IO[Double] =
    croApi
      .getExchangeRate(i)
      .|>(OptionT.apply)
      .orElse(
        marketInfoProvider
          .findIntermediaryCurrency(i.base, i.quote)
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

  val getTop24: IO[List[models.Symbol]] =
    croApi.getTop24Instrument.mapEvery(_.symbol)

  def convert(s: String, s1: String): IO[java.math.BigDecimal] =
    convert(s, s1, BigDecimal(1).underlying)

  lazy val sigProvider = new SigProvider(croConfig.apiKey, croConfig.privateKey)
}
