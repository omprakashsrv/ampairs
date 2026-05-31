package com.ampairs.event.domain.kafka

import java.time.Instant

data class EcomOrderStatusEvent(
    val ecomOrderRef: String,
    val workspaceId: String,
    val newStatus: String,
    val managementOrderRef: String? = null,
    val confirmedLineItems: List<ConfirmedLineItemPayload>? = null,
    val updatedAt: Instant,
)

data class ConfirmedLineItemPayload(
    val managementProductId: String,
    val quantityConfirmed: Int,
    val status: String,
)
