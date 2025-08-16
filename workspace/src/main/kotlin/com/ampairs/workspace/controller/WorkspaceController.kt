package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.user.model.User
import com.ampairs.workspace.model.dto.CreateWorkspaceRequest
import com.ampairs.workspace.model.dto.UpdateWorkspaceRequest
import com.ampairs.workspace.model.dto.WorkspaceListResponse
import com.ampairs.workspace.model.dto.WorkspaceResponse
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.model.enums.WorkspaceType
import com.ampairs.workspace.service.WorkspaceMemberService
import com.ampairs.workspace.service.WorkspaceService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * REST controller for workspace CRUD operations and management
 */
@RestController
@RequestMapping("/workspace/v1")
class WorkspaceController(
    private val workspaceService: WorkspaceService,
    private val memberService: WorkspaceMemberService,
) {

    /**
     * Create a new workspace
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createWorkspace(@RequestBody @Valid request: CreateWorkspaceRequest): ApiResponse<WorkspaceResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val workspace = workspaceService.createWorkspace(request, user.uid)
        return ApiResponse.success(workspace)
    }

    /**
     * Get workspace by ID
     */
    @GetMapping("/{workspaceId}")
    fun getWorkspace(@PathVariable workspaceId: String): ApiResponse<WorkspaceResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val workspace = workspaceService.getWorkspaceById(workspaceId, user.uid)
        return ApiResponse.success(workspace)
    }

    /**
     * Get workspace by slug
     */
    @GetMapping("/by-slug/{slug}")
    fun getWorkspaceBySlug(@PathVariable slug: String): ApiResponse<WorkspaceResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val workspace = workspaceService.getWorkspaceBySlug(slug, user.uid)
        return ApiResponse.success(workspace)
    }

    /**
     * Update workspace (requires WORKSPACE_UPDATE permission)
     */
    @PutMapping("/{workspaceId}")
    fun updateWorkspace(
        @PathVariable workspaceId: String,
        @RequestBody @Valid request: UpdateWorkspaceRequest,
    ): ApiResponse<WorkspaceResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Check if user has permission to update workspace
        if (!memberService.hasPermission(workspaceId, user.uid, "WORKSPACE_UPDATE")) {
            return ApiResponse.error("ACCESS_DENIED", "Insufficient permissions to update workspace", "workspace")
        }

        val workspace = workspaceService.updateWorkspace(workspaceId, request, user.uid)
        return ApiResponse.success(workspace)
    }

    /**
     * Get user's workspaces
     */
    @GetMapping
    fun getUserWorkspaces(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ApiResponse<Page<WorkspaceListResponse>> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val workspaces = workspaceService.getUserWorkspaces(user.uid, pageable)
        return ApiResponse.success(workspaces)
    }

    /**
     * Search workspaces
     */
    @GetMapping("/search")
    fun searchWorkspaces(
        @RequestParam query: String,
        @RequestParam(required = false) workspaceType: WorkspaceType?,
        @RequestParam(required = false) subscriptionPlan: SubscriptionPlan?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ApiResponse<Page<WorkspaceListResponse>> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val pageable = PageRequest.of(page, size)
        val workspaces = workspaceService.searchWorkspaces(query, workspaceType, subscriptionPlan, user.uid, pageable)
        return ApiResponse.success(workspaces)
    }

    /**
     * Archive workspace (requires WORKSPACE_SETTINGS permission)
     */
    @PostMapping("/{workspaceId}/archive")
    fun archiveWorkspace(@PathVariable workspaceId: String): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Check if user has permission to archive workspace
        if (!memberService.hasPermission(workspaceId, user.uid, "WORKSPACE_SETTINGS")) {
            return ApiResponse.error("ACCESS_DENIED", "Insufficient permissions to archive workspace", "workspace")
        }

        val result = workspaceService.archiveWorkspace(workspaceId, user.uid)
        return ApiResponse.success(result)
    }

    /**
     * Soft delete workspace (requires OWNER role)
     */
    @DeleteMapping("/{workspaceId}")
    fun deleteWorkspace(@PathVariable workspaceId: String): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Check if user is workspace owner
        if (!memberService.isWorkspaceOwner(workspaceId, user.uid)) {
            return ApiResponse.error("ACCESS_DENIED", "Only workspace owners can delete workspaces", "workspace")
        }

        val result = workspaceService.archiveWorkspace(workspaceId, user.uid)
        return ApiResponse.success(result)
    }

    /**
     * Permanently delete workspace (requires OWNER role)
     */
    @DeleteMapping("/{workspaceId}/permanent")
    fun permanentlyDeleteWorkspace(@PathVariable workspaceId: String): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Check if user is workspace owner
        if (!memberService.isWorkspaceOwner(workspaceId, user.uid)) {
            return ApiResponse.error(
                "ACCESS_DENIED",
                "Only workspace owners can permanently delete workspaces",
                "workspace"
            )
        }

        val result = workspaceService.deleteWorkspace(workspaceId, user.uid)
        return ApiResponse.success(result)
    }

    /**
     * Check slug availability
     */
    @GetMapping("/check-slug/{slug}")
    fun checkSlugAvailability(@PathVariable slug: String): ApiResponse<Map<String, Boolean>> {
        val result = workspaceService.checkSlugAvailability(slug)
        return ApiResponse.success(result)
    }
}