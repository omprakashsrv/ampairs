package com.ampairs.notification.event

/**
 * Published by the notification transport when a dispatched message reaches a new terminal/progressed
 * delivery status. Carries the credential attribution so the originating module (e.g. communication)
 * can update its delivery log and write its usage/billing ledger.
 *
 * Consumers correlate via (sourceModule, sourceRef). Status values mirror the transport's lifecycle
 * (SENT/DELIVERED/READ/FAILED/EXHAUSTED) and must be treated as monotonic by the consumer.
 */
data class NotificationDeliveryUpdatedEvent(
    val sourceModule: String,
    val sourceRef: String,
    val status: String,
    val providerMessageId: String? = null,
    val error: String? = null,
    val credentialUid: String? = null,
    val providerAccountRef: String? = null,
    val billingMode: String? = null,
    val costUnits: Int? = null,
    val costCategory: String? = null,
    /** Set by provider webhooks on a hard bounce/complaint so the source module suppresses the address. */
    val suppress: Boolean = false,
    val suppressionReason: String? = null,
)
