lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    version := "1.0",
    scalaVersion := sys.props("scala.version"),
    libraryDependencies += guice,
    libraryDependencies += "io.swagger" % "swagger-annotations" % "1.6.1",
    swaggerPlayConfiguration := Some(Map(
      "api.version" -> version.value,
      "swagger.api.basepath" -> "/api",
      "swagger.api.info.contact" -> "my contact",
      "swagger.api.info.title" -> "my title",
      "swagger.api.info.description" -> "my description",
      "swagger.api.info.license" -> "my license",
      "swagger.api.info.licenseUrl" -> "/license"
    ))
  )
