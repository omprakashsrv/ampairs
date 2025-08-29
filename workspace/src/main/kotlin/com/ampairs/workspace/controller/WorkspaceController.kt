package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.user.model.User
import com.ampairs.workspace.model.dto.CreateWorkspaceRequest
import com.ampairs.workspace.model.dto.UpdateWorkspaceRequest
import com.ampairs.workspace.model.dto.WorkspaceListResponse
import com.ampairs.workspace.model.dto.WorkspaceResponse
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.model.enums.WorkspaceType
import com.ampairs.workspace.security.WorkspacePermission
import com.ampairs.workspace.service.WorkspaceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * **Workspace Management Controller**
 * 
 * Core workspace operations including creation, management, and access control.
 * Handles multi-tenant workspace functionality with role-based permissions.
 */
@Tag(
    name = "Workspace Management", 
    description = """
    ## 🏢 **Multi-Tenant Workspace Management**
    
    **Core workspace operations for creating and managing business workspaces.**
    
    ### 🎯 **Key Features**
    - **Workspace Creation**: Set up new business workspaces
    - **Multi-Tenancy**: Complete data isolation between workspaces
    - **Access Control**: Role-based permissions and member management
    - **Workspace Discovery**: Search and browse available workspaces
    - **Settings Management**: Configure workspace preferences and settings
    
    ### 🔐 **Security Model**
    - **Tenant Isolation**: All data scoped to specific workspace context
    - **Role Hierarchy**: OWNER → ADMIN → MANAGER → MEMBER → GUEST → VIEWER
    - **Permission-Based Access**: Fine-grained control over workspace operations
    
    ### 📋 **API Categories**
    - **Public APIs**: No workspace context required (creation, listing, search)
    - **Workspace-Scoped APIs**: Require X-Workspace-ID header for multi-tenant operations
    """
)
@RestController
@RequestMapping("/workspace/v1")
@SecurityRequirement(name = "BearerAuth")
class WorkspaceController(
    private val workspaceService: WorkspaceService,
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
     * Get workspace by ID (requires workspace membership)
     */
    @GetMapping("/{workspaceId}")
    @PreAuthorize("@workspaceAuthorizationService.isWorkspaceMember(authentication, #workspaceId)")
    fun getWorkspace(@PathVariable workspaceId: String): ApiResponse<WorkspaceResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val workspace = workspaceService.getWorkspaceById(workspaceId, user.uid)
        return ApiResponse.success(workspace)
    }

    /**
     * Get workspace by slug (requires workspace membership)
     */
    @GetMapping("/by-slug/{slug}")
    fun getWorkspaceBySlug(@PathVariable slug: String): ApiResponse<WorkspaceResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val workspace = workspaceService.getWorkspaceBySlug(slug, user.uid)
        return ApiResponse.success(workspace)
    }

    /**
     * Update workspace (requires WORKSPACE_MANAGE permission)
     */
    @PutMapping("/{workspaceId}")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).WORKSPACE_MANAGE)")
    fun updateWorkspace(
        @PathVariable workspaceId: String,
        @RequestBody @Valid request: UpdateWorkspaceRequest,
    ): ApiResponse<WorkspaceResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Authorization handled by @PreAuthorize - no manual permission check needed
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
    ): ApiResponse<PageResponse<WorkspaceListResponse>> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val workspaces = workspaceService.getUserWorkspaces(user.uid, pageable)
        return ApiResponse.success(PageResponse.from(workspaces))
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
    ): ApiResponse<PageResponse<WorkspaceListResponse>> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val pageable = PageRequest.of(page, size)
        val workspaces = workspaceService.searchWorkspaces(query, workspaceType, subscriptionPlan, user.uid, pageable)
        return ApiResponse.success(PageResponse.from(workspaces))
    }

    /**
     * Archive workspace (requires WORKSPACE_DELETE permission)
     */
    @PostMapping("/{workspaceId}/archive")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).WORKSPACE_DELETE)")
    fun archiveWorkspace(@PathVariable workspaceId: String): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val result = workspaceService.archiveWorkspace(workspaceId, user.uid)
        return ApiResponse.success(result)
    }

    /**
     * Soft delete workspace (requires WORKSPACE_DELETE permission)
     */
    @DeleteMapping("/{workspaceId}")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).WORKSPACE_DELETE)")
    fun deleteWorkspace(@PathVariable workspaceId: String): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val result = workspaceService.archiveWorkspace(workspaceId, user.uid)
        return ApiResponse.success(result)
    }

    /**
     * Permanently delete workspace (requires WORKSPACE_DELETE permission)
     */
    @DeleteMapping("/{workspaceId}/permanent")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).WORKSPACE_DELETE)")
    fun permanentlyDeleteWorkspace(@PathVariable workspaceId: String): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

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