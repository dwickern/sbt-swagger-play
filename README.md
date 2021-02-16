# sbt-swagger-play

[![build](https://github.com/dwickern/sbt-swagger-play/workflows/build/badge.svg)](https://github.com/dwickern/sbt-swagger-play/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.dwickern/sbt-swagger-play_2.12_1.0/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.dwickern/sbt-swagger-play_2.12_1.0)

An sbt plugin which runs [swagger-play](https://github.com/dwickern/swagger-play) as part of your build.

Advantages:
* Your application has no runtime dependency on swagger-play or swagger-core (only on swagger-annotations).
* Your application serves `swagger.json` as a static asset.
* This plugin optionally includes some extra validation for common Swagger annotation mistakes

## Usage

In `project/plugins.sbt`:
```sbt
addSbtPlugin("com.github.dwickern" % "sbt-swagger-play" % "0.1")
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
Also make sure to set up your [application.conf](https://github.com/dwickern/swagger-play#applicationconf---config-options).

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
