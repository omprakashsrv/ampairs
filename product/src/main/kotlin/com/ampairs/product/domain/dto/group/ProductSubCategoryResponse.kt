package com.ampairs.product.domain.dto.group

import com.ampairs.file.domain.dto.FileResponse
import com.ampairs.file.domain.dto.toFileResponse
import com.ampairs.product.domain.model.group.ProductSubCategory

data class ProductSubCategoryResponse(
    var id: String, var name: String, var refId: String?,
    var image: FileResponse?,
)

fun ProductSubCategory.asResponse(): ProductSubCategoryResponse {
    return ProductSubCategoryResponse(
        id = uid,
        name = name,
        refId = refId,
        image = image?.toFileResponse(),
    )
}

fun List<ProductSubCategory>.asResponse(): List<ProductSubCategoryResponse> {
    return map { it.asResponse() }
}
