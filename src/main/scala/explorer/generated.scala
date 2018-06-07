package explorer

import cats.effect.Sync
import cats.implicits._
import fs2.{Chunk, Segment, Stream}
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Printer}
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.{HttpService, Request}

import core._

object generated {

  case class Message1(value: String)

  case class Message3(a: List[String] = List.empty)

  // Needs to be implemented by the server
  trait Service[F[_]] {

    def rpc(message1: Message1)(implicit ctx: Context): F[Message3]

    def clientStreaming(stream: Stream[F, Message1])(implicit ctx: Context): F[Message3]

    def serverStreaming(message1: Message1)(implicit ctx: Context): Stream[F, Message3]

    def bidirectional(stream: Stream[F, Message1])(implicit ctx: Context): Stream[F, Message3]

  }

  // Missing in this hand-written code: the protobuf part (in encoder/decoder)

  def httpClient[F[_] : Sync](client: Client[F]): Service[F] = new Service[F] {

    import org.http4s.Uri._

    private val base = uri("http://localhost:8080/package/service")

    private implicit val ed = jsonOf[F, Message3]
    private implicit val ee2 = jsonEncoderOf[F, List[Message1]]
    private implicit val ee3 = jsonEncoderOf[F, Message1]

    override def rpc(message1: Message1)(implicit ctx: Context): F[Message3] =
      for {
        req <- Request[F](uri = base / "rpc").withBody[Message1](message1)
        res <- client.expect[Message3](req)
      } yield res


    override def clientStreaming(stream: Stream[F, Message1])(implicit ctx: Context): F[Message3] =
      client.expect[Message3](Request[F](uri = base / "clientStreaming").withBodyStream(streamBody(stream)))

    override def serverStreaming(message1: Message1)(implicit ctx: Context): Stream[F, Message3] = {
      for {
        req <- Stream.eval(Request[F](uri = base / "serverStreaming").withBody(message1))
        item <- streamResponse(req)
      } yield item
    }

    override def bidirectional(stream: Stream[F, Message1])(implicit ctx: Context): Stream[F, Message3] =
        streamResponse(Request[F](uri = base / "bidirectional").withBodyStream(streamBody(stream)))

    private def streamBody[A: Encoder](s: Stream[F, A]): Stream[F, Byte] =
      s.flatMap { m =>
        val bb = Printer.noSpaces.prettyByteBuffer(m.asJson)
        val segment = Segment.chunk(Chunk.byteBuffer(bb))

        Stream.segment(segment)
      }

    private def streamResponse(req: Request[F]): Stream[F, Message3] = {
      import io.circe.jawn.CirceSupportParser.facade
      import jawnfs2._

      client.streaming(req)(res => res.body.chunks.parseJsonStream)
        .map(_.as[Message3])
        .flatMap {
          case Left(_) => Stream.empty
          case Right(i) => Stream.emit(i)
        }
    }
  }

  def httpServer[F[_] : Sync](implementation: Service[F]): HttpService[F] = {
    val dsl = org.http4s.dsl.Http4sDsl[F]
    import dsl._

    implicit val et1 = jsonOf[F, Message1]
    implicit val et2 = jsonOf[F, List[Message1]]
    implicit val et5 = jsonEncoderOf[F, Message3]

    // Needs to be figured later
    def contextFromRequest(req: Request[F]): F[Context] = Sync[F].pure(NoContext)

    // Json stream parsing
    import io.circe.jawn.CirceSupportParser.facade
    import jawnfs2._

    def streamBodyFromRequest[A: Decoder](request: Request[F]): Stream[F, A] =
      request.body.chunks.parseJsonStream
        .map(_.as[A])
        .flatMap {
          case Left(_) => Stream.empty
          case Right(i) => Stream.emit(i)
        }

    // Below
    HttpService {
      case req@GET -> Root / "package" / "service" / "rpc" =>
        for {
          ctx <- contextFromRequest(req)
          param <- req.as[Message1]
          res <- implementation.rpc(param)(ctx)
          res <- Ok(res)
        } yield res

      case req@GET -> Root / "package" / "service" / "clientStreaming" =>

        for {
          ctx <- contextFromRequest(req)
          inStream = streamBodyFromRequest[Message1](req)
          res <- implementation.clientStreaming(inStream)(ctx)
          res <- Ok(res)
        } yield res

      case req@GET -> Root / "package" / "service" / "serverStreaming" =>
        for {
          ctx <- contextFromRequest(req)
          param <- req.as[Message1]
          res <- Sync[F].delay(implementation.serverStreaming(param)(ctx))
          res <- Ok(res)
        } yield res

      case req@GET -> Root / "package" / "service" / "bidirectional" =>
        for {
          ctx <- contextFromRequest(req)
          inStream = streamBodyFromRequest[Message1](req)
          res <- Sync[F].delay(implementation.bidirectional(inStream)(ctx))
          res <- Ok(res)
        } yield res
    }
  }

}
