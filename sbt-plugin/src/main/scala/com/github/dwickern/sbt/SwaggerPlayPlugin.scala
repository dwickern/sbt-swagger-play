package com.github.dwickern.sbt

import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.SbtWeb.autoImport.Assets
import play.core.PlayVersion
import play.sbt.PlayWeb
import sbt.Keys._
import sbt.{io => _, _}

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
  }
  import autoImport._

  override lazy val projectSettings = Seq(
    swaggerPlayTarget := (Assets / resourceManaged).value / "swagger.json",
    swaggerPlayValidate := true,
    swaggerPlayHost := None,
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
          def run(rootPath: File, host: String, validate: Boolean): String
        }
        val mainClass = classLoader.loadClass("com.github.dwickern.swagger.SwaggerRunner$")
        val mainInstance = mainClass.getField("MODULE$").get(null).asInstanceOf[SwaggerRunner]
        mainInstance.run(baseDirectory.value, swaggerPlayHost.value.orNull, swaggerPlayValidate.value)
      }
    },
    swaggerPlayResourceGenerator := {
      val out = swaggerPlayTarget.value
      IO.write(out, swaggerPlayJson.value)
      Seq(out)
    },
    (Assets / resourceGenerators) += swaggerPlayResourceGenerator.taskValue
  )

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
