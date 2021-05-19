package faith.knowledge.croclient

import com.crypto.{CroApi, CroFactory, SigProvider}
import com.crypto.CroApi.HttpEnv
import faith.knowledge.common.syntax.BooleanSyntax.TextualBoolean
import faith.knowledge.common.CroError.{AssetNotFound, InstrumentNotExchangeableWithIntermediary, NonProcessedOrder}
import faith.knowledge.common.syntax.ZioSyntax.{OptionZIO, _}
import faith.knowledge.common.{CroError, Instrument}
import faith.knowledge.croclient.CroClient._
import guru.broken.brokers.ExchangeClient
import guru.broken.models
import guru.broken.models.AssetAmount
import reactor.core.publisher.Mono
import squants.market.Currency
import sttp.client3.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import zio._
import zio.clock.Clock
import zio.console.Console
import zio.logging.Logging
import zio.random.Random
import zio.stream.ZStream

import java.util
import scala.jdk.CollectionConverters._

class CroClient private (croApi: CroApi, croConfig: CroConfig) extends ExchangeClient {

  override def getBalances =
    croApi.getAccountSummary
      .mapEvery(balance => new AssetAmount(balance.currency.code, balance.available.underlying))
      .map(_.asJava)
      .provideCustomLayer(appLayer)
      .unsafeToMono(runtime)

  override def isValidPair(baseAsset: String, quoteAsset: String): Boolean = {
    val i = Instrument.from(baseAsset, baseAsset)
    runtime.unsafeRun(isValidInstrument(i))
  }

  private def isValidInstrument(instrument: Instrument): ZIO[Any, CroError, Boolean] =
    croApi.getInstruments.map(
      _.exists(i => (i.instrumentName == instrument) or (i.instrumentName == instrument.opposite))
    )

  // Done
  override def exchange(baseAsset: String, quoteAsset: String): Mono[AssetAmount] = {
    val i = Instrument.from(baseAsset, quoteAsset)
    (isValidInstrument(i) zip isValidInstrument(i.opposite))
      .flatMap {
        case (true, _) => sellAll(i)
        case (_, true) => buyAll(i)
        case _         => exchangeWithIntermediary(i)
      }
      .provideCustomLayer(appLayer)
      .unsafeToMono(runtime)
  }

  private def exchangeWithIntermediary(instrument: Instrument): ZIO[ApiEnv, CroError, AssetAmount] =
    sellViaIntermediary(instrument)
      .flatOrElse(buyViaIntermediary(instrument))
      .foldEmptyToError(InstrumentNotExchangeableWithIntermediary)

  private def sellViaIntermediary(instrument: Instrument): ZIO[ApiEnv, CroError, Option[AssetAmount]] =
    (for {
      intermediary <- findIntermediaryAsset(instrument).some
      _            <- sellAll(Instrument(instrument.base, intermediary)).mapError(Some(_))
      amount       <- sellAll(Instrument(intermediary, instrument.quote)).mapError(Some(_))
    } yield amount).optional

  private def buyViaIntermediary(instrument: Instrument): ZIO[ApiEnv, CroError, Option[AssetAmount]] =
    (for {
      intermediary <- findIntermediaryAsset(instrument.opposite).some
      _            <- buyAll(Instrument(intermediary, instrument.base)).mapError(Some(_))
      amount       <- buyAll(Instrument(instrument.quote, intermediary)).mapError(Some(_))
    } yield amount).optional

  private def sellAll(i: Instrument): ZIO[ApiEnv, CroError, AssetAmount] =
    for {
      baseBalance <- getAssetBalance(i.base)
      quantity <-
        croApi
          .requestSellOrder(i, baseBalance.available.toDouble)
          .map(_.result.orderInfo.quantity)
          .foldEmptyToError(NonProcessedOrder)
    } yield new AssetAmount(i.quote.code, BigDecimal(quantity).underlying)

  private def buyAll(i: Instrument): ZIO[ApiEnv, CroError, AssetAmount] =
    for {
      baseBalance <- getAssetBalance(i.base)
      quantity <-
        croApi
          .requestBuyOrder(i, baseBalance.available.toDouble)
          .map(_.result.orderInfo.quantity)
          .foldEmptyToError[CroError](NonProcessedOrder)
    } yield new AssetAmount(i.quote.code, BigDecimal(quantity).underlying)

  private def getAssetBalance(asset: Currency) =
    croApi.getAccountSummary.map(_.find(_.currency == asset)).flatMap {
      case Some(balance) => ZIO.succeed(balance)
      case None          => ZIO.fail(AssetNotFound)
    }

  override def convert(
    baseAsset: String,
    quoteAsset: String,
    amount: java.math.BigDecimal
  ): Mono[java.math.BigDecimal] = {
    val instrument = Instrument.from(baseAsset, quoteAsset)
    getExchangeRate(instrument)
      .map(BigDecimal(_) * amount)
      .map(_.underlying)
      .provideCustomLayer(appLayer)
      .unsafeToMono(runtime)
  }

  private def findIntermediaryAsset(instrument: Instrument): IO[CroError, Option[Currency]] =
    ZStream
      .fromIterable(croConfig.stableAssets)
      .filterM(stable => isValidInstrument(Instrument(instrument.base, stable)))
      .runFindM(stable => isValidInstrument(Instrument(stable, instrument.quote)))

  private def getExchangeRate(i: Instrument): ZIO[HttpEnv, CroError, Double] =
    croApi
      .getExchangeRate(i)
      .switchIfEmpty(
        findIntermediaryAsset(i)
          .flatMapEvery(intermediary => getExchangeRateWithIntermediary(i, intermediary))
          .foldEmptyToError(InstrumentNotExchangeableWithIntermediary)
      )

  private def getExchangeRateWithIntermediary(i: Instrument, intermediary: Currency): ZIO[HttpEnv, CroError, Double] = {
    type GetRateResult = IO[CroError, Double]
    for {
      baseRateOption  <- croApi.getExchangeRate(Instrument(i.base, intermediary))
      baseRate        <- baseRateOption.fold[GetRateResult](IO.fail(InstrumentNotExchangeableWithIntermediary))(IO.succeed(_))
      quoteRateOption <- croApi.getExchangeRate(Instrument(intermediary, i.quote))
      quoteRate <-
        quoteRateOption.fold[GetRateResult](IO.fail(InstrumentNotExchangeableWithIntermediary))(IO.succeed(_))
    } yield baseRate * quoteRate
  }

  override def getTop24(baseAsset: String, i: Int, set: util.Set[String]): Mono[util.List[models.Symbol]] =
    croApi.getTop24Instrument.mapEvery(_.symbol).map(_.asJava).provideCustomLayer(appLayer).unsafeToMono(runtime)

  override def convert(s: String, s1: String): Mono[java.math.BigDecimal] = convert(s, s1, BigDecimal(1).underlying)

  lazy val sigProvider = new SigProvider(croConfig.apiKey, croConfig.privateKey)
}

object CroClient {
  type ApiEnv = Logging with SttpClient with Clock with Random
  val runtime: Runtime[zio.ZEnv] = Runtime.default

  private val appLayer: ZLayer[Console with Clock with Any, Throwable, Logging with SttpClient] =
    Logging.console() ++ AsyncHttpClientZioBackend.layer()

}
