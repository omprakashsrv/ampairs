package com.ampairs.inventory.domain.dto

import com.ampairs.inventory.domain.model.Inventory
import java.time.Instant


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
    val createdAt: Instant?,
    val updatedAt: Instant?,
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
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        unitId = this.unitId,
    )
}