package com.ampairs.invoice.service

import com.ampairs.core.service.BuyerInvoiceDetail
import com.ampairs.core.service.BuyerInvoiceLine
import com.ampairs.core.service.BuyerInvoiceSummary
import com.ampairs.core.service.InvoiceEcomService
import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.invoice.domain.model.InvoiceItem
import com.ampairs.invoice.repository.InvoiceRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Spec 029 — buyer-facing read of workspace invoices, exposed to `ecom` via the `core`
 * [InvoiceEcomService] interface. Returns invoices the seller has actually issued — `NEW` (created)
 * and `INVOICED` (finalized) — hiding only `DRAFT` work-in-progress. (The app creates invoices as
 * `NEW` and has no finalize step, so an `INVOICED`-only filter showed the buyer nothing.) Every read
 * is guarded by `customerId == partyUid`. Requires an active tenant context (set by the ecom controller).
 */
@Service
@Transactional(readOnly = true)
class InvoiceEcomServiceImpl(
    private val invoiceRepository: InvoiceRepository,
) : InvoiceEcomService {

    override fun listBuyerInvoices(partyUid: String, pageable: Pageable): Page<BuyerInvoiceSummary> =
        invoiceRepository.findByCustomerIdAndStatusIn(partyUid, BUYER_VISIBLE, pageable)
            .map { it.toSummary() }

    override fun getBuyerInvoice(invoiceUid: String, partyUid: String): BuyerInvoiceDetail? {
        val invoice = invoiceRepository.findByUid(invoiceUid) ?: return null
        if (invoice.customerId != partyUid) return null
        if (invoice.status !in BUYER_VISIBLE) return null
        return invoice.toDetail()
    }

    override fun listInvoicesForOrder(orderRefId: String, partyUid: String): List<BuyerInvoiceSummary> =
        invoiceRepository.findByOrderRefIdAndStatusIn(orderRefId, BUYER_VISIBLE)
            .filter { it.customerId == partyUid }
            .map { it.toSummary() }

    private fun Invoice.toSummary() = BuyerInvoiceSummary(
        invoiceUid = uid,
        invoiceNumber = invoiceNumber,
        invoiceDate = invoiceDate,
        total = BigDecimal.valueOf(totalCost),
        orderRefId = orderRefId,
    )

    private fun Invoice.toDetail() = BuyerInvoiceDetail(
        invoiceUid = uid,
        invoiceNumber = invoiceNumber,
        invoiceDate = invoiceDate,
        orderRefId = orderRefId,
        lines = invoiceItems.filter { it.active }.sortedBy { it.index }.map { it.toLine() },
        subtotal = BigDecimal.valueOf(basePrice),
        taxTotal = BigDecimal.valueOf(totalTax),
        total = BigDecimal.valueOf(totalCost),
    )

    private fun InvoiceItem.toLine() = BuyerInvoiceLine(
        description = description,
        quantity = BigDecimal.valueOf(quantity),
        unitPrice = BigDecimal.valueOf(sellingPrice),
        // Net (pre-tax) line amount, so the lines reconcile with the detail's subtotal (invoice.basePrice)
        // + tax = total breakdown. Using totalCost (tax-inclusive) here would make the rows sum to the
        // grand total instead of the subtotal, double-presenting tax alongside the separate tax line.
        lineTotal = BigDecimal.valueOf(basePrice),
    )

    private companion object {
        /**
         * Statuses a storefront buyer may see: issued invoices — `NEW` (created) and `INVOICED`
         * (finalized). Only true `DRAFT` work-in-progress is hidden. `INVOICED` alone showed nothing,
         * because the app creates invoices as `NEW` with no finalize action to reach `INVOICED`.
         */
        val BUYER_VISIBLE = setOf(InvoiceStatus.NEW, InvoiceStatus.INVOICED)
    }
}
