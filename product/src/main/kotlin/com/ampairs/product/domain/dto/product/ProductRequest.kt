package com.ampairs.product.domain.dto.product

import com.ampairs.unit.domain.dto.UnitConversionRequest
import com.ampairs.product.domain.model.Product
import jakarta.validation.constraints.NotBlank

data class ProductRequest(
    val id: String = "",
    val refId: String? = null,
    @field:NotBlank(message = "Product name is required")
    val name: String,
    val code: String = "",
    val sku: String? = null,
    val description: String? = null,
    val status: String? = null,
    val taxCode: String? = null,
    val taxCodeId: String? = null,
    val unitId: String? = null,
    val basePrice: Double? = null,
    val costPrice: Double? = null,
    val groupId: String? = null,
    val brandId: String? = null,
    val categoryId: String? = null,
    val subCategoryId: String? = null,
    val baseUnitId: String? = null,
    val attributes: Map<String, Any>? = null,
    val active: Boolean? = null,
    val mrp: Double? = null,
    val dp: Double? = null,
    val sellingPrice: Double? = null,
    val unitConversions: List<UnitConversionRequest>? = null,
    val lastUpdated: Long? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

fun List<ProductRequest>.asDatabaseModel(): List<Product> {
    return map {
        val product = Product()
        product.uid = it.id
        product.refId = it.refId
        product.name = it.name
        product.code = it.code
        product.sku = it.sku?.takeIf { sku -> sku.isNotBlank() }
        product.description = it.description
        product.status = it.status ?: "ACTIVE"
        product.taxCode = it.taxCode ?: ""
        product.taxCodeId = it.taxCodeId
        product.unitId = it.unitId
        product.basePrice = it.basePrice ?: 0.0
        product.costPrice = it.costPrice ?: 0.0
        product.groupId = it.groupId?.takeIf { id -> id.isNotBlank() }
        product.categoryId = it.categoryId?.takeIf { id -> id.isNotBlank() }
        product.subCategoryId = it.subCategoryId?.takeIf { id -> id.isNotBlank() }
        product.brandId = it.brandId?.takeIf { id -> id.isNotBlank() }
        product.baseUnitId = it.baseUnitId
        product.attributes = it.attributes ?: emptyMap()
        product.mrp = it.mrp ?: 0.0
        product.dp = it.dp ?: 0.0
        product.sellingPrice = it.sellingPrice ?: 0.0
        product
    }
}
