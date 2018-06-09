package printers

import com.google.protobuf.Descriptors.{MethodDescriptor, ServiceDescriptor}
import scalapb.compiler.FunctionalPrinter.PrinterEndo
import scalapb.compiler.{FunctionalPrinter, GeneratorParams, StreamType}

// Largely inspired by the fs2-grpc project: Need to mention it somewhere.
// Also we needs to generate circe decoder / encoder, as it's required to have this generated code compile
// Need to setup a
class Http4sServicePrinter(service: ServiceDescriptor, override val params: GeneratorParams) extends Printer {

  def print(printer: FunctionalPrinter): FunctionalPrinter =
    printer
      .add("package " + service.getFile.scalaPackageName, "")
      .add(imports: _*)
      .add("")
      .call(serviceTrait)
      .add("")
      .call(serviceObject)

  private[this] val imports = Seq(
    "import _root_.cats.implicits._",
    "import _root_.cats.effect.Sync",
    "import org.http4s.client.Client",
    "import org.http4s.{EntityDecoder, EntityEncoder, HttpService, Request, Uri}",
    "import org.http4s.circe._",
    "import fs2.Stream",
    "import _root_.explorer.core._",
    "import _root_.explorer.http4s.client._",
    "import _root_.explorer.http4s.server._",
    s"import ${service.name}JsonInstances._"
  )

  private[this] def serviceTrait: PrinterEndo =
    _.add(s"trait ${service.name}[F[_]] {")
      .indent
      .call(serviceMethods)
      .outdent
      .add("}")

  private[this] def serviceMethods: PrinterEndo =
    _.seq(service.methods.map(serviceMethodSignature))

  private[this] def serviceMethodSignature(method: MethodDescriptor) = {
    s"def ${method.name}" + (method.streamType match {
      case StreamType.Unary =>
        s"(request: ${method.scalaIn})(implicit ctx: Context): F[${method.scalaOut}]"
      case StreamType.ClientStreaming =>
        s"(request: _root_.fs2.Stream[F, ${method.scalaIn}])(implicit ctx: Context): F[${method.scalaOut}]"
      case StreamType.ServerStreaming =>
        s"(request: ${method.scalaIn})(implicit ctx: Context): _root_.fs2.Stream[F, ${method.scalaOut}]"
      case StreamType.Bidirectional =>
        s"(request: _root_.fs2.Stream[F, ${method.scalaIn}])(implicit ctx: Context): _root_.fs2.Stream[F, ${method.scalaOut}]"
    })
  }

  private[this] def serviceObject: PrinterEndo =
    _.add(s"object ${service.name} {")
      .indent
      .call(serviceClient)
      .call(serviceServer)
      .outdent
      .add("}")

  //
  // HTTP4S HTTP SERVICE GENERATION
  //

  private[this] def serviceServer: PrinterEndo = {
    _.add(s"def httpServer[F[_]: Sync](service: ${service.name}[F]): HttpService[F] = {")
      .indent
      .add(
        "// Http4s DSL in F",
        "val dsl = org.http4s.dsl.Http4sDsl[F]",
        "import dsl._",
        "",
        "// Adding entity decoder and encoder, for the non-streaming part"
      )
      .call(encoderDecoder(service.methods, serverEntity))
      .add("", "HttpService {")
      .indent
      .call(service.methods.map(serverMethodCase): _*)
      .outdent
      .add("}")
      .outdent
      .add("}")
  }

  private[this] def serverEntity(method: MethodDescriptor): Seq[String] =
    Seq(
      s"EntityDecoder[F, ${method.scalaIn}] = jsonOf[F, ${method.scalaIn}]",
      s"EntityEncoder[F, ${method.scalaOut}] = jsonEncoderOf[F, ${method.scalaOut}]"
    )

