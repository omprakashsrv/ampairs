package com.ampairs.sequence.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.sequence.domain.dto.SequenceDefinitionRequest
import com.ampairs.sequence.domain.dto.SequenceDefinitionResponse
import com.ampairs.sequence.domain.dto.SequenceNextRequest
import com.ampairs.sequence.domain.dto.SequenceNumberResponse
import com.ampairs.sequence.domain.dto.SequencePreviewResponse
import com.ampairs.sequence.exception.SequenceDefinitionNotFoundException
import com.ampairs.sequence.service.SequenceDefinitionService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/sequence/v1/definitions")
@Validated
class SequenceDefinitionController(
    private val definitionService: SequenceDefinitionService,
) {

    /**
     * Incremental sync feed (canonical contract): definitions updated at/after last_sync,
     * INCLUDING inactive rows, ordered by updatedAt ASC, paginated.
     */
    @GetMapping("/sync")
    fun getDefinitionsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<SequenceDefinitionResponse>> {
        val jpaPropertyName = when (sortBy) {
            "entityType" -> "entityType"
            "active" -> "active"
            "createdAt" -> "createdAt"
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaPropertyName))
        return ApiResponse.success(PageResponse.from(definitionService.getDefinitionsAfterSync(lastSync, pageable)))
    }

    /**
     * Bulk upsert keyed by uid (canonical contract). Deactivations ride in-band
     * (active = false); the server counter is authoritative.
     */
    @PostMapping("/sync")
    fun bulkUpsertDefinitions(
        @Valid @RequestBody requests: List<SequenceDefinitionRequest>,
    ): ApiResponse<List<SequenceDefinitionResponse>> {
        return ApiResponse.success(definitionService.bulkUpsert(requests))
    }

    @GetMapping
    fun listDefinitions(): ApiResponse<List<SequenceDefinitionResponse>> {
        return ApiResponse.success(definitionService.findAll())
    }

    @PostMapping
    fun createDefinition(
        @Valid @RequestBody request: SequenceDefinitionRequest,
    ): ApiResponse<SequenceDefinitionResponse> {
        return ApiResponse.success(definitionService.create(request))
    }

    @PutMapping("/{uid}")
    fun updateDefinition(
        @PathVariable uid: String,
        @Valid @RequestBody request: SequenceDefinitionRequest,
    ): ApiResponse<SequenceDefinitionResponse> {
        return ApiResponse.success(definitionService.update(uid, request))
    }

    @GetMapping("/{uid}")
    fun getDefinition(@PathVariable uid: String): ApiResponse<SequenceDefinitionResponse> {
        val definition = definitionService.findByUid(uid)
            ?: throw SequenceDefinitionNotFoundException("Sequence definition not found for uid: $uid")
        return ApiResponse.success(definition)
    }

    /** Preview the next number without advancing or reserving anything (FR-006). */
    @GetMapping("/{uid}/preview")
    fun previewNext(@PathVariable uid: String): ApiResponse<SequencePreviewResponse> {
        return ApiResponse.success(definitionService.preview(uid))
    }

    /**
     * Direct server-side generation (FR-015): resolves USER-scope for the caller, then
     * WORKSPACE-scope, then auto-provisions a default; atomically advances the counter.
     */
    @PostMapping("/next")
    fun generateNext(
        @Valid @RequestBody request: SequenceNextRequest,
    ): ApiResponse<SequenceNumberResponse> {
        return ApiResponse.success(definitionService.next(request.entityType, currentUserId()))
    }

    private fun currentUserId(): String? = runCatching {
        SecurityContextHolder.getContext().authentication?.let { AuthenticationHelper.getCurrentUserId(it) }
    }.getOrNull()
}
