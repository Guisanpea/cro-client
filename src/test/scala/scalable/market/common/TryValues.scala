package scalable.market.common

import scala.util.{Failure, Try}

trait TryValues {
  implicit final class TryValuesSyntax[A](val t: Try[A]) {
    def success: A = t.getOrElse(throw new NoSuchElementException(s"Expected Success was $t"))
    def failure: Throwable =
      t match {
        case Failure(exception) => exception
        case _                  => throw new NoSuchElementException(s"Expected Failure was $t")
      }
  }
}