  private[this] def serverMethodCase(method: MethodDescriptor): PrinterEndo =
    _.add(s"""case req@GET -> Root / "${service.getFile.scalaPackageName}" / "${service.name}" / "${method.name}" => """)
      .indent
      .add(method.streamType match {
        case StreamType.Unary =>
          s"rpc(req, service.${method.name}(_: ${method.scalaIn})(_: Context))"
        case StreamType.ClientStreaming =>
          s"clientStreaming(req, service.clientStreaming(_: _root_.fs2.Stream[F, ${method.scalaIn}])(_: Context))"
        case StreamType.ServerStreaming =>
          s"serverStreaming(req, service.${method.name}(_: ${method.scalaIn})(_: Context))"
        case StreamType.Bidirectional =>
          s"bidirectional(req, service.${method.name}(_: _root_.fs2.Stream[F, ${method.scalaIn}])(_: Context))"
      })
      .outdent
      .add("")

  //
  // HTTP4S CLIENT GENERATION
  //

  private[this] def serviceClient: PrinterEndo =
    _.add(
      s"def httpClient[F[_]: Sync](base: Uri, client: Client[F]): ${service.name}[F] = new ${service.name}[F] {"
    )
      .indent
      .add(
        s"""val baseService = base / "${service.getFile.scalaPackageName}" / "${service.name}" """,
        "",
        "// Adding entity decoder and encoder, for the non-streaming part"
      )
      .call(encoderDecoder(service.methods, clientEntity))
      .call(service.methods.map(clientMethodCase): _*)
      .outdent
      .add("}")

  private[this] def clientEntity(method: MethodDescriptor): Seq[String] =
    Seq(
      s"EntityEncoder[F, ${method.scalaIn}] = jsonEncoderOf[F, ${method.scalaIn}]",
      s"EntityDecoder[F, ${method.scalaOut}] = jsonOf[F, ${method.scalaOut}]"
    )

  private[this] def clientMethodCase(m: MethodDescriptor): PrinterEndo = p =>
    m.streamType match {
      case StreamType.Unary =>
        p.add("", s"override def ${m.name}(in: ${m.scalaIn})(implicit ctx: Context): F[${m.scalaOut}] =")
          .indent
          .add("for {")
          .indent
          .add(
            s"""req <- Request[F](uri = baseService / "${m.name}").withBody(in)""",
            s"res <- client.expect[${m.scalaOut}](req)"
          )
          .outdent
          .add("} yield res")
          .outdent

      case StreamType.ClientStreaming =>
        p.add("", s"override def ${m.name}(stream: Stream[F, ${m.scalaIn}])(implicit ctx: Context): F[${m.scalaOut}] =")
          .indent
          .add(s"client.expect[${m.scalaOut}](")
          .indent
          .add(s"""Request[F](uri = baseService / "${m.name}")""")
          .indent
          .add(s".withBodyStream(streamBodyToRequest[F, ${m.scalaIn}](stream))")
          .outdent
          .outdent
          .add(")")
          .outdent

      case StreamType.ServerStreaming =>
        p.add("", s"override def ${m.name}(in: ${m.scalaIn})(implicit ctx: Context): Stream[F, ${m.scalaOut}] =")
          .indent
          .add("for {")
          .indent
          .add(
            s"""req <- Stream.eval(Request[F](uri = baseService / "${m.name}").withBody(in))""",
            s"res <- client.streamResponse[${m.scalaOut}](req)"
          )
          .outdent
          .add("} yield res")
          .outdent

      case StreamType.Bidirectional =>
        p.add("", s"override def ${m.name}(stream: Stream[F, ${m.scalaIn}])(implicit ctx: Context): Stream[F, ${m.scalaOut}] =")
          .indent
          .add(s"client.streamResponse[${m.scalaOut}](")
          .indent
          .add(s"""Request[F](uri = baseService / "${m.name}")""")
          .indent
          .add(s".withBodyStream(streamBodyToRequest[F, ${m.scalaIn}](stream))")
          .outdent
          .outdent
          .add(")")
          .outdent
    }

  //
  // HTTP4S CLIENT AND SERVER HELPERS
  //

  private[this] def encoderDecoder(methods: Seq[MethodDescriptor], gen: MethodDescriptor => Seq[String]): PrinterEndo =
    _.seq(service
      .methods
      .flatMap(gen)
      .distinct
      .zipWithIndex
      .map { case (end, index) => s"implicit val e$index: $end" }
    )
}
