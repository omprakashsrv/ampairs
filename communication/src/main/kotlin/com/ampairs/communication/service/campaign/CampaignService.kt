package com.ampairs.communication.service.campaign

import com.ampairs.communication.domain.dto.CampaignRequest
import com.ampairs.communication.domain.dto.CampaignResponse
import com.ampairs.communication.domain.dto.applyRequest
import com.ampairs.communication.domain.enums.DeliveryStatus
import com.ampairs.communication.domain.model.Campaign
import com.ampairs.communication.repository.CampaignRepository
import com.ampairs.communication.repository.CommunicationLogRepository
import com.ampairs.communication.repository.CommunicationRequestRepository
import com.ampairs.communication.service.CommunicationConfigService
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.sync.EntityChangePublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Campaign lifecycle + `/sync` authoring + rollup. Start defers into SCHEDULED when inside quiet
 * hours (FR-025); otherwise transitions to RUNNING and hands off to [CampaignRunner].
 */
@Service
class CampaignService(
    private val campaignRepository: CampaignRepository,
    private val requestRepository: CommunicationRequestRepository,
    private val logRepository: CommunicationLogRepository,
    private val configService: CommunicationConfigService,
    private val quietHours: QuietHours,
    // Lazy to break the CampaignService ↔ CampaignRunner construction cycle.
    private val campaignRunner: org.springframework.beans.factory.ObjectProvider<CampaignRunner>,
    private val entityChangePublisher: EntityChangePublisher,
) {
    private val logger = LoggerFactory.getLogger(CampaignService::class.java)

    @Transactional(readOnly = true)
    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<CampaignResponse> {
        val page: Page<Campaign> = if (lastSync.isNullOrBlank()) {
            campaignRepository.findAllForSync(pageable)
        } else {
            try {
                campaignRepository.findByUpdatedAtAfter(
                    Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8)), pageable
                )
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', full feed", lastSync, e)
                campaignRepository.findAllForSync(pageable)
            }
        }
        return page.map { it.toResponse() }
    }

    @Transactional
    fun bulkUpsert(requests: List<CampaignRequest>): List<CampaignResponse> = requests.map { req ->
        val campaign = (campaignRepository.findByUid(req.uid) ?: Campaign()).applyRequest(req)
        campaignRepository.save(campaign)
            .also { entityChangePublisher.updated("campaign", it.uid) }
            .toResponse()
    }

    @Transactional
    fun start(uid: String): CampaignResponse {
        val campaign = require(uid)
        check(campaign.status in setOf("DRAFT", "SCHEDULED")) { "Campaign $uid is ${campaign.status}, cannot start" }
        val cfg = configService.getOrCreate()
        val tz = "UTC" // campaign quiet hours evaluated in workspace config tz (UTC default; see config)
        if (quietHours.isWithin(Instant.now(), cfg.quietHoursStart, cfg.quietHoursEnd, tz)) {
            campaign.status = "SCHEDULED"
            campaign.scheduledAt = quietHours.nextEnd(Instant.now(), cfg.quietHoursStart, cfg.quietHoursEnd, tz)
            return campaignRepository.save(campaign).toResponse()
        }
        campaign.status = "RUNNING"
        campaign.startedAt = Instant.now()
        val saved = campaignRepository.save(campaign)
        campaignRunner.ifAvailable?.run(saved.uid, TenantContextHolder.getCurrentTenant() ?: saved.ownerId)
        return saved.toResponse()
    }

    @Transactional
    fun pause(uid: String): CampaignResponse {
        val campaign = require(uid)
        if (campaign.status == "RUNNING") campaign.status = "PAUSED"
        return campaignRepository.save(campaign).toResponse()
    }

    @Transactional
    fun resume(uid: String): CampaignResponse {
        val campaign = require(uid)
        if (campaign.status == "PAUSED") {
            campaign.status = "RUNNING"
            val saved = campaignRepository.save(campaign)
            campaignRunner.ifAvailable?.run(saved.uid, TenantContextHolder.getCurrentTenant() ?: saved.ownerId)
            return saved.toResponse()
        }
        return campaign.toResponse()
    }

    // ---- helpers used by the runner (each its own short transaction) ----

    @Transactional(readOnly = true)
    fun statusOf(uid: String): String? = campaignRepository.findByUid(uid)?.status

    @Transactional
    fun setTargeted(uid: String, count: Int) {
        campaignRepository.findByUid(uid)?.let { it.targetedCount = count; campaignRepository.save(it) }
    }

    @Transactional
    fun markDone(uid: String) {
        campaignRepository.findByUid(uid)?.let {
            if (it.status == "RUNNING") { it.status = "DONE"; it.completedAt = Instant.now(); campaignRepository.save(it) }
        }
    }

    private fun require(uid: String): Campaign =
        campaignRepository.findByUid(uid) ?: throw IllegalArgumentException("No campaign '$uid'")

    private fun Campaign.toResponse(): CampaignResponse {
        val requestUids = requestRepository.findBySourceRef(uid).map { it.uid }
        val logs = if (requestUids.isEmpty()) emptyList() else logRepository.findByRequestUidIn(requestUids)
        val skipped = logs.count { it.status == DeliveryStatus.SKIPPED.name }.toLong()
        val failed = logs.count { it.status == DeliveryStatus.FAILED.name || it.status == DeliveryStatus.EXHAUSTED.name }.toLong()
        val delivered = logs.count { it.status == DeliveryStatus.DELIVERED.name || it.status == DeliveryStatus.READ.name }.toLong()
        val sent = logs.size.toLong() - skipped - failed // everything that went out or is in flight
        return CampaignResponse(
            uid = uid, name = name, templateUid = templateUid, channel = channel,
            audienceType = audienceType, audienceRef = audienceRef, status = status,
            scheduledAt = scheduledAt?.toString(), throttlePerMinute = throttlePerMinute,
            startedAt = startedAt?.toString(), completedAt = completedAt?.toString(),
            targetedCount = targetedCount, sentCount = sent, deliveredCount = delivered,
            failedCount = failed, skippedCount = skipped, active = active, updatedAt = updatedAt?.toString(),
        )
    }

    @Transactional(readOnly = true)
    fun response(uid: String): CampaignResponse = require(uid).toResponse()
}
