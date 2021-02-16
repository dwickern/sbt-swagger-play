package controllers

import io.swagger.annotations.{Api, ApiOperation}

import javax.inject._
import play.api._
import play.api.mvc._

@Api
@Singleton
class Application @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  @ApiOperation(value = "get model", response = classOf[models.DirectDependencyModel])
  def index = Action {
    Ok("")
  }
}
