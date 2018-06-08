package explorer.generated.service

import _root_.cats.implicits._
import org.http4s.client.Client
import org.http4s.{EntityDecoder, EntityEncoder, HttpService, Request, Uri}
import org.http4s.circe._
import fs2.Stream
import _root_.explorer.core._
import _root_.explorer.http4s._

trait Service[F[_]] {
  def rpc(request: explorer.generated.service.MessageIn)(implicit ctx: Context): F[explorer.generated.service.MessageOut]
  def clientStreaming(request: _root_.fs2.Stream[F, explorer.generated.service.MessageIn])(implicit ctx: Context): F[explorer.generated.service.MessageOut]
  def serverStreaming(request: explorer.generated.service.MessageIn)(implicit ctx: Context): _root_.fs2.Stream[F, explorer.generated.service.MessageOut]
  def bidirectional(request: _root_.fs2.Stream[F, explorer.generated.service.MessageIn])(implicit ctx: Context): _root_.fs2.Stream[F, explorer.generated.service.MessageOut]
}

object Service {
  def httpClient[F[_]: _root_.cats.effect.Sync](base: Uri, client: Client[F]): Service[F] = new Service[F] {
    private val baseService = base / "explorer.generated.service" / "Service"

    // Adding entity decoder and encoder, for the non-streaming part
    implicit val e0: EntityEncoder[F, explorer.generated.service.MessageIn] = jsonEncoderOf[F, explorer.generated.service.MessageIn]
    implicit val e1: EntityDecoder[F, explorer.generated.service.MessageOut] = jsonOf[F, explorer.generated.service.MessageOut]

    override def rpc(in: explorer.generated.service.MessageIn)(implicit ctx: Context): F[explorer.generated.service.MessageOut] =
      for {
        req <- Request[F](uri = baseService / "rpc").withBody(in)
        res <- client.expect[explorer.generated.service.MessageOut](req)
      } yield res

    override def clientStreaming(stream: Stream[F, explorer.generated.service.MessageIn])(implicit ctx: Context): F[explorer.generated.service.MessageOut] =
      client.expect[explorer.generated.service.MessageOut](
        Request[F](uri = baseService / "clientStreaming")
          .withBodyStream(streamBodyToRequest[F, explorer.generated.service.MessageIn](stream))
      )

    override def serverStreaming(in: explorer.generated.service.MessageIn)(implicit ctx: Context): Stream[F, explorer.generated.service.MessageOut] =
      for {
        req <- Stream.eval(Request[F](uri = baseService / "serverStreaming").withBody(in))
        res <- client.streamResponse[explorer.generated.service.MessageOut](req)
      } yield res

    override def bidirectional(stream: Stream[F, explorer.generated.service.MessageIn])(implicit ctx: Context): Stream[F, explorer.generated.service.MessageOut] =
      client.streamResponse[explorer.generated.service.MessageOut](
        Request[F](uri = baseService / "bidirectional")
          .withBodyStream(streamBodyToRequest[F, explorer.generated.service.MessageIn](stream))
      )
  }
  def httpServer[F[_]: _root_.cats.effect.Sync](service: Service[F]): HttpService[F] = {
    // Http4s DSL in F
    val dsl = org.http4s.dsl.Http4sDsl[F]
    import dsl._

    // Adding entity decoder and encoder, for the non-streaming part
    implicit val e0: EntityDecoder[F, explorer.generated.service.MessageIn] = jsonOf[F, explorer.generated.service.MessageIn]
    implicit val e1: EntityEncoder[F, explorer.generated.service.MessageOut] = jsonEncoderOf[F, explorer.generated.service.MessageOut]

    HttpService {
      case req@GET -> Root / "explorer.generated.service" / "Service" / "rpc" =>
        rpc(req, service.rpc(_: explorer.generated.service.MessageIn)(_: Context))

      case req@GET -> Root / "explorer.generated.service" / "Service" / "clientStreaming" =>
        clientStreaming(req, service.clientStreaming(_: _root_.fs2.Stream[F, explorer.generated.service.MessageIn])(_: Context))

      case req@GET -> Root / "explorer.generated.service" / "Service" / "serverStreaming" =>
        serverStreaming(req, service.serverStreaming(_: explorer.generated.service.MessageIn)(_: Context))

      case req@GET -> Root / "explorer.generated.service" / "Service" / "bidirectional" =>
        bidirectional(req, service.bidirectional(_: _root_.fs2.Stream[F, explorer.generated.service.MessageIn])(_: Context))

    }
  }
}