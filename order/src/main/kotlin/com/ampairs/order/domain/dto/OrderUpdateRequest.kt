package com.ampairs.order.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.order.domain.enums.OrderStatus
import com.ampairs.order.domain.model.Order

data class OrderUpdateRequest(
    val id: String = "",
    val orderDate: java.time.Instant? = null,
    val orderNumber: String = "",
    val invoiceRefId: String = "",
    var customerId: String? = null,
    var customerName: String? = null,
    var customerPhone: String? = null,
    var customerGst: String = "",
    var isWalkIn: Boolean = false,
    var placeOfSupply: String = "",
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
    var priceMode: String = "TAX_EXCLUSIVE",
    var overallDiscountMode: String = "POST_TAX_REDUCTION",
)

fun OrderUpdateRequest.toOrder(): Order {
    val order = Order()
    order.uid = this.id
    order.orderNumber = this.orderNumber
    order.invoiceRefId = this.invoiceRefId
    order.customerId = this.customerId
    order.customerName = this.customerName
    order.customerPhone = this.customerPhone
    order.customerGst = this.customerGst
    order.isWalkIn = this.isWalkIn
    order.placeOfSupply = this.placeOfSupply
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
    order.priceMode = this.priceMode
    order.overallDiscountMode = this.overallDiscountMode
    if (this.orderDate != null) order.orderDate = this.orderDate
    return order
}

fun Order.toInvoice(): Invoice {
    val invoice = Invoice()
    invoice.id = 0
    invoice.invoiceNumber = ""
    invoice.orderRefId = this.uid
    invoice.customerId = this.customerId
    invoice.customerName = this.customerName
    invoice.customerPhone = this.customerPhone
    invoice.customerGst = this.customerGst
    invoice.placeOfSupply = this.placeOfSupply
    invoice.basePrice = this.basePrice
    invoice.totalItems = this.totalItems
    invoice.totalCost = this.totalCost
    invoice.status = InvoiceStatus.NEW
    invoice.totalQuantity = this.totalQuantity
    invoice.billingAddress = this.billingAddress
    invoice.shippingAddress = this.shippingAddress
    invoice.taxInfos = this.taxInfos.toInvoiceTaxInfos()
    invoice.totalTax = this.totalTax
    invoice.discount = this.discount?.toInvoiceDiscount()
    invoice.priceMode = this.priceMode
    invoice.overallDiscountMode = this.overallDiscountMode
    return invoice
}

