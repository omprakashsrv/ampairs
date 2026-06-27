package com.ampairs.notification.service

import com.ampairs.notification.provider.NotificationChannel

/**
 * Public structured-enqueue API used by the communication module (and any other orchestrator) to
 * hand a fully-rendered message to the notification transport. The transport owns the queue, retry,
 * provider selection, per-workspace credential resolution, and delivery-status feedback.
 *
 * Returns the `notification_queue` uid so the caller can correlate delivery-status events
 * (see [com.ampairs.notification.event.NotificationDeliveryUpdatedEvent]) back to its own record.
 */
interface NotificationDispatchService {
    fun enqueue(request: DispatchRequest): String
}

/**
 * A single rendered message destined for one recipient on one channel. Content is already rendered
 * by the caller (subject/body/textBody); the transport does not template.
 */
data class DispatchRequest(
    val channel: NotificationChannel,
    val recipient: String,
    val subject: String? = null,
    val body: String,
    val textBody: String? = null,
    val title: String? = null,
    val dataPayload: Map<String, String> = emptyMap(),
    val providerTemplateId: String? = null,
    val params: List<String> = emptyList(),
    val category: String = "TRANSACTIONAL",
    val sourceModule: String,
    val sourceRef: String,
)
