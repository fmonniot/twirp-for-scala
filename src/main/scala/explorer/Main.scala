package explorer

import cats.implicits._
import cats.effect.{Effect, IO, Sync, Timer}
import explorer.generated_manual.{Message1, Message3}
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import org.http4s.client.blaze.Http1Client
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import core._
import org.http4s.Uri.uri


// Streaming doesn't support Protobuf in its current format
// What's the story regarding Errors ? For now we are living in a perfect world but we know how it goes
// For rpc it's relatively easy, as we can return an error instead of the response
// for client streaming the same
// but for server streaming ? What happens when the server start streaming and then encounters an error ?
object Main extends StreamApp[IO] {

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] =
    createStream[IO]

  // That's my implementation \o/
  def impl[F[_]](implicit F: Sync[F]) = new generated_manual.Service[F] {

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

  // Parameterized function on the effect type
  def createStream[F[_] : Effect: Timer]: Stream[F, ExitCode] = {
    implicit val ctx: core.Context = core.NoContext

    def stdout(s: String) = Stream.eval(Sync[F].delay(println(s)))

    def wait(d: FiniteDuration) = Stream.eval(Timer[F].sleep(d))

    val tests = for {
      httpClient <- Http1Client.stream[F]()
      client = generated_manual.httpClient(uri("http://localhost:8080"), httpClient)


      _ <- stdout("Wait a bit before making the requests")
      _ <- wait(1.second)

      _ <- stdout("Making the RPC request")
      answer <- Stream.eval(client.rpc(Message1("1.1")))
      _ <- stdout(s"Got the correct answer: ${answer == Message3(List("1.1"))}")

      _ <- stdout("Making the client streaming request")
      answer <- Stream.eval(client.clientStreaming(Stream(Message1("2.1"), Message1("2.2"))))
      _ <- stdout(s"Got the correct answer: ${answer == Message3(List("2.1", "2.2"))}")

      _ <- stdout("Making the server streaming request")
      answer <- Stream.eval(client.serverStreaming(Message1("3")).compile.toList)
      expected = List(Message3(List("3.1")), Message3(List("3.2")), Message3(List("3.3")))
      _ <- stdout(s"Got the correct answer: ${answer == expected}")

      _ <- stdout("Making the bidirectional request")
      answer <- Stream.eval(client.bidirectional(Stream(Message1("4"))).compile.toList)
      expected = List(Message3(List("4.1")), Message3(List("4.2")))
      _ <- stdout(s"Got the correct answer: ${answer == expected}")

      _ <- wait(1.second)

    } yield ExitCode.Success

    val server = BlazeBuilder[F]
      .mountService(generated_manual.httpServer(impl))
      .bindHttp() // Default address `localhost:8080`
      .serve

    tests.mergeHaltL(server)
  }
}
