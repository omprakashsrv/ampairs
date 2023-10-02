package com.ampairs.product.domain.dto.product

import com.ampairs.core.domain.dto.FileResponse
import com.ampairs.core.domain.dto.toFileResponse
import com.ampairs.product.domain.dto.tax.TaxCodeResponse
import com.ampairs.product.domain.dto.tax.asResponse
import com.ampairs.product.domain.dto.unit.UnitConversionResponse
import com.ampairs.product.domain.dto.unit.UnitResponse
import com.ampairs.product.domain.dto.unit.asResponse
import com.ampairs.product.domain.dto.unit.asUnitConversionResponse
import com.ampairs.product.domain.model.Product

data class ProductResponse(
    val id: String,
    val name: String,
    val code: String,
    val taxCode: String,
    val groupId: String,
    val brandId: String,
    val categoryId: String,
    val subCategoryId: String,
    val active: Boolean,
    val softDeleted: Boolean,
    val mrp: Double,
    val dp: Double,
    val sellingPrice: Double,
    val taxCodes: List<TaxCodeResponse>,
    val unitConversions: List<UnitConversionResponse>,
    val lastUpdated: Long?,
    val createdAt: String?,
    val updatedAt: String?,
    val baseUnit: UnitResponse?,
    val baseUnitId: String?,
    val images: List<FileResponse>?,
)

fun List<Product>.asResponse(): List<ProductResponse> {
    return map {
        ProductResponse(
            id = it.id,
            name = it.name,
            code = it.code,
            mrp = it.mrp,
            dp = it.dp,
            sellingPrice = it.sellingPrice,
            taxCode = it.taxCode,
            active = it.active,
            taxCodes = it.taxCodes?.asResponse() ?: arrayListOf(),
            groupId = it.groupId ?: "",
            categoryId = it.categoryId ?: "",
            subCategoryId = it.subCategoryId ?: "",
            brandId = it.brandId ?: "",
            lastUpdated = it.lastUpdated,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
            unitConversions = it.unitConversions.asUnitConversionResponse(),
            baseUnit = it.baseUnit?.asResponse(),
            images = it.images.map { productImage ->
                val fileResponse = productImage.image?.toFileResponse() ?: FileResponse()
                fileResponse.refId = productImage.id
                fileResponse
            },
            softDeleted = it.softDeleted,
            baseUnitId = it.baseUnitId
        )
    }
}