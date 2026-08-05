package com.ampairs.invoice.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.event.domain.events.InvoiceCancelledEvent
import com.ampairs.event.domain.events.InvoiceCreatedEvent
import com.ampairs.event.domain.events.InvoiceFinalizedEvent
import com.ampairs.event.domain.events.InvoiceStatusChangedEvent
import com.ampairs.event.domain.events.InvoiceUpdatedEvent
import com.ampairs.invoice.domain.dto.InvoiceResponse
import com.ampairs.invoice.domain.dto.InvoiceUpdateRequest
import com.ampairs.invoice.domain.dto.toInvoice
import com.ampairs.invoice.domain.dto.toInvoiceItems
import com.ampairs.invoice.domain.dto.toResponse
import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.invoice.domain.model.InvoiceItem
import com.ampairs.inventory.service.InventoryStockService
import com.ampairs.inventory.service.StockLine
import com.ampairs.inventory.service.StockMutationCommand
import com.ampairs.inventory.service.StockSourceType
import java.math.BigDecimal
import com.ampairs.invoice.repository.InvoiceItemRepository
import com.ampairs.invoice.repository.InvoicePagingRepository
import com.ampairs.invoice.repository.InvoiceRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlin.jvm.optionals.getOrDefault

@Service
@Transactional(readOnly = true)
class InvoiceService(
    val invoiceRepository: InvoiceRepository,
    val invoiceItemRepository: InvoiceItemRepository,
    val invoicePagingRepository: InvoicePagingRepository,
    val inventoryStockService: InventoryStockService,
    val eventPublisher: ApplicationEventPublisher
) {

    /**
     * Spec 014 (US1): move stock when an invoice crosses the finalize boundary. Gated inside the
     * inventory engine by (a) inventory-management installed and (b) auto_deduct_on_order; idempotent
     * per invoice line; untracked products skipped. Finalize (→ INVOICED) applies the sale; leaving
     * INVOICED restores it.
     */
    private fun syncStockForInvoice(invoice: Invoice, items: List<InvoiceItem>, previousStatus: InvoiceStatus?) {
        // Never move stock for a removed (soft-deleted) line.
        val activeItems = items.filter { it.active }
        if (activeItems.isEmpty()) return
        // Avoid double-deduction: an invoice converted from an order lets the ORDER own the stock move.
        if (!invoice.orderRefId.isNullOrBlank()) return
        val nowFinalized = invoice.status == InvoiceStatus.INVOICED
        val wasFinalized = previousStatus == InvoiceStatus.INVOICED
        if (nowFinalized == wasFinalized) return
        val command = StockMutationCommand(
            sourceType = StockSourceType.INVOICE,
            sourceId = invoice.uid,
            referenceNumber = invoice.invoiceNumber,
            performedBy = getUserId(),
            lines = activeItems.map {
                StockLine(
                    sourceLineUid = it.uid,
                    productId = it.productId,
                    quantity = BigDecimal.valueOf(it.quantity),
                )
            },
        )
        if (nowFinalized) inventoryStockService.applySale(command)
        else inventoryStockService.reverseSale(command)
    }

    /**
     * Helper methods for event publishing
     */
    private fun getWorkspaceId(): String = TenantContextHolder.getCurrentTenant() ?: ""

    private fun getUserId(): String {
        val auth = SecurityContextHolder.getContext().authentication
        return auth?.let { AuthenticationHelper.getCurrentUserId(it) } ?: ""
    }

    private fun getDeviceId(): String = DeviceContextHolder.getCurrentDevice() ?: ""

    @Transactional
    fun updateInvoice(invoice: Invoice, invoiceItems: List<InvoiceItem>): InvoiceResponse {
        val existingInvoice = invoiceRepository.findByUid(invoice.uid)
        val isNewInvoice = existingInvoice == null
        val oldStatus = existingInvoice?.status

        invoice.uid = existingInvoice?.uid ?: invoice.uid
        invoice.invoiceNumber = existingInvoice?.invoiceNumber ?: ""
        if (invoice.invoiceNumber.isEmpty()) {
            val invoiceNumber = invoiceRepository.findMaxInvoiceNumber().getOrDefault("0").toIntOrNull() ?: 0
            invoice.invoiceNumber = (invoiceNumber + 1).toString()
        }
        val updatedInvoice = invoiceRepository.save(invoice)
        invoice.uid = updatedInvoice.uid

        invoiceItems.forEach { invoiceItem ->
            if (invoiceItem.uid.isNotEmpty()) {
                val existingInvoiceItem = invoiceItemRepository.findByUid(invoiceItem.uid)
                invoiceItem.id = existingInvoiceItem?.id ?: 0
            }
            invoiceItem.invoiceId = invoice.uid
            invoiceItemRepository.save(invoiceItem)
        }

        // Publish events
        if (isNewInvoice) {
            eventPublisher.publishEvent(
                InvoiceCreatedEvent(
                    source = this,
                    workspaceId = getWorkspaceId(),
                    entityId = updatedInvoice.uid,
                    userId = getUserId(),
                    deviceId = getDeviceId(),
                    invoiceNumber = updatedInvoice.invoiceNumber,
                    customerName = updatedInvoice.customerName ?: "",
                    totalAmount = updatedInvoice.totalCost
                )
            )
        } else {
            eventPublisher.publishEvent(
                InvoiceUpdatedEvent(
                    source = this,
                    workspaceId = getWorkspaceId(),
                    entityId = updatedInvoice.uid,
                    userId = getUserId(),
                    deviceId = getDeviceId(),
                    fieldChanges = mapOf("invoice" to "updated", "items" to invoiceItems.size)
                )
            )

            // Publish status changed event if status changed
            if (oldStatus != null && oldStatus != updatedInvoice.status) {
                eventPublisher.publishEvent(
                    InvoiceStatusChangedEvent(
                        source = this,
                        workspaceId = getWorkspaceId(),
                        entityId = updatedInvoice.uid,
                        userId = getUserId(),
                        deviceId = getDeviceId(),
                        invoiceNumber = updatedInvoice.invoiceNumber,
                        oldStatus = oldStatus.name,
                        newStatus = updatedInvoice.status.name
                    )
                )
            }
        }

        // Spec 014: apply/reverse inventory on the finalize boundary (idempotent; no-op if off).
        syncStockForInvoice(updatedInvoice, invoiceItems, oldStatus)

        return invoice.toResponse(invoiceItems)
    }

    /**
     * Offline-sync bulk upsert (spec 010). Client UID is authoritative; no tax/total recompute.
     * Numbering collision: if another invoice in this workspace already owns
     * (series, sequence_number), that row is skipped (NOT returned) — the server never renumbers
     * (C5/FR-B09). The client detects the missing uid in the response and keeps it unsynced.
     */
    @Transactional
    fun bulkUpsertInvoices(requests: List<InvoiceUpdateRequest>): List<InvoiceResponse> {
        val results = mutableListOf<InvoiceResponse>()
        val seenInBatch = mutableSetOf<String>()
        for (request in requests) {
            val invoice = request.toInvoice()
            if (invoice.sequenceNumber > 0) {
                val key = "${invoice.series}|${invoice.sequenceNumber}"
                if (!seenInBatch.add(key)) continue // duplicate within this batch — skip
                val clash = invoiceRepository.findBySeriesAndSequenceNumber(invoice.series, invoice.sequenceNumber)
                if (clash != null && clash.uid != invoice.uid) continue // conflict — skip, do not renumber
            }
            results.add(upsertInvoice(invoice, request.invoiceItems.toInvoiceItems()))
        }
        return results
    }

    private fun upsertInvoice(invoice: Invoice, invoiceItems: List<InvoiceItem>): InvoiceResponse {
        val existing = if (invoice.uid.isNotEmpty()) invoiceRepository.findByUid(invoice.uid) else null
        val previousStatus = existing?.status
        if (existing != null) {
            invoice.id = existing.id
            invoice.uid = existing.uid
            invoice.createdAt = existing.createdAt
            if (invoice.invoiceNumber.isEmpty()) invoice.invoiceNumber = existing.invoiceNumber
            // Preserve an already-captured external ref (e.g. Tally master id) when a client that
            // doesn't carry it re-syncs the invoice — never null out an existing ref on upsert.
            if (invoice.refId == null) invoice.refId = existing.refId
        }
        if (invoice.invoiceNumber.isEmpty() && invoice.sequenceNumber > 0) {
            invoice.invoiceNumber = "${invoice.series}-${invoice.sequenceNumber}"
        }
        val savedInvoice = invoiceRepository.save(invoice)
        val savedItems = invoiceItems.map { item ->
            val existingItem = if (item.uid.isNotEmpty()) invoiceItemRepository.findByUid(item.uid) else null
            if (existingItem != null) {
                item.id = existingItem.id
                item.createdAt = existingItem.createdAt
            }
            item.invoiceId = savedInvoice.uid
            invoiceItemRepository.save(item)
        }
        publishFinalizationEvents(savedInvoice, previousStatus)
        // Spec 014: apply/reverse inventory on the finalize boundary (idempotent; no-op if off).
        syncStockForInvoice(savedInvoice, savedItems, previousStatus)
        return savedInvoice.toResponse(savedItems)
    }

    /**
     * Bridges the invoice lifecycle to the `payment` party ledger (spec 013, FR-013/014):
     * publishes [InvoiceFinalizedEvent] when an invoice newly reaches INVOICED, and
     * [InvoiceCancelledEvent] when it leaves INVOICED. Drafts never post.
     */
    private fun publishFinalizationEvents(invoice: Invoice, previousStatus: InvoiceStatus?) {
        val nowFinalized = invoice.status == InvoiceStatus.INVOICED
        val wasFinalized = previousStatus == InvoiceStatus.INVOICED
        if (nowFinalized && !wasFinalized) {
            eventPublisher.publishEvent(
                InvoiceFinalizedEvent(
                    source = this,
                    workspaceId = getWorkspaceId(),
                    entityId = invoice.uid,
                    userId = getUserId(),
                    deviceId = getDeviceId(),
                    invoiceNumber = invoice.invoiceNumber,
                    customerUid = invoice.customerId ?: "",
                    totalAmount = invoice.totalCost,
                    invoiceDateEpochMillis = invoice.invoiceDate.toEpochMilli(),
                )
            )
        } else if (wasFinalized && !nowFinalized) {
            eventPublisher.publishEvent(
                InvoiceCancelledEvent(
                    source = this,
                    workspaceId = getWorkspaceId(),
                    entityId = invoice.uid,
                    userId = getUserId(),
                    deviceId = getDeviceId(),
                    invoiceNumber = invoice.invoiceNumber,
                    customerUid = invoice.customerId ?: "",
                )
            )
        }
    }

    /**
     * Incremental sync feed: invoices updated at/after [lastSync] (ISO-8601), INCLUDING cancelled rows,
     * ordered by updatedAt ASC, paginated. Falls back to the full feed when the cursor is absent/invalid.
     */
    fun getInvoicesAfterSync(lastSync: String?, pageable: Pageable): Page<Invoice> {
        if (lastSync.isNullOrBlank()) return invoicePagingRepository.findAllBy(pageable)
        return try {
            val decoded = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
            invoicePagingRepository.findByUpdatedAtGreaterThanEqual(Instant.parse(decoded), pageable)
        } catch (e: Exception) {
            invoicePagingRepository.findAllBy(pageable)
        }
    }

    @Transactional(readOnly = true)
    fun getInvoices(lastUpdated: Instant?): List<Invoice> {
        val invoices =
            invoicePagingRepository.findAllByUpdatedAtGreaterThanEqual(
                lastUpdated ?: Instant.EPOCH, PageRequest.of(0, 50, Sort.by("lastUpdated").ascending())
            )
        return invoices
    }

    fun getInvoice(uid: String): Invoice? {
        return invoiceRepository.findByUid(uid = uid)
    }


}
