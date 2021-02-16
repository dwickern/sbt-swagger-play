lazy val commonSettings = Seq(
  scalaVersion := sys.props("scala.version"),
  libraryDependencies += "io.swagger" % "swagger-annotations" % "1.6.1",
)

lazy val mainApp = (project in file("main-app"))
  .enablePlugins(PlayScala)
  .dependsOn(directDependency)
  .settings(
    commonSettings,
    libraryDependencies += guice,
  )

lazy val directDependency = (project in file("dependency-direct"))
  .dependsOn(transitiveDependency)
  .settings(commonSettings)

lazy val transitiveDependency = (project in file("dependency-transitive"))
  .settings(commonSettings)
