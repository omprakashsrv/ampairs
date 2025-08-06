package com.ampairs.order.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.order.domain.enums.OrderStatus
import com.ampairs.order.domain.model.Order

data class OrderUpdateRequest(
    val id: String = "",
    val orderDate: String = "",
    val orderNumber: String = "",
    val invoiceRefId: String = "",
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
    order.uid = this.id
    order.orderNumber = this.orderNumber
    order.invoiceRefId = this.invoiceRefId
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

fun Order.toInvoice(): Invoice {
    val invoice = Invoice()
    invoice.id = 0
    invoice.invoiceNumber = ""
    invoice.orderRefId = this.uid
    invoice.fromCustomerId = this.fromCustomerId
    invoice.toCustomerId = this.toCustomerId
    invoice.fromCustomerName = this.fromCustomerName
    invoice.toCustomerName = this.toCustomerName
    invoice.fromCustomerGst = this.fromCustomerGst
    invoice.toCustomerGst = this.toCustomerGst
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
    return invoice
}

