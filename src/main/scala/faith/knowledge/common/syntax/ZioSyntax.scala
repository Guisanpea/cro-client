package faith.knowledge.common.syntax

import reactor.core.publisher.{Mono, MonoSink}
import reactor.core.scala.publisher.SMono
import zio.Exit.{Failure, Success}
import zio.stream.ZStream
import zio.{Runtime, ZEnv, ZIO}

object ZioSyntax {
  implicit class ListZIO[R, E, A](zio: ZIO[R, E, List[A]]) {
    def mapEvery[B](f: A => B): ZIO[R, E, List[B]] =
      zio.map(col => col.map(f))
  }

  implicit class ThrowableZIO[E <: Throwable, A](zio: ZIO[ZEnv, E, A]) {
    def unsafeToMono(runtime: Runtime[ZEnv]): Mono[A] =
      SMono
        .create { monoSink: MonoSink[A] =>
          runtime.unsafeRunAsync(zio) {
            case Success(value) => monoSink.success(value)
            case Failure(cause) => monoSink.error(cause.squashTrace)
          }
        }
        .asJava()
  }

  implicit class OptionZIO[R, E, A](zio: ZIO[R, E, Option[A]]) {
    def flatOrElse[B >: A](zio2: => ZIO[R, E, Option[B]]): ZIO[R, E, Option[B]] =
      zio.flatMap {
        case some: Some[A] => ZIO.succeed(some)
        case None          => zio2
      }

    def switchIfEmpty[E1 >: E, B >: A](zio2: ZIO[R, E1, B]): ZIO[R, E1, B] =
      zio.flatMap {
        case Some(a) => ZIO.succeed(a)
        case None    => zio2
      }

    def foldEmptyToError[E1 >: E](ifEmpty: => E1): ZIO[R, E1, A] =
      zio.flatMap(a => a.fold[ZIO[R, E1, A]](ZIO.fail(ifEmpty))(ZIO.succeed(_)))

    def mapEvery[B](f: A => B): ZIO[R, E, Option[B]] =
      zio.map(option => option.map(f))

    def flatMapEvery[R1 <: R, E1 >: E, B](f: A => ZIO[R, E, B]): ZIO[R, E, Option[B]] =
      zio.flatMap {
        case Some(a) => f(a).map(Some(_))
        case None    => ZIO.succeed(None)
      }
  }

  implicit class ZStreamSyntax[R, E, A](zstream: ZStream[R, E, A]) {
    def runFindM(pred: A => ZIO[R, E, Boolean]): ZIO[R, E, Option[A]] =
      zstream.filterM(pred).runHead
  }
}
