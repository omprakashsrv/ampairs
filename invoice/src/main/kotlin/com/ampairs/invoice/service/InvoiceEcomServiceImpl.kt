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
 * [InvoiceEcomService] interface. Only finalized (`INVOICED`) invoices are ever returned; drafts and
 * pre-finalization (`NEW`) invoices are never shown to a buyer. Every read is guarded by
 * `customerId == partyUid`. Requires an active tenant context (set by the ecom controller).
 */
@Service
@Transactional(readOnly = true)
class InvoiceEcomServiceImpl(
    private val invoiceRepository: InvoiceRepository,
) : InvoiceEcomService {

    override fun listBuyerInvoices(partyUid: String, pageable: Pageable): Page<BuyerInvoiceSummary> =
        invoiceRepository.findByCustomerIdAndStatusIn(partyUid, FINALIZED, pageable)
            .map { it.toSummary() }

    override fun getBuyerInvoice(invoiceUid: String, partyUid: String): BuyerInvoiceDetail? {
        val invoice = invoiceRepository.findByUid(invoiceUid) ?: return null
        if (invoice.customerId != partyUid) return null
        if (invoice.status !in FINALIZED) return null
        return invoice.toDetail()
    }

    override fun listInvoicesForOrder(orderRefId: String, partyUid: String): List<BuyerInvoiceSummary> =
        invoiceRepository.findByOrderRefIdAndStatusIn(orderRefId, FINALIZED)
            .filter { it.customerId == partyUid }
            .map { it.toSummary() }

    private fun Invoice.toSummary() = BuyerInvoiceSummary(
        invoiceUid = uid,
        invoiceNumber = invoiceNumber,
        invoiceDate = invoiceDate,
        status = buyerStatus(status),
        total = BigDecimal.valueOf(totalCost),
        orderRefId = orderRefId,
    )

    private fun Invoice.toDetail() = BuyerInvoiceDetail(
        invoiceUid = uid,
        invoiceNumber = invoiceNumber,
        invoiceDate = invoiceDate,
        status = buyerStatus(status),
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
        lineTotal = BigDecimal.valueOf(totalCost),
    )

    private companion object {
        /** The sole finalize boundary the system keys off (ledger/stock/analytics). */
        val FINALIZED = setOf(InvoiceStatus.INVOICED)

        /**
         * Buyer-facing status string (never the raw enum — DTO isolation). Only [InvoiceStatus.INVOICED]
         * is ever surfaced today; the mapping is centralized so the vocabulary can grow (spec OQ-6).
         */
        fun buyerStatus(status: InvoiceStatus): String = when (status) {
            InvoiceStatus.INVOICED -> "RAISED"
            InvoiceStatus.DRAFT -> "DRAFT"
            InvoiceStatus.NEW -> "PENDING"
        }
    }
}
