package com.ampairs.purchase.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.core.sync.EntityChangeType
import com.ampairs.inventory.service.InventoryStockService
import com.ampairs.inventory.service.StockLine
import com.ampairs.inventory.service.StockMutationCommand
import com.ampairs.inventory.service.StockSourceType
import com.ampairs.purchase.domain.dto.PurchaseResponse
import com.ampairs.purchase.domain.dto.PurchaseUpdateRequest
import com.ampairs.purchase.domain.dto.toPurchase
import com.ampairs.purchase.domain.dto.toPurchaseItems
import com.ampairs.purchase.domain.dto.toResponse
import com.ampairs.purchase.domain.enums.PurchaseStatus
import com.ampairs.purchase.domain.model.Purchase
import com.ampairs.purchase.domain.model.PurchaseItem
import com.ampairs.purchase.repository.PurchaseItemRepository
import com.ampairs.purchase.repository.PurchasePagingRepository
import com.ampairs.purchase.repository.PurchaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

@Service
@Transactional(readOnly = true)
class PurchaseService(
    val purchaseRepository: PurchaseRepository,
    val purchaseItemRepository: PurchaseItemRepository,
    val purchasePagingRepository: PurchasePagingRepository,
    val inventoryStockService: InventoryStockService,
    private val entityChangePublisher: EntityChangePublisher,
) {

    private fun getUserId(): String {
        val auth = SecurityContextHolder.getContext().authentication
        return auth?.let { AuthenticationHelper.getCurrentUserId(it) } ?: ""
    }

    /**
     * Offline-sync bulk upsert. Client UID is authoritative — preserved on insert and matched on
     * update. No tax/total recomputation: values are stored exactly as supplied. Assigns a server
     * purchaseNumber when blank. A RECEIVED purchase increases inventory; CANCELLED reverses it.
     */
    @Transactional
    fun bulkUpsertPurchases(requests: List<PurchaseUpdateRequest>): List<PurchaseResponse> =
        requests.map { upsertPurchase(it.toPurchase(), it.purchaseItems.toPurchaseItems()) }

    private fun upsertPurchase(purchase: Purchase, purchaseItems: List<PurchaseItem>): PurchaseResponse {
        val existing = if (purchase.uid.isNotEmpty()) purchaseRepository.findByUid(purchase.uid).getOrNull() else null
        if (existing != null) {
            purchase.id = existing.id
            purchase.uid = existing.uid
            purchase.createdAt = existing.createdAt
            if (purchase.purchaseNumber.isEmpty()) purchase.purchaseNumber = existing.purchaseNumber
        }
        if (purchase.purchaseNumber.isEmpty()) {
            val maxNumber = purchaseRepository.findMaxPurchaseNumber().getOrDefault("0").toIntOrNull() ?: 0
            purchase.purchaseNumber = (maxNumber + 1).toString()
        }
        val saved = purchaseRepository.save(purchase)
        val savedItems = purchaseItems.map { item ->
            val existingItem = if (item.uid.isNotEmpty()) purchaseItemRepository.findByUid(item.uid).getOrNull() else null
            if (existingItem != null) {
                item.id = existingItem.id
                item.createdAt = existingItem.createdAt
            }
            item.purchaseId = saved.uid
            purchaseItemRepository.save(item)
        }

        syncStockForPurchase(saved, savedItems)

        // Broadcast so other devices of this workspace pull the bulk-synced change.
        entityChangePublisher.publish(
            "purchase",
            saved.uid,
            if (saved.status == PurchaseStatus.CANCELLED) EntityChangeType.DELETED else EntityChangeType.UPDATED,
        )
        return saved.toResponse(savedItems)
    }

    /**
     * Move stock for the purchase based on its status. RECEIVED adds inbound stock (mirror of a
     * confirmed sale's deduction); CANCELLED removes it. Gated inside the inventory engine by the
     * inventory-management module + the auto_add_on_purchase config; idempotent per source line.
     */
    private fun syncStockForPurchase(purchase: Purchase, items: List<PurchaseItem>) {
        if (items.isEmpty()) return
        val command = StockMutationCommand(
            sourceType = StockSourceType.PURCHASE,
            sourceId = purchase.uid,
            referenceNumber = purchase.purchaseNumber,
            performedBy = getUserId(),
            lines = items.map {
                StockLine(
                    sourceLineUid = it.uid,
                    productId = it.productId,
                    quantity = BigDecimal.valueOf(it.quantity),
                    unitCost = BigDecimal.valueOf(it.purchasePrice),
                )
            },
        )
        when (purchase.status) {
            PurchaseStatus.RECEIVED -> inventoryStockService.applyPurchase(command)
            PurchaseStatus.CANCELLED -> inventoryStockService.reversePurchase(command)
            else -> Unit
        }
    }

    /**
     * Incremental sync feed: purchases updated at/after [lastSync] (ISO-8601), INCLUDING cancelled rows,
     * ordered by updatedAt ASC, paginated. Falls back to the full feed when the cursor is absent/invalid.
     */
    fun getPurchasesAfterSync(lastSync: String?, pageable: Pageable): Page<Purchase> {
        if (lastSync.isNullOrBlank()) return purchasePagingRepository.findAllBy(pageable)
        return try {
            val decoded = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
            purchasePagingRepository.findByUpdatedAtGreaterThanEqual(Instant.parse(decoded), pageable)
        } catch (e: Exception) {
            purchasePagingRepository.findAllBy(pageable)
        }
    }

    fun getPurchaseByUid(uid: String): Purchase? = purchaseRepository.findByUid(uid).getOrNull()
}
