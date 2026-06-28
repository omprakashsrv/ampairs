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
    var unitId: String = "",
    var baseQuantity: Double = 0.0,
    var variantSku: String? = null,
    val taxInfos: List<TaxInfo> = arrayListOf(),
    val active: Boolean = true,
    val softDeleted: Boolean = false,
    val discount: List<Discount>? = null,
    // 009 pricing snapshot.
    var resolvedUnitPriceMinor: Long? = null,
    var currency: String? = null,
    var priceSource: String? = null,
    var matchedPriceListUid: String? = null,
    var appliedTierMinQty: Double? = null,
    var belowMoq: Boolean? = null,
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
            unitId = it.unitId,
            baseQuantity = it.baseQuantity,
            variantSku = it.variantSku,
            taxInfos = it.taxInfos,
            discount = it.discount,
            resolvedUnitPriceMinor = it.resolvedUnitPriceMinor,
            currency = it.currency,
            priceSource = it.priceSource,
            matchedPriceListUid = it.matchedPriceListUid,
            appliedTierMinQty = it.appliedTierMinQty,
            belowMoq = it.belowMoq,
        )
    }
}