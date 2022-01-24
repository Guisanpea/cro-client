package faith.knowledge.common

import guru.broken.models.Symbol
import io.circe.{Decoder, Encoder}
import squants.market.Currency

import scala.util.{Failure, Success}

case class Instrument(base: Currency, quote: Currency) {
  val instrumentName = s"${base.code}_${quote.code}"
  val currencies     = Set(base, quote)
  lazy val opposite  = Instrument(base = quote, quote = base)

  lazy val symbol = new Symbol(base.code, quote.code)
}

object Instrument {
  implicit val encoder: Encoder[Instrument] = Encoder.encodeString.contramap[Instrument](_.instrumentName)
  implicit val decoder: Decoder[Instrument] = Decoder.decodeString.emapTry(Instrument.fromName)

  def fromName(instrument: String) =
    instrument.split('_').toList match {
      case base :: quote :: _ => Success(Instrument(Currencies(base), Currencies(quote)))
      case _ =>
        Failure(
          new IllegalArgumentException(s""" The passed instrument $instrument was not in the format "base_quote" """)
        )
    }

  def from(baseAsset: String, quoteAsset: String): Instrument =
    Instrument(Currencies(baseAsset), Currencies(quoteAsset))
}
