import sbt.ScriptedPlugin.autoImport._
import sbt._
import sbt.internal.ProjectMatrix

case class PlayAxis(version: String) extends VirtualAxis.WeakAxis {
  val directorySuffix: String = "-play" + version
  val idSuffix: String = directorySuffix.replace('.', '_')
}

object PlayAxis {
  implicit class RichProjectMatrix(val matrix: ProjectMatrix) extends AnyVal {
    def scriptedTests(playAxis: PlayAxis, scalaVersion: String): ProjectMatrix = matrix.customRow(
      autoScalaLibrary = false,
      axisValues = Seq(playAxis, VirtualAxis.scalaPartialVersion(scalaVersion), VirtualAxis.jvm),
      _.settings(
        scriptedLaunchOpts ++= Seq(
          s"-Dscala.version=$scalaVersion",
          s"-Dplay.version=${playAxis.version}"
        )
      )
    )
  }
}
