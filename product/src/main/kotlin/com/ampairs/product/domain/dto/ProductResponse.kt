package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.Product

data class ProductResponse(
    val id: String,
    val name: String,
    val group: String,
    val category: String,
    val active: Boolean,
    val taxCodes: List<TaxCodeResponse>
)

fun List<Product>.asProductResponse(): List<ProductResponse> {
    return map {
        ProductResponse(
            id = it.id,
            name = it.name,
            active = it.active,
            taxCodes = it.taxCodes.asTaxCodeResponse(),
            group = it.group?.name ?: "",
            category = it.category?.name ?: ""
        )
    }
}