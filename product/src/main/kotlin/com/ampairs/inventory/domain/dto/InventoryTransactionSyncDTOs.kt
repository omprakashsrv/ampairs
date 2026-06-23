package com.ampairs.inventory.domain.dto

import com.ampairs.inventory.domain.model.InventoryTransaction
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

/**
 * Offline-sync DTOs for InventoryTransaction (spec 014). Movements are an **append-only** ledger:
 * the push only ever creates new movements (client-generated uid); it never updates or deletes an
 * existing one. The server fills `balance_after` and `transaction_number`.
 */
data class InventoryTransactionSyncResponse(
    val uid: String,
    val transactionNumber: String,
    val transactionType: String,
    val transactionReason: String,
    val inventoryItemId: String,
    val warehouseId: String,
    val quantity: BigDecimal,
    val balanceAfter: BigDecimal,
    val unitCost: BigDecimal,
    val totalCost: BigDecimal,
    val sourceType: String?,
    val sourceId: String?,
    val sourceLineUid: String?,
    val referenceNumber: String?,
    val transactionDate: Instant,
    val performedBy: String?,
    val notes: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

data class InventoryTransactionSyncRequest(
    @field:NotBlank val uid: String = "",
    @field:NotBlank val transactionType: String = "",
    @field:NotBlank val transactionReason: String = "",
    @field:NotBlank val inventoryItemId: String = "",
    val warehouseId: String? = null,
    val quantity: BigDecimal = BigDecimal.ZERO,
    val unitCost: BigDecimal? = null,
    val sourceType: String? = null,
    val sourceId: String? = null,
    val sourceLineUid: String? = null,
    val referenceNumber: String? = null,
    val transactionDate: Instant? = null,
    val notes: String? = null,
)

fun InventoryTransaction.asInventoryTransactionSyncResponse(): InventoryTransactionSyncResponse =
    InventoryTransactionSyncResponse(
        uid = uid,
        transactionNumber = transactionNumber,
        transactionType = transactionType,
        transactionReason = transactionReason,
        inventoryItemId = inventoryItemId,
        warehouseId = warehouseId,
        quantity = quantity,
        balanceAfter = balanceAfter,
        unitCost = unitCost,
        totalCost = totalCost,
        sourceType = sourceType,
        sourceId = sourceId,
        sourceLineUid = sourceLineUid,
        referenceNumber = referenceNumber,
        transactionDate = transactionDate,
        performedBy = performedBy,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
