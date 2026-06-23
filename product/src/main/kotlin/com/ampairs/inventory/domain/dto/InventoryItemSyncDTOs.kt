package com.ampairs.inventory.domain.dto

import com.ampairs.inventory.domain.model.InventoryItem
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

/**
 * Offline-sync DTOs for InventoryItem (spec 014, canonical /sync contract).
 *
 * Pull returns these (including soft-deleted rows, `is_active=false`). Push is a UID-keyed bulk
 * upsert. NOTE: item push updates pricing/metadata/reorder/active only — on-hand stock is NOT
 * changed by an item push (stock moves through the transactions feed). `current_stock` on a request
 * is honored only when creating a new item (opening balance).
 */
data class InventoryItemSyncResponse(
    val uid: String,
    val name: String,
    val sku: String?,
    val productId: String?,
    val productVariantId: String?,
    val unitId: String?,
    val warehouseId: String?,
    val currentStock: BigDecimal,
    val reservedStock: BigDecimal,
    val availableStock: BigDecimal,
    val reorderLevel: BigDecimal,
    val costPrice: BigDecimal,
    val sellingPrice: BigDecimal,
    val mrp: BigDecimal,
    val isActive: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

data class InventoryItemSyncRequest(
    @field:NotBlank val uid: String = "",
    @field:NotBlank val name: String = "",
    val sku: String? = null,
    val productId: String? = null,
    val productVariantId: String? = null,
    val unitId: String? = null,
    val warehouseId: String? = null,
    val currentStock: BigDecimal? = null,
    val reservedStock: BigDecimal? = null,
    val reorderLevel: BigDecimal? = null,
    val costPrice: BigDecimal? = null,
    val sellingPrice: BigDecimal? = null,
    val mrp: BigDecimal? = null,
    val isActive: Boolean = true,
)

fun InventoryItem.asInventoryItemSyncResponse(): InventoryItemSyncResponse =
    InventoryItemSyncResponse(
        uid = uid,
        name = name,
        sku = sku.ifBlank { null },
        productId = productId,
        productVariantId = productVariantId,
        unitId = unitId,
        warehouseId = warehouseId.ifBlank { null },
        currentStock = currentStock,
        reservedStock = reservedStock,
        availableStock = availableStock,
        reorderLevel = reorderLevel,
        costPrice = costPrice,
        sellingPrice = sellingPrice,
        mrp = mrp,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

/** Apply a pushed request to an existing item — pricing/metadata/reorder/active only (not stock). */
fun InventoryItem.applyInventoryItemSyncRequest(request: InventoryItemSyncRequest): InventoryItem = apply {
    name = request.name
    request.sku?.takeIf { it.isNotBlank() }?.let { sku = it }
    productId = request.productId
    productVariantId = request.productVariantId
    unitId = request.unitId
    request.warehouseId?.takeIf { it.isNotBlank() }?.let { warehouseId = it }
    request.reorderLevel?.let { reorderLevel = it }
    request.costPrice?.let { costPrice = it }
    request.sellingPrice?.let { sellingPrice = it }
    request.mrp?.let { mrp = it }
    isActive = request.isActive
    recalculateAvailableStock()
}

/** Build a new item from a pushed request — honors opening `current_stock`. Caller sets warehouseId. */
fun InventoryItemSyncRequest.toInventoryItem(defaultWarehouseId: String): InventoryItem = InventoryItem().also { item ->
    item.uid = uid
    item.name = name
    item.sku = sku?.takeIf { it.isNotBlank() } ?: uid
    item.productId = productId
    item.productVariantId = productVariantId
    item.unitId = unitId
    item.warehouseId = warehouseId?.takeIf { it.isNotBlank() } ?: defaultWarehouseId
    item.currentStock = currentStock ?: BigDecimal.ZERO
    item.reservedStock = reservedStock ?: BigDecimal.ZERO
    item.reorderLevel = reorderLevel ?: BigDecimal.ZERO
    item.costPrice = costPrice ?: BigDecimal.ZERO
    item.sellingPrice = sellingPrice ?: BigDecimal.ZERO
    item.mrp = mrp ?: BigDecimal.ZERO
    item.isActive = isActive
    item.recalculateAvailableStock()
}
