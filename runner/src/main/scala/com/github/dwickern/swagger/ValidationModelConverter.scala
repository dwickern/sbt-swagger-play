package com.github.dwickern.swagger

import com.fasterxml.jackson.databind.`type`.{CollectionLikeType, TypeFactory}
import io.swagger.annotations.ApiModelProperty
import io.swagger.converter.{ModelConverter, ModelConverterContext}
import io.swagger.models.Model
import io.swagger.models.properties.Property

import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.util

trait ValidationWarning {
  def id: String
  def message: String
}

class ValidationModelConverter(warn: ValidationWarning => Unit) extends ModelConverter {
  def resolveProperty(propertyType: Type, context: ModelConverterContext, annotations: Array[Annotation], chain: util.Iterator[ModelConverter]): Property = {
    val hasHiddenAnnotation = Option(annotations)
      .getOrElse(Array.empty[Annotation])
      .exists {
        case p: ApiModelProperty if p.hidden => true
        case _ => false
      }
    propertyType match {
      case _ if hasHiddenAnnotation =>
        report(
          id = "hidden",
          problem = "Property annotated with @ApiModelProperty(hidden = true) is not actually hidden",
          resolution = "Annotate with @(ApiModelProperty @field @getter)(hidden = true)"
        )
      case coll: CollectionLikeType if coll.getContentType.getRawClass == classOf[Object] =>
        report(
          id = "collection-of-object",
          problem = s"Property type of collection was erased to ${coll.getRawClass.getSimpleName}[Object]",
          resolution = "Annotate the property with @ApiModelProperty(dataType = \"list[integer]\")"
        )
      case RawClass(ScalaEnum) =>
        report(
          id = "enum",
          problem = s"Property type of ${propertyType.getTypeName}",
          resolution = "Annotate the property with @ApiModelProperty(dataType = \"org.example.YourEnum$\")"
        )
      case RawClass(ScalaResult | JavaResult) =>
        report(
          id = "result",
          problem = s"Controller action return type of ${propertyType.getTypeName}",
          resolution = "Annotate the action with @ApiOperation(response = classOf[YourResponseType])"
        )
      case RawClass(cls) if EssentialAction.isAssignableFrom(cls) =>
        report(
          id = "action",
          problem = s"Controller action return type of ${propertyType.getTypeName}",
          resolution = "Annotate the action with @ApiOperation(response = classOf[YourResponseType])"
        )
      case _ =>
    }
    if (chain.hasNext) chain.next().resolveProperty(propertyType, context, annotations, chain) else null
  }

  def resolve(modelType: Type, context: ModelConverterContext, chain: util.Iterator[ModelConverter]): Model = {
    if (chain.hasNext) chain.next().resolve(modelType, context, chain) else null
  }

  private object RawClass {
    def unapply(t: Type): Some[Class[_]] = Some(TypeFactory.rawClass(t))
  }
  private val ScalaEnum = classOf[Enumeration#Value]
  private val EssentialAction = classOf[play.api.mvc.EssentialAction]
  private val ScalaResult = classOf[play.api.mvc.Result]
  private val JavaResult = classOf[play.mvc.Result]

  private def report(id: String, problem: String, resolution: String): Unit = {
    val message =
      "Swagger validation warning\n" +
      s"Problem: $problem\n" +
      s"Resolution: $resolution"
    warn(ValidationReport(id, message))
  }

  private case class ValidationReport(id: String, message: String) extends ValidationWarning
}
