lazy val example = project.in(file("."))
  .enablePlugins(CodeGenPlugin)
  .settings(
    skip in publish := true,
    libraryDependencies ++= Seq(
      "eu.monniot.rpc4s" %% "http4s-runtime" % Rpc4sVersion,
      "org.http4s"      %% "http4s-blaze-server"  % Http4sVersion % Test,
      "org.http4s"      %% "http4s-blaze-client"  % Http4sVersion % Test,
      "org.scalatest"   %% "scalatest"            % ScalaTestVersion  % Test
    ),
    scalacOptions := Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-Ypartial-unification"
    )
  )

val Rpc4sVersion = "0.1.0-SNAPSHOT"
val Http4sVersion = "0.18.12"
val ScalaTestVersion = "3.0.3"