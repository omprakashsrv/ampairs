package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.Product

data class ProductResponse(
    val id: String,
    val name: String,
    val taxCode: String,
    val group: String,
    val brand: String,
    val category: String,
    val subCategory: String,
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
            mrp = it.mrp,
            dp = it.dp,
            sellingPrice = it.sellingPrice,
            taxCode = it.taxCode,
            active = it.active,
            taxCodes = it.taxCodes.asResponse(),
            group = it.group?.name ?: "",
            category = it.category?.name ?: "",
            subCategory = it.subCategory?.name ?: "",
            brand = it.brand?.name ?: "",
            lastUpdated = it.lastUpdated,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
            unitConversions = it.unitConversions.asUnitConversionResponse(),
            baseUnit = it.baseUnit?.asResponse()
        )
    }
}