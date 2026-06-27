package com.ampairs.invoice.domain.dto

/**
 * Self-contained, entity-free projection of a finalized invoice for the `analytics` bounded context
 * (Principle IX: cross-module access via public service + DTO only — never the JPA entity).
 *
 * Money is the invoice's snapshot in major currency units; GST is pre-classified intra/inter-state so
 * analytics needs no tax-module knowledge.
 */
data class FinalizedInvoiceProjection(
    val invoiceDateEpochMillis: Long,
    val gross: Double,        // totalCost
    val net: Double,          // basePrice (taxable base)
    val tax: Double,          // totalTax
    val customerId: String,   // "" when walk-in / unset
    val intraState: Boolean,  // placeOfSupply == sellerPlaceOfSupply → CGST+SGST, else IGST
    val taxLines: List<TaxLineProjection>,
)

/** One output-tax line: the GST rate and the tax amount snapshotted on the invoice. */
data class TaxLineProjection(
    val rate: Double,
    val taxValue: Double,
)
