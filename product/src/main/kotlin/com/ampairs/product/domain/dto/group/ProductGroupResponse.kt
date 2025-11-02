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

fun List<ProductGroup>.asResponse(): List<ProductGroupResponse> {
    return map {
        ProductGroupResponse(
            id = it.uid,
            name = it.name,
            refId = it.refId,
            imageId = it.imageId,
            image = it.image?.toFileResponse(),
        )
    }
}
