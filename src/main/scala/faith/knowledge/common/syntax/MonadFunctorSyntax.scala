package faith.knowledge.common.syntax

import cats.{Functor, Monad}
import cats.implicits._

object MonadFunctorSyntax {
  implicit class MonadFunctorOps[F[_]: Functor, G[_]: Monad, A](private val value: F[G[A]]) {
    def flatMapEvery[B](f: A => G[B]): F[G[B]] = value.map(_.flatMap(f))
  }
}
