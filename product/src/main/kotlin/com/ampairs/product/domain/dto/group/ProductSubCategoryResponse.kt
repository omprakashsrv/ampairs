package com.ampairs.product.domain.dto.group

import com.ampairs.file.domain.dto.FileResponse
import com.ampairs.file.domain.dto.toFileResponse
import com.ampairs.product.domain.model.group.ProductSubCategory

data class ProductSubCategoryResponse(
    var id: String, var name: String, var refId: String?,
    var image: FileResponse?,
)

fun List<ProductSubCategory>.asResponse(): List<ProductSubCategoryResponse> {
    return map {
        ProductSubCategoryResponse(
            id = it.uid,
            name = it.name,
            refId = it.refId,
            image = it.image?.toFileResponse(),
        )
    }
}
