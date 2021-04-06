package com.github.dwickern.swagger

import io.swagger.converter.ModelConverters
import io.swagger.parser.SwaggerParser
import io.swagger.util.Json
import org.slf4j.LoggerFactory
import play.api.{Configuration, Environment, Mode}
import play.modules.swagger.SwaggerPluginImpl

import java.io.File
import java.util.{Map => JMap}

import scala.jdk.CollectionConverters._

object SwaggerRunner {
  private lazy val logger = LoggerFactory.getLogger(getClass)

  // configuration should be an Option, but it does not play nice with reflective loading, so we use Java-style nullable map instead
  def run(rootPath: File, host: String, validate: Boolean, configuration: JMap[String, Any]): String = {
    val classLoader = getClass.getClassLoader
    val env = Environment(rootPath, classLoader, Mode.Dev)
    val conf = if (configuration == null) {
      Configuration.load(env)
    } else {
      Configuration.from(defaultConfiguration ++ configuration.asScala)
    }
    if (validate) {
      ModelConverters.getInstance().addConverter(new ValidationModelConverter(warning => logger.warn(warning.message)))
    }
    val plugin = new SwaggerPluginImpl(env, conf)
    val swaggerModel = plugin.apiListingCache.listing(host)
    val json = Json.pretty(swaggerModel)
    if (validate) {
      new SwaggerParser().readWithInfo(json).getMessages.forEach(logger.warn(_))
    }
    json
  }

  // as map-based configurations will throw if keys are missing, we need to create a map with defaults for them
  private lazy val defaultConfiguration: Map[String, Any] = Map[String, Any](
    // https://github.com/swagger-api/swagger-play/blob/master/src/main/scala/play/modules/swagger/PlaySwaggerConfig.scala#L28
    "api.version" -> "",
    "swagger.api.info.description" -> "",
    "swagger.api.host" -> "",
    "swagger.api.basepath" -> "",
    "swagger.api.schemes" -> Nil,
    "swagger.api.info.title" -> "",
    "swagger.api.info.contact" -> "",
    "swagger.api.info.termsOfServiceUrl" -> "",
    "swagger.api.info.license" -> "",
    "swagger.api.info.licenseUrl" -> "",
    "swagger.filter" -> null,

    // https://github.com/swagger-api/swagger-play/blob/master/src/main/scala/play/modules/swagger/SwaggerPlugin.scala#L52
    "play.http.router" -> null
  )
}
