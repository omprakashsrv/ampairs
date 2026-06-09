package com.ampairs.setting.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.setting.domain.dto.SettingDefinitionResponse
import com.ampairs.setting.domain.dto.SettingRequest
import com.ampairs.setting.domain.dto.SettingResponse
import com.ampairs.setting.domain.dto.asDefinitionResponses
import com.ampairs.setting.domain.dto.asSettingResponse
import com.ampairs.setting.domain.dto.asSettingResponses
import com.ampairs.setting.service.SettingService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Workspace settings expose only the two bulk-sync endpoints — there is no per-record CRUD; the
 * mobile/web clients are offline-first and reconcile through [com.ampairs.setting.service.SettingService].
 * Tenant context is set by `SessionUserFilter` from the `X-Workspace-ID` header.
 */
@RestController
@RequestMapping("/setting/v1/settings")
@Validated
class SettingController(
    private val settingService: SettingService,
) {

    /** Incremental pull. `last_sync` is an ISO-8601 instant; omit it for a full snapshot. */
    @GetMapping
    fun pull(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<SettingResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), "updatedAt"))
        val settings = settingService.getSettingsAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(settings) { it.asSettingResponse() })
    }

    /** Bulk push: upsert (and soft-delete via `active = false`) a batch of setting overrides. */
    @PostMapping
    fun push(@RequestBody @Valid requests: List<@Valid SettingRequest>): ApiResponse<List<SettingResponse>> {
        return ApiResponse.success(settingService.upsertSettings(requests).asSettingResponses())
    }

    /** Setting definition catalog for the current workspace's installed modules (read-only). */
    @GetMapping("/definitions")
    fun definitions(): ApiResponse<List<SettingDefinitionResponse>> {
        return ApiResponse.success(settingService.listDefinitions().asDefinitionResponses())
    }
}
