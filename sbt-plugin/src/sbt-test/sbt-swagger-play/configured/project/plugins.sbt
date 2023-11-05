ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

val pluginVersion = sys.props("plugin.version")
val playVersion = sys.props("play.version")
val playOrg = if (playVersion.startsWith("2.")) "com.typesafe.play" else "org.playframework"

addSbtPlugin("com.github.dwickern" % "sbt-swagger-play" % pluginVersion)
addSbtPlugin("com.github.dwickern" % "sbt-swagger-play-testkit" % pluginVersion)
addSbtPlugin(playOrg % "sbt-plugin" % playVersion)
