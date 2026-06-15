package com.ampairs.connector.controller

import com.ampairs.connector.domain.dto.InstallationResponse
import com.ampairs.connector.domain.dto.asResponse
import com.ampairs.connector.service.ConnectorInstallationService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Metadata pull for client mirroring: connector installations changed since `last_sync` (includes
 * uninstalled rows so removals propagate). Config/mappings/checkpoints are fetched per installation.
 */
@RestController
@RequestMapping("/connector/v1/sync")
class ConnectorSyncController(
    private val installationService: ConnectorInstallationService,
) {
    @GetMapping
    fun pull(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<InstallationResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy))
        val installations = installationService.syncInstallations(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(installations) { it.asResponse() })
    }
}
