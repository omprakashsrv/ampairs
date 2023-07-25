package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.ProductCategory

data class ProductCategoryRequest(
    var id: String, var name: String, var refId: String
)

fun List<ProductCategoryRequest>.asDatabaseModel(): List<ProductCategory> {
    return map {
        val unit = ProductCategory()
        unit.name = it.name
        unit.refId = it.refId
        unit
    }
}