package com.ampairs.printing.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.printing.domain.dto.PrintTemplateRequest
import com.ampairs.printing.domain.dto.PrintTemplateResponse
import com.ampairs.printing.service.PrintTemplateService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/**
 * Canonical offline-sync contract for print templates (see docs/guides/offline-sync-contract.md):
 * GET + POST `/printing/v1/templates/sync`. Mirrors the unit/customer sync controllers.
 * Tenant context is set by SessionUserFilter from the X-Workspace-ID header.
 */
@RestController
@RequestMapping("/printing/v1/templates")
@Validated
class PrintTemplateController(
    private val printTemplateService: PrintTemplateService,
) {

    /**
     * Incremental sync feed: templates updated at/after last_sync, INCLUDING inactive (soft-deleted)
     * rows, ordered by updatedAt ASC, paginated — lets clients do batched pulls and detect deletions.
     */
    @GetMapping("/sync")
    fun getTemplatesSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<PrintTemplateResponse>> {
        val jpaPropertyName = when (sortBy) {
            "name" -> "name"
            "documentType" -> "documentType"
            "printerClass" -> "printerClass"
            "active" -> "active"
            "createdAt" -> "createdAt"
            "updatedAt" -> "updatedAt"
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaPropertyName))
        return ApiResponse.success(PageResponse.from(printTemplateService.getTemplatesAfterSync(lastSync, pageable)))
    }

    /**
     * Bulk upsert templates keyed by uid (create if absent, update if present). Soft-deleted rows
     * (active = false) are accepted in-band so deletions propagate without a per-row DELETE.
     */
    @PostMapping("/sync")
    fun bulkUpsertTemplates(
        @RequestBody requests: List<@Valid PrintTemplateRequest>,
    ): ApiResponse<List<PrintTemplateResponse>> {
        return ApiResponse.success(printTemplateService.bulkUpsert(requests))
    }
}
