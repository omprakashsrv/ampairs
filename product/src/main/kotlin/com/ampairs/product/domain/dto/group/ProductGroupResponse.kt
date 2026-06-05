package com.ampairs.product.domain.dto.group

import com.ampairs.file.domain.dto.FileResponse
import com.ampairs.file.domain.dto.toFileResponse
import com.ampairs.product.domain.model.group.ProductGroup

data class ProductGroupResponse(
    var id: String, var name: String,
    var refId: String?,
    var imageId: String?,
    var image: FileResponse?,
)

fun ProductGroup.asResponse(): ProductGroupResponse {
    return ProductGroupResponse(
        id = uid,
        name = name,
        refId = refId,
        imageId = imageId,
        image = image?.toFileResponse(),
    )
}

fun List<ProductGroup>.asResponse(): List<ProductGroupResponse> {
    return map { it.asResponse() }
}
