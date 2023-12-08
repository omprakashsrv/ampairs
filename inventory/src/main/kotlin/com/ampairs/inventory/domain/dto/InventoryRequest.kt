package com.ampairs.inventory.domain.dto

import com.ampairs.inventory.domain.model.Inventory

data class InventoryRequest(
    val id: String,
    val refId: String?,
    val productId: String?,
    val description: String,
    val code: String,
    val taxCode: String?,
    val baseUnitId: String?,
    val active: Boolean,
    val mrp: Double,
    val dp: Double,
    val stock: Double,
    val sellingPrice: Double,
    val buyingPrice: Double,
    val lastUpdated: Long?,
    val createdAt: String?,
    val updatedAt: String?,
)

fun List<InventoryRequest>.asDatabaseModel(): List<Inventory> {
    return map {
        val inventory = Inventory()
        inventory.id = it.id
        inventory.refId = it.refId
        inventory.productId = it.productId ?: ""
        inventory.unitId = it.baseUnitId
        inventory.description = it.description
        inventory.active = it.active
        inventory.mrp = it.mrp
        inventory.dp = it.dp
        inventory.stock = it.stock
        inventory.sellingPrice = it.sellingPrice
        inventory.buyingPrice = it.buyingPrice
        inventory
    }
}