package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.ProductSubCategory

data class ProductSubCategoryResponse(
    var id: String, var name: String, var refId: String?
)

fun List<ProductSubCategory>.asResponse(): List<ProductSubCategoryResponse> {
    return map {
        ProductSubCategoryResponse(
            id = it.id,
            name = it.name,
            refId = it.refId
        )
    }
}