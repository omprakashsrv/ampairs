package com.ampairs.product.domain.dto.group

import com.ampairs.file.domain.dto.FileResponse
import com.ampairs.file.domain.dto.toFileResponse
import com.ampairs.product.domain.model.group.ProductBrand

data class ProductBrandResponse(
    var id: String, var name: String, var refId: String?,
    var image: FileResponse?,
)

fun ProductBrand.asResponse(): ProductBrandResponse {
    return ProductBrandResponse(
        id = uid,
        name = name,
        refId = refId,
        image = image?.toFileResponse(),
    )
}

fun List<ProductBrand>.asResponse(): List<ProductBrandResponse> {
    return map { it.asResponse() }
}
