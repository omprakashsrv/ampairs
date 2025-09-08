package com.ampairs.invoice.domain.dto

import com.ampairs.invoice.domain.model.InvoiceItem

data class InvoiceItemResponse(
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

fun List<InvoiceItem>.toResponse(): List<InvoiceItemResponse> {
    return map {
        InvoiceItemResponse(
            id = it.uid,
            itemNo = it.index,
            description = it.description,
            quantity = it.quantity,
            price = it.sellingPrice,
            productPrice = it.productPrice,
            mrp = it.mrp,
            dp = it.dp,
            totalCost = it.totalCost,
            totalTax = it.totalTax,
            basePrice = it.basePrice,
            invoiceId = it.invoiceId,
            productId = it.productId,
            taxCode = it.taxCode,
            taxInfos = it.taxInfos,
            active = it.active,
            softDeleted = it.softDeleted,
            discount = it.discount
        )
    }
}