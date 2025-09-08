package com.ampairs.product.domain.dto.group

import com.ampairs.core.domain.dto.FileResponse
import com.ampairs.core.domain.dto.toFileResponse
import com.ampairs.product.domain.model.group.ProductCategory

data class ProductCategoryResponse(
    var id: String, var name: String, var refId: String?,
    var active: Boolean,
    var image: FileResponse?,
    val softDeleted: Boolean,
)

fun List<ProductCategory>.asResponse(): List<ProductCategoryResponse> {
    return map {
        ProductCategoryResponse(
            id = it.uid,
            name = it.name,
            refId = it.refId,
            active = it.active,
            image = it.image?.toFileResponse(),
            softDeleted = it.softDeleted
        )
    }
}