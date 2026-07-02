package com.ampairs.event.domain.kafka

import com.ampairs.core.domain.model.Address
import java.math.BigDecimal
import java.time.Instant

data class EcomOrderPlacedEvent(
    val ecomOrderRef: String,
    val workspaceId: String,
    val storefrontId: String,
    val customerId: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String? = null,
    val deliveryAddress: Address,
    val lineItems: List<EcomOrderLineItemPayload>,
    val subtotal: BigDecimal,
    val totalAmount: BigDecimal,
    val placedAt: Instant,
    /** The CRM distributor account checkout already resolved (and confirmed linked) for this buyer. */
    val requestedCustomerId: String? = null,
)

data class EcomOrderLineItemPayload(
    val listedProductId: String,
    val managementProductId: String,
    val productName: String,
    val unitPrice: BigDecimal,
    val quantityOrdered: Int,
    val lineTotal: BigDecimal,
)
