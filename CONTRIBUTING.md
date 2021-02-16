# How To Contribute

## Architecture

This repository contains these components:

* `sbt-plugin` - The main sbt plugin
* `runner` - The runtime entrypoint, cross-compiled for different Play and Scala versions
* `test-plugin` - Extra testing utilities used in `scripted` tests (not published)

Here's how this plugin works:

1. The sbt plugin picks an appropriate runner based on the current application's Play and Scala version
1. The sbt plugin constructs a ClassLoader containing the application's classpath and the runner's classpath
1. The sbt plugin calls the runner's entrypoint
1. The runner generates a Swagger spec, by reflecting over the application's routes and models
1. The sbt plugin adds the generated `swagger.json` as a managed resource

## Running tests

* `test` – Runs the unit tests
* `scripted` – Runs [sbt script tests](http://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html)
