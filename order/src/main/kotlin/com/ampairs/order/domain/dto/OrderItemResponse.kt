package com.ampairs.order.domain.dto

import com.ampairs.order.domain.model.OrderItem

data class OrderItemResponse(
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
    var orderId: String = "",
    var productId: String = "",
    var taxCode: String = "",
    val taxInfos: List<TaxInfo> = arrayListOf(),
    val active: Boolean = true,
    val softDeleted: Boolean = false,
    val discount: List<Discount>? = null,
)

fun List<OrderItem>.toResponse(): List<OrderItemResponse> {
    return map {
        OrderItemResponse(
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
            orderId = it.orderId,
            productId = it.productId,
            taxCode = it.taxCode,
            taxInfos = it.taxInfos,
            discount = it.discount
        )
    }
}