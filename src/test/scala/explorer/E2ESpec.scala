package explorer

import cats.effect.{IO, Sync}
import explorer.generated_manual.{Message1, Message3}
import fs2.Stream
import org.http4s.Uri.uri
import org.http4s.client.blaze.Http1Client
import org.http4s.server.blaze.BlazeBuilder
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global

class E2ESpec extends FlatSpec with Matchers {

  it should "make a request and return the result" in {
    implicit val ctx: core.Context = core.NoContext

    val tests = for {
      httpClient <- Http1Client.stream[IO]()
      client = generated.httpClient(uri("http://localhost:8080"), httpClient)

      _ <- stdout("Making the RPC request")
      answer <- Stream.eval(client.rpc(Message1("1.1")))
      _ = answer shouldEqual Message3(List("1.1"))

      _ <- stdout("Making the client streaming request")
      answer <- Stream.eval(client.clientStreaming(Stream(Message1("2.1"), Message1("2.2"))))
      _ = answer shouldEqual Message3(List("2.1", "2.2"))

      _ <- stdout("Making the server streaming request")
      answer <- Stream.eval(client.serverStreaming(Message1("3")).compile.toList)
      _ = answer shouldEqual List(Message3(List("3.1")), Message3(List("3.2")), Message3(List("3.3")))

      _ <- stdout("Making the bidirectional request")
      answer <- Stream.eval(client.bidirectional(Stream(Message1("4"))).compile.toList)
      _ = answer shouldEqual List(Message3(List("4.1")), Message3(List("4.2")))
    } yield ()

    tests.mergeHaltL(server).compile.drain.unsafeRunSync()
  }

  // TODO Find a standardized way of expression errors
  // This is how Google is doing it:
  // https://github.com/googleapis/googleapis/blob/master/google/rpc/status.proto
  it should "return an error when a service fail" in {
    pending
  }

  val server = BlazeBuilder[IO]
    .mountService(generated.httpServer(impl))
    .bindHttp() // Default address `localhost:8080`
    .serve

  def stdout(s: String) = Stream.eval(IO(println(s)))

  def impl[F[_]](implicit F: Sync[F]) = new generated_manual.Service[F] {

    import cats.implicits._
    import core.Context

    override def rpc(message1: Message1)(implicit ctx: Context) =
      F.pure(Message3(a = List(message1.value)))

    override def clientStreaming(stream: Stream[F, Message1])(implicit ctx: Context): F[Message3] =
      stream.compile.toList.map(l => Message3(a = l.map(_.value)))

    override def serverStreaming(message1: Message1)(implicit ctx: Context): Stream[F, Message3] =
      Stream.range(1, 4, 1)
        .map(i => Message3(a = List(s"${message1.value}.$i")))
        .covary[F]

    override def bidirectional(stream: Stream[F, Message1])(implicit ctx: Context) =
      stream.flatMap { message =>
        Stream.range(1, 3, 1)
          .map(i => Message3(a = List(s"${message.value}.$i")))
      }
  }
}
