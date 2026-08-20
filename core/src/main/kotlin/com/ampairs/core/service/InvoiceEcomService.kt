package com.ampairs.core.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Instant

/**
 * Cross-module bridge (spec 029): read a storefront buyer's invoices from the workspace `invoice`
 * module. The interface lives in `core`, the implementation in `invoice`, so `ecom` depends only on
 * `core` (same pattern as [OrderEcomService] / [EcomCustomerService]).
 *
 * Every read is keyed by the resolved CRM party (`partyUid == Invoice.customerId`) — the ecom
 * controller resolves the buyer's login to a linked CRM customer before calling. Requires an active
 * tenant context (set by the controller from the storefront slug). Only **finalized** invoices
 * (`InvoiceStatus.INVOICED`) are ever returned; drafts are never exposed to a buyer.
 *
 * The returned DTOs carry the raw workspace [BuyerInvoiceSummary.orderRefId] / [BuyerInvoiceDetail.orderRefId];
 * the ecom controller swaps it for the buyer-facing storefront order ref before serializing.
 */
interface InvoiceEcomService {

    /** Finalized invoices for [partyUid], newest first, paginated. */
    fun listBuyerInvoices(partyUid: String, pageable: Pageable): Page<BuyerInvoiceSummary>

    /**
     * A single finalized invoice, but only if it belongs to [partyUid]. Returns null when the invoice
     * does not exist, belongs to another party, or is not finalized — the controller maps null → 404.
     */
    fun getBuyerInvoice(invoiceUid: String, partyUid: String): BuyerInvoiceDetail?

    /**
     * Finalized invoices raised from the workspace order [orderRefId] (== `EcomOrder.managementOrderRef`),
     * scoped to [partyUid]. Empty when the order has no invoice yet. Backs the order↔invoice link.
     */
    fun listInvoicesForOrder(orderRefId: String, partyUid: String): List<BuyerInvoiceSummary>
}

/**
 * Buyer-safe invoice summary. Carries no cost/margin, audit, or tenant fields. [orderRefId] is the
 * raw workspace order uid; the ecom controller replaces it with the buyer-facing storefront ref.
 */
data class BuyerInvoiceSummary(
    val invoiceUid: String,
    val invoiceNumber: String,
    val invoiceDate: Instant,
    val status: String,
    val total: BigDecimal,
    val orderRefId: String?,
)

/** Buyer-safe invoice detail (line items + totals). No cost/margin exposed. */
data class BuyerInvoiceDetail(
    val invoiceUid: String,
    val invoiceNumber: String,
    val invoiceDate: Instant,
    val status: String,
    val orderRefId: String?,
    val lines: List<BuyerInvoiceLine>,
    val subtotal: BigDecimal,
    val taxTotal: BigDecimal,
    val total: BigDecimal,
)

/** Buyer-safe invoice line. [unitPrice] is the display selling price — never cost. */
data class BuyerInvoiceLine(
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
)
