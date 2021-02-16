lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    scalaVersion := sys.props("scala.version"),
    libraryDependencies += guice,
    libraryDependencies += "io.swagger" % "swagger-annotations" % "1.6.1",
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
  )
