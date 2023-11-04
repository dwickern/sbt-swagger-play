ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("com.github.dwickern" % "sbt-swagger-play" % sys.props("plugin.version"))
addSbtPlugin("com.github.dwickern" % "sbt-swagger-play-testkit" % sys.props("plugin.version"))
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % sys.props("play.version"))
