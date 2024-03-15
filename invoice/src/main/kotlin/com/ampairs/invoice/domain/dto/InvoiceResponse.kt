package com.ampairs.invoice.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.invoice.domain.model.InvoiceItem
import java.text.SimpleDateFormat
import java.util.*

data class InvoiceResponse(
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
    var invoiceItems: List<InvoiceItemResponse> = arrayListOf(),
    val taxInfos: List<TaxInfo> = arrayListOf(),
    val active: Boolean = true,
    val softDeleted: Boolean = false,
    val discount: List<Discount>? = null,
)

fun List<Invoice>.toResponse(): List<InvoiceResponse> {
    return map {
        it.toResponse(it.invoiceItems)
    }
}

fun Invoice.toResponse(invoiceItems: List<InvoiceItem>): InvoiceResponse {
    return InvoiceResponse(
        id = this.id,
        invoiceDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(this.invoiceDate),
        invoiceNumber = this.invoiceNumber,
        orderRefId = this.orderRefId,
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
        invoiceItems = invoiceItems.toResponse(),
        active = this.active,
        softDeleted = this.softDeleted,
        discount = this.discount
    )
}

