package com.ampairs.product.domain.dto.group

import com.ampairs.product.domain.model.group.ProductCategory

data class ProductCategoryRequest(
    var id: String,
    var name: String,
    var refId: String?,
    var active: Boolean?,
    var softDeleted: Boolean?,
    var imageId: String?,
)

fun List<ProductCategoryRequest>.asDatabaseModel(): List<ProductCategory> {
    return map {
        val productCategory = ProductCategory()
        productCategory.uid = it.id
        productCategory.name = it.name
        productCategory.refId = it.refId
        productCategory.imageId = it.imageId
        productCategory
    }
}