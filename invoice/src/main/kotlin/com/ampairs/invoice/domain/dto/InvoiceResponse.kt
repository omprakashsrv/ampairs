package com.ampairs.invoice.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.invoice.domain.model.InvoiceItem
import java.time.Instant

data class InvoiceResponse(
    val id: String = "",
    val invoiceDate: Instant = Instant.now(),
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
    var sellerPlaceOfSupply: String? = null,
    var totalCost: Double = 0.0,
    var basePrice: Double = 0.0,
    var totalTax: Double = 0.0,
    var status: InvoiceStatus = InvoiceStatus.DRAFT,
    var totalItems: Int = 0,
    var totalQuantity: Double = 0.0,
    var billingAddress: Address = Address(),
    var shippingAddress: Address = Address(),
    var invoiceItems: List<InvoiceItemResponse> = arrayListOf(),
    val taxInfos: List<TaxInfo> = arrayListOf(),
    val active: Boolean = true,
    val softDeleted: Boolean = false,
    val discount: List<Discount>? = null,
    var priceMode: String = "TAX_EXCLUSIVE",
    var overallDiscountMode: String = "POST_TAX_REDUCTION",
    var series: String = "INV",
    var sequenceNumber: Long = 0,
)

fun List<Invoice>.toResponse(): List<InvoiceResponse> {
    return map {
        it.toResponse(it.invoiceItems)
    }
}

fun Invoice.toResponse(invoiceItems: List<InvoiceItem>): InvoiceResponse {
    return InvoiceResponse(
        id = this.uid,
        invoiceDate = this.invoiceDate,
        invoiceNumber = this.invoiceNumber,
        orderRefId = this.orderRefId,
        customerId = this.customerId,
        customerName = this.customerName,
        customerPhone = this.customerPhone,
        customerGst = this.customerGst,
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
        invoiceItems = invoiceItems.toResponse(),
        discount = this.discount,
        priceMode = this.priceMode,
        overallDiscountMode = this.overallDiscountMode,
        series = this.series,
        sequenceNumber = this.sequenceNumber
    )
}

