package com.ampairs.product.domain.dto.group

import com.ampairs.product.domain.model.group.ProductBrand

data class ProductBrandRequest(
    var id: String,
    var name: String,
    var refId: String?,
    var active: Boolean?,
    var softDeleted: Boolean?,
    var imageId: String?,
)

fun List<ProductBrandRequest>.asDatabaseModel(): List<ProductBrand> {
    return map {
        val productGroup = ProductBrand()
        productGroup.id = it.id
        productGroup.name = it.name
        productGroup.refId = it.refId
        productGroup.imageId = it.imageId
        productGroup.active = it.active ?: true
        productGroup.softDeleted = it.softDeleted ?: false
        productGroup
    }
}