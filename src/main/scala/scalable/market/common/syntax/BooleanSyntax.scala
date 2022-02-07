package scalable.market.common.syntax

object BooleanSyntax {
  implicit class TextualBoolean(b: Boolean) {
    def or(b2: Boolean)  = b || b2
    def and(b2: Boolean) = b && b2
  }
}
