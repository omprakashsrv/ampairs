package com.ampairs.purchase.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.purchase.domain.enums.PurchaseStatus
import com.ampairs.purchase.domain.model.Purchase

data class PurchaseUpdateRequest(
    val id: String = "",
    val purchaseDate: java.time.Instant? = null,
    val purchaseNumber: String = "",
    var supplierId: String? = null,
    var supplierName: String? = null,
    var supplierPhone: String? = null,
    var supplierGst: String = "",
    var supplierInvoiceNumber: String? = null,
    var purchaseType: String = "REGULAR",
    var placeOfSupply: String = "",
    var totalCost: Double = 0.0,
    var basePrice: Double = 0.0,
    var totalTax: Double = 0.0,
    var status: PurchaseStatus = PurchaseStatus.DRAFT,
    var totalItems: Int = 0,
    var totalQuantity: Double = 0.0,
    var billingAddress: Address = Address(),
    var shippingAddress: Address = Address(),
    var purchaseItems: List<PurchaseItemRequest> = arrayListOf(),
    val taxInfos: List<TaxInfo> = arrayListOf(),
    val discount: List<Discount>? = null,
    var priceMode: String = "TAX_EXCLUSIVE",
    var overallDiscountMode: String = "POST_TAX_REDUCTION",
)

fun PurchaseUpdateRequest.toPurchase(): Purchase {
    val purchase = Purchase()
    purchase.uid = this.id
    purchase.purchaseNumber = this.purchaseNumber
    purchase.purchaseType = this.purchaseType
    purchase.supplierId = this.supplierId
    purchase.supplierName = this.supplierName
    purchase.supplierPhone = this.supplierPhone
    purchase.supplierGst = this.supplierGst
    purchase.supplierInvoiceNumber = this.supplierInvoiceNumber
    purchase.placeOfSupply = this.placeOfSupply
    purchase.basePrice = this.basePrice
    purchase.totalItems = this.totalItems
    purchase.totalCost = this.totalCost
    purchase.status = this.status
    purchase.totalQuantity = this.totalQuantity
    purchase.billingAddress = this.billingAddress
    purchase.shippingAddress = this.shippingAddress
    purchase.taxInfos = this.taxInfos
    purchase.totalTax = this.totalTax
    purchase.discount = this.discount
    purchase.priceMode = this.priceMode
    purchase.overallDiscountMode = this.overallDiscountMode
    if (this.purchaseDate != null) purchase.purchaseDate = this.purchaseDate
    return purchase
}
