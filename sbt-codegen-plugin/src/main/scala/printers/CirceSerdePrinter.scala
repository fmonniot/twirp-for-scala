package printers

import com.google.protobuf.Descriptors.{MethodDescriptor, ServiceDescriptor}
import scalapb.compiler.FunctionalPrinter.PrinterEndo
import scalapb.compiler.{FunctionalPrinter, GeneratorParams}

// Largely inspired by the fs2-grpc project: Need to mention it somewhere.
// Also we needs to generate circe decoder / encoder, as it's required to have this generated code compile
// Need to setup a
class CirceSerdePrinter(service: ServiceDescriptor, override val params: GeneratorParams) extends Printer {

  def print(printer: FunctionalPrinter): FunctionalPrinter =
    printer
      .add("package " + service.getFile.scalaPackageName, "")
      .add("import _root_.io.circe.{Decoder, Encoder}")
      .add("import _root_.io.circe.generic.semiauto._")
      .add("")
      .add(s"object ${service.name}JsonInstances {")
      .indent
      .call(encoderDecoder(service.methods, entity))
      .outdent
      .add("}")

  private[this] def entity(method: MethodDescriptor): Seq[String] =
    Seq(
      s"Encoder[${method.scalaIn}] = deriveEncoder[${method.scalaIn}]",
      s"Decoder[${method.scalaIn}] = deriveDecoder[${method.scalaIn}]",
      s"Encoder[${method.scalaOut}] = deriveEncoder[${method.scalaOut}]",
      s"Decoder[${method.scalaOut}] = deriveDecoder[${method.scalaOut}]"
    )

  //
  // HTTP4S CLIENT AND SERVER HELPERS
  //

  private[this] def encoderDecoder(methods: Seq[MethodDescriptor], gen: MethodDescriptor => Seq[String]): PrinterEndo =
    _.seq(service
      .methods
      .flatMap(gen)
      .distinct
      .zipWithIndex
      .map { case (end, index) => s"implicit val circeInstance$index: $end" }
    )
}
