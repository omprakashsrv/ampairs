package com.ampairs.communication.controller

import com.ampairs.communication.domain.dto.PreviewRequest
import com.ampairs.communication.domain.dto.PreviewResponse
import com.ampairs.communication.domain.dto.TemplateAggregateRequest
import com.ampairs.communication.domain.dto.TemplateAggregateResponse
import com.ampairs.communication.service.template.PreviewService
import com.ampairs.communication.service.template.TemplateService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/**
 * Aggregate-grained offline-sync for message templates plus a preview action.
 * See docs/guides/offline-sync-contract.md. Tenant set by SessionUserFilter (X-Workspace-ID).
 */
@RestController
@RequestMapping("/communication/v1/templates")
@Validated
class TemplateController(
    private val templateService: TemplateService,
    private val previewService: PreviewService,
) {

    @GetMapping("/sync")
    fun sync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<TemplateAggregateResponse>> {
        val property = if (sortBy in setOf("name", "code", "createdAt", "updatedAt")) sortBy else "updatedAt"
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), property))
        return ApiResponse.success(PageResponse.from(templateService.getTemplatesAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun push(
        @RequestBody requests: List<@Valid TemplateAggregateRequest>,
    ): ApiResponse<List<TemplateAggregateResponse>> =
        ApiResponse.success(templateService.bulkUpsert(requests))

    @PostMapping("/{code}/preview")
    fun preview(
        @PathVariable code: String,
        @RequestBody @Valid request: PreviewRequest,
    ): ApiResponse<PreviewResponse> =
        ApiResponse.success(previewService.preview(code, request.channel, request.locale, request.variables))
}
