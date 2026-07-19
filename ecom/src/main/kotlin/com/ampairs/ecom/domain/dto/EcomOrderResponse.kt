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
    orderNumber = orderNumber,
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
