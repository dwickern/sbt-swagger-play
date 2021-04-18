import sbtide.Keys.ideSkipProject
import sbt.Keys._
import sbt.ScriptedPlugin.autoImport._
import sbt._
import sbt.internal.ProjectMatrix

case class PlayAxis(version: String, idSuffix: String, directorySuffix: String) extends VirtualAxis.WeakAxis

object PlayAxis {
  def apply(version: String): PlayAxis =
    PlayAxis(version, "-play" + version.replace('.', '_'), "-play" + version)

  implicit class RichProjectMatrix(val matrix: ProjectMatrix) extends AnyVal {
    def scriptedTests(playAxis: PlayAxis, scalaVersion: String): ProjectMatrix = matrix.customRow(
      autoScalaLibrary = false,
      axisValues = Seq(playAxis, VirtualAxis.scalaPartialVersion(scalaVersion), VirtualAxis.jvm),
      _.enablePlugins(ScriptedPlugin).settings(
        compile / skip := true,
        publish / skip := true,
        ideSkipProject := true,
        moduleName := s"${moduleName.value}-test-${playAxis.version}-$scalaVersion",
        scriptedLaunchOpts ++= Seq(
          "-Xmx1024M",
          s"-Dplugin.version=${version.value}",
          s"-Dscala.version=$scalaVersion",
          s"-Dplay.version=${playAxis.version}"
        ),
        scriptedBufferLog := true
      )
    )
  }
}
