package com.ampairs.order.domain.dto

import com.ampairs.invoice.domain.model.InvoiceItem
import com.ampairs.order.domain.model.OrderItem

data class OrderItemRequest(
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

fun List<OrderItemRequest>.toOrderItems(): List<OrderItem> {
    return map {
        val orderItem = OrderItem()
        orderItem.uid = it.id
        orderItem.index = it.itemNo
        orderItem.description = it.description
        orderItem.quantity = it.quantity
        orderItem.sellingPrice = it.price
        orderItem.productPrice = it.productPrice
        orderItem.mrp = it.mrp
        orderItem.dp = it.dp
        orderItem.totalCost = it.totalCost
        orderItem.totalTax = it.totalTax
        orderItem.basePrice = it.basePrice
        orderItem.orderId = it.orderId
        orderItem.productId = it.productId
        orderItem.taxCode = it.taxCode
        orderItem.taxInfos = it.taxInfos
        orderItem.discount = it.discount
        orderItem
    }
}

fun List<OrderItem>.toInvoiceItems(): List<InvoiceItem> {
    return map {
        val orderItem = InvoiceItem()
        orderItem.uid = it.uid
        orderItem.index = it.index
        orderItem.description = it.description
        orderItem.quantity = it.quantity
        orderItem.sellingPrice = it.sellingPrice
        orderItem.productPrice = it.productPrice
        orderItem.mrp = it.mrp
        orderItem.dp = it.dp
        orderItem.totalCost = it.totalCost
        orderItem.totalTax = it.totalTax
        orderItem.basePrice = it.basePrice
        orderItem.invoiceId = ""
        orderItem.productId = it.productId
        orderItem.taxCode = it.taxCode
        orderItem.taxInfos = it.taxInfos.toInvoiceTaxInfos()
        orderItem.discount = it.discount?.toInvoiceDiscount()
        orderItem
    }
}