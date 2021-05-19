package faith.knowledge.common

import io.circe.Decoder
import squants.market.{Currency, MoneyContext, defaultMoneyContext}

import scala.util.Success

object Currencies {
  object USDT extends Currency("USDT", "Tether", "₮", 2)
  object CRO  extends Currency("CRO", "Crypto.org Coin", "cro", 15)

  def otherCurrency(currencySymbol: String) = new Currency(currencySymbol, currencySymbol, currencySymbol, 15) {}

  def apply(symbol: String): Currency = Currency.apply(symbol).getOrElse(otherCurrency(symbol))

  val customCurrencies                           = Set(USDT, CRO)
  implicit val customCurrenciesCtx: MoneyContext = defaultMoneyContext.withAdditionalCurrencies(customCurrencies)
  implicit val decoder: Decoder[Currency]        = Decoder.decodeString.emapTry(s => Success(Currencies.apply(s)))
}
