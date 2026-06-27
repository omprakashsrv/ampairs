package com.ampairs.report.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.report.domain.dto.ExportTemplateRequest
import com.ampairs.report.domain.dto.ExportTemplateResponse
import com.ampairs.report.service.ExportTemplateService
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
 * Canonical offline-sync contract for saved export/report templates.
 * Mirrors every other syncable resource (see docs/guides/offline-sync-contract.md):
 * `GET /sync` pulls (incl. soft-deleted) and `POST /sync` UID-keyed bulk-upserts.
 */
@RestController
@RequestMapping("/report/v1/templates")
@Validated
class ExportTemplateController(
    private val exportTemplateService: ExportTemplateService,
) {

    @GetMapping("/sync")
    fun getTemplatesSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<ExportTemplateResponse>> {
        val jpaPropertyName = when (sortBy) {
            "name" -> "name"
            "moduleKey" -> "moduleKey"
            "createdAt" -> "createdAt"
            "updatedAt" -> "updatedAt"
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaPropertyName))
        return ApiResponse.success(PageResponse.from(exportTemplateService.getTemplatesAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun bulkUpsertTemplates(
        @RequestBody requests: List<@Valid ExportTemplateRequest>,
    ): ApiResponse<List<ExportTemplateResponse>> {
        return ApiResponse.success(exportTemplateService.bulkUpsert(requests))
    }
}
