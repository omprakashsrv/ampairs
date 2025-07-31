package com.ampairs.inventory.domain.dto

import com.ampairs.inventory.domain.model.Inventory

data class InventoryRequest(
    val id: String?,
    val refId: String?,
    val productId: String?,
    val productRefId: String?,
    val description: String? = "",
    val code: String?,
    val taxCode: String?,
    val baseUnitId: String?,
    val active: Boolean? = true,
    val mrp: Double? = 0.0,
    val dp: Double? = 0.0,
    val stock: Double? = 0.0,
    val sellingPrice: Double? = 0.0,
    val buyingPrice: Double? = 0.0,
    val lastUpdated: Long?,
    val createdAt: String?,
    val updatedAt: String?,
)

fun List<InventoryRequest>.asDatabaseModel(): List<Inventory> {
    return map {
        it.asDatabaseModel()
    }
}

fun InventoryRequest.asDatabaseModel(): Inventory {
    val inventory = Inventory()
    inventory.seqId = this.id ?: ""
    inventory.refId = this.refId
    inventory.productId = this.productId ?: ""
    inventory.unitId = this.baseUnitId
    inventory.description = this.description ?: ""
    inventory.active = this.active ?: true
    inventory.mrp = this.mrp ?: 0.0
    inventory.dp = this.dp ?: 0.0
    inventory.stock = this.stock ?: 0.0
    inventory.sellingPrice = this.sellingPrice ?: 0.0
    inventory.buyingPrice = this.buyingPrice ?: 0.0
    return inventory
}