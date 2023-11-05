package com.github.dwickern.sbt.testkit

import play.api.libs.json.{JsValue, Json}
import sbt._

import scala.annotation.tailrec

object SwaggerPlayTestPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  object autoImport {
    val verifyHttpRequest = inputKey[Unit]("perform a http GET request and verify that it was successful")
    val verifyJsonEqual = inputKey[Unit]("compare two files for logical JSON equality")
  }
  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    verifyHttpRequest := {
      Def.spaceDelimited().parsed match {
        case Seq(p) => httpRequest(url(p))
        case _ => sys.error("Expected 1 argument")
      }
    },
    verifyJsonEqual := {
      Def.spaceDelimited().parsed match {
        case Seq(f1, f2) =>
          if (toJson(f1) != toJson(f2)) {
            sys.error(s"JSON did not match: $f1; $f2")
          }
        case _ => sys.error("Expected 2 arguments")
      }
    },
  )

  private def toJson(path: String): JsValue = {
    Json.parse(IO.readBytes(file(path)))
  }

  private def httpRequest(requestUrl: URL): Unit = {
    @tailrec def read(attempts: Int): String = {
      try {
        val conn = requestUrl.openConnection().asInstanceOf[java.net.HttpURLConnection]
        if (conn.getResponseCode != 200) {
          throw new Exception(s"response code = ${conn.getResponseCode}")
        }
        IO.readStream(conn.getInputStream)
      } catch {
        case e: Exception =>
          if (attempts <= 0) {
            throw new Exception(s"Failed to read from $requestUrl", e)
          }
          // retry
          Thread.sleep(1000)
          read(attempts - 1)
      }
    }
    read(attempts = 30)
  }
}
