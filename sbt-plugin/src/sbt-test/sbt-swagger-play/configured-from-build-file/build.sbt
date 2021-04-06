lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    scalaVersion := sys.props("scala.version"),
    libraryDependencies += guice,
    libraryDependencies += "io.swagger" % "swagger-annotations" % "1.6.1",
    swaggerPlayConfiguration := Some(Map(
      "play.http.secret.key" -> "HH<9q6]:4=Erks[qh/qdRyy/fsNaB_BBf``QAeIR12Hy3KP9SJnx?8z98c5U`e22",
      "api.version" -> "beta",
      "swagger.api.basepath" -> "/api",
      "swagger.api.info.contact" -> "my contact",
      "swagger.api.info.title" -> "my title",
      "swagger.api.info.description" -> "my description",
      "swagger.api.info.termsOfService" -> "",
      "swagger.api.info.license" -> "my license",
      "swagger.api.info.licenseUrl" -> "/license"
    ))
  )
