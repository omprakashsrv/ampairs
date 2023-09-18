package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.ProductCategory

data class ProductCategoryRequest(
    var id: String, var name: String, var refId: String,
)

fun List<ProductCategoryRequest>.asDatabaseModel(): List<ProductCategory> {
    return map {
        val productCategory = ProductCategory()
        productCategory.name = it.name
        productCategory.refId = it.refId
        productCategory
    }
}