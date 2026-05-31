package com.ampairs.ecom.domain.dto

import com.ampairs.ecom.domain.enums.EcomLineItemStatus
import com.ampairs.ecom.domain.enums.EcomOrderStatus
import com.ampairs.ecom.domain.model.EcomOrder
import com.ampairs.ecom.domain.model.EcomOrderLineItem
import java.math.BigDecimal
import java.time.Instant

data class EcomOrderLineItemEditRequest(
    val uid: String,
    val quantityConfirmed: Int,
    val status: EcomLineItemStatus,
)

data class EcomOrderLineItemManagementResponse(
    val uid: String,
    val managementProductId: String,
    val productName: String,
    val unitPrice: BigDecimal,
    val quantityOrdered: Int,
    val quantityConfirmed: Int?,
    val lineTotal: BigDecimal,
    val status: EcomLineItemStatus,
)

data class EcomOrderManagementResponse(
    val uid: String,
    val ecomOrderRef: String,
    val status: EcomOrderStatus,
    val storefrontId: String,
    val workspaceId: String,
    val customerId: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String?,
    val deliveryAddress: Map<String, Any?>,
    val lineItems: List<EcomOrderLineItemManagementResponse>,
    val subtotal: BigDecimal,
    val totalAmount: BigDecimal,
    val notes: String?,
    val placedAt: Instant,
    val confirmedAt: Instant?,
    val merchantReviewedAt: Instant?,
    val managementOrderRef: String?,
)

fun EcomOrderLineItem.asLineItemManagementResponse() = EcomOrderLineItemManagementResponse(
    uid = uid,
    managementProductId = managementProductId,
    productName = productName,
    unitPrice = unitPrice,
    quantityOrdered = quantityOrdered,
    quantityConfirmed = quantityConfirmed,
    lineTotal = lineTotal,
    status = status,
)

fun EcomOrder.asEcomOrderManagementResponse() = EcomOrderManagementResponse(
    uid = uid,
    ecomOrderRef = ecomOrderRef,
    status = status,
    storefrontId = storefrontId,
    workspaceId = workspaceId,
    customerId = customerId,
    customerName = customerName,
    customerEmail = customerEmail,
    customerPhone = customerPhone,
    deliveryAddress = deliveryAddress,
    lineItems = lineItems.map { it.asLineItemManagementResponse() },
    subtotal = subtotal,
    totalAmount = totalAmount,
    notes = notes,
    placedAt = placedAt,
    confirmedAt = confirmedAt,
    merchantReviewedAt = merchantReviewedAt,
    managementOrderRef = managementOrderRef,
)
