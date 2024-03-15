package com.ampairs.invoice.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice

data class InvoiceUpdateRequest(
    val id: String = "",
    val invoiceDate: String = "",
    val invoiceNumber: String = "",
    val orderRefId: String? = null,
    var fromCustomerId: String = "",
    var fromCustomerName: String = "",
    var toCustomerId: String = "",
    var toCustomerName: String = "",
    var fromCustomerGst: String = "",
    var toCustomerGst: String = "",
    var totalCost: Double = 0.0,
    var basePrice: Double = 0.0,
    var totalTax: Double = 0.0,
    var status: InvoiceStatus = InvoiceStatus.DRAFT,
    var totalItems: Int = 0,
    var totalQuantity: Double = 0.0,
    var billingAddress: Address = Address(),
    var shippingAddress: Address = Address(),
    var invoiceItems: List<InvoiceItemRequest> = arrayListOf(),
    val taxInfos: List<TaxInfo> = arrayListOf(),
    val discount: List<Discount>? = null,
)

fun InvoiceUpdateRequest.toInvoice(): Invoice {
    val invoice = Invoice()
    invoice.id = this.id
    invoice.invoiceNumber = this.invoiceNumber
    invoice.orderRefId = this.orderRefId
    invoice.fromCustomerId = this.fromCustomerId
    invoice.toCustomerId = this.toCustomerId
    invoice.fromCustomerName = this.fromCustomerName
    invoice.toCustomerName = this.toCustomerName
    invoice.fromCustomerGst = this.fromCustomerGst
    invoice.toCustomerGst = this.toCustomerGst
    invoice.basePrice = this.basePrice
    invoice.totalItems = this.totalItems
    invoice.totalCost = this.totalCost
    invoice.status = this.status
    invoice.totalQuantity = this.totalQuantity
    invoice.billingAddress = this.billingAddress
    invoice.shippingAddress = this.shippingAddress
    invoice.taxInfos = this.taxInfos
    invoice.totalTax = this.totalTax
    invoice.discount = this.discount
    return invoice
}
