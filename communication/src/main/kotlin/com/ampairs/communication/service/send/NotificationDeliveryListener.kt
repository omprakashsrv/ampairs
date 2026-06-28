package com.ampairs.communication.service.send

import com.ampairs.communication.domain.enums.Channel
import com.ampairs.communication.domain.enums.DeliveryStatus
import com.ampairs.communication.domain.enums.SuppressionReason
import com.ampairs.communication.domain.model.CommunicationUsage
import com.ampairs.communication.repository.CommunicationLogRepository
import com.ampairs.communication.repository.CommunicationUsageRepository
import com.ampairs.communication.service.consent.SuppressionService
import com.ampairs.notification.event.NotificationDeliveryUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Applies transport delivery-status feedback to the originating [com.ampairs.communication.domain.model.CommunicationLog]
 * and writes the append-only usage ledger row on the first SENT/DELIVERED. Status is MONOTONIC —
 * out-of-order/late events never regress a more-progressed status (FR-010).
 *
 * Runs synchronously in the transport's tenant context so @TenantId-filtered lookups resolve to the
 * correct workspace.
 */
@Component
class NotificationDeliveryListener(
    private val logRepository: CommunicationLogRepository,
    private val usageRepository: CommunicationUsageRepository,
    private val suppressionService: SuppressionService,
) {
    private val logger = LoggerFactory.getLogger(NotificationDeliveryListener::class.java)

    @EventListener
    @Transactional
    fun onDeliveryUpdated(event: NotificationDeliveryUpdatedEvent) {
        if (event.sourceModule != "communication") return
        val log = logRepository.findByUid(event.sourceRef) ?: run {
            logger.debug("No communication_log for sourceRef={}", event.sourceRef)
            return
        }

        // Hard bounce / complaint → suppress the address for all future sends (FR-031). Independent of
        // the monotonic status guard below, since a bounce can arrive after a "sent" status.
        if (event.suppress) {
            val ch = runCatching { Channel.valueOf(log.channel) }.getOrNull()
            val reason = runCatching { SuppressionReason.valueOf(event.suppressionReason ?: "HARD_BOUNCE") }
                .getOrDefault(SuppressionReason.HARD_BOUNCE)
            if (ch != null && log.recipientAddress.isNotBlank()) {
                suppressionService.suppress(ch, log.recipientAddress, reason)
            }
        }

        val newStatus = runCatching { DeliveryStatus.valueOf(event.status.uppercase()) }.getOrNull() ?: return
        if (rank(newStatus) <= rank(DeliveryStatus.valueOf(log.status))) {
            return // monotonic: ignore same/less-progressed updates
        }

        log.status = newStatus.name
        event.providerMessageId?.let { log.providerMessageId = it }
        event.error?.let { log.errorMessage = it }
        event.credentialUid?.let { log.credentialUid = it }
        event.providerAccountRef?.let { log.providerAccountRef = it }
        event.billingMode?.let { log.billingMode = it }
        val now = Instant.now()
        if (newStatus == DeliveryStatus.SENT && log.sentAt == null) log.sentAt = now
        if (newStatus == DeliveryStatus.DELIVERED && log.deliveredAt == null) log.deliveredAt = now
        logRepository.save(log)

        // Append exactly one usage row when the message first goes out.
        if ((newStatus == DeliveryStatus.SENT || newStatus == DeliveryStatus.DELIVERED) &&
            usageRepository.findByCommunicationLogUid(log.uid) == null
        ) {
            usageRepository.save(CommunicationUsage().apply {
                communicationLogUid = log.uid
                channel = log.channel
                credentialUid = log.credentialUid
                providerAccountRef = log.providerAccountRef
                billingMode = log.billingMode ?: "PLATFORM"
                providerMessageId = log.providerMessageId
                costUnits = event.costUnits ?: 1
                costCategory = event.costCategory
                occurredAt = now
            })
        }
    }

    private fun rank(status: DeliveryStatus): Int = when (status) {
        DeliveryStatus.QUEUED -> 0
        DeliveryStatus.SKIPPED -> 1
        DeliveryStatus.FAILED -> 1
        DeliveryStatus.SENT -> 2
        DeliveryStatus.DELIVERED -> 3
        DeliveryStatus.READ -> 4
        DeliveryStatus.EXHAUSTED -> 2
    }
}
