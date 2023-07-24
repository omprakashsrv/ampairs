package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.ProductGroup

data class ProductGroupRequest(
    var name: String, var refId: String
)

fun List<ProductGroupRequest>.asDatabaseModel(): List<ProductGroup> {
    return map {
        val unit = ProductGroup()
        unit.name = it.name
        unit.refId = it.refId
        unit
    }
}