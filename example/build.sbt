lazy val root = project.in(file("."))
  .enablePlugins(CodeGenPlugin)
  .settings(
    skip in publish := true
  )