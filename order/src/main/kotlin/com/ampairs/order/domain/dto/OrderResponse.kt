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
    var customerId: String? = null,
    var customerName: String? = null,
    var customerPhone: String? = null,
    var customerGst: String = "",
    var isWalkIn: Boolean = false,
    var sellerName: String? = null,
    var sellerAddress: String? = null,
    var sellerGst: String? = null,
    var placeOfSupply: String = "",
    var sellerPlaceOfSupply: String? = null,
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
    var createdAt: Instant? = null,
    var updatedAt: Instant? = null,
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
        customerId = this.customerId,
        customerName = this.customerName,
        customerPhone = this.customerPhone,
        customerGst = this.customerGst,
        isWalkIn = this.isWalkIn,
        sellerName = this.sellerName,
        sellerAddress = this.sellerAddress,
        sellerGst = this.sellerGst,
        placeOfSupply = this.placeOfSupply,
        sellerPlaceOfSupply = this.sellerPlaceOfSupply,
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
        overallDiscountMode = this.overallDiscountMode,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
}

