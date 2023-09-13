package com.ampairs.product.domain.dto

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
    val mrp: Double,
    val dp: Double,
    val sellingPrice: Double,
    val taxCodes: List<TaxCodeResponse>,
    val unitConversions: List<UnitConversionResponse>,
    val lastUpdated: Long?,
    val createdAt: String?,
    val updatedAt: String?,
    val baseUnit: UnitResponse?,
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
            taxCodes = it.taxCodes.asResponse(),
            groupId = it.groupId ?: "",
            categoryId = it.categoryId ?: "",
            subCategoryId = it.subCategoryId ?: "",
            brandId = it.brandId ?: "",
            lastUpdated = it.lastUpdated,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
            unitConversions = it.unitConversions.asUnitConversionResponse(),
            baseUnit = it.baseUnit?.asResponse()
        )
    }
}