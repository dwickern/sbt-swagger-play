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
      Configuration.reference ++ Configuration.from(configuration.asScala.toMap)
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
}
