package com.ampairs.product.domain.dto.product

import com.ampairs.core.domain.dto.FileResponse
import com.ampairs.core.domain.dto.toFileResponse
import com.ampairs.inventory.domain.dto.InventoryResponse
import com.ampairs.inventory.domain.dto.asResponse
import com.ampairs.unit.domain.dto.UnitConversionResponse
import com.ampairs.unit.domain.dto.UnitResponse
import com.ampairs.unit.domain.dto.asUnitConversionResponses
import com.ampairs.unit.domain.dto.asUnitResponse
import com.ampairs.product.domain.model.Product
import java.time.Instant

data class ProductResponse(
    val id: String,
    val name: String,
    val code: String,
    val sku: String,
    val description: String?,
    val status: String,
    val taxCode: String,
    val taxCodeId: String?,
    val unitId: String?,
    val basePrice: Double,
    val costPrice: Double,
    val groupId: String,
    val brandId: String,
    val categoryId: String,
    val subCategoryId: String,
    val attributes: Map<String, Any>,
    val mrp: Double,
    val dp: Double,
    val sellingPrice: Double,
    val unitConversions: List<UnitConversionResponse>,
    val lastUpdated: Long?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val baseUnit: UnitResponse?,
    val baseUnitId: String?,
    val images: List<FileResponse>?,
    val inventory: InventoryResponse?,
)

fun List<Product>.asResponse(): List<ProductResponse> {
    return map {
        ProductResponse(
            id = it.uid,
            name = it.name,
            code = it.code,
            sku = it.sku,
            description = it.description,
            status = it.status,
            taxCode = it.taxCode,
            taxCodeId = it.taxCodeId,
            unitId = it.unitId,
            basePrice = it.basePrice,
            costPrice = it.costPrice,
            attributes = it.attributes,
            mrp = it.mrp,
            dp = it.dp,
            sellingPrice = it.sellingPrice,
            groupId = it.groupId ?: "",
            categoryId = it.categoryId ?: "",
            subCategoryId = it.subCategoryId ?: "",
            brandId = it.brandId ?: "",
            lastUpdated = it.lastUpdated,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
            unitConversions = it.unitConversions.asUnitConversionResponses(),
            baseUnit = it.baseUnit?.asUnitResponse(),
            images = it.images.map { productImage ->
                val fileResponse = productImage.image?.toFileResponse() ?: FileResponse()
                fileResponse.refId = productImage.uid
                fileResponse
            },
            baseUnitId = it.baseUnitId,
            inventory = if (it.inventory.size > 0) it.inventory[0].asResponse() else null
        )
    }
}

fun Product.asResponse(): ProductResponse {
    return ProductResponse(
        id = uid,
        name = name,
        code = code,
        sku = sku,
        description = description,
        status = status,
        taxCode = taxCode,
        taxCodeId = taxCodeId,
        unitId = unitId,
        basePrice = basePrice,
        costPrice = costPrice,
        attributes = attributes,
        mrp = mrp,
        dp = dp,
        sellingPrice = sellingPrice,
        groupId = groupId ?: "",
        categoryId = categoryId ?: "",
        subCategoryId = subCategoryId ?: "",
        brandId = brandId ?: "",
        lastUpdated = lastUpdated,
        createdAt = createdAt,
        updatedAt = updatedAt,
        unitConversions = unitConversions.asUnitConversionResponses(),
        baseUnit = baseUnit?.asUnitResponse(),
        images = images.map { productImage ->
            val fileResponse = productImage.image?.toFileResponse() ?: FileResponse()
            fileResponse.refId = productImage.uid
            fileResponse
        },
        baseUnitId = baseUnitId,
        inventory = if (inventory.size > 0) inventory[0].asResponse() else null
    )
}
