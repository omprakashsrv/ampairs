package com.ampairs.product.domain.dto.group

import com.ampairs.core.domain.dto.FileResponse
import com.ampairs.core.domain.dto.toFileResponse
import com.ampairs.product.domain.model.group.ProductGroup

data class ProductGroupResponse(
    var id: String, var name: String,
    var refId: String?,
    var imageId: String?,
    var active: Boolean?,
    var image: FileResponse?,
    val softDeleted: Boolean,
)

fun List<ProductGroup>.asResponse(): List<ProductGroupResponse> {
    return map {
        ProductGroupResponse(
            id = it.uid,
            name = it.name,
            refId = it.refId,
            active = it.active,
            imageId = it.imageId,
            image = it.image?.toFileResponse(),
            softDeleted = it.softDeleted
        )
    }
}