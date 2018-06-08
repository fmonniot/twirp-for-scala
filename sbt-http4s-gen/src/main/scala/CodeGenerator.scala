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
    file.getServices.asScala.map { service =>
      val p = new Http4sServicePrinter(service, params)

      import p.{FileDescriptorPimp, ServiceDescriptorPimp}
      val code = p.printService(FunctionalPrinter()).result()

      val b    = CodeGeneratorResponse.File.newBuilder()
      b.setName(file.scalaDirectory + "/" + service.objectName + "Fs2.scala")
      b.setContent(code)
      println(b.getName)

      b.build
    }
  }

  // this is not a suggestion, but an artifact that MUST be available in the class path
  override def suggestedDependencies: Seq[Artifact] = Seq(
//    Artifact("eu.monniot.rpc", "rpc-runtime", scalapb.compiler.Version.scalapbVersion, crossVersion = true)
  )
}