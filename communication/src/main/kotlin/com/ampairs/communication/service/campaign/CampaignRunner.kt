package com.ampairs.communication.service.campaign

import com.ampairs.communication.domain.enums.Channel
import com.ampairs.communication.domain.enums.MessageCategory
import com.ampairs.communication.domain.enums.SkipReason
import com.ampairs.communication.domain.enums.TriggerType
import com.ampairs.communication.port.CustomerAudiencePort
import com.ampairs.communication.repository.CampaignRepository
import com.ampairs.communication.service.CommunicationConfigService
import com.ampairs.communication.service.send.CommunicationDispatchService
import com.ampairs.communication.service.template.TemplateService
import com.ampairs.core.multitenancy.TenantContextHolder
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Executes a RUNNING campaign in the background: resolves the audience, gates each recipient on
 * promotional consent (opt-out → SKIPPED:OPTED_OUT, FR-024), dispatches the rest paced to the
 * throttle (FR-026), and stops early if the campaign is paused (FR-027). One request+log per
 * recipient (source_ref = campaign.uid) feeds the rollup. Suppression + quiet-hours/no-address are
 * enforced inside the dispatch engine.
 */
@Component
class CampaignRunner(
    private val campaignRepository: CampaignRepository,
    private val templateService: TemplateService,
    private val audiencePort: CustomerAudiencePort,
    private val dispatchService: CommunicationDispatchService,
    private val consentGate: ConsentGate,
    private val configService: CommunicationConfigService,
    private val campaignService: CampaignService,
) {
    private val logger = LoggerFactory.getLogger(CampaignRunner::class.java)
    private val objectMapper = ObjectMapper()

    @Async
    fun run(campaignUid: String, ownerId: String) {
        val prior = TenantContextHolder.getCurrentTenant()
        TenantContextHolder.setCurrentTenant(ownerId)
        try {
            execute(campaignUid)
        } catch (e: Exception) {
            logger.error("Campaign {} run failed: {}", campaignUid, e.message, e)
        } finally {
            if (prior != null) TenantContextHolder.setCurrentTenant(prior) else TenantContextHolder.clearTenantContext()
        }
    }

    private fun execute(campaignUid: String) {
        val campaign = campaignRepository.findByUid(campaignUid) ?: return
        if (campaign.status != "RUNNING") return
        val channel = runCatching { Channel.valueOf(campaign.channel.uppercase()) }.getOrNull() ?: run {
            logger.warn("Campaign {} has invalid channel {}", campaignUid, campaign.channel); return
        }
        val (template, variants) = templateService.findByUid(campaign.templateUid) ?: run {
            logger.warn("Campaign {} references missing template {}", campaignUid, campaign.templateUid); return
        }
        val recipients = audiencePort.resolve(campaign.audienceType, campaign.audienceRef, explicit = emptyList())
        campaignService.setTargeted(campaignUid, recipients.size)

        val throttle = campaign.throttlePerMinute ?: configService.getOrCreate().defaultThrottlePerMinute
        val delayMs = if (throttle > 0) 60_000L / throttle else 0L
        val variables = parseVars(campaign.variablesJson)

        for (recipient in recipients) {
            if (campaignService.statusOf(campaignUid) == "PAUSED") {
                logger.info("Campaign {} paused mid-run; stopping", campaignUid); return
            }
            val dedup = "campaign:$campaignUid:${recipient.customerUid ?: recipient.addressFor(channel) ?: ""}"
            if (consentGate.isAllowed(recipient, channel)) {
                dispatchService.dispatch(
                    template = template, variants = variants, channels = listOf(channel),
                    recipients = listOf(recipient), variables = variables,
                    triggerType = TriggerType.CAMPAIGN, sourceRef = campaignUid, dedupKey = dedup,
                )
            } else {
                dispatchService.recordSkip(
                    templateUid = campaign.templateUid, channel = channel, recipient = recipient,
                    category = MessageCategory.PROMOTIONAL, sourceRef = campaignUid,
                    dedupKey = dedup, reason = SkipReason.OPTED_OUT,
                )
            }
            if (delayMs > 0) Thread.sleep(delayMs)
        }
        campaignService.markDone(campaignUid)
    }

    private fun parseVars(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            objectMapper.readValue(json, object : TypeReference<Map<String, String>>() {})
        }.getOrDefault(emptyMap())
    }
}
