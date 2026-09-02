package com.ampairs.cb_maintenance.scheduler

import com.ampairs.cb_maintenance.config.Constants
import com.ampairs.cb_maintenance.service.EscalationService
import com.ampairs.cb_maintenance.service.PmEntryService
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.setting.service.SettingService
import com.ampairs.workspace.repository.WorkspaceModuleRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Nightly PM generation + overdue escalation for California Burrito. A `@Scheduled` method runs with
 * NO ambient tenant, so this driver enumerates the workspaces where `cb-maintenance` is enabled and
 * sets `TenantContextHolder` per workspace itself (mirrors subscription's workspace-iterating
 * batches). The per-workspace work lives in the services; this is only the loop + tenant boundary.
 */
@Component
@ConditionalOnProperty(
    name = ["cb-maintenance.scheduled-jobs.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class CbMaintenanceScheduledJobs(
    private val workspaceModuleRepository: WorkspaceModuleRepository,
    private val pmEntryService: PmEntryService,
    private val escalationService: EscalationService,
    private val settingService: SettingService,
) {
    private val logger = LoggerFactory.getLogger(CbMaintenanceScheduledJobs::class.java)

    /** Roll preventive-maintenance entries forward. Nightly at 01:30 UTC. */
    @Scheduled(cron = "0 30 1 * * *")
    fun generatePreventiveMaintenance() {
        forEachEnabledWorkspace("PM generation") {
            val windowDays = runCatching {
                settingService.getString(Constants.SETTING_MODULE, Constants.KEY_PM_GENERATION_WINDOW_DAYS)
                    ?.toIntOrNull()
            }.getOrNull() ?: Constants.DEFAULT_PM_GENERATION_WINDOW_DAYS
            pmEntryService.generateDueEntries(windowDays)
        }
    }

    /** Notify managers about overdue PMs. Nightly at 02:00 UTC. */
    @Scheduled(cron = "0 0 2 * * *")
    fun escalateOverdue() {
        forEachEnabledWorkspace("overdue escalation") {
            escalationService.escalateOverdue()
        }
    }

    private fun forEachEnabledWorkspace(jobName: String, work: () -> Unit) {
        val installs = runCatching {
            workspaceModuleRepository.findByMasterModuleModuleCodeAndEnabledTrue(Constants.MODULE_CODE)
        }.getOrElse {
            logger.error("[{}] failed to enumerate enabled workspaces", jobName, it)
            return
        }
        val workspaceIds = installs.map { it.workspaceId }.filter { it.isNotBlank() }.distinct()
        logger.info("[{}] running for {} workspace(s)", jobName, workspaceIds.size)
        for (workspaceId in workspaceIds) {
            try {
                TenantContextHolder.withTenant(workspaceId) { work() }
            } catch (e: Exception) {
                logger.error("[{}] failed for workspace {}", jobName, workspaceId, e)
            }
        }
    }
}
