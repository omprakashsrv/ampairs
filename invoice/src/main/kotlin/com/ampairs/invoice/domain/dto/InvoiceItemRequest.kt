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
    val taxInfos: List<TaxInfo> = arrayListOf(),
    val active: Boolean = true,
    val softDeleted: Boolean = false,
    val discount: List<Discount>? = null,
)

fun List<InvoiceItemRequest>.toInvoiceItems(): List<InvoiceItem> {
    return map {
        val invoiceItem = InvoiceItem()
        invoiceItem.seqId = it.id
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
        invoiceItem.productId = it.productId
        invoiceItem.taxCode = it.taxCode
        invoiceItem.taxInfos = it.taxInfos
        invoiceItem.active = it.active
        invoiceItem.softDeleted = it.softDeleted
        invoiceItem.discount = it.discount
        invoiceItem
    }
}