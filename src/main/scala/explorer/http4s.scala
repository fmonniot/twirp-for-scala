package explorer

import cats.{Applicative, FlatMap, Monad}
import cats.implicits._
import explorer.core.{Context, NoContext}
import fs2.Stream
import io.circe.Decoder
import org.http4s.dsl.impl.Statuses
import org.http4s.{EntityDecoder, EntityEncoder, Request, Response}

// Collection of function which are common to all http4s code generated server.
// Those are extracted to ease maintenance and accelerate development.
object http4s {
  // Json stream parsing
  import io.circe.jawn.CirceSupportParser.facade
  import jawnfs2._

  // Needs to be figured later
  def contextFromRequest[F[_] : Applicative](req: Request[F]): F[Context] = Applicative[F].pure(NoContext)


  def streamBodyFromRequest[F[_], A: Decoder](request: Request[F]): Stream[F, A] =
    request.body.chunks.parseJsonStream
      .map(_.as[A])
      .flatMap {
        case Left(_) => Stream.empty
        case Right(i) => Stream.emit(i)
      }

  // Doing the conversion to function as a value may have a performance implication
  def rpc[F[_] : Monad, A, B](request: Request[F], f: (A, Context) => F[B])
                             (implicit ed: EntityDecoder[F, A], ee: EntityEncoder[F, B]): F[Response[F]] = {
    val dsl = org.http4s.dsl.Http4sDsl[F]
    import dsl._

    for {
      ctx <- contextFromRequest[F](request)
      param <- request.as[A]
      res <- f(param, ctx)
      res <- Ok(res)
    } yield res
  }

  def clientStreaming[F[_] : Monad, A, B](request: Request[F], f: (Stream[F, A], Context) => F[B])
                                         (implicit ed: Decoder[A], ee: EntityEncoder[F, B]): F[Response[F]] = {
    val dsl = org.http4s.dsl.Http4sDsl[F]
    import dsl._

    for {
      ctx <- contextFromRequest[F](request)
      inStream = streamBodyFromRequest[F, A](request)
      res <- f(inStream, ctx)
      res <- Ok(res)
    } yield res
  }

  def serverStreaming[F[_] : Monad, A, B](request: Request[F], f: (A, Context) => Stream[F, B])
                                         (implicit ed: EntityDecoder[F, A], ee: EntityEncoder[F, B]): F[Response[F]] = {
    val dsl = org.http4s.dsl.Http4sDsl[F]
    import dsl._

    for {
      ctx <- contextFromRequest[F](request)
      param <- request.as[A]
      res <- Ok(f(param, ctx))
    } yield res
  }

  def bidirectional[F[_] : Monad, A, B](request: Request[F], f: (Stream[F, A], Context) => Stream[F, B])
                                       (implicit ed: Decoder[A], ee: EntityEncoder[F, B]): F[Response[F]] = {
    val dsl = org.http4s.dsl.Http4sDsl[F]
    import dsl._

    for {
      ctx <- contextFromRequest[F](request)
      inStream = streamBodyFromRequest[F, A](request)
      res <- Ok(f(inStream, ctx))
    } yield res
  }
}
