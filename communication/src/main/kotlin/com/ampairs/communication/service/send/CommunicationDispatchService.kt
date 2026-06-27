package com.ampairs.communication.service.send

import com.ampairs.communication.config.Constants
import com.ampairs.communication.domain.enums.Channel
import com.ampairs.communication.domain.enums.DeliveryStatus
import com.ampairs.communication.domain.enums.MessageCategory
import com.ampairs.communication.domain.enums.SkipReason
import com.ampairs.communication.domain.enums.TriggerType
import com.ampairs.communication.domain.model.CommunicationLog
import com.ampairs.communication.domain.model.CommunicationRequest
import com.ampairs.communication.domain.model.MessageTemplate
import com.ampairs.communication.domain.model.TemplateVariant
import com.ampairs.communication.port.Recipient
import com.ampairs.communication.repository.CommunicationLogRepository
import com.ampairs.communication.repository.CommunicationRequestRepository
import com.ampairs.communication.service.CommunicationConfigService
import com.ampairs.communication.service.template.TemplateRenderer
import com.ampairs.communication.service.template.TemplateRenderException
import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.service.DispatchRequest
import com.ampairs.notification.service.NotificationDispatchService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The send engine: renders the right variant per recipient/channel and hands each message to the
 * notification transport. Per-recipient/channel outcomes are recorded as independent
 * [CommunicationLog] rows (FR-011); missing addresses/variants are SKIPPED, not failures (FR-014).
 *
 * Credential attribution + the usage ledger row are filled in later from the transport's
 * delivery-status event (see NotificationDeliveryListener) — not here.
 */
