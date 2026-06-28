package com.ampairs.notification.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.notification.credential.WorkspaceChannelCredentialResolver
import com.ampairs.notification.model.NotificationQueue
import com.ampairs.notification.provider.NotificationStatus
import com.ampairs.notification.repository.NotificationQueueRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Persists a [DispatchRequest] as a PENDING [NotificationQueue] row. The existing scheduled
 * processor picks it up and routes it to the channel provider. The structured columns
 * (subject/source_module/source_ref/credential/billing) let delivery-status feedback correlate
 * back to the originating module.
 */
@Service
class NotificationDispatchServiceImpl(
    private val queueRepository: NotificationQueueRepository,
    private val credentialResolver: WorkspaceChannelCredentialResolver,
) : NotificationDispatchService {

    private val logger = LoggerFactory.getLogger(NotificationDispatchServiceImpl::class.java)
    private val objectMapper = ObjectMapper()

    @Transactional
    override fun enqueue(request: DispatchRequest): String {
        val tenantId = TenantContextHolder.getCurrentTenant() ?: "default"

        // Resolve which credential this send uses. For client-owned-only channels (WhatsApp) with no
        // workspace credential this throws NoCredentialException — the caller records NO_CREDENTIAL and
        // never falls back to a platform sender (FR-037). Attribution is stamped on the row for billing.
        val attribution = credentialResolver.resolve(request.channel)

        // WhatsApp/SMS approved-template references travel in the data payload until a typed provider
        // consumes them (kept non-secret).
        val data = LinkedHashMap(request.dataPayload)
        request.providerTemplateId?.let { data["provider_template_id"] = it }
        if (request.params.isNotEmpty()) data["provider_params"] = objectMapper.writeValueAsString(request.params)

        val row = NotificationQueue().apply {
            this.recipient = request.recipient
            this.message = request.body
            this.subject = request.subject
            this.title = request.title
            this.dataPayload = if (data.isEmpty()) null else objectMapper.writeValueAsString(data)
            this.channel = request.channel
            this.status = NotificationStatus.PENDING
            this.scheduledAt = Instant.now()
            this.sourceModule = request.sourceModule
            this.sourceRef = request.sourceRef
            this.credentialUid = attribution.credentialUid
            this.billingMode = attribution.billingMode
            this.ownerId = tenantId
        }
        val saved = queueRepository.save(row)
        logger.info(
            "Dispatch enqueued: channel={} source={}:{} queueUid={} (tenant={})",
            request.channel, request.sourceModule, request.sourceRef, saved.uid, tenantId
        )
        return saved.uid
    }
}
