package com.ampairs.payment.event

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.domain.events.InvoiceCancelledEvent
import com.ampairs.event.domain.events.InvoiceFinalizedEvent
import com.ampairs.payment.domain.enums.EntryType
import com.ampairs.payment.domain.enums.SourceType
import com.ampairs.payment.service.BalanceService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

/**
 * Bridges finalized invoices into the party ledger (spec 013, R9 / T020). Runs synchronously in the
 * publisher's thread so the tenant context (set at the invoice controller) is still active.
 *
 * - [InvoiceFinalizedEvent] → upsert `LDG_<invoice.uid>` (SALES_INVOICE, DR, totalCost).
 * - [InvoiceCancelledEvent] → reverse that entry (soft-delete, audit retained).
 */
@Component
class InvoiceLedgerListener(
    private val balanceService: BalanceService,
) {
    private val logger = LoggerFactory.getLogger(InvoiceLedgerListener::class.java)

    @EventListener
    @Transactional
    fun onInvoiceFinalized(event: InvoiceFinalizedEvent) {
        val partyUid = event.customerUid
        if (partyUid.isBlank()) {
            logger.warn("InvoiceFinalizedEvent for {} has no customer; skipping ledger post", event.entityId)
            return
        }
        withTenant(event.workspaceId) {
            balanceService.postSourceEntry(
                partyUid = partyUid,
                entryType = EntryType.SALES_INVOICE,
                amount = BigDecimal.valueOf(event.totalAmount),
                sourceType = SourceType.INVOICE,
                sourceUid = event.entityId,
                voucherNo = event.invoiceNumber,
                entryDate = Instant.ofEpochMilli(event.invoiceDateEpochMillis),
                narration = "Sales invoice ${event.invoiceNumber}",
            )
        }
    }

    @EventListener
    @Transactional
    fun onInvoiceCancelled(event: InvoiceCancelledEvent) {
        withTenant(event.workspaceId) {
            balanceService.reverseSourceEntry(event.entityId)
        }
    }

    /**
     * Ensures the tenant context is set for the ledger write. If already set (synchronous path),
     * it is reused; otherwise it is set from the event and cleared afterwards.
     */
    private inline fun withTenant(workspaceId: String, block: () -> Unit) {
        val alreadySet = TenantContextHolder.getCurrentTenant() != null
        if (!alreadySet && workspaceId.isNotBlank()) {
            TenantContextHolder.setCurrentTenant(workspaceId)
        }
        try {
            block()
        } finally {
            if (!alreadySet && workspaceId.isNotBlank()) {
                TenantContextHolder.clearTenantContext()
            }
        }
    }
}
