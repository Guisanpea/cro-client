package scalable.market.common.syntax

import cats.Functor
import cats.implicits._

object NestedFunctorSyntax {
  implicit class NestedFunctorOps[F[_]: Functor, G[_]: Functor, A](private val value: F[G[A]]) {
    def mapEvery[B](f: A => B): F[G[B]] = value.map(_.map(f))
  }
}
