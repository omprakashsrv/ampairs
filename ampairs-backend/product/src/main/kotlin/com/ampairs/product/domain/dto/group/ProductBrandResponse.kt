package com.ampairs.product.domain.dto.group

import com.ampairs.core.domain.dto.FileResponse
import com.ampairs.core.domain.dto.toFileResponse
import com.ampairs.product.domain.model.group.ProductBrand

data class ProductBrandResponse(
    var id: String, var name: String, var refId: String?,
    var image: FileResponse?,
)

fun List<ProductBrand>.asResponse(): List<ProductBrandResponse> {
    return map {
        ProductBrandResponse(
            id = it.uid,
            name = it.name,
            refId = it.refId,
            image = it.image?.toFileResponse(),
        )
    }
}