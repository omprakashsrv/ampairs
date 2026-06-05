package com.ampairs.product.domain.dto.group

import com.ampairs.file.domain.dto.FileResponse
import com.ampairs.file.domain.dto.toFileResponse
import com.ampairs.product.domain.model.group.ProductCategory

data class ProductCategoryResponse(
    var id: String, var name: String, var refId: String?,
    var image: FileResponse?,
)

fun ProductCategory.asResponse(): ProductCategoryResponse {
    return ProductCategoryResponse(
        id = uid,
        name = name,
        refId = refId,
        image = image?.toFileResponse(),
    )
}

fun List<ProductCategory>.asResponse(): List<ProductCategoryResponse> {
    return map { it.asResponse() }
}
