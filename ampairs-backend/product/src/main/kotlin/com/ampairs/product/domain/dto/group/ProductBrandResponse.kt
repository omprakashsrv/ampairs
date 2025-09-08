package com.ampairs.product.domain.dto.group

import com.ampairs.core.domain.dto.FileResponse
import com.ampairs.core.domain.dto.toFileResponse
import com.ampairs.product.domain.model.group.ProductBrand

data class ProductBrandResponse(
    var id: String, var name: String, var refId: String?, var active: Boolean?,
    var image: FileResponse?,
    val softDeleted: Boolean,
)

fun List<ProductBrand>.asResponse(): List<ProductBrandResponse> {
    return map {
        ProductBrandResponse(
            id = it.uid,
            name = it.name,
            refId = it.refId,
            active = it.active,
            image = it.image?.toFileResponse(),
            softDeleted = it.softDeleted
        )
    }
}