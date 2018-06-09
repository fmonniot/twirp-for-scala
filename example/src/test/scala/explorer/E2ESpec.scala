package explorer

import cats.effect.{IO, Sync}
import fs2.Stream
import org.http4s.Uri.uri
import org.http4s.client.blaze.Http1Client
import org.http4s.server.blaze.BlazeBuilder
import org.scalatest.{FlatSpec, Matchers}
import explorer.core.Context
import explorer.generated.service._

import scala.concurrent.ExecutionContext.Implicits.global

class E2ESpec extends FlatSpec with Matchers {

  it should "make a request and return the result" in {
    implicit val ctx: Context = Context.NoContext

    val tests = for {
      httpClient <- Http1Client.stream[IO]()
      client = Service.httpClient(uri("http://localhost:8080"), httpClient)

      _ <- stdout("Making the RPC request")
      answer <- Stream.eval(client.rpc(MessageIn("1.1")))
      _ = answer shouldEqual MessageOut(List("1.1"))

      _ <- stdout("Making the client streaming request")
      answer <- Stream.eval(client.clientStreaming(Stream(MessageIn("2.1"), MessageIn("2.2"))))
      _ = answer shouldEqual MessageOut(List("2.1", "2.2"))

      _ <- stdout("Making the server streaming request")
      answer <- Stream.eval(client.serverStreaming(MessageIn("3")).compile.toList)
      _ = answer shouldEqual List(MessageOut(List("3.1")), MessageOut(List("3.2")), MessageOut(List("3.3")))

      _ <- stdout("Making the bidirectional request")
      answer <- Stream.eval(client.bidirectional(Stream(MessageIn("4"))).compile.toList)
      _ = answer shouldEqual List(MessageOut(List("4.1")), MessageOut(List("4.2")))
    } yield ()

    tests.mergeHaltL(server).compile.drain.unsafeRunSync()
  }

  // TODO Find a standardized way of expression errors
  it should "return an error when a service fail" in {
    pending
  }

  val server = BlazeBuilder[IO]
    .mountService(Service.httpServer(impl))
    .bindHttp() // Default address `localhost:8080`
    .serve

  def stdout(s: String) = Stream.eval(IO(println(s)))

  def impl[F[_]](implicit F: Sync[F]) = new Service[F] {

    import cats.implicits._
    import core.Context

    override def rpc(messageIn: MessageIn)(implicit ctx: Context) =
      F.pure(MessageOut(List(messageIn.value)))

    override def clientStreaming(stream: Stream[F, MessageIn])(implicit ctx: Context): F[MessageOut] =
      stream.compile.toList.map(l => MessageOut(l.map(_.value)))

    override def serverStreaming(messageIn: MessageIn)(implicit ctx: Context): Stream[F, MessageOut] =
      Stream.range(1, 4, 1)
        .map(i => MessageOut(List(s"${messageIn.value}.$i")))
        .covary[F]

    override def bidirectional(stream: Stream[F, MessageIn])(implicit ctx: Context) =
      stream.flatMap { message =>
        Stream.range(1, 3, 1)
          .map(i => MessageOut(List(s"${message.value}.$i")))
      }
  }
}
