package com.ampairs.order.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.order.domain.enums.OrderStatus
import com.ampairs.order.domain.model.Order

data class OrderUpdateRequest(
    val id: String = "",
    val orderDate: String = "",
    val orderNumber: String = "",
    var fromCustomerId: String = "",
    var fromCustomerName: String = "",
    var toCustomerId: String = "",
    var toCustomerName: String = "",
    var fromCustomerGst: String = "",
    var toCustomerGst: String = "",
    var totalCost: Double = 0.0,
    var basePrice: Double = 0.0,
    var totalTax: Double = 0.0,
    var status: OrderStatus = OrderStatus.DRAFT,
    var totalItems: Int = 0,
    var totalQuantity: Double = 0.0,
    var billingAddress: Address = Address(),
    var shippingAddress: Address = Address(),
    var orderItems: List<OrderItemRequest> = arrayListOf(),
    val taxInfos: List<TaxInfo> = arrayListOf(),
    val discount: List<Discount>? = null,
)

fun OrderUpdateRequest.toOrder(): Order {
    val order = Order()
    order.id = this.id
    order.orderNumber = this.orderNumber
    order.fromCustomerId = this.fromCustomerId
    order.toCustomerId = this.toCustomerId
    order.fromCustomerName = this.fromCustomerName
    order.toCustomerName = this.toCustomerName
    order.fromCustomerGst = this.fromCustomerGst
    order.toCustomerGst = this.toCustomerGst
    order.basePrice = this.basePrice
    order.totalItems = this.totalItems
    order.totalCost = this.totalCost
    order.status = this.status
    order.totalQuantity = this.totalQuantity
    order.billingAddress = this.billingAddress
    order.shippingAddress = this.shippingAddress
    order.taxInfos = this.taxInfos
    order.totalTax = this.totalTax
    order.discount = this.discount
    return order
}
