package com.ampairs.order.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.order.domain.enums.OrderStatus
import com.ampairs.order.domain.model.Order
import com.ampairs.order.domain.model.OrderItem
import java.time.Instant

data class OrderResponse(
    val id: String = "",
    val orderDate: Instant = Instant.now(),
    val orderNumber: String = "",
    val invoiceRefId: String? = null,
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
    var orderItems: List<OrderItemResponse> = arrayListOf(),
    val taxInfos: List<TaxInfo> = arrayListOf(),
    val active: Boolean = true,
    val softDeleted: Boolean = false,
    val discount: List<Discount>? = null,
    var priceMode: String = "TAX_EXCLUSIVE",
    var overallDiscountMode: String = "POST_TAX_REDUCTION",
)

fun List<Order>.toResponse(): List<OrderResponse> {
    return map {
        it.toResponse(it.orderItems)
    }
}

fun Order.toResponse(orderItems: List<OrderItem>): OrderResponse {
    return OrderResponse(
        id = this.uid,
        orderDate = this.orderDate,
        orderNumber = this.orderNumber,
        invoiceRefId = this.invoiceRefId,
        fromCustomerId = this.fromCustomerId,
        fromCustomerName = this.fromCustomerName,
        toCustomerId = this.toCustomerId,
        toCustomerName = this.toCustomerName,
        fromCustomerGst = this.fromCustomerGst,
        toCustomerGst = this.toCustomerGst,
        totalCost = this.totalCost,
        basePrice = this.basePrice,
        totalTax = this.totalTax,
        status = this.status,
        totalItems = this.totalItems,
        totalQuantity = this.totalQuantity,
        billingAddress = this.billingAddress,
        shippingAddress = this.shippingAddress,
        taxInfos = this.taxInfos,
        orderItems = orderItems.toResponse(),
        discount = this.discount,
        priceMode = this.priceMode,
        overallDiscountMode = this.overallDiscountMode
    )
}

