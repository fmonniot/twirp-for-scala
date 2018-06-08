package explorer

import cats.{Applicative, Monad}
import cats.implicits._
import explorer.core.{Context, NoContext}
import fs2.{Chunk, Segment, Stream}
import io.circe.{Decoder, Encoder, Printer}
import io.circe.syntax._
import org.http4s.client.Client
import org.http4s.{EntityDecoder, EntityEncoder, Request, Response}

// Collection of function which are common to all http4s code generated server.
// Those are extracted to ease maintenance and accelerate development.
// TODO Make http4s a package and have one server object and one client object
// instead of mixing everything as I'm doing right now
object http4s {
  // Json stream parsing
  import io.circe.jawn.CirceSupportParser.facade
  import jawnfs2._

  //
  // Server oriented methods
  //

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

  //
  // Client oriented methods
  //

  def streamBodyToRequest[F[_], A: Encoder](s: Stream[F, A]): Stream[F, Byte] =
    s.flatMap { a =>
      val bb = Printer.noSpaces.prettyByteBuffer(a.asJson)
      val segment = Segment.chunk(Chunk.byteBuffer(bb))

      Stream.segment(segment)
    }

  implicit class ClientOps[F[_]](val client: Client[F]) extends AnyVal {
    def streamResponse[B: Decoder](req: Request[F]): Stream[F, B] = {
      import io.circe.jawn.CirceSupportParser.facade
      import jawnfs2._

      client.streaming(req)(res => res.body.chunks.parseJsonStream)
        .map(_.as[B])
        .flatMap {
          // Swallow up errors for now, we should at least warn something
          case Left(_) => Stream.empty
          case Right(i) => Stream.emit(i)
        }
    }
  }

}
