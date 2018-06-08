package explorer

import cats.effect.Sync
import cats.implicits._
import explorer.core._
import fs2.Stream
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.{EntityDecoder, EntityEncoder, HttpService, Request, Uri}

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

  def httpClient[F[_] : Sync](base: Uri, client: Client[F]): Service[F] = new Service[F] {

    import http4s._

    private val baseService = base / "package" / "service"

    private implicit val ee3 = jsonEncoderOf[F, Message1]
    private implicit val ed = jsonOf[F, Message3]

    override def rpc(message1: Message1)(implicit ctx: Context): F[Message3] =
      for {
        req <- Request[F](uri = baseService / "rpc").withBody(message1)
        res <- client.expect[Message3](req)
      } yield res

    override def clientStreaming(stream: Stream[F, Message1])(implicit ctx: Context): F[Message3] =
      client.expect[Message3](
        Request[F](uri = baseService / "clientStreaming")
          .withBodyStream(streamBodyToRequest[F, Message1](stream))
      )

    override def serverStreaming(message1: Message1)(implicit ctx: Context): Stream[F, Message3] = {
      for {
        req <- Stream.eval(Request[F](uri = baseService / "serverStreaming").withBody(message1))
        item <- client.streamResponse[Message3](req)
      } yield item
    }

    override def bidirectional(stream: Stream[F, Message1])(implicit ctx: Context): Stream[F, Message3] =
      client.streamResponse[Message3](
        Request[F](uri = baseService / "bidirectional")
          .withBodyStream(streamBodyToRequest[F, Message1](stream))
      )

  }

  def httpServer[F[_] : Sync](implementation: Service[F]): HttpService[F] = {
    // Http4s DSL in effect F
    val dsl = org.http4s.dsl.Http4sDsl[F]
    import dsl._
    import http4s._

    // Because they needs to be explicit somehow
    implicit val et1: EntityDecoder[F, Message1] = jsonOf[F, Message1]
    implicit val et5: EntityEncoder[F, Message3] = jsonEncoderOf[F, Message3]

    HttpService {
      case req@GET -> Root / "package" / "service" / "rpc" =>
        rpc(req, implementation.rpc(_: Message1)(_: Context))

      case req@GET -> Root / "package" / "service" / "clientStreaming" =>
        clientStreaming(req, implementation.clientStreaming(_: Stream[F, Message1])(_: Context))

      case req@GET -> Root / "package" / "service" / "serverStreaming" =>
        serverStreaming(req, implementation.serverStreaming(_: Message1)(_: Context))

      case req@GET -> Root / "package" / "service" / "bidirectional" =>
        bidirectional(req, implementation.bidirectional(_: Stream[F, Message1])(_: Context))
    }
  }

}
