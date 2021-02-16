ThisBuild / organization := "com.github.dwickern"
ThisBuild / version := "0.1-SNAPSHOT"

lazy val play27 = ConfigAxis("play27", "play2.7")
lazy val play28 = ConfigAxis("play28", "play2.8")

lazy val scala212 = "2.12.13"
lazy val scala213 = "2.13.4"

lazy val plugin = (project in file("sbt-plugin"))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(ScriptedPlugin)
  .settings(
    name := "sbt-swagger-play",
    sbtPlugin := true,
    addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.9" % Provided),
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "com.github.dwickern.sbt",
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      s"-Dplugin.version=${version.value}",
      "-Dplay.version=2.8.7",
      "-Dscala.version=2.13.3"
    ),
    scriptedDependencies := {
      def use(@deprecated("unused", "") x: Any*): Unit = () // avoid unused warnings
      use(
        scriptedDependencies.value,
        (testPlugin / publishLocal).value,
        runner.projectRefs.map(_ / publishLocal).join.value
      )
    },
  )

lazy val testPlugin = (project in file("test-plugin"))
  .settings(
    name := "sbt-swagger-play-testkit",
    sbtPlugin := true,
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.2",
    publish := {},
  )

lazy val runner = (projectMatrix in file("runner"))
  .settings(
    name := "sbt-swagger-play-runner",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.2" % Test,
      "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
    ),
  )
  .customRow(
    scalaVersions = Seq(scala213, scala212),
    axisValues = Seq(play28, VirtualAxis.jvm),
    _.settings(
      moduleName := "sbt-swagger-play2.8-runner",
      libraryDependencies ++= Seq(
        "com.github.dwickern" %% "swagger-play2.8" % "3.0.0",
        "io.swagger" % "swagger-parser" % "1.0.54",
      ),
    )
  )
  .customRow(
    scalaVersions = Seq(scala213, scala212),
    axisValues = Seq(play27, VirtualAxis.jvm),
    _.settings(
      moduleName := "sbt-swagger-play2.7-runner",
      libraryDependencies ++= Seq(
        "com.github.dwickern" %% "swagger-play2.7" % "3.0.0",
        "io.swagger" % "swagger-parser" % "1.0.47",
      ),
    )
  )
