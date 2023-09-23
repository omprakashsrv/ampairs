package com.ampairs.product.domain.dto.product

import com.ampairs.product.domain.dto.unit.UnitConversionRequest
import com.ampairs.product.domain.model.Product

data class ProductRequest(
    val id: String,
    val refId: String?,
    val name: String,
    val code: String,
    val taxCode: String?,
    val groupId: String?,
    val brandId: String?,
    val categoryId: String?,
    val subCategoryId: String?,
    val baseUnitId: String?,
    val active: Boolean,
    val mrp: Double,
    val dp: Double,
    val sellingPrice: Double,
    val unitConversions: List<UnitConversionRequest>?,
    val lastUpdated: Long?,
    val createdAt: String?,
    val updatedAt: String?,
)

fun List<ProductRequest>.asDatabaseModel(): List<Product> {
    return map {
        val product = Product()
        product.id = it.id
        product.refId = it.refId
        product.name = it.name
        product.code = it.code
        product.taxCode = it.taxCode ?: ""
        product.groupId = it.groupId
        product.categoryId = it.categoryId
        product.subCategoryId = it.subCategoryId
        product.brandId = it.brandId
        product.baseUnitId = it.baseUnitId
        product.active = it.active
        product.mrp = it.mrp
        product.dp = it.dp
        product.sellingPrice = it.sellingPrice
        product
    }
}