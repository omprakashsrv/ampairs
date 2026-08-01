package com.ampairs.purchase.domain.dto

import com.ampairs.purchase.domain.model.PurchaseItem

data class PurchaseItemResponse(
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

fun List<PurchaseItem>.toResponse(): List<PurchaseItemResponse> {
    return map {
        PurchaseItemResponse(
            id = it.uid,
            itemNo = it.index,
            description = it.description,
            quantity = it.quantity,
            price = it.purchasePrice,
            productPrice = it.productPrice,
            mrp = it.mrp,
            dp = it.dp,
            totalCost = it.totalCost,
            totalTax = it.totalTax,
            basePrice = it.basePrice,
            purchaseId = it.purchaseId,
            productId = it.productId,
            taxCode = it.taxCode,
            unitId = it.unitId,
            baseQuantity = it.baseQuantity,
            variantSku = it.variantSku,
            taxInfos = it.taxInfos,
            discount = it.discount
        )
    }
}
