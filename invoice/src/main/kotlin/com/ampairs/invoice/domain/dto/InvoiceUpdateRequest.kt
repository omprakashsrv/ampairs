package com.ampairs.invoice.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice

data class InvoiceUpdateRequest(
    val id: String = "",
    val invoiceDate: java.time.Instant? = null,
    val invoiceNumber: String = "",
    val orderRefId: String? = null,
    var customerId: String? = null,
    var customerName: String? = null,
    var customerPhone: String? = null,
    var customerGst: String = "",
    var sellerName: String? = null,
    var sellerAddress: String? = null,
    var sellerGst: String? = null,
    var placeOfSupply: String = "",
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
    var priceMode: String = "TAX_EXCLUSIVE",
    var overallDiscountMode: String = "POST_TAX_REDUCTION",
    var series: String = "INV",
    var sequenceNumber: Long = 0,
)

fun InvoiceUpdateRequest.toInvoice(): Invoice {
    val invoice = Invoice()
    invoice.uid = this.id
    invoice.invoiceNumber = this.invoiceNumber
    invoice.orderRefId = this.orderRefId
    invoice.customerId = this.customerId
    invoice.customerName = this.customerName
    invoice.customerPhone = this.customerPhone
    invoice.customerGst = this.customerGst
    invoice.sellerName = this.sellerName
    invoice.sellerAddress = this.sellerAddress
    invoice.sellerGst = this.sellerGst
    invoice.placeOfSupply = this.placeOfSupply
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
    invoice.priceMode = this.priceMode
    invoice.overallDiscountMode = this.overallDiscountMode
    invoice.series = this.series.ifBlank { "INV" }
    invoice.sequenceNumber = this.sequenceNumber
    if (this.invoiceDate != null) invoice.invoiceDate = this.invoiceDate
    return invoice
}
