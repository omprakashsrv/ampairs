package com.ampairs.product.domain.dto

import com.ampairs.product.domain.model.Product

data class ProductResponse(
    val id: String,
    val name: String,
    val taxCode: String,
    val group: String,
    val category: String,
    val active: Boolean,
    val mrp: Double,
    val sellingPrice: Double,
    val taxCodes: List<TaxCodeResponse>,
    val unitConversions: List<UnitConversionResponse>,
    val lastUpdated: Long?,
    val createdAt: String?,
    val updatedAt: String?,
    val baseUnit: UnitResponse?,
)

fun List<Product>.asProductResponse(): List<ProductResponse> {
    return map {
        ProductResponse(
            id = it.id,
            name = it.name,
            mrp = it.mrp,
            sellingPrice = it.sellingPrice,
            taxCode = it.taxCode,
            active = it.active,
            taxCodes = it.taxCodes.asTaxCodeResponse(),
            group = it.group?.name ?: "",
            category = it.category?.name ?: "",
            lastUpdated = it.lastUpdated,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
            unitConversions = it.unitConversions.asUnitConversionResponse(),
            baseUnit = it.baseUnit?.asUnitResponse()
        )
    }
}