package com.ampairs.product.domain.dto

import com.ampairs.core.domain.dto.FileResponse
import com.ampairs.core.domain.dto.toFileResponse
import com.ampairs.product.domain.model.ProductBrand

data class ProductBrandResponse(
    var id: String, var name: String, var refId: String?, var active: Boolean?,
    var image: FileResponse?,
)

fun List<ProductBrand>.asResponse(): List<ProductBrandResponse> {
    return map {
        ProductBrandResponse(
            id = it.id,
            name = it.name,
            refId = it.refId,
            active = it.active,
            image = it.image?.toFileResponse()
        )
    }
}