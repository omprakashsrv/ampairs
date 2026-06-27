package com.ampairs.invoice.service

import com.ampairs.invoice.domain.dto.FinalizedInvoiceProjection
import com.ampairs.invoice.domain.dto.TaxLineProjection
import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.invoice.repository.InvoiceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Public read-only port exposing finalized-invoice aggregates to the `analytics` module without
 * leaking the JPA entity (Principle II/IX). Tenant scope is enforced by `@TenantId` on `Invoice`.
 */
@Service
class InvoiceAnalyticsQueryService(
    private val invoiceRepository: InvoiceRepository,
) {

    /**
     * Finalized (INVOICED) invoices whose `invoiceDate` is in [fromInclusive, toExclusive), projected
     * for KPI roll-up. Drafts and de-finalized invoices are excluded by construction (FR-013).
     */
    @Transactional(readOnly = true)
    fun finalizedBetween(fromInclusive: Instant, toExclusive: Instant): List<FinalizedInvoiceProjection> =
        invoiceRepository
            .findByStatusAndInvoiceDateGreaterThanEqualAndInvoiceDateLessThan(
                InvoiceStatus.INVOICED, fromInclusive, toExclusive,
            )
            .map { it.toProjection() }

    private fun Invoice.toProjection(): FinalizedInvoiceProjection {
        val seller = sellerPlaceOfSupply?.trim().orEmpty()
        val buyer = placeOfSupply.trim()
        // Intra-state when both are known and equal; otherwise treated as inter-state (IGST).
        val intra = seller.isNotEmpty() && buyer.isNotEmpty() && seller.equals(buyer, ignoreCase = true)
        return FinalizedInvoiceProjection(
            invoiceDateEpochMillis = invoiceDate.toEpochMilli(),
            gross = totalCost,
            net = basePrice,
            tax = totalTax,
            customerId = customerId.orEmpty(),
            intraState = intra,
            taxLines = taxInfos
                .filter { it.value != 0.0 }
                .map { TaxLineProjection(rate = it.percentage, taxValue = it.value) },
        )
    }
}
