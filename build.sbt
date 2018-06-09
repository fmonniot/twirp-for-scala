lazy val CatsEffectVersion = "1.0.0-RC"
lazy val Fs2Version        = "0.10.4"
lazy val Http4sVersion     = "0.18.12"
lazy val CirceVersion      = "0.9.2"
lazy val LogbackVersion    = "1.2.3"
lazy val ScalaTestVersion  = "3.0.3"
lazy val ScalaCheckVersion = "1.13.4"


lazy val root = (project in file("."))
  .settings(publish := (), publishLocal := ())
  .aggregate(core, `http4s-runtime`, `sbt-codegen-plugin`)

lazy val core = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest"   %% "scalatest"            % ScalaTestVersion  % Test,
      "org.scalacheck"  %% "scalacheck"           % ScalaCheckVersion % Test
    )
  )

lazy val `http4s-runtime` = project
  .settings(commonSettings)
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"   %% "cats-effect"          % CatsEffectVersion,
      "co.fs2"          %% "fs2-core"             % Fs2Version,

      "org.http4s"      %% "http4s-client"        % Http4sVersion,
      "org.http4s"      %% "http4s-circe"         % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"           % Http4sVersion,

      "io.circe"        %% "circe-core"           % CirceVersion,
      "io.circe"        %% "circe-generic"        % CirceVersion,


      "org.http4s"      %% "http4s-blaze-server"  % Http4sVersion % Test,
      "org.http4s"      %% "http4s-blaze-client"  % Http4sVersion % Test,
      "org.scalatest"   %% "scalatest"            % ScalaTestVersion  % Test,
      "org.scalacheck"  %% "scalacheck"           % ScalaCheckVersion % Test
    )
  )

lazy val `sbt-codegen-plugin` = (project in file("sbt-codegen-plugin"))
  .settings(commonSettings)
  .settings(
    sbtPlugin := true,
    crossSbtVersions := List(sbtVersion.value, "0.13.17"),
    addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.18"),
    libraryDependencies ++= List(
      "com.thesamet.scalapb" %% "compilerplugin" % "0.7.4"
    )
  )


val commonSettings = Seq(
  organization := "eu.monniot.rpc4s",
  scalaVersion := "2.12.6",
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