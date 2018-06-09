import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.Descriptors._
import com.google.protobuf.compiler.PluginProtos
import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorRequest, CodeGeneratorResponse}
import protocbridge.{Artifact, ProtocCodeGenerator}

import scala.collection.JavaConverters._
import scalapb.compiler._
import scalapb.options.compiler.Scalapb

// Extension for protobuf to hook in our code gen
object CodeGenerator extends ProtocCodeGenerator {


  override def run(req: Array[Byte]): Array[Byte] = {
    println("Running Code Generation")
    val registry = ExtensionRegistry.newInstance()
    Scalapb.registerAllExtensions(registry)
    val request = CodeGeneratorRequest.parseFrom(req, registry)

    handleCodeGeneratorRequest(request).toByteArray
  }

  def handleCodeGeneratorRequest(request: PluginProtos.CodeGeneratorRequest): PluginProtos.CodeGeneratorResponse = {
    val b = CodeGeneratorResponse.newBuilder

    ProtobufGenerator.parseParameters(request.getParameter) match {
      case Right(params) =>
        try {
          val filesByName: Map[String, FileDescriptor] =
            request.getProtoFileList.asScala.foldLeft[Map[String, FileDescriptor]](Map.empty) {
              case (acc, fp) =>
                val dependencies = fp.getDependencyList.asScala.map(acc)
                acc + (fp.getName -> FileDescriptor.buildFrom(fp, dependencies.toArray))
            }

          val files = request.getFileToGenerateList.asScala.map(filesByName).flatMap(generateServiceFiles(_, params))
          b.addAllFile(files.asJava)

        } catch {
          case e: GeneratorException =>
            b.setError(e.message)
        }

      case Left(error) =>
        b.setError(error)
    }

    b.build()
  }

  def generateServiceFiles(file: FileDescriptor,
                           params: GeneratorParams): Seq[PluginProtos.CodeGeneratorResponse.File] = {
    println("Services: " + file.getServices.asScala)
    file.getServices.asScala.flatMap { service =>
      val serviceFile = {
        val servicePrinter = new Http4sServicePrinter(service, params)

        import servicePrinter.{FileDescriptorPimp, ServiceDescriptorPimp}
        val code = servicePrinter.printService(FunctionalPrinter()).result()

        CodeGeneratorResponse.File.newBuilder()
          .setName(file.scalaDirectory + "/" + service.name + "Rpc4s.scala")
          .setContent(code)
          .build
      }

      val jsonFile = {
        val printer = new CirceSerdePrinter(service, params)
        import printer.{FileDescriptorPimp, ServiceDescriptorPimp}

        val code = printer.print(FunctionalPrinter()).result()

        CodeGeneratorResponse.File.newBuilder()
          .setName(file.scalaDirectory + "/" + service.name + "JsonInstances.scala")
          .setContent(code)
          .build
      }

      Seq(serviceFile, jsonFile)
    }
  }

  // this is not a suggestion, but an artifact that MUST be available in the class path
  override def suggestedDependencies: Seq[Artifact] = Seq(
    //    Artifact("eu.monniot.rpc", "rpc-runtime", scalapb.compiler.Version.scalapbVersion, crossVersion = true)
  )
}