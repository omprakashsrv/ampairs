package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.workspace.model.dto.LlmModelResponse
import com.ampairs.workspace.service.WorkspaceLlmModelService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Pull-only catalog of on-device LLM models for the mobile assistant. Any workspace member can read
 * it; the catalog is global and server-seeded (no admin/setup UI). Mirrors the module catalog.
 */
@RestController
@RequestMapping("/workspace/v1/llm-models")
@SecurityRequirement(name = "BearerAuth")
@SecurityRequirement(name = "WorkspaceContext")
class WorkspaceLlmModelController(
    private val workspaceLlmModelService: WorkspaceLlmModelService,
) {

    @GetMapping("/catalog")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getCatalog(): ApiResponse<List<LlmModelResponse>> {
        return ApiResponse.success(workspaceLlmModelService.getCatalog())
    }
}
