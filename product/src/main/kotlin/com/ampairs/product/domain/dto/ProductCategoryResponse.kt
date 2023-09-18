package com.ampairs.product.domain.dto

import com.ampairs.core.domain.dto.FileResponse
import com.ampairs.core.domain.dto.toFileResponse
import com.ampairs.product.domain.model.ProductCategory

data class ProductCategoryResponse(
    var id: String, var name: String, var refId: String?,
    var image: FileResponse?,
)

fun List<ProductCategory>.asResponse(): List<ProductCategoryResponse> {
    return map {
        ProductCategoryResponse(
            id = it.id,
            name = it.name,
            refId = it.refId,
            image = it.image?.toFileResponse()
        )
    }
}