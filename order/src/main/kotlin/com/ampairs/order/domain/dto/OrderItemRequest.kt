package com.ampairs.order.domain.dto

import com.ampairs.order.domain.model.OrderItem

data class OrderItemRequest(
    var id: String = "",
    var itemNo: Int = 0,
    var description: String = "",
    var quantity: Double = 0.0,
    var price: Double = 0.0,
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
)

fun List<OrderItemRequest>.toOrderItems(): List<OrderItem> {
    return map {
        val orderItem = OrderItem()
        orderItem.id = it.id
        orderItem.index = it.itemNo
        orderItem.description = it.description
        orderItem.quantity = it.quantity
        orderItem.sellingPrice = it.price
        orderItem.mrp = it.mrp
        orderItem.dp = it.dp
        orderItem.totalCost = it.totalCost
        orderItem.totalTax = it.totalTax
        orderItem.basePrice = it.basePrice
        orderItem.orderId = it.orderId
        orderItem.productId = it.productId
        orderItem.taxCode = it.taxCode
        orderItem.taxInfos = it.taxInfos
        orderItem.active = it.active
        orderItem.softDeleted = it.softDeleted
        orderItem
    }
}