package com.ampairs.purchase.domain.dto

import com.ampairs.purchase.domain.model.PurchaseItem

data class PurchaseItemRequest(
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
    var purchaseId: String = "",
    var productId: String = "",
    var taxCode: String = "",
    var unitId: String = "",
    var baseQuantity: Double = 0.0,
    var variantSku: String? = null,
    val taxInfos: List<TaxInfo> = arrayListOf(),
    val active: Boolean = true,
    val softDeleted: Boolean = false,
    val discount: List<Discount>? = null,
)

fun List<PurchaseItemRequest>.toPurchaseItems(): List<PurchaseItem> {
    return map {
        val item = PurchaseItem()
        item.uid = it.id
        item.index = it.itemNo
        item.description = it.description
        item.quantity = it.quantity
        item.purchasePrice = it.price
        item.unitPrice = it.price
        item.productPrice = it.productPrice
        item.mrp = it.mrp
        item.dp = it.dp
        item.totalCost = it.totalCost
        item.totalTax = it.totalTax
        item.basePrice = it.basePrice
        item.purchaseId = it.purchaseId
        item.productId = it.productId
        item.taxCode = it.taxCode
        item.unitId = it.unitId
        item.baseQuantity = it.baseQuantity
        item.variantSku = it.variantSku
        item.taxInfos = it.taxInfos
        item.discount = it.discount
        item
    }
}
