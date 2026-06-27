package com.ampairs.communication.service.campaign

import com.ampairs.communication.repository.CampaignRepository
import com.ampairs.core.multitenancy.TenantContextHolder
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Starts SCHEDULED campaigns whose time has arrived (deferred-by-quiet-hours or explicitly scheduled).
 * `start()` re-checks quiet hours, so a campaign deferred again simply re-schedules.
 */
@Component
class CampaignSweeper(
    private val campaignRepository: CampaignRepository,
    private val campaignService: CampaignService,
) {
    private val logger = LoggerFactory.getLogger(CampaignSweeper::class.java)

    @Scheduled(
        fixedDelayString = "\${communication.scheduler.tick-seconds:60}000",
        initialDelayString = "\${communication.scheduler.tick-seconds:60}000",
    )
    fun sweep() {
        val due = runCatching { campaignRepository.findDueScheduled(Instant.now()) }.getOrElse {
            logger.warn("Campaign sweep query failed: {}", it.message); return
        }
        if (due.isEmpty()) return
        logger.info("Campaign sweep: {} due", due.size)
        due.forEach { campaign ->
            val prior = TenantContextHolder.getCurrentTenant()
            TenantContextHolder.setCurrentTenant(campaign.ownerId)
            try {
                campaignService.start(campaign.uid)
            } catch (e: Exception) {
                logger.error("Failed to start scheduled campaign {}: {}", campaign.uid, e.message)
            } finally {
                if (prior != null) TenantContextHolder.setCurrentTenant(prior) else TenantContextHolder.clearTenantContext()
            }
        }
    }
}
