package com.ampairs.inventory.domain.dto

import com.ampairs.inventory.domain.model.Inventory
import java.time.LocalDateTime


data class InventoryResponse(
    val id: String,
    val refId: String?,
    val productId: String?,
    val description: String,
    val unitId: String?,
    val mrp: Double,
    val dp: Double,
    val stock: Double,
    val sellingPrice: Double,
    val buyingPrice: Double,
    val lastUpdated: Long?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)

fun List<Inventory>.asResponse(): List<InventoryResponse> {
    return map {
        it.asResponse()
    }
}

fun Inventory.asResponse(): InventoryResponse {
    return InventoryResponse(
        id = this.uid,
        productId = this.productId,
        refId = this.refId,
        description = this.description,
        mrp = this.mrp,
        dp = this.dp,
        stock = this.stock,
        sellingPrice = this.sellingPrice,
        buyingPrice = this.buyingPrice,
        lastUpdated = this.lastUpdated,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        unitId = this.unitId,
    )
}