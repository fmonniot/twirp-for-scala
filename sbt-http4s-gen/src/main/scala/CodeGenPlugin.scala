import protocbridge.{JvmGenerator, Target}
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbtprotoc.ProtocPlugin.autoImport.PB

object CodeGenPlugin extends AutoPlugin {

  override def requires = sbtprotoc.ProtocPlugin && JvmPlugin

  override def trigger = NoTrigger

  object autoImport {
    val scalapbCodeGenerators =
      settingKey[Seq[Target]]("Code generators for scalapb")
  }

  import autoImport._

  def scalaPbGen = scalapb.gen(
    flatPackage = false,
    javaConversions = false,
    grpc = false,
    singleLineToProtoString = false,
    asciiFormatToString = false
  )

  val ourCodeGenOptions = (JvmGenerator("scala-codegen", CodeGenerator), Seq.empty[String])

  override def projectSettings: Seq[Def.Setting[_]] = List(
    PB.targets := scalapbCodeGenerators.value,
    scalapbCodeGenerators := {
      val t1 = Target(scalaPbGen, (sourceManaged in Compile).value / "scalapb")
      val t2 = Target(ourCodeGenOptions, (sourceManaged in Compile).value / "our-codegen")

      Seq(t1, t2)
    }
  )

}
