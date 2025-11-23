package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.workspace.model.dto.CreateWorkspaceRequest
import com.ampairs.workspace.model.dto.UpdateWorkspaceRequest
import com.ampairs.workspace.model.dto.WorkspaceListResponse
import com.ampairs.workspace.model.dto.WorkspaceResponse
import com.ampairs.workspace.model.dto.toResponse
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.model.enums.WorkspaceType
import com.ampairs.workspace.service.WorkspaceAvatarNotFoundException
import com.ampairs.workspace.service.WorkspaceAvatarService
import com.ampairs.workspace.service.WorkspaceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.TimeUnit
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

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
    private val workspaceAvatarService: WorkspaceAvatarService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createWorkspace(
        @RequestBody @Valid request: CreateWorkspaceRequest,
    ): ApiResponse<WorkspaceResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        val workspace = workspaceService.createWorkspace(request, userId)
        return ApiResponse.success(workspace)
    }

    @Operation(
        summary = "Get Workspace Details",
        description = "Retrieve detailed information about a specific workspace. Requires workspace membership.",
        tags = ["Workspace Management"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Workspace details retrieved successfully"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "🚫 Access denied - Not a workspace member"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "❌ Workspace not found"
            )
        ]
    )
    @GetMapping("/{workspaceId}")
    @PreAuthorize("@workspaceAuthorizationService.isWorkspaceMember(authentication, #workspaceId)")
    fun getWorkspace(@PathVariable workspaceId: String): ApiResponse<WorkspaceResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        val workspace = workspaceService.getWorkspaceById(workspaceId, userId)
        return ApiResponse.success(workspace)
    }

    @Operation(
        summary = "Get Workspace by Slug",
        description = "Retrieve workspace details using the workspace slug/URL identifier. Requires workspace membership.",
        tags = ["Workspace Management"]
    )
    @GetMapping("/by-slug/{slug}")
    fun getWorkspaceBySlug(@PathVariable slug: String): ApiResponse<WorkspaceResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        val workspace = workspaceService.getWorkspaceBySlug(slug, userId)
        return ApiResponse.success(workspace)
    }

    @Operation(
        summary = "Update Workspace",
        description = "Update workspace settings and configuration. Requires WORKSPACE_MANAGE permission.",
        tags = ["Workspace Management"]
    )
    @PutMapping("/{workspaceId}")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).WORKSPACE_MANAGE)")
    fun updateWorkspace(
        @PathVariable workspaceId: String,
        @RequestBody @Valid request: UpdateWorkspaceRequest,
    ): ApiResponse<WorkspaceResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        // Authorization handled by @PreAuthorize - no manual permission check needed
        val workspace = workspaceService.updateWorkspace(workspaceId, request, userId)
        return ApiResponse.success(workspace)
    }

    @Operation(
        summary = "Get User's Workspaces",
        description = "Retrieve all workspaces the authenticated user has access to with pagination.",
        tags = ["Workspace Management"]
    )
    @GetMapping
    fun getUserWorkspaces(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ApiResponse<PageResponse<WorkspaceListResponse>> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        // Map JPA property names to database column names for native query compatibility
        val dbColumnName = when (sortBy) {
            "createdAt" -> "created_at"
            "updatedAt" -> "updated_at"
            "name" -> "name"
            "type" -> "type"
            else -> "created_at" // default fallback
        }

        val sort = Sort.by(Sort.Direction.fromString(sortDir), dbColumnName)
        val pageable = PageRequest.of(page, size, sort)

        val workspaces = workspaceService.getUserWorkspaces(userId, pageable)
        return ApiResponse.success(PageResponse.from(workspaces))
    }

    @Operation(
        summary = "Search Workspaces",
        description = "Search for workspaces with optional filters by type and subscription plan.",
        tags = ["Workspace Management"]
    )
    @GetMapping("/search")
    fun searchWorkspaces(
        @RequestParam query: String,
        @RequestParam(required = false) workspaceType: WorkspaceType?,
        @RequestParam(required = false) subscriptionPlan: SubscriptionPlan?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ApiResponse<PageResponse<WorkspaceListResponse>> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        val pageable = PageRequest.of(page, size)
        val workspaces = workspaceService.searchWorkspaces(query, workspaceType, subscriptionPlan, userId, pageable)
        return ApiResponse.success(PageResponse.from(workspaces))
    }

    @Operation(
        summary = "Archive Workspace",
        description = "Archive a workspace (soft delete). Requires WORKSPACE_DELETE permission.",
        tags = ["Workspace Management"]
    )
    @PostMapping("/{workspaceId}/archive")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).WORKSPACE_DELETE)")
    fun archiveWorkspace(@PathVariable workspaceId: String): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        val result = workspaceService.archiveWorkspace(workspaceId, userId)
        return ApiResponse.success(result)
    }

    /**
     * Soft delete workspace (requires WORKSPACE_DELETE permission)
     */
    @DeleteMapping("/{workspaceId}")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).WORKSPACE_DELETE)")
    fun deleteWorkspace(@PathVariable workspaceId: String): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        val result = workspaceService.archiveWorkspace(workspaceId, userId)
        return ApiResponse.success(result)
    }

    /**
     * Permanently delete workspace (requires WORKSPACE_DELETE permission)
     */
    @DeleteMapping("/{workspaceId}/permanent")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).WORKSPACE_DELETE)")
    fun permanentlyDeleteWorkspace(@PathVariable workspaceId: String): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        val result = workspaceService.deleteWorkspace(workspaceId, userId)
        return ApiResponse.success(result)
    }

    @Operation(
        summary = "Check Slug Availability",
        description = "Check if a workspace slug is available for use.",
        tags = ["Workspace Management"]
    )
    @GetMapping("/check-slug/{slug}")
    fun checkSlugAvailability(@PathVariable slug: String): ApiResponse<Map<String, Boolean>> {
        val result = workspaceService.checkSlugAvailability(slug)
        return ApiResponse.success(result)
    }

    // ==================== Avatar Endpoints ====================

    /**
     * Upload an avatar for a workspace.
     *
     * Accepts JPEG, PNG, or WebP images up to 5MB.
     * The image will be resized to a maximum of 512x512 pixels.
     * A thumbnail of 256x256 pixels will also be generated.
     *
     * @param workspaceId The workspace ID
     * @param file The image file to upload
     * @return Updated workspace with avatar URLs
     */
    @Operation(
        summary = "Upload Workspace Avatar",
        description = "Upload an avatar image for a workspace. Requires WORKSPACE_MANAGE permission.",
        tags = ["Workspace Management"]
    )
    @PostMapping("/{workspaceId}/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).WORKSPACE_MANAGE)")
    fun uploadAvatar(
        @PathVariable workspaceId: String,
        @RequestPart("file") file: MultipartFile
    ): ApiResponse<WorkspaceResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        val workspace = workspaceService.getWorkspaceEntity(workspaceId)
        val updatedWorkspace = workspaceAvatarService.uploadAvatar(workspace, file)
        return ApiResponse.success(updatedWorkspace.toResponse())
    }

    /**
     * Delete a workspace's avatar.
     *
     * @param workspaceId The workspace ID
     * @return Updated workspace with null avatar URLs
     */
    @Operation(
        summary = "Delete Workspace Avatar",
        description = "Delete a workspace's avatar. Requires WORKSPACE_MANAGE permission.",
        tags = ["Workspace Management"]
    )
    @DeleteMapping("/{workspaceId}/avatar")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).WORKSPACE_MANAGE)")
    fun deleteAvatar(@PathVariable workspaceId: String): ApiResponse<WorkspaceResponse> {
        val workspace = workspaceService.getWorkspaceEntity(workspaceId)
        val updatedWorkspace = workspaceAvatarService.deleteAvatar(workspace)
        return ApiResponse.success(updatedWorkspace.toResponse())
    }

    /**
     * Get a workspace's avatar (full size).
     *
     * @param workspaceId The workspace ID
     * @return The avatar image bytes
     */
    @Operation(
        summary = "Get Workspace Avatar",
        description = "Get a workspace's avatar image (full size).",
        tags = ["Workspace Management"]
    )
    @GetMapping("/{workspaceId}/avatar")
    fun getAvatar(@PathVariable workspaceId: String): ResponseEntity<ByteArray> {
        val workspace = workspaceService.getWorkspaceEntity(workspaceId)
        val objectKey = workspace.avatarUrl
            ?: throw WorkspaceAvatarNotFoundException("No avatar set for this workspace")

        val imageBytes = workspaceAvatarService.getAvatar(objectKey)

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
            .contentType(getMediaTypeFromKey(objectKey))
            .body(imageBytes)
    }

    /**
     * Get a workspace's avatar thumbnail.
     *
     * @param workspaceId The workspace ID
     * @return The avatar thumbnail image bytes
     */
    @Operation(
        summary = "Get Workspace Avatar Thumbnail",
        description = "Get a workspace's avatar thumbnail image (256x256).",
        tags = ["Workspace Management"]
    )
    @GetMapping("/{workspaceId}/avatar/thumbnail")
    fun getAvatarThumbnail(@PathVariable workspaceId: String): ResponseEntity<ByteArray> {
        val workspace = workspaceService.getWorkspaceEntity(workspaceId)
        val objectKey = workspace.avatarThumbnailUrl
            ?: throw WorkspaceAvatarNotFoundException("No avatar set for this workspace")

        val imageBytes = workspaceAvatarService.getAvatar(objectKey)

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
            .contentType(getMediaTypeFromKey(objectKey))
            .body(imageBytes)
    }

    private fun getMediaTypeFromKey(objectKey: String): MediaType {
        return when {
            objectKey.endsWith(".png") -> MediaType.IMAGE_PNG
            objectKey.endsWith(".webp") -> MediaType.parseMediaType("image/webp")
            else -> MediaType.IMAGE_JPEG
        }
    }
}