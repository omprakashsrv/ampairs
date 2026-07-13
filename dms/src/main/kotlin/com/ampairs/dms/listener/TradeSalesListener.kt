package com.ampairs.dms.listener

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.customer.domain.service.CustomerService
import com.ampairs.dms.domain.InvoiceSaleLineMapper
import com.ampairs.dms.domain.service.SecondarySalesRecomputeService
import com.ampairs.dms.domain.service.SnapshotDebounceCoordinator
import com.ampairs.event.domain.events.InvoiceCancelledEvent
import com.ampairs.event.domain.events.InvoiceFinalizedEvent
import com.ampairs.invoice.service.InvoiceService
import com.ampairs.product.service.ProductService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Rebuilds a distributor's secondary-sales snapshots when its invoices change. On a finalized or
 * cancelled invoice event it debounces per distributor (≤5 min, FR-022), then recomputes wholesale
 * from the distributor's INVOICED invoices — resolving each line's brand label via ProductService
 * (Hop A) and the retailer pincode via CustomerService, attributing via trade consent.
 *
 * Tenant context is set for the async handler so the distributor's @TenantId-filtered reads resolve;
 * it is always cleared in finally.
 */
@Component
class TradeSalesListener(
    private val invoiceService: InvoiceService,
    private val productService: ProductService,
    private val customerService: CustomerService,
    private val recomputeService: SecondarySalesRecomputeService,
    private val debounce: SnapshotDebounceCoordinator,
) {

    @Async
    @EventListener
    fun onInvoiceFinalized(event: InvoiceFinalizedEvent) = rebuild(event.workspaceId)

    @Async
    @EventListener
    fun onInvoiceCancelled(event: InvoiceCancelledEvent) = rebuild(event.workspaceId)

    fun rebuild(distributorWorkspaceId: String) {
        if (distributorWorkspaceId.isBlank()) return
        if (!debounce.shouldRebuild(distributorWorkspaceId, Instant.now().toEpochMilli())) return

        TenantContextHolder.setCurrentTenant(distributorWorkspaceId)
        try {
            val invoices = invoiceService.getInvoices(null)
            val lines = InvoiceSaleLineMapper.toSaleLines(distributorWorkspaceId, invoices)
            recomputeService.recompute(
                distributorWorkspaceId,
                lines,
                brandLabelOf = { productService.getProductByUid(it)?.brandId?.takeIf { b -> b.isNotBlank() } },
                pincodeOf = { customerService.getCustomerByUid(it)?.billingAddress?.pincode },
            )
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
