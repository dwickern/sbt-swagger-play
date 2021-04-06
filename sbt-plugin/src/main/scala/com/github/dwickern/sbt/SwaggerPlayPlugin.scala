package com.github.dwickern.sbt

import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.SbtWeb.autoImport.Assets
import play.core.PlayVersion
import play.sbt.PlayWeb
import sbt.Keys._
import sbt._

import java.net.URLClassLoader
import java.security.{AccessController, PrivilegedAction}

object SwaggerPlayPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = PlayWeb && SbtWeb

  object autoImport {
    val swaggerPlayJson = taskKey[String]("Generated swagger JSON content")
    val swaggerPlayValidate = settingKey[Boolean]("Whether to run extra validation")
    val swaggerPlayHost = settingKey[Option[String]]("Swagger hostname entry")
    val swaggerPlayTarget = settingKey[File]("Target file path for swagger.json")
    val swaggerPlayResourceGenerator = taskKey[Seq[File]]("The sbt-web resource generator which produces swagger.json")
    val swaggerPlayRunnerArtifact = settingKey[ModuleID]("The runner artifact for the appropriate play+scala version")
    val swaggerPlayRunnerClasspath = taskKey[Classpath]("Injected classpath entries for the runner and its dependencies")
    val swaggerPlayClassLoader = taskKey[ClassLoader]("ClassLoader for the Play application containing the injected runner classes")
    val swaggerPlayConfiguration = settingKey[Option[Map[String, Any]]]("Play configuration to use instead of reading the application.conf file")
  }
  import autoImport._

  override lazy val projectSettings = Seq(
    swaggerPlayTarget := (Assets / resourceManaged).value / "swagger.json",
    swaggerPlayValidate := true,
    swaggerPlayHost := None,
    swaggerPlayConfiguration := None,
    swaggerPlayRunnerArtifact := {
      val playVersion = CrossVersion.partialVersion(PlayVersion.current) match {
        case Some((major, minor)) => s"$major.$minor"
        case None =>
      }
      "com.github.dwickern" %% s"sbt-swagger-play$playVersion-runner" % BuildInfo.version
    },
    swaggerPlayRunnerClasspath := {
      val log = streams.value.log("sbt-swagger-play")
      val retrieved = dependencyResolution.value.retrieve(
        swaggerPlayRunnerArtifact.value,
        scalaModuleInfo.value,
        managedDirectory.value,
        log
      )
      retrieved match {
        case Right(files) => Attributed.blankSeq(files)
        case Left(unresolvedWarning) =>
          import ShowLines._
          unresolvedWarning.lines.foreach(log.warn(_))
          throw unresolvedWarning.resolveException
      }
    },
    swaggerPlayClassLoader := {
      val externalClasspath = (Runtime / externalDependencyClasspath).value
      val dependencyClasspath = projectDependencyClasspathTask.value
      val runnerClasspath = swaggerPlayRunnerClasspath.value
      new URLClassLoader(
        (externalClasspath ++ dependencyClasspath ++ runnerClasspath).files.distinct.getURLs(),
        ClassLoader.getSystemClassLoader.getParent
      ) {
        override def toString = s"SwaggerPlay ClassLoader: ${getURLs.mkString(",")}"
      }
    },
    swaggerPlayJson := {
      val classLoader = swaggerPlayClassLoader.value
      withContextClassLoader(classLoader) {
        import scala.language.reflectiveCalls
        type SwaggerRunner = {
          def run(rootPath: File, host: String, validate: Boolean, configuration: Option[Map[String, Any]]): String
        }
        val mainClass = classLoader.loadClass("com.github.dwickern.swagger.SwaggerRunner$")
        val mainInstance = mainClass.getField("MODULE$").get(null).asInstanceOf[SwaggerRunner]
        mainInstance.run(baseDirectory.value, swaggerPlayHost.value.orNull, swaggerPlayValidate.value, swaggerPlayConfiguration.value)
      }
    },
    swaggerPlayResourceGenerator := {
      if (swaggerPlayResourceGenerator.inputFileChanges.hasChanges || !swaggerPlayTarget.value.exists()) {
        val jsonFile = swaggerPlayTarget.value
        IO.write(jsonFile, swaggerPlayJson.value)
        Seq(jsonFile)
      } else {
        Seq(swaggerPlayTarget.value)
      }
    },
    swaggerPlayResourceGenerator / sbt.nio.Keys.fileInputs ++= monitoredFilesSetting.value.map(dir => Glob(dir) / **),
    (Assets / resourceGenerators) += swaggerPlayResourceGenerator.taskValue
  )

  /**
   * Copied from [[play.sbt.PlayCommands.playMonitoredFilesTask]] except it's a setting
   * rather than a task so that it can be used as [[sbt.nio.Keys.fileInputs]]
   */
  private def monitoredFilesSetting: Def.Initialize[List[File]] = Def.settingDyn {
    val projectRef = thisProjectRef.value

    def filter = ScopeFilter(
      inDependencies(projectRef),
      inConfigurations(Compile, Assets)
    )

    Def.setting {
      val allDirectories =
        (unmanagedSourceDirectories ?? Nil).all(filter).value.flatten ++
          (unmanagedResourceDirectories ?? Nil).all(filter).value.flatten

      val existingDirectories = allDirectories.filter(_.exists)

      // Filter out directories that are sub paths of each other, by sorting them lexicographically, then folding, excluding
      // entries if the previous entry is a sub path of the current
      val distinctDirectories = existingDirectories
        .map(_.getCanonicalFile.toPath)
        .sorted
        .foldLeft(List.empty[java.nio.file.Path]) { (result, next) =>
          result.headOption match {
            case Some(previous) if next.startsWith(previous) => result
            case _                                           => next :: result
          }
        }

      distinctDirectories.map(_.toFile)
    }
  }

  private def projectDependencyClasspathTask: Def.Initialize[Task[Classpath]] = Def.taskDyn {
    val thisProj = thisProjectRef.value
    val config = Runtime
    val data = settingsData.value
    val deps = buildDependencies.value
    val transitiveDependencies = Classpaths.interSort(thisProj, config, data, deps)
    Def.value {
      val classpaths = transitiveDependencies.map { case (project, _) =>
        sbt.Classpaths.getClasspath(exportedProducts, project, config.name, data)
      }
      joinTasks(classpaths).join.map(_.flatten.distinct)
    }
  }

  private def withContextClassLoader[T](classLoader: ClassLoader)(f: => T): T = {
    val thread = Thread.currentThread
    val oldLoader = thread.getContextClassLoader
    AccessController.doPrivileged(
      new PrivilegedAction[T]() {
        def run: T = {
          thread.setContextClassLoader(classLoader)
          try f finally thread.setContextClassLoader(oldLoader)
        }
      },
      AccessController.getContext
    )
  }
}
