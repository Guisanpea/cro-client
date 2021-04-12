package faith.knowledge.common

import squants.market.Currency

case class CurrencyPair(base: Currency, quote: Currency) {
  val instrumentName = s"${base.code}_${quote.code}"
  lazy val oppositePair = CurrencyPair(base = quote, quote = base)
}
