package com.ampairs.inventory.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.inventory.config.Constants
import com.ampairs.inventory.domain.dto.InventoryItemSyncRequest
import com.ampairs.inventory.domain.dto.InventoryItemSyncResponse
import com.ampairs.inventory.domain.dto.InventoryTransactionSyncRequest
import com.ampairs.inventory.domain.dto.InventoryTransactionSyncResponse
import com.ampairs.inventory.domain.dto.applyInventoryItemSyncRequest
import com.ampairs.inventory.domain.dto.asInventoryItemSyncResponse
import com.ampairs.inventory.domain.dto.asInventoryTransactionSyncResponse
import com.ampairs.inventory.domain.dto.toInventoryItem
import com.ampairs.inventory.domain.model.InventoryItem
import com.ampairs.inventory.domain.model.InventoryTransaction
import com.ampairs.inventory.repository.InventoryItemRepository
import com.ampairs.inventory.repository.InventoryTransactionRepository
import com.ampairs.inventory.repository.WarehouseRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Canonical offline-sync (`/sync`) service for inventory items + transactions (spec 014, workstream B).
 *
 * - Items: UID-keyed bulk upsert. The sync feed includes soft-deleted (`is_active=false`) rows so
 *   deletions propagate. Item push updates pricing/metadata/reorder/active only; opening stock is
 *   honored only on create. On-hand stock changes ride the transactions feed.
 * - Transactions: **append-only** ledger. A push only creates new movements (idempotent by uid),
 *   applies the signed delta to item stock under this `@Transactional`, and computes `balance_after`.
 *   Direction is carried by the transaction type (STOCK_IN = +, STOCK_OUT = −); the reason carries
 *   the semantic (CORRECTION, DAMAGE, OPENING, COUNT_ADJUSTMENT, …).
 */
@Service
class InventorySyncService(
    private val itemRepository: InventoryItemRepository,
    private val transactionRepository: InventoryTransactionRepository,
    private val warehouseRepository: WarehouseRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {
    private val log = LoggerFactory.getLogger(InventorySyncService::class.java)

    // ------------------------------------------------------------------------
    // Items
    // ------------------------------------------------------------------------

    fun getItemsAfterSync(lastSync: String?, pageable: Pageable): Page<InventoryItem> =
        if (lastSync.isNullOrBlank()) {
            itemRepository.findAllForSync(pageable)
        } else {
            runCatching {
                val instant = Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8))
                itemRepository.findByUpdatedAtAfter(instant, pageable)
            }.getOrElse {
                log.warn("Invalid last_sync '{}', falling back to full item sync feed", lastSync, it)
                itemRepository.findAllForSync(pageable)
            }
        }

    @Transactional
    fun bulkUpsertItems(requests: List<InventoryItemSyncRequest>): List<InventoryItemSyncResponse> {
        val defaultWarehouseId = warehouseRepository.findByIsDefaultTrue()?.uid ?: ""
        return requests.map { request ->
            val existing = request.uid.takeIf { it.isNotBlank() }?.let { itemRepository.findByUid(it) }
            val saved = if (existing != null) {
                itemRepository.save(existing.applyInventoryItemSyncRequest(request))
                    .also { entityChangePublisher.updated(ENTITY_ITEM, it.uid) }
            } else {
                itemRepository.save(request.toInventoryItem(defaultWarehouseId))
                    .also { entityChangePublisher.created(ENTITY_ITEM, it.uid) }
            }
            saved.asInventoryItemSyncResponse()
        }
    }

    // ------------------------------------------------------------------------
    // Transactions (append-only)
    // ------------------------------------------------------------------------

    fun getTransactionsAfterSync(lastSync: String?, pageable: Pageable): Page<InventoryTransaction> =
        if (lastSync.isNullOrBlank()) {
            transactionRepository.findAllForSync(pageable)
        } else {
            runCatching {
                val instant = Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8))
                transactionRepository.findByUpdatedAtAfter(instant, pageable)
            }.getOrElse {
                log.warn("Invalid last_sync '{}', falling back to full transaction sync feed", lastSync, it)
                transactionRepository.findAllForSync(pageable)
            }
        }

    @Transactional
    fun bulkUpsertTransactions(
        requests: List<InventoryTransactionSyncRequest>,
    ): List<InventoryTransactionSyncResponse> = requests.map { request ->
        // Append-only + idempotent by uid: an already-applied movement is returned unchanged.
        val existing = request.uid.takeIf { it.isNotBlank() }?.let { transactionRepository.findByUid(it) }
        if (existing != null) return@map existing.asInventoryTransactionSyncResponse()

        val item = request.inventoryItemId.takeIf { it.isNotBlank() }?.let { itemRepository.findByUid(it) }
        val signedDelta = when (request.transactionType) {
            Constants.TXN_TYPE_STOCK_IN -> request.quantity
            Constants.TXN_TYPE_STOCK_OUT -> request.quantity.negate()
            else -> BigDecimal.ZERO   // ADJUSTMENT/COUNT/TRANSFER carry no implicit sign in single-warehouse
        }

        var balanceAfter = BigDecimal.ZERO
        if (item != null && signedDelta.signum() != 0) {
            item.currentStock = item.currentStock.add(signedDelta)
            item.recalculateAvailableStock()
            itemRepository.save(item)
            entityChangePublisher.updated(ENTITY_ITEM, item.uid)
            balanceAfter = item.currentStock
        } else if (item != null) {
            balanceAfter = item.currentStock
        }

        val txn = InventoryTransaction().apply {
            uid = request.uid
            transactionNumber = generateTransactionNumber()
            transactionType = request.transactionType
            transactionReason = request.transactionReason
            inventoryItemId = request.inventoryItemId
            warehouseId = request.warehouseId?.takeIf { it.isNotBlank() }
                ?: item?.warehouseId
                ?: warehouseRepository.findByIsDefaultTrue()?.uid.orEmpty()
            quantity = request.quantity
            this.balanceAfter = balanceAfter
            unitCost = request.unitCost ?: item?.costPrice ?: BigDecimal.ZERO
            sourceType = request.sourceType
            sourceId = request.sourceId
            sourceLineUid = request.sourceLineUid
            referenceNumber = request.referenceNumber
            transactionDate = request.transactionDate ?: Instant.now()
            notes = request.notes
            calculateTotalCost()
        }
        transactionRepository.save(txn)
            .also { entityChangePublisher.created(ENTITY_TRANSACTION, it.uid) }
            .asInventoryTransactionSyncResponse()
    }

    private fun generateTransactionNumber(): String {
        val today = LocalDate.now()
        val dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val startOfDay = today.atStartOfDay(ZoneId.of("UTC")).toInstant()
        val count = transactionRepository.countTransactionsToday(startOfDay)
        return "TXN-$dateStr-" + (count + 1).toString().padStart(4, '0')
    }

    companion object {
        private const val ENTITY_ITEM = "inventory"
        private const val ENTITY_TRANSACTION = "inventory_transaction"
    }
}
