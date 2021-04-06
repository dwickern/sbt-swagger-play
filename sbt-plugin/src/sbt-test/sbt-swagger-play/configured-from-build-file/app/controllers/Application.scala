package controllers

import io.swagger.annotations.{Api, ApiOperation}

import javax.inject._
import play.api._
import play.api.mvc._

@Api
@Singleton
class Application @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  @ApiOperation(value = "say hello", response = classOf[String])
  def index = Action {
    Ok("hello")
  }
}
