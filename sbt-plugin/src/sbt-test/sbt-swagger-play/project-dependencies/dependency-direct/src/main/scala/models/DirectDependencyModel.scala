package models

import io.swagger.annotations.ApiModel

@ApiModel("direct")
case class DirectDependencyModel(transitive: TransitiveDependencyModel)
