package com.ampairs.analytics.event

import com.ampairs.analytics.service.KpiRollupService
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.domain.events.InvoiceFinalizedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Updates the analytics SALES summary after an invoice is finalized. Runs AFTER_COMMIT (the source
 * row is durable) and off the request thread; sets the tenant context from the event and clears it in
 * a finally block — mirrors the established `EcomOrderPlacedListener` pattern.
 */
@Component
class AnalyticsInvoiceEventListener(
    private val rollupService: KpiRollupService,
) {
    private val log = LoggerFactory.getLogger(AnalyticsInvoiceEventListener::class.java)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onInvoiceFinalized(event: InvoiceFinalizedEvent) {
        TenantContextHolder.setCurrentTenant(event.workspaceId)
        try {
            // Reconcile the affected business day from source invoices — idempotent and self-healing
            // (handles redelivery, backdated edits and de-finalization, which the event payload can't).
            rollupService.reconcileDayOf(event.invoiceDateEpochMillis)
        } catch (e: Exception) {
            log.error("Failed to roll up finalized invoice {}: {}", event.invoiceNumber, e.message, e)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
