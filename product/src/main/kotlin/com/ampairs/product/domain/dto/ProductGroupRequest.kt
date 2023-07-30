package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.ProductGroup

data class ProductGroupRequest(
    var id: String, var name: String, var refId: String?, var active: Boolean?
)

fun List<ProductGroupRequest>.asDatabaseModel(): List<ProductGroup> {
    return map {
        val productGroup = ProductGroup()
        productGroup.name = it.name
        productGroup.refId = it.refId
        productGroup.active = it.active ?: true
        productGroup
    }
}