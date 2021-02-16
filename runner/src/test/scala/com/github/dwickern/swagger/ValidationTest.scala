package com.github.dwickern.swagger

import io.swagger.annotations.ApiModelProperty
import io.swagger.converter.ModelConverters
import io.swagger.models.Model
import io.swagger.models.properties._
import org.scalatest.LoneElement
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.meta.{field, getter}
import scala.reflect.ClassTag

object Color extends Enumeration {
  type Color = Value
  val Red: Color = Value("red")
  val Blue: Color = Value("blue")
  val Green: Color = Value("green")
}

object EnumerationModels {
  case class Bad(
    color: Color.Color = Color.Green
  )
  case class Good(
    @ApiModelProperty(dataType = "com.github.dwickern.swagger.Color$")
    color: Color.Color = Color.Green
  )
}

object CollectionModels {
  case class Bad(
    values: Vector[Int]
  )
  case class Good(
    @ApiModelProperty(dataType = "list[integer]")
    values: Vector[Int]
  )
}

object HiddenPropertyModels {
  case class Bad(
    @ApiModelProperty(hidden = true)
    text: String
  )
  case class Good(
    @(ApiModelProperty @field @getter)(hidden = true)
    text: String
  )
}

class SwaggerPlayTest extends AnyFunSpec with Matchers with LoneElement {
  describe("enumerations") {
    it("incorrectly serialize to an Enumeration#Value if no dataType is set") {
      val model = read[EnumerationModels.Bad]
      val property = model.getProperties.get("color").asInstanceOf[RefProperty]
      property.getOriginalRef should === ("Value")
    }

    it("correctly serialize to a swagger enum if dataType is set") {
      val model = read[EnumerationModels.Good]
      val property = model.getProperties.get("color").asInstanceOf[StringProperty]
      property.getEnum should contain theSameElementsAs List("red", "green", "blue")
    }

    it("validator prints an error when used incorrectly") {
      val error = expectValidationError {
        read[EnumerationModels.Bad]
      }
      error.id should === ("enum")
    }
  }

  describe("collections of primitives") {
    it("incorrectly serialize to a List[Object] if no dataType is set") {
      val model = read[CollectionModels.Bad]
      val property = model.getProperties.get("values").asInstanceOf[ArrayProperty]
      property.getItems shouldBe an [ObjectProperty]
    }

    it("correctly serialize to a List[Int] if dataType is set") {
      val model = read[CollectionModels.Good]
      val property = model.getProperties.get("values").asInstanceOf[ArrayProperty]
      property.getItems shouldBe an [IntegerProperty]
    }

    it("validator prints an error when used incorrectly") {
      val error = expectValidationError {
        read[CollectionModels.Bad]
      }
      error.id should === ("collection-of-object")
    }
  }

  describe("hidden properties") {
    it("incorrectly visible if the annotation is not applied to the field") {
      val model = read[HiddenPropertyModels.Bad]
      model.getProperties should contain key "text"
    }

    it("correctly hidden if the annotation is applied to both the field and property") {
      val model = read[HiddenPropertyModels.Good]
      model.getProperties should be (null)
    }

    it("validator prints an error when used incorrectly") {
      val error = expectValidationError {
        read[HiddenPropertyModels.Bad]
      }
      error.id should === ("hidden")
    }
  }

  def read[A](implicit tag: ClassTag[A]): Model =
    ModelConverters.getInstance()
      .read(tag.runtimeClass)
      .loneElement
      .value

  val converter = new ValidationModelConverter(warn = err => throw ValidationException(err))

  case class ValidationException(err: ValidationWarning) extends RuntimeException

  def expectValidationError(f: => Unit): ValidationWarning = {
    ModelConverters.getInstance().addConverter(converter)
    try intercept[ValidationException](f).err finally ModelConverters.getInstance().removeConverter(converter)
  }
}
