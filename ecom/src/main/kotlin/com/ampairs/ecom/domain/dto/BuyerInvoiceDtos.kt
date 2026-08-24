package com.ampairs.ecom.domain.dto

import com.ampairs.core.service.BuyerInvoiceDetail
import com.ampairs.core.service.BuyerInvoiceLine
import com.ampairs.core.service.BuyerInvoiceSummary
import java.math.BigDecimal
import java.time.Instant

/**
 * Spec 029 — ecom wire shapes for a buyer's invoices. The `core` [BuyerInvoiceSummary]/[BuyerInvoiceDetail]
 * carry the raw workspace `orderRefId`; the ecom controller resolves it to the buyer-facing storefront
 * order ref ([orderRef], serialized `order_ref`) via `EcomOrder.managementOrderRef` before mapping here.
 *
 * [status] is the buyer-facing payment state ("Paid" / "Unpaid"), computed by the ecom controller from
 * the party ledger's open bills (spec OQ-6) — an invoice with an outstanding balance is "Unpaid". It is
 * passed in rather than read off the `core` DTO, whose status reflects only the invoice finalize state.
 */
data class BuyerInvoiceSummaryResponse(
    val invoiceUid: String,
    val invoiceNumber: String,
    val invoiceDate: Instant,
    val status: String,
    val total: BigDecimal,
    val orderRef: String?,
)

data class BuyerInvoiceDetailResponse(
    val invoiceUid: String,
    val invoiceNumber: String,
    val invoiceDate: Instant,
    val status: String,
    val orderRef: String?,
    val lines: List<BuyerInvoiceLineResponse>,
    val subtotal: BigDecimal,
    val taxTotal: BigDecimal,
    val total: BigDecimal,
)

data class BuyerInvoiceLineResponse(
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
)

fun BuyerInvoiceSummary.toResponse(orderRef: String?, status: String) = BuyerInvoiceSummaryResponse(
    invoiceUid = invoiceUid,
    invoiceNumber = invoiceNumber,
    invoiceDate = invoiceDate,
    status = status,
    total = total,
    orderRef = orderRef,
)

fun BuyerInvoiceDetail.toResponse(orderRef: String?, status: String) = BuyerInvoiceDetailResponse(
    invoiceUid = invoiceUid,
    invoiceNumber = invoiceNumber,
    invoiceDate = invoiceDate,
    status = status,
    orderRef = orderRef,
    lines = lines.map { it.toResponse() },
    subtotal = subtotal,
    taxTotal = taxTotal,
    total = total,
)

fun BuyerInvoiceLine.toResponse() = BuyerInvoiceLineResponse(
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    lineTotal = lineTotal,
)
