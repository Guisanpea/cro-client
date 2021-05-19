package faith.knowledge.common

import cats.implicits.catsSyntaxOptionId
import com.crypto.request.{CreateOrderRequestParams, CroPrivateRequest}
import faith.knowledge.common.syntax.ChainingSyntax.scalaUtilChainingOps
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.chaining.scalaUtilChainingOps

class CurrenciesSpec extends AnyWordSpec with Matchers {
  "Currencies apply should generate a currency if no one is found" in {
    val santiCoin = Currencies("SCN")
    santiCoin.symbol shouldBe "SCN"
    santiCoin.code shouldBe "SCN"
    santiCoin.name shouldBe "SCN"
    santiCoin.formatDecimals shouldBe 15
  }
}