@Service
class CommunicationDispatchService(
    private val requestRepository: CommunicationRequestRepository,
    private val logRepository: CommunicationLogRepository,
    private val renderer: TemplateRenderer,
    private val configService: CommunicationConfigService,
    private val dispatchProvider: ObjectProvider<NotificationDispatchService>,
) {
    private val logger = LoggerFactory.getLogger(CommunicationDispatchService::class.java)
    private val objectMapper: ObjectMapper = ObjectMapper()

    @Transactional
    fun dispatch(
        template: MessageTemplate,
        variants: List<TemplateVariant>,
        channels: List<Channel>,
        recipients: List<Recipient>,
        variables: Map<String, String>,
        triggerType: TriggerType,
        sourceRef: String?,
        dedupKey: String?,
    ): CommunicationRequest {
        // Idempotency: a duplicate trigger with the same dedup key returns the original request.
        dedupKey?.let { key ->
            requestRepository.findByDedupKey(key)?.let {
                logger.info("Duplicate dispatch suppressed for dedup_key={} (request {})", key, it.uid)
                return it
            }
        }

        val channelsCsv = channels.joinToString(",") { it.name }
        val request = CommunicationRequest().apply {
            templateUid = template.uid
            this.triggerType = triggerType.name
            this.sourceRef = sourceRef
            this.channels = channelsCsv
            audienceType = "LIST"
            variablesJson = runCatching { objectMapper.writeValueAsString(variables) }.getOrNull()
            this.dedupKey = dedupKey
            status = DeliveryStatus.QUEUED.name
        }
        val savedRequest = requestRepository.save(request)

        val category = runCatching { MessageCategory.valueOf(template.category.uppercase()) }
            .getOrDefault(MessageCategory.TRANSACTIONAL)
        val footer = if (category == MessageCategory.PROMOTIONAL) configService.getOrCreate().promotionalFooterHtml else null

        for (recipient in recipients) {
            for (channel in channels) {
                dispatchOne(savedRequest.uid, template, variants, channel, recipient, variables, category, footer)
            }
        }
        return savedRequest
    }

    private fun dispatchOne(
        requestUid: String,
        template: MessageTemplate,
        variants: List<TemplateVariant>,
        channel: Channel,
        recipient: Recipient,
        variables: Map<String, String>,
        category: MessageCategory,
        footerHtml: String?,
    ) {
        val locale = recipient.locale ?: template.defaultLocale
        val variant = selectVariant(variants, channel, locale, template.defaultLocale)
        if (variant == null) {
            saveSkipped(requestUid, recipient, channel, category, SkipReason.NO_VARIANT)
            return
        }
        val address = recipient.addressFor(channel)
        if (address == null) {
            saveSkipped(requestUid, recipient, channel, category, SkipReason.NO_ADDRESS)
            return
        }

        val log = CommunicationLog().apply {
            this.requestUid = requestUid
            customerUid = recipient.customerUid
            this.channel = channel.name
            recipientAddress = address
            this.category = category.name
            status = DeliveryStatus.QUEUED.name
        }

        try {
            val subject = variant.subject?.let { renderer.renderRequired(it, variables) }
            val (body, textBody) = renderBodies(channel, variant, variables, category, footerHtml)
            val saved = logRepository.save(log)

            val queueUid = dispatchProvider.ifAvailable?.enqueue(
                DispatchRequest(
                    channel = toNotificationChannel(channel),
                    recipient = address,
                    subject = subject,
                    body = body,
                    textBody = textBody,
                    providerTemplateId = variant.providerTemplateId,
                    params = parseParams(variant.providerParamsJson),
                    category = category.name,
                    sourceModule = Constants.SOURCE_MODULE,
                    sourceRef = saved.uid,
                )
            )
            if (queueUid != null) {
                saved.notificationUid = queueUid
                logRepository.save(saved)
            } else {
                logger.warn("No NotificationDispatchService available; log {} left QUEUED", saved.uid)
            }
        } catch (e: TemplateRenderException) {
            log.status = DeliveryStatus.FAILED.name
            log.errorMessage = e.message
            logRepository.save(log)
            logger.warn("Render failed for request {} channel {}: {}", requestUid, channel, e.message)
        }
    }

    /** EMAIL → (html, plain-text alt); other channels → (text, null). */
    private fun renderBodies(
        channel: Channel,
        variant: TemplateVariant,
        variables: Map<String, String>,
        category: MessageCategory,
        footerHtml: String?,
    ): Pair<String, String?> {
        return if (channel == Channel.EMAIL) {
            var html = renderer.renderRequired(variant.htmlBody, variables)
            if (category == MessageCategory.PROMOTIONAL && !footerHtml.isNullOrBlank()) {
                html += renderer.render(footerHtml, variables).output
            }
            val text = if (!variant.textBody.isNullOrBlank()) renderer.renderRequired(variant.textBody, variables)
            else renderer.htmlToPlainText(html)
            html to text
        } else {
            renderer.renderRequired(variant.textBody, variables) to null
        }
    }

    private fun saveSkipped(
        requestUid: String,
        recipient: Recipient,
        channel: Channel,
        category: MessageCategory,
        reason: SkipReason,
    ) {
        logRepository.save(CommunicationLog().apply {
            this.requestUid = requestUid
            customerUid = recipient.customerUid
            this.channel = channel.name
            recipientAddress = recipient.addressFor(channel) ?: ""
            this.category = category.name
            status = DeliveryStatus.SKIPPED.name
            skipReason = reason.name
        })
    }

    private fun selectVariant(
        variants: List<TemplateVariant>,
        channel: Channel,
        locale: String,
        defaultLocale: String,
    ): TemplateVariant? {
        val forChannel = variants.filter { it.active && it.channel.equals(channel.name, ignoreCase = true) }
        return forChannel.firstOrNull { it.locale.equals(locale, ignoreCase = true) }
            ?: forChannel.firstOrNull { it.locale.equals(defaultLocale, ignoreCase = true) }
            ?: forChannel.firstOrNull()
    }

    private fun parseParams(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { objectMapper.readValue(json, Array<String>::class.java).toList() }.getOrDefault(emptyList())
    }

    private fun toNotificationChannel(channel: Channel): NotificationChannel = when (channel) {
        Channel.EMAIL -> NotificationChannel.EMAIL
        Channel.SMS -> NotificationChannel.SMS
        Channel.WHATSAPP -> NotificationChannel.WHATSAPP
        Channel.PUSH -> NotificationChannel.PUSH_NOTIFICATION
    }
}
