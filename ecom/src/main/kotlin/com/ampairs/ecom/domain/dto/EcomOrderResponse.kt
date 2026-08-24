package com.ampairs.ecom.domain.dto

import com.ampairs.ecom.domain.enums.EcomLineItemStatus
import com.ampairs.ecom.domain.enums.EcomOrderStatus
import com.ampairs.ecom.domain.model.EcomOrder
import com.ampairs.ecom.domain.model.EcomOrderLineItem
import java.math.BigDecimal
import java.time.Instant

data class EcomOrderLineItemResponse(
    val uid: String,
    val listedProductId: String,
    val managementProductId: String,
    val productName: String,
    val unitPrice: BigDecimal,
    val quantityOrdered: Int,
    val quantityConfirmed: Int?,
    val lineTotal: BigDecimal,
    val status: EcomLineItemStatus,
)

data class EcomOrderResponse(
    val uid: String,
    val ecomOrderRef: String,
    /** Human-friendly order number (e.g. "ECO-00001") — what the buyer sees/quotes to track this order. */
    val orderNumber: String,
    val storefrontId: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String?,
    val deliveryAddress: Map<String, Any>,
    val status: EcomOrderStatus,
    val managementOrderRef: String?,
    val lineItems: List<EcomOrderLineItemResponse>,
    val subtotal: BigDecimal,
    val totalAmount: BigDecimal,
    val notes: String?,
    val placedAt: Instant,
    val confirmedAt: Instant?,
    /**
     * Spec 029 — finalized invoices raised for this order (order↔invoice link). Populated by the
     * buyer order-detail path; empty on the list view and when no invoice has been raised yet.
     */
    val invoices: List<BuyerInvoiceSummaryResponse> = emptyList(),
)

fun EcomOrderLineItem.asLineItemResponse() = EcomOrderLineItemResponse(
    uid = uid,
    listedProductId = listedProductId,
    managementProductId = managementProductId,
    productName = productName,
    unitPrice = unitPrice,
    quantityOrdered = quantityOrdered,
    quantityConfirmed = quantityConfirmed,
    lineTotal = lineTotal,
    status = status,
)

fun EcomOrder.asEcomOrderResponse() = EcomOrderResponse(
    uid = uid,
    ecomOrderRef = ecomOrderRef,
    // Defensive: rows created before V1.0.123/124 (or loaded from a stale replica) can still surface
    // as a real null here despite the non-null Kotlin type — Hibernate sets fields via reflection,
    // bypassing the null check the constructor would otherwise enforce.
    orderNumber = orderNumber.orEmpty(),
    storefrontId = storefrontId,
    customerName = customerName,
    customerEmail = customerEmail,
    customerPhone = customerPhone,
    deliveryAddress = deliveryAddress,
    status = status,
    managementOrderRef = managementOrderRef,
    lineItems = lineItems.map { it.asLineItemResponse() },
    subtotal = subtotal,
    totalAmount = totalAmount,
    notes = notes,
    placedAt = placedAt,
    confirmedAt = confirmedAt,
)
