package com.ampairs.product.domain.dto.group

import com.ampairs.product.domain.model.group.ProductSubCategory

data class ProductSubCategoryRequest(
    var id: String,
    var name: String,
    var refId: String?,
    var active: Boolean?,
    var softDeleted: Boolean?,
    var imageId: String?,
)

fun List<ProductSubCategoryRequest>.asDatabaseModel(): List<ProductSubCategory> {
    return map {
        val productCategory = ProductSubCategory()
        productCategory.uid = it.id
        productCategory.name = it.name
        productCategory.refId = it.refId
        productCategory.imageId = it.imageId
        productCategory.active = it.active ?: true
        productCategory.softDeleted = it.softDeleted ?: false
        productCategory
    }
}