# sbt-swagger-play

[![build](https://github.com/dwickern/sbt-swagger-play/workflows/build/badge.svg)](https://github.com/dwickern/sbt-swagger-play/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.dwickern/sbt-swagger-play/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.dwickern/sbt-swagger-play)

An sbt plugin which runs [swagger-play](https://github.com/dwickern/swagger-play) as part of your build.

Advantages:
* Your application has no runtime dependency on swagger-play or swagger-core (only on swagger-annotations).
* Your application serves `swagger.json` as a static asset.
* This plugin optionally includes some extra validation for common Swagger annotation mistakes

## Usage

In `project/plugins.sbt`:
```sbt
addSbtPlugin("com.github.dwickern" % "sbt-swagger-play" % "0.5.0")
```

In `build.sbt`:
```sbt
libraryDependencies += "io.swagger" % "swagger-annotations" % "1.6.1"
```

In `conf/routes`:
```
GET     /swagger.json        controllers.Assets.at(path="/public", file="swagger.json")
```

That's it. Now you can annotate your REST endpoints and models with Swagger annotations.

## Configuration

This plugin supports the [swagger-play configuration options](https://github.com/dwickern/swagger-play#applicationconf---config-options) in `application.conf`:
```hocon
api.version = "beta"
swagger.api.basepath = "/api"
swagger.api.info = {
  contact = "my contact"
  title = "my title"
  description = "my description"
  termsOfService = ""
  license = "my license"
  licenseUrl = "/license"
}
```
See [example application.conf](sbt-plugin/src/sbt-test/sbt-swagger-play/configured/conf/application.conf).

Alternatively, you can pass those same configuration options (fully-qualified) in `build.sbt`.
This means you can easily pass settings from the build:
```sbt
swaggerPlayConfiguration := Some(Map(
  "api.version" -> version.value,
  "swagger.api.basepath" -> "/api",
  "swagger.api.info.contact" -> "my contact",
  "swagger.api.info.title" -> "my title",
  "swagger.api.info.description" -> "my description",
  "swagger.api.info.license" -> "my license",
  "swagger.api.info.licenseUrl" -> "/license"
))
```
See [example build.sbt](sbt-plugin/src/sbt-test/sbt-swagger-play/configured-from-build-file/build.sbt)

## Migrating from swagger-play

If you're already using swagger-play as a dependency in your application, this plugin is meant as a drop-in replacement.

In `conf/application.conf`, remove any reference to `SwaggerModule`:
```
# REMOVE:
play.modules.enabled += "play.modules.swagger.SwaggerModule"
```

In `conf/routes`, remove any reference to `ApiHelpController` and instead serve `swagger.json` as a static asset:
```
# REMOVE:
GET     /swagger.json        controllers.ApiHelpController.getResources

# ADD:
GET     /swagger.json        controllers.Assets.at(path="/public", file="swagger.json")
```
