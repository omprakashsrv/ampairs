package com.ampairs.purchase.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.purchase.domain.enums.PurchaseStatus
import com.ampairs.purchase.domain.model.Purchase
import com.ampairs.purchase.domain.model.PurchaseItem
import java.time.Instant

data class PurchaseResponse(
    val id: String = "",
    val purchaseDate: Instant = Instant.now(),
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
    var purchaseItems: List<PurchaseItemResponse> = arrayListOf(),
    val taxInfos: List<TaxInfo> = arrayListOf(),
    val active: Boolean = true,
    val softDeleted: Boolean = false,
    val discount: List<Discount>? = null,
    var priceMode: String = "TAX_EXCLUSIVE",
    var overallDiscountMode: String = "POST_TAX_REDUCTION",
)

fun List<Purchase>.toResponse(): List<PurchaseResponse> {
    return map { it.toResponse(it.purchaseItems) }
}

fun Purchase.toResponse(purchaseItems: List<PurchaseItem>): PurchaseResponse {
    return PurchaseResponse(
        id = this.uid,
        purchaseDate = this.purchaseDate,
        purchaseNumber = this.purchaseNumber,
        supplierId = this.supplierId,
        supplierName = this.supplierName,
        supplierPhone = this.supplierPhone,
        supplierGst = this.supplierGst,
        supplierInvoiceNumber = this.supplierInvoiceNumber,
        purchaseType = this.purchaseType,
        placeOfSupply = this.placeOfSupply,
        totalCost = this.totalCost,
        basePrice = this.basePrice,
        totalTax = this.totalTax,
        status = this.status,
        totalItems = this.totalItems,
        totalQuantity = this.totalQuantity,
        billingAddress = this.billingAddress,
        shippingAddress = this.shippingAddress,
        taxInfos = this.taxInfos,
        purchaseItems = purchaseItems.toResponse(),
        discount = this.discount,
        priceMode = this.priceMode,
        overallDiscountMode = this.overallDiscountMode
    )
}
