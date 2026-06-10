package com.ampairs.form.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.form.domain.dto.FormSchemaRequest
import com.ampairs.form.domain.dto.FormSchemaResponse
import com.ampairs.form.domain.service.FormConfigService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Form configuration API. Base path `/form/v1/config`.
 *
 * The form schema is a DDD aggregate and the aggregate is the sync unit, so there is exactly ONE
 * sync feed (one GET + one POST) — uid-keyed by entityType. Tenant context is established upstream
 * by the session filter (`@TenantId` auto-filters every query).
 */
@RestController
@RequestMapping("/form/v1/config")
class ConfigController(
    private val configService: FormConfigService,
) {

    /** Read-only aggregate fetch for the UI; seeds + merges registry defaults on first access. */
    @GetMapping("/schema")
    fun getConfigSchema(
        @RequestParam("entity_type") entityType: String,
    ): ApiResponse<FormSchemaResponse> = ApiResponse.success(configService.getConfigSchema(entityType))

    /** Aggregate sync pull — one FormSchema record per entityType, changed since last_sync. */
    @GetMapping("/schema/sync")
    fun syncPull(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<FormSchemaResponse>> =
        ApiResponse.success(configService.getAfterSync(lastSync, page, size))

    /** Aggregate sync push — replace each aggregate (upsert present, delete absent) w/ optimistic version. */
    @PostMapping("/schema/sync")
    fun syncPush(
        @RequestBody requests: List<FormSchemaRequest>,
    ): ApiResponse<List<FormSchemaResponse>> = ApiResponse.success(configService.replaceAggregates(requests))
}
