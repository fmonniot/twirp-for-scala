package explorer.http4s

import cats.implicits._
import fs2.{Chunk, Segment, Stream}
import io.circe.{Decoder, Encoder, Printer}
import io.circe.syntax._
import org.http4s.Request
import org.http4s.client.Client

object client {

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
