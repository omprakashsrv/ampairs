package com.ampairs.product.domain.dto.group

import com.ampairs.product.domain.model.group.ProductGroup

data class ProductGroupRequest(
    var id: String,
    var name: String,
    var refId: String?,
    var active: Boolean?,
    var softDeleted: Boolean?,
    var imageId: String?,
)

fun List<ProductGroupRequest>.asDatabaseModel(): List<ProductGroup> {
    return map {
        val productGroup = ProductGroup()
        productGroup.uid = it.id
        productGroup.name = it.name
        productGroup.refId = it.refId
        productGroup.imageId = it.imageId
        productGroup.active = it.active ?: true
        productGroup.softDeleted = it.softDeleted ?: false
        productGroup
    }
}