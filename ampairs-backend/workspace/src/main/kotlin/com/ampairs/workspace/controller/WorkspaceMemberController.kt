package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.model.enums.WorkspaceRole
import com.ampairs.workspace.security.WorkspacePermission
import com.ampairs.workspace.service.WorkspaceMemberService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * **Workspace Member Management Controller**
 *
 * Comprehensive member management operations including role assignments,
 * permissions, and member lifecycle management within workspace contexts.
 */
@Tag(
    name = "Workspace Member Management",
    description = """
    ## 👥 **Multi-Tenant Member Management System**
    
    **Advanced member management for workspace collaboration and access control.**
    
    ### 🎯 **Core Capabilities**
    - **Member Discovery**: View and search workspace members with detailed information
    - **Role Management**: Assign and modify member roles (OWNER, ADMIN, MANAGER, MEMBER, VIEWER)
    - **Permission Control**: Fine-grained permission management for workspace operations
    - **Member Lifecycle**: Add, update, and remove members from workspaces
    - **Access Monitoring**: Track member activity and access patterns
    
    ### 🔐 **Security & Access Control**
    - **Role Hierarchy**: OWNER → ADMIN → MANAGER → MEMBER → GUEST → VIEWER
    - **Permission-Based Operations**: Each endpoint requires specific workspace permissions
    - **Tenant Isolation**: Complete data separation between different workspaces
    - **Audit Trail**: Comprehensive logging of all member management actions
    
    ### 📋 **Member Role Definitions**
    
    | Role      | Description                           | Key Permissions                      |
    |-----------|---------------------------------------|-------------------------------------|
    | **OWNER** | Workspace owner with full control    | All permissions, cannot be removed |
    | **ADMIN** | Administrative access to workspace   | Manage members, settings, billing   |
    | **MANAGER**| Business operations management       | Manage data, reports, limited admin |
    | **MEMBER**| Standard workspace participant       | Create/edit data, basic operations  |
    | **GUEST** | Limited temporary access             | View-only with restricted scope     |
    | **VIEWER**| Read-only access to workspace        | View data, generate basic reports   |
    
    ### 🔑 **Required Headers**
    - **Authorization**: `Bearer {jwt_token}` - Valid JWT authentication token
    - **X-Workspace-ID**: `{workspace_id}` - Target workspace identifier for multi-tenant operations
    
    ### 📊 **API Usage Patterns**
    1. **List Members**: View all workspace members with roles and permissions
    2. **Member Details**: Get comprehensive information about specific members
    3. **Role Updates**: Modify member roles and permission assignments
    4. **Member Removal**: Remove members from workspace access
    5. **Permission Check**: Verify current user's permissions and capabilities
    """
)
@RestController
@RequestMapping("/workspace/v1/member")
@SecurityRequirement(name = "BearerAuth")
@SecurityRequirement(name = "WorkspaceContext")
class WorkspaceMemberController(
    private val memberService: WorkspaceMemberService,
) {

    @Operation(
        summary = "Get Workspace Members",
        tags = ["Member Management"]
    )
    @GetMapping
    @PreAuthorize("@workspaceAuthorizationService.hasCurrentTenantPermission(authentication, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun getWorkspaceMembers(

        @Parameter(
            name = "page",
            description = "**Page number** (0-based) for pagination. Default: 0",
            example = "0"
        )
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(
            name = "size",
            description = "**Page size** - number of members per page (1-100). Default: 20",
            example = "20"
        )
        @RequestParam(defaultValue = "20") size: Int,

        @Parameter(
            name = "sortBy",
            example = "joinedAt"
        )
        @RequestParam(defaultValue = "joinedAt") sortBy: String,

        @Parameter(
            name = "sortDir",
            description = "**Sort direction**: `asc` (ascending) or `desc` (descending). Default: desc",
            example = "desc"
        )
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ApiResponse<PageResponse<MemberListResponse>> {
        val sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val members = memberService.getWorkspaceMembersOptimized(pageable)
        return ApiResponse.success(PageResponse.from(members))
    }

    @Operation(
        summary = "Get Member Details",
        tags = ["Member Details"]
    )
    @GetMapping("/{memberId}")
    @PreAuthorize("@workspaceAuthorizationService.hasCurrentTenantPermission(authentication, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun getMemberDetails(

        @Parameter(
            name = "memberId",
            required = true,
            example = "MBR_001_JOHN_DOE"
        )
        @PathVariable memberId: String,
    ): ApiResponse<MemberResponse> {
        val member = memberService.getMemberById(memberId)
        return ApiResponse.success(member)
    }

    @Operation(
        summary = "Update Member Role & Permissions",
        tags = ["Member Management"]
    )
    @PutMapping("/{memberId}")
    @PreAuthorize("@workspaceAuthorizationService.hasCurrentTenantPermission(authentication, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_MANAGE)")
    fun updateMember(

        @Parameter(
            name = "memberId",
            required = true,
            example = "MBR_001_JOHN_DOE"
        )
        @PathVariable memberId: String,
        @RequestBody @Valid request: UpdateMemberRequest,
    ): ApiResponse<MemberResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context found")
        val updatedMember = memberService.updateMember(workspaceId, memberId, request, userId)
        return ApiResponse.success(updatedMember)
    }

    @Operation(
        summary = "Remove Member from Workspace",
        tags = ["Member Management"]
    )
    @DeleteMapping("/{memberId}")
    @PreAuthorize("@workspaceAuthorizationService.hasCurrentTenantPermission(authentication, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_DELETE)")
    fun removeMember(

        @Parameter(
            name = "memberId",
            required = true,
            example = "MBR_001_JOHN_DOE"
        )
        @PathVariable memberId: String,
    ): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context found")
        val result = memberService.removeMember(workspaceId, memberId, userId)
        return ApiResponse.success(result)
    }

    @Operation(
        summary = "Get Current User's Role & Permissions",
        tags = ["Permission Management"]
    )
    @GetMapping("/my-role")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getMyRole(): ApiResponse<UserRoleResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context found")
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        // Get member details
        val member = memberService.getWorkspaceMember(userId)
            ?: throw IllegalStateException("User is not a member of workspace")

        // Build role hierarchy
        val roleHierarchy = mapOf(
            "OWNER" to memberService.isWorkspaceOwner(userId),
            "ADMIN" to (member.role.name in listOf("OWNER", "ADMIN")),
            "MANAGER" to (member.role.name in listOf("OWNER", "ADMIN", "MANAGER")),
            "MEMBER" to (member.role.name in listOf("OWNER", "ADMIN", "MANAGER", "MEMBER")),
            "VIEWER" to true
        )

        // Build detailed permissions
        val permissions = mapOf(
            "workspace" to mapOf(
                "manage" to memberService.hasPermission(
                    userId,
                    WorkspacePermission.WORKSPACE_MANAGE
                ),
                "view" to true,
                "delete" to memberService.hasPermission(
                    userId,
                    WorkspacePermission.WORKSPACE_DELETE
                )
            ),
            "members" to mapOf(
                "view" to memberService.hasPermission(
                    userId,
                    WorkspacePermission.MEMBER_VIEW
                ),
                "invite" to memberService.hasPermission(
                    userId,
                    WorkspacePermission.MEMBER_INVITE
                ),
                "manage" to memberService.hasPermission(
                    userId,
                    WorkspacePermission.MEMBER_MANAGE
                ),
                "remove" to memberService.hasPermission(
                    userId,
                    WorkspacePermission.MEMBER_DELETE
                )
            )
        )

        // Module access based on role
        val moduleAccess = when (member.role) {
            WorkspaceRole.OWNER,
            WorkspaceRole.ADMIN -> listOf("all")

            WorkspaceRole.MANAGER -> listOf(
                "customer",
                "product",
                "order",
                "invoice",
                "reports"
            )

            WorkspaceRole.MEMBER -> listOf("customer", "product", "order")
            else -> listOf("customer")
        }

        val result = UserRoleResponse(
            userId = userId,
            workspaceId = workspaceId,
            currentRole = member.role.name,
            membershipStatus = if (member.isActive) "ACTIVE" else "INACTIVE",
            joinedAt = (member.joinedAt ?: member.createdAt ?: java.time.LocalDateTime.now()).toString(),
            lastActivity = member.lastActiveAt?.toString(),
            roleHierarchy = roleHierarchy,
            permissions = permissions,
            moduleAccess = moduleAccess
        )

        return ApiResponse.success(result)
    }

    @Operation(
        summary = "Search Workspace Members",
        tags = ["Member Search"]
    )
    @GetMapping("/search")
    @PreAuthorize("@workspaceAuthorizationService.hasCurrentTenantPermission(authentication, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun searchWorkspaceMembers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "joinedAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
        @RequestParam(required = false) role: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) department: String?,
        @RequestParam(required = false) search_query: String?
    ): ApiResponse<PageResponse<MemberListResponse>> {
        val sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context found")
        // Use the same service method with additional filtering
        val members = memberService.searchWorkspaceMembers(
            workspaceId,
            search_query,
            role,
            status,
            department,
            pageable
        )
        return ApiResponse.success(PageResponse.from(members))
    }

    @Operation(
        summary = "Get Member Statistics",
        tags = ["Member Analytics"]
    )
    @GetMapping("/statistics")
    @PreAuthorize("@workspaceAuthorizationService.hasCurrentTenantPermission(authentication, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun getMemberStatistics(): ApiResponse<Map<String, Any>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context found")
        val statistics = memberService.getMemberStatistics(workspaceId)
        return ApiResponse.success(statistics)
    }

    @Operation(
        summary = "Get Workspace Departments",
        tags = ["Department Management"]
    )
    @GetMapping("/departments")
    @PreAuthorize("@workspaceAuthorizationService.hasCurrentTenantPermission(authentication, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun getWorkspaceDepartments(): ApiResponse<List<String>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context found")
        val departments = memberService.getWorkspaceDepartments(workspaceId)
        return ApiResponse.success(departments)
    }

    @Operation(
        summary = "Bulk Update Members",
        tags = ["Bulk Operations"]
    )
    @PutMapping("/bulk")
    @PreAuthorize("@workspaceAuthorizationService.hasCurrentTenantPermission(authentication, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_MANAGE)")
    fun bulkUpdateMembers(
        @RequestBody request: Map<String, Any>
    ): ApiResponse<Map<String, Any>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context found")
        val result = memberService.bulkUpdateMembers(workspaceId, request)
        return ApiResponse.success(result)
    }

    @Operation(
        summary = "Bulk Remove Members",
        tags = ["Bulk Operations"]
    )
    @DeleteMapping("/bulk")
    @PreAuthorize("@workspaceAuthorizationService.hasCurrentTenantPermission(authentication, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_DELETE)")
    fun bulkRemoveMembers(
        @RequestBody request: Map<String, Any>
    ): ApiResponse<Map<String, Any>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context found")
        val result = memberService.bulkRemoveMembers(workspaceId, request)
        return ApiResponse.success(result)
    }

    @Operation(
        summary = "Export Members Data",
        tags = ["Data Export"]
    )
    @GetMapping("/export")
    @PreAuthorize("@workspaceAuthorizationService.hasCurrentTenantPermission(authentication, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun exportMembers(
        @RequestParam(defaultValue = "CSV") format: String,
        @RequestParam(required = false) role: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) department: String?,
        @RequestParam(required = false) search_query: String?
    ): ResponseEntity<ByteArray> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context found")
        val exportData = memberService.exportMembers(workspaceId, format, role, status, department, search_query)

        val contentType = when (format.uppercase()) {
            "EXCEL" -> "application/vnd.ms-excel"
            else -> "text/csv"
        }

        val filename = "workspace-members-${java.time.LocalDate.now()}.${format.lowercase()}"

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .body(exportData)
    }

    @Operation(
        summary = "Update Member Status",
        tags = ["Member Status"]
    )
    @PatchMapping("/{memberId}/status")
    @PreAuthorize("@workspaceAuthorizationService.hasCurrentTenantPermission(authentication, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_MANAGE)")
    fun updateMemberStatus(
        @PathVariable memberId: String,
        @RequestBody request: Map<String, Any>
    ): ApiResponse<MemberResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context found")
        val status = request["status"] as? String ?: throw IllegalArgumentException("Status is required")
        val reason = request["reason"] as? String

        val updatedMember = memberService.updateMemberStatus(workspaceId, memberId, status, reason)
        return ApiResponse.success(updatedMember)
    }

    @Operation(
        summary = "Get Workspace Roles",
        tags = ["Workspace Configuration"]
    )
    @GetMapping("/roles")
    @PreAuthorize("@workspaceAuthorizationService.hasCurrentTenantPermission(authentication, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun getWorkspaceRoles(): ApiResponse<List<WorkspaceRoleResponse>> {
        val roles = WorkspaceRole.entries.map { role ->
            WorkspaceRoleResponse(
                name = role.name,
                displayName = role.displayName,
                level = role.level,
                description = role.description,
                manageableRoles = role.getManageableRoles().map { it.name }
            )
        }
        return ApiResponse.success(roles)
    }

    @Operation(
        summary = "Get Workspace Permissions",
        tags = ["Workspace Configuration"]
    )
    @GetMapping("/permissions")
    @PreAuthorize("@workspaceAuthorizationService.hasCurrentTenantPermission(authentication, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun getWorkspacePermissions(): ApiResponse<List<WorkspacePermissionResponse>> {
        val permissions = WorkspacePermission.entries.map { permission ->
            WorkspacePermissionResponse(
                name = permission.name,
                permissionName = permission.permissionName,
                description = when (permission) {
                    WorkspacePermission.WORKSPACE_MANAGE -> "Manage workspace settings, details, and configuration"
                    WorkspacePermission.WORKSPACE_DELETE -> "Delete or archive the entire workspace"
                    WorkspacePermission.MEMBER_VIEW -> "View workspace members and their basic information"
                    WorkspacePermission.MEMBER_INVITE -> "Send invitations to new workspace members"
                    WorkspacePermission.MEMBER_MANAGE -> "Manage member roles, permissions, and settings"
                    WorkspacePermission.MEMBER_DELETE -> "Remove members from the workspace"
                }
            )
        }
        return ApiResponse.success(permissions)
    }

}