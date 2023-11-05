import PlayAxis.RichProjectMatrix

ThisBuild / organization := "com.github.dwickern"

lazy val play27 = PlayAxis("2.7.9")
lazy val play28 = PlayAxis("2.8.7")
lazy val play288 = PlayAxis("2.8.8")
lazy val play29 = PlayAxis("2.9.0")
lazy val play30 = PlayAxis("3.0.0")

lazy val scala212 = "2.12.13"
lazy val scala213 = "2.13.4"
lazy val scala3 = "3.3.1"

lazy val swaggerPlayVersion = "4.0.0"

lazy val root = (project in file("."))
  .aggregate(plugin.projectRefs: _*)
  .aggregate(pluginTests.projectRefs: _*)
  .aggregate(runner.projectRefs: _*)
  .aggregate(testPlugin)
  .settings(
    name := "sbt-swagger-play",
    publish / skip := true
  )

lazy val plugin = (projectMatrix in file("sbt-plugin"))
  .customRow(
    autoScalaLibrary = false,
    axisValues = Seq(VirtualAxis.jvm),
    _.enablePlugins(BuildInfoPlugin).settings(
      name := "sbt-swagger-play",
      sbtPlugin := true,
      addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.9" % Provided),
      buildInfoKeys := Seq[BuildInfoKey](version),
      buildInfoPackage := "com.github.dwickern.sbt",
    )
  )

lazy val pluginTests = plugin
  .enablePlugins(ScriptedPlugin)
  .settings(
    compile / skip := true,
    publish / skip := true,
    ideSkipProject.withRank(KeyRanks.Invisible) := true,
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      s"-Dplugin.version=${version.value}"
    ),
    scriptedBufferLog := true,
    scriptedDependencies := Def.task(())
      .dependsOn(plugin.jvm(false) / publishLocal)
      .dependsOn(testPlugin / publishLocal)
      .dependsOn(runner.projectRefs.map(_ / publishLocal).join)
      .value
  )
  .scriptedTests(play30, scala3)
  .scriptedTests(play30, scala213)
  .scriptedTests(play29, scala3)
  .scriptedTests(play29, scala213)
  .scriptedTests(play288, scala213)
  .scriptedTests(play288, scala212)
  .scriptedTests(play28, scala213)
  .scriptedTests(play28, scala212)
  .scriptedTests(play27, scala213)
  .scriptedTests(play27, scala212)

lazy val testPlugin = (project in file("test-plugin"))
  .settings(
    name := "sbt-swagger-play-testkit",
    sbtPlugin := true,
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.2",
    publish := {},
    PgpKeys.publishSigned := {},
  )

lazy val runner = (projectMatrix in file("runner"))
  .settings(
    name := "sbt-swagger-play-runner",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
      "ch.qos.logback" % "logback-classic" % "1.2.12" % Test,
    ),
  )
  .customRow(
    scalaVersions = Seq(scala3, scala213),
    axisValues = Seq(play30, VirtualAxis.jvm),
    _.settings(
      moduleName := "sbt-swagger-play3.0-runner",
      libraryDependencies ++= Seq(
        "com.github.dwickern" %% "swagger-play3.0" % swaggerPlayVersion,
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.3",
        "io.swagger" % "swagger-core" % "1.6.11",
        "io.swagger" % "swagger-parser" % "1.0.67",
      ),
    )
  )
  .customRow(
    scalaVersions = Seq(scala3, scala213),
    axisValues = Seq(play29, VirtualAxis.jvm),
    _.settings(
      moduleName := "sbt-swagger-play2.9-runner",
      libraryDependencies ++= Seq(
        "com.github.dwickern" %% "swagger-play2.9" % swaggerPlayVersion,
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.3",
        "io.swagger" % "swagger-core" % "1.6.11",
        "io.swagger" % "swagger-parser" % "1.0.67",
      ),
    )
  )
  .customRow(
    scalaVersions = Seq(scala213, scala212),
    axisValues = Seq(play288, VirtualAxis.jvm),
    _.settings(
      moduleName := "sbt-swagger-play2.8.8-runner",
      libraryDependencies ++= Seq(
        "com.github.dwickern" %% "swagger-play2.8" % swaggerPlayVersion,
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.11.1",
        "io.swagger" % "swagger-core" % "1.6.2",
        "io.swagger" % "swagger-parser" % "1.0.54",
      ),
    )
  )
  .customRow(
    scalaVersions = Seq(scala213, scala212),
    axisValues = Seq(play28, VirtualAxis.jvm),
    _.settings(
      moduleName := "sbt-swagger-play2.8-runner",
      libraryDependencies ++= Seq(
        "com.github.dwickern" %% "swagger-play2.8" % swaggerPlayVersion,
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
        "com.github.dwickern" %% "swagger-play2.7" % swaggerPlayVersion,
        "io.swagger" % "swagger-parser" % "1.0.47",
      ),
    )
  )

ThisBuild / homepage := scmInfo.value.map(_.browseUrl)
ThisBuild / licenses := Seq(License.MIT)
ThisBuild / developers := List(
  Developer(
    id = "dwickern",
    name = "Derek Wickern",
    email = "dwickern@gmail.com",
    url = url("https://github.com/dwickern")
  )
)
