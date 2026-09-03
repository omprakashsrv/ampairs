package com.ampairs.cb_maintenance.service

import com.ampairs.cb_employee.service.EmployeeService
import com.ampairs.cb_maintenance.config.Constants
import com.ampairs.cb_maintenance.domain.model.PmEntryStatus
import com.ampairs.cb_maintenance.repository.PmEntryRepository
import com.ampairs.notification.service.NotificationService
import com.ampairs.setting.service.SettingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

/**
 * Overdue escalation (module plan §4): PM entries past their due date by more than the
 * workspace-configured threshold get their assignee's escalation target notified via SMS. Operates
 * on the CURRENT workspace (tenant context set by the caller — controller or scheduled driver).
 */
@Service
class EscalationService(
    private val pmEntryRepository: PmEntryRepository,
    private val employeeService: EmployeeService,
    private val notificationService: NotificationService,
    private val settingService: SettingService,
) {
    private val logger = LoggerFactory.getLogger(EscalationService::class.java)

    private val escalatableStatuses = listOf(
        PmEntryStatus.DUE, PmEntryStatus.OVERDUE, PmEntryStatus.ASSIGNED, PmEntryStatus.IN_PROGRESS,
    )

    @Transactional
    fun escalateOverdue(): Int {
        val thresholdDays = runCatching {
            settingService.getString(Constants.SETTING_MODULE, Constants.KEY_ESCALATION_THRESHOLD_DAYS)
                ?.toIntOrNull()
        }.getOrNull() ?: Constants.DEFAULT_ESCALATION_THRESHOLD_DAYS

        val cutoff = Instant.now().minus(Duration.ofDays(thresholdDays.toLong()))
        val overdue = pmEntryRepository.findByStatusInAndDueDateBeforeAndActiveTrue(escalatableStatuses, cutoff)
        var notified = 0
        for (entry in overdue) {
            if (entry.status == PmEntryStatus.DUE || entry.status == PmEntryStatus.ASSIGNED) {
                entry.status = PmEntryStatus.OVERDUE
                pmEntryRepository.save(entry)
            }
            val assignee = entry.assignedToEmployeeId?.takeIf { it.isNotBlank() } ?: continue
            val target = runCatching { employeeService.resolveEscalationTarget(assignee) }.getOrNull() ?: continue
            val mobile = target.mobile?.takeIf { it.isNotBlank() } ?: continue
            runCatching {
                notificationService.queueSms(
                    mobile,
                    "Overdue PM at store ${entry.storeId} (${entry.assetCategory}). Please follow up.",
                )
                notified++
            }.onFailure { logger.warn("Failed to queue escalation SMS to {}", target.uid, it) }
        }
        if (overdue.isNotEmpty()) logger.info("Escalation scanned {} overdue PM entries, {} notified", overdue.size, notified)
        return notified
    }
}
