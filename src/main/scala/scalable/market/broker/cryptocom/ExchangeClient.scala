package scalable.market.broker.cryptocom

import cats.effect.IO
import squants.market.Money

trait ExchangeClient {
  def balances: IO[List[Money]]

  def exchange(baseAsset: String, quoteAsset: String): IO[Money]

  //def convert(baseAsset: String, quoteAsset: String, amount: java.math.BigDecimal): IO[java.math.BigDecimal]
}
