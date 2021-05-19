package faith.knowledge.common.syntax

import scala.language.implicitConversions

object ChainingSyntax {
  @`inline` implicit final def scalaUtilChainingOps[A](a: A): ChainingOps[A] = new ChainingOps(a)

  final class ChainingOps[A](private val self: A) extends AnyVal {

    /** Converts the value by applying the function `f`.
      *
      * {{{
      *   scala> import import faith.knowledge.common.ChainingSyntax._
      *
      *   scala> val times6 = (_: Int) * 6
      *   times6: Int => Int = \$\$Lambda\$2023/975629453@17143b3b
      *
      *   scala> val i = (1 - 2 - 3) |> times6 |> scala.math.abs
      *   i: Int = 24
      * }}}
      *
      * Note: `(1 - 2 - 3) |> times6` may have a small amount of overhead at
      * runtime compared to the equivalent  `{ val temp = 1 - 2 - 3; times6(temp) }`.
      *
      *  @param f      the function to apply to the value.
      *  @tparam B     the result type of the function `f`.
      *  @return       a new value resulting from applying the given function
      *                `f` to this value.
      */
    def |>[B](f: A => B): B = f(self)
  }
}
