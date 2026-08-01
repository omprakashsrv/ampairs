package com.ampairs.invoice.domain.dto

import com.ampairs.invoice.domain.model.InvoiceItem

data class InvoiceItemRequest(
    var id: String = "",
    var itemNo: Int = 0,
    var description: String = "",
    var quantity: Double = 0.0,
    var price: Double = 0.0,
    var productPrice: Double = 0.0,
    var mrp: Double = 0.0,
    var dp: Double = 0.0,
    var totalCost: Double = 0.0,
    var totalTax: Double = 0.0,
    var basePrice: Double = 0.0,
    var invoiceId: String = "",
    var productId: String = "",
    var taxCode: String = "",
    var unitId: String = "",
    var baseQuantity: Double = 0.0,
    var variantSku: String? = null,
    val taxInfos: List<TaxInfo> = arrayListOf(),
    val active: Boolean = true,
    val softDeleted: Boolean = false,
    val discount: List<Discount>? = null,
    // 009 pricing snapshot — client-resolved; persisted verbatim (no server re-resolution).
    var resolvedUnitPriceMinor: Long? = null,
    var currency: String? = null,
    var priceSource: String? = null,
    var matchedPriceListUid: String? = null,
    var appliedTierMinQty: Double? = null,
    var belowMoq: Boolean? = null,
)

fun List<InvoiceItemRequest>.toInvoiceItems(): List<InvoiceItem> {
    return map {
        val invoiceItem = InvoiceItem()
        invoiceItem.uid = it.id
        invoiceItem.index = it.itemNo
        invoiceItem.description = it.description
        invoiceItem.quantity = it.quantity
        invoiceItem.sellingPrice = it.price
        invoiceItem.productPrice = it.productPrice
        invoiceItem.mrp = it.mrp
        invoiceItem.dp = it.dp
        invoiceItem.totalCost = it.totalCost
        invoiceItem.totalTax = it.totalTax
        invoiceItem.basePrice = it.basePrice
        invoiceItem.invoiceId = it.invoiceId
        // A removed line arrives as active = false (or softDeleted = true); persisted verbatim so the
        // deletion round-trips on the invoice /sync feed.
        invoiceItem.active = it.active && !it.softDeleted
        invoiceItem.productId = it.productId
        invoiceItem.taxCode = it.taxCode
        invoiceItem.unitId = it.unitId
        invoiceItem.baseQuantity = it.baseQuantity
        invoiceItem.variantSku = it.variantSku
        invoiceItem.taxInfos = it.taxInfos
        invoiceItem.discount = it.discount
        invoiceItem.resolvedUnitPriceMinor = it.resolvedUnitPriceMinor
        invoiceItem.currency = it.currency
        invoiceItem.priceSource = it.priceSource
        invoiceItem.matchedPriceListUid = it.matchedPriceListUid
        invoiceItem.appliedTierMinQty = it.appliedTierMinQty
        invoiceItem.belowMoq = it.belowMoq
        invoiceItem
    }
}