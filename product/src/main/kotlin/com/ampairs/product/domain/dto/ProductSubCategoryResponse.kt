package com.ampairs.product.domain.dto

import com.ampairs.core.domain.dto.FileResponse
import com.ampairs.core.domain.dto.toFileResponse
import com.ampairs.product.domain.model.ProductSubCategory

data class ProductSubCategoryResponse(
    var id: String, var name: String, var refId: String?,
    val active: Boolean,
    var image: FileResponse?,
)

fun List<ProductSubCategory>.asResponse(): List<ProductSubCategoryResponse> {
    return map {
        ProductSubCategoryResponse(
            id = it.id,
            name = it.name,
            refId = it.refId,
            active = it.active,
            image = it.image?.toFileResponse()
        )
    }
}