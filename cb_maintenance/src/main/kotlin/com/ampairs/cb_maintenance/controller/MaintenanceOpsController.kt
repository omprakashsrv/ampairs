package com.ampairs.cb_maintenance.controller

import com.ampairs.cb_maintenance.config.Constants
import com.ampairs.cb_maintenance.service.EscalationService
import com.ampairs.cb_maintenance.service.PmEntryService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.setting.service.SettingService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * On-demand ops endpoints for the CURRENT workspace (tenant set by SessionUserFilter). Same work the
 * nightly scheduled driver runs across all enabled workspaces — exposed here for manual runs + tests.
 */
@RestController
@RequestMapping("/cb_maintenance/v1/ops")
@Validated
class MaintenanceOpsController(
    private val pmEntryService: PmEntryService,
    private val escalationService: EscalationService,
    private val settingService: SettingService,
) {

    /** Roll preventive-maintenance entries for the current workspace. Returns the count created. */
    @PostMapping("/generate-pm")
    fun generatePm(
        @RequestParam("window_days", required = false) windowDays: Int?,
    ): ApiResponse<Int> {
        val window = windowDays ?: runCatching {
            settingService.getString(Constants.SETTING_MODULE, Constants.KEY_PM_GENERATION_WINDOW_DAYS)?.toIntOrNull()
        }.getOrNull() ?: Constants.DEFAULT_PM_GENERATION_WINDOW_DAYS
        return ApiResponse.success(pmEntryService.generateDueEntries(window))
    }

    /** Escalate overdue PMs for the current workspace. Returns the count of notifications queued. */
    @PostMapping("/escalate-overdue")
    fun escalateOverdue(): ApiResponse<Int> =
        ApiResponse.success(escalationService.escalateOverdue())
}
