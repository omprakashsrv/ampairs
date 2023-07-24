package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.ProductGroup

data class ProductGroupResponse(
    var id: String, var name: String
)

fun List<ProductGroup>.asResponse(): List<ProductGroupResponse> {
    return map {
        ProductGroupResponse(
            id = it.id,
            name = it.name,
        )
    }
}