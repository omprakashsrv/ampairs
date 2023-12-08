package com.ampairs.inventory.domain.dto

import com.ampairs.inventory.domain.model.Inventory


data class InventoryResponse(
    val id: String,
    val refId: String?,
    val description: String,
    val unitId: String?,
    val active: Boolean,
    val mrp: Double,
    val dp: Double,
    val sellingPrice: Double,
    val buyingPrice: Double,
    val lastUpdated: Long?,
    val createdAt: String?,
    val updatedAt: String?,
)

fun List<Inventory>.asResponse(): List<InventoryResponse> {
    return map {
        InventoryResponse(
            id = it.id,
            refId = it.refId,
            description = it.description,
            mrp = it.mrp,
            dp = it.dp,
            sellingPrice = it.sellingPrice,
            buyingPrice = it.buyingPrice,
            active = it.active,
            lastUpdated = it.lastUpdated,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt,
            unitId = it.unitId,
        )
    }
}