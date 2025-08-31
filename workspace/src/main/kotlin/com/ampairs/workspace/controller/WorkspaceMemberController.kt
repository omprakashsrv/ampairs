package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.workspace.model.dto.MemberListResponse
import com.ampairs.workspace.model.dto.MemberResponse
import com.ampairs.workspace.model.dto.UpdateMemberRequest
import com.ampairs.workspace.security.WorkspacePermission
import com.ampairs.workspace.service.WorkspaceMemberService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

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
@RequestMapping("/workspace/v1")
@SecurityRequirement(name = "BearerAuth")
@SecurityRequirement(name = "WorkspaceContext")
class WorkspaceMemberController(
    private val memberService: WorkspaceMemberService,
) {

    @Operation(
        summary = "Get Workspace Members",
        description = """
        ## 👥 **Retrieve Paginated Workspace Member List**
        
        Fetches a comprehensive list of all workspace members with their roles,
        permissions, activity status, and contact information.
        
        ### **Response Information:**
        - **Member Profiles**: Name, email, avatar, and contact details
        - **Role Information**: Current role assignments and effective permissions
        - **Activity Status**: Last seen, login frequency, and engagement metrics
        - **Join Information**: Join date, invitation details, and onboarding status
        - **Access Control**: Current permissions and restrictions for each member
        
        ### **Sorting Options:**
        - **`joinedAt`** (default): Order by member join date
        - **`name`**: Alphabetical ordering by member name
        - **`role`**: Group by role hierarchy (Owner → Admin → Manager → Member → Viewer)
        - **`lastActivity`**: Order by most recent activity or login
        - **`email`**: Alphabetical ordering by email address
        
        ### **Use Cases:**
        - **Member Directory**: Display comprehensive member directory in admin panels
        - **Access Review**: Audit member access and permissions for compliance
        - **Activity Monitoring**: Track member engagement and workspace participation
        - **Team Management**: Support HR and team coordination activities
        
        ### **Business Benefits:**
        - Provides complete visibility into workspace membership and access
        - Enables effective team management and collaboration coordination
        - Supports compliance and security auditing requirements
        """,
        tags = ["Member Management"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Successfully retrieved workspace members",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Workspace Members Response",
                        value = """{{
  "success": true,
  "data": {{
    "content": [
      {{
        "id": "MBR_001_JOHN_DOE",
        "user_id": "USR_JOHN_DOE_123",
        "email": "john.doe@example.com",
        "name": "John Doe",
        "role": "ADMIN",
        "status": "ACTIVE",
        "joined_at": "2025-01-10T09:15:00Z",
        "last_activity": "2025-01-15T14:30:00Z",
        "permissions": [
          "WORKSPACE_MANAGE",
          "MEMBER_INVITE",
          "MEMBER_MANAGE"
        ],
        "avatar_url": "https://example.com/avatars/john-doe.jpg",
        "phone": "+1-555-0123",
        "department": "Engineering",
        "is_online": true
      }},
      {{
        "id": "MBR_002_JANE_SMITH",
        "user_id": "USR_JANE_SMITH_456",
        "email": "jane.smith@example.com",
        "name": "Jane Smith",
        "role": "MANAGER",
        "status": "ACTIVE",
        "joined_at": "2025-01-12T11:20:00Z",
        "last_activity": "2025-01-15T13:45:00Z",
        "permissions": [
          "MEMBER_VIEW",
          "DATA_MANAGE"
        ],
        "avatar_url": null,
        "phone": "+1-555-0456",
        "department": "Sales",
        "is_online": false
      }}
    ],
    "page": 0,
    "size": 20,
    "total_elements": 15,
    "total_pages": 1,
    "is_first": true,
    "is_last": true
  }},
  "timestamp": "2025-01-15T10:30:00Z"
}}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "⛔ Access denied - Missing MEMBER_VIEW permission or workspace access"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "🔍 Workspace not found - Invalid workspace ID"
            )
        ]
    )
    @GetMapping("/{workspaceId}/members")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun getWorkspaceMembers(
        @Parameter(
            name = "workspaceId",
            description = """
            **Target Workspace Identifier**
            
            The unique identifier of the workspace whose members you want to retrieve.
            
            **Format:** Usually follows pattern 'WS_XXXXX_YYYYY'
            **Example:** 'WS_ABC123_XYZ789'
            
            **How to get Workspace ID:**
            1. Call `GET /workspace/v1` to list user's workspaces
            2. Use the `id` field from workspace list
            3. Or use workspace selection from user interface
            """,
            required = true,
            example = "WS_ABC123_XYZ789"
        )
        @PathVariable workspaceId: String,

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
            description = """
            **Sort field** for ordering results. Available options:
            - `joinedAt` (default) - Order by join date
            - `name` - Alphabetical by member name
            - `email` - Alphabetical by email address
            - `role` - Group by role hierarchy
            - `lastActivity` - Order by recent activity
            """,
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

        val members = memberService.getWorkspaceMembersOptimized(workspaceId, pageable)
        return ApiResponse.success(PageResponse.from(members))
    }

    @Operation(
        summary = "Get Member Details",
        description = """
        ## 👤 **Retrieve Comprehensive Member Information**
        
        Fetches detailed information about a specific workspace member including
        profile data, role assignments, permissions, activity history, and preferences.
        
        ### **Detailed Information Provided:**
        - **Profile Data**: Full name, email, phone, avatar, and bio information
        - **Role & Permissions**: Current role, effective permissions, and access restrictions
        - **Activity Timeline**: Recent actions, login history, and engagement patterns
        - **Workspace Context**: Join date, invitation details, and workspace-specific settings
        - **Contact Information**: Communication preferences and availability status
        
        ### **Use Cases:**
        - **Member Profile Pages**: Display comprehensive member profiles in UI
        - **Permission Verification**: Check specific member's access rights and capabilities
        - **Activity Monitoring**: Review individual member engagement and contributions
        - **Contact Management**: Access member contact details for communication
        - **Security Auditing**: Review member access patterns and permissions for compliance
        
        ### **Business Benefits:**
        - Enables personalized member management and communication
        - Provides detailed insights for performance reviews and team coordination
        - Supports security auditing and compliance requirements
        """,
        tags = ["Member Details"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Successfully retrieved member details",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Member Details Response",
                        value = """{{
  "success": true,
  "data": {{
    "id": "MBR_001_JOHN_DOE",
    "user_id": "USR_JOHN_DOE_123",
    "workspace_id": "WS_ABC123_XYZ789",
    "email": "john.doe@example.com",
    "name": "John Doe",
    "role": "ADMIN",
    "status": "ACTIVE",
    "joined_at": "2025-01-10T09:15:00Z",
    "last_activity": "2025-01-15T14:30:00Z",
    "profile": {{
      "avatar_url": "https://example.com/avatars/john-doe.jpg",
      "phone": "+1-555-0123",
      "department": "Engineering",
      "job_title": "Senior Software Engineer",
      "bio": "Full-stack developer with 8+ years experience",
      "timezone": "America/New_York",
      "is_online": true,
      "last_seen": "2025-01-15T14:30:00Z"
    }},
    "permissions": [
      "WORKSPACE_MANAGE",
      "MEMBER_INVITE",
      "MEMBER_MANAGE",
      "MEMBER_VIEW",
      "DATA_MANAGE",
      "REPORTS_VIEW"
    ],
    "activity_summary": {{
      "total_logins": 47,
      "days_active": 28,
      "last_action": "Updated customer record",
      "favorite_modules": ["Customer CRM", "Sales Pipeline"]
    }},
    "workspace_context": {{
      "invited_by": "jane.admin@example.com",
      "invitation_accepted_at": "2025-01-10T09:45:00Z",
      "role_changed_at": "2025-01-12T15:20:00Z",
      "settings": {{
        "notifications_enabled": true,
        "email_updates": "weekly",
        "dashboard_layout": "advanced"
      }}
    }}
  }},
  "timestamp": "2025-01-15T10:30:00Z"
}}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "⛔ Access denied - Missing MEMBER_VIEW permission"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "🔍 Member not found - Invalid member ID or not in workspace"
            )
        ]
    )
    @GetMapping("/{workspaceId}/members/{memberId}")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun getMemberDetails(
        @Parameter(
            name = "workspaceId",
            description = "**Target workspace identifier** where the member belongs",
            required = true,
            example = "WS_ABC123_XYZ789"
        )
        @PathVariable workspaceId: String,

        @Parameter(
            name = "memberId",
            description = """
            **Unique Member Identifier**
            
            The specific identifier for the workspace member whose details you want to retrieve.
            
            **Format:** Usually follows pattern 'MBR_###_NAME'
            **Example:** 'MBR_001_JOHN_DOE'
            
            **How to find Member ID:**
            1. Call `GET /workspace/v1/{workspaceId}/members` to list all members
            2. Use the `id` field from the member list response
            3. Member IDs are consistent across all API calls
            """,
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
        description = """
        ## ⚙️ **Modify Member Role and Permission Assignments**
        
        Updates a workspace member's role, permissions, and access settings.
        This operation allows workspace administrators to modify member privileges
        and access control based on business requirements and security policies.
        
        ### **Updatable Properties:**
        
        #### **🔑 Role Changes**
        - **Role Promotion**: Elevate member to higher responsibility roles
        - **Role Demotion**: Reduce member access for security or organizational changes
        - **Role Restrictions**: Apply temporary or permanent access limitations
        
        #### **🔐 Permission Updates**
        - **Custom Permissions**: Grant or revoke specific operational permissions
        - **Module Access**: Control access to specific business modules and features
        - **Data Scope**: Limit data access to specific categories or departments
        
        #### **📋 Profile Updates**
        - **Contact Information**: Update member communication preferences
        - **Department Assignment**: Change organizational department or team
        - **Notification Settings**: Modify alert and communication preferences
        
        ### **Role Change Rules:**
        
        | Current Role | Can Update To | Restrictions |
        |--------------|---------------|-------------|
        | **OWNER** | None | Cannot be changed (only transferred) |
        | **ADMIN** | MANAGER, MEMBER, VIEWER | Requires OWNER permission |
        | **MANAGER** | MEMBER, VIEWER | Can be updated by ADMIN+ |
        | **MEMBER** | VIEWER, GUEST | Can be updated by MANAGER+ |
        | **VIEWER** | GUEST | Can be updated by MANAGER+ |
        
        ### **Security Considerations:**
        - **Permission Elevation**: Role promotions require higher-level authorization
        - **Audit Trail**: All role changes are logged with timestamps and reasons
        - **Immediate Effect**: Permission changes take effect immediately across all sessions
        - **Notification**: Affected members receive automatic notifications of access changes
        
        ### **Use Cases:**
        - **Organizational Changes**: Reflect promotions, demotions, or department transfers
        - **Access Control**: Implement security policies and compliance requirements
        - **Temporary Access**: Grant temporary elevated permissions for specific projects
        - **Onboarding**: Gradually increase new member permissions as they gain experience
        """,
        tags = ["Member Management"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Member successfully updated",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Updated Member Response",
                        value = """{{
  "success": true,
  "data": {{
    "id": "MBR_001_JOHN_DOE",
    "user_id": "USR_JOHN_DOE_123",
    "workspace_id": "WS_ABC123_XYZ789",
    "email": "john.doe@example.com",
    "name": "John Doe",
    "role": "MANAGER",
    "status": "ACTIVE",
    "updated_at": "2025-01-15T10:30:00Z",
    "updated_by": "admin@example.com",
    "permissions": [
      "MEMBER_VIEW",
      "DATA_MANAGE",
      "REPORTS_VIEW"
    ],
    "change_summary": {{
      "previous_role": "ADMIN",
      "new_role": "MANAGER",
      "permissions_removed": ["WORKSPACE_MANAGE", "MEMBER_MANAGE"],
      "permissions_added": [],
      "reason": "Organizational restructuring"
    }}
  }},
  "timestamp": "2025-01-15T10:30:00Z"
}}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "❌ Bad request - Invalid role or permission data"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "⛔ Access denied - Missing MEMBER_MANAGE permission or insufficient role level"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "🔍 Member not found - Invalid member ID"
            ),
            SwaggerApiResponse(
                responseCode = "409",
                description = "⚠️ Conflict - Cannot update OWNER role or role hierarchy violation"
            )
        ]
    )
    @PutMapping("/{workspaceId}/members/{memberId}")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_MANAGE)")
    fun updateMember(
        @Parameter(
            name = "workspaceId",
            description = "**Target workspace identifier** containing the member to update",
            required = true,
            example = "WS_ABC123_XYZ789"
        )
        @PathVariable workspaceId: String,

        @Parameter(
            name = "memberId",
            description = """
            **Member ID to Update**
            
            The unique identifier of the workspace member whose role and permissions you want to modify.
            
            **Important Notes:**
            - Cannot update OWNER role (only transfer ownership)
            - Role changes are subject to hierarchy restrictions
            - Member will receive notification of role changes
            """,
            required = true,
            example = "MBR_001_JOHN_DOE"
        )
        @PathVariable memberId: String,

        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = """
            **Member Update Information**
            
            Specify the new role and permissions for the workspace member.
            
            **Example Request Body:**
            ```json
            {
              "role": "MANAGER",
              "custom_permissions": ["DATA_MANAGE", "REPORTS_VIEW"],
              "department": "Sales",
              "reason": "Promotion to team lead position",
              "notify_member": true
            }
            ```
            
            **Available Roles:** ADMIN, MANAGER, MEMBER, VIEWER, GUEST
            **Available Permissions:** WORKSPACE_MANAGE, MEMBER_INVITE, MEMBER_MANAGE, MEMBER_VIEW, DATA_MANAGE, REPORTS_VIEW
            """,
            required = true
        )
        @RequestBody @Valid request: UpdateMemberRequest,
    ): ApiResponse<MemberResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        val updatedMember = memberService.updateMember(workspaceId, memberId, request, userId)
        return ApiResponse.success(updatedMember)
    }

    @Operation(
        summary = "Remove Member from Workspace",
        description = """
        ## 🗑️ **Remove Member Access from Workspace**
        
        Permanently removes a member from the workspace, revoking all access rights
        and permissions. This operation is irreversible and requires high-level authorization.
        
        ### **Removal Process:**
        
        #### **🚪 Access Revocation**
        - **Immediate Effect**: All access tokens and sessions are invalidated instantly
        - **Permission Removal**: All workspace permissions are permanently revoked
        - **Data Access**: Member loses access to all workspace data and resources
        - **Module Access**: Removed from all business modules and integrations
        
        #### **📋 Data Handling**
        - **Ownership Transfer**: Member's data ownership is transferred to workspace admin
        - **Audit Trail**: Complete removal history is maintained for compliance
        - **Backup Creation**: Optional data backup before member removal
        - **Reference Cleanup**: Member references in shared documents are handled gracefully
        
        #### **📧 Notification Process**
        - **Member Notification**: Removed member receives notification (if enabled)
        - **Admin Notification**: Workspace admins are notified of member removal
        - **Audit Log**: Detailed removal record is created for security audit
        
        ### **Removal Restrictions:**
        
        | Member Role | Can Be Removed By | Special Considerations |
        |-------------|------------------|------------------------|
        | **OWNER** | Cannot be removed | Only ownership transfer available |
        | **ADMIN** | OWNER only | Requires OWNER-level authorization |
        | **MANAGER** | OWNER, ADMIN | Requires ADMIN+ authorization |
        | **MEMBER** | OWNER, ADMIN, MANAGER | Standard removal process |
        | **VIEWER** | OWNER, ADMIN, MANAGER | Standard removal process |
        
        ### **Security Considerations:**
        - **Access Validation**: Verify remover has sufficient permissions
        - **Session Termination**: All active sessions are immediately terminated
        - **Token Revocation**: JWT tokens are blacklisted across all devices
        - **Data Security**: Sensitive data access is immediately revoked
        
        ### **Use Cases:**
        - **Employee Offboarding**: Remove departing employees from workspace access
        - **Security Incident**: Immediately revoke access for compromised accounts
        - **Access Control**: Remove members who no longer require workspace access
        - **Compliance**: Meet regulatory requirements for access management
        
        ### **Recovery Options:**
        - **Re-invitation**: Removed members can be re-invited with new permissions
        - **Data Recovery**: Admin can access transferred data if needed
        - **Audit Review**: Complete removal history available for investigation
        """,
        tags = ["Member Management"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Member successfully removed from workspace",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Member Removal Success",
                        value = """{{
  "success": true,
  "data": {{
    "message": "Member MBR_001_JOHN_DOE successfully removed from workspace WS_ABC123_XYZ789",
    "removal_details": {{
      "member_id": "MBR_001_JOHN_DOE",
      "member_name": "John Doe",
      "member_email": "john.doe@example.com",
      "workspace_id": "WS_ABC123_XYZ789",
      "removed_at": "2025-01-15T10:30:00Z",
      "removed_by": "admin@example.com",
      "previous_role": "MANAGER"
    }},
    "impact_summary": {{
      "sessions_terminated": 2,
      "tokens_revoked": 3,
      "data_ownership_transferred": true,
      "notification_sent": true
    }},
    "recovery_options": {{
      "can_reinvite": true,
      "data_retention_days": 30,
      "audit_trail_available": true
    }}
  }},
  "timestamp": "2025-01-15T10:30:00Z"
}}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "⛔ Access denied - Missing MEMBER_DELETE permission or insufficient role level"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "🔍 Member not found - Invalid member ID or already removed"
            ),
            SwaggerApiResponse(
                responseCode = "409",
                description = "⚠️ Conflict - Cannot remove OWNER or violation of business rules"
            )
        ]
    )
    @DeleteMapping("/{workspaceId}/members/{memberId}")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_DELETE)")
    fun removeMember(
        @Parameter(
            name = "workspaceId",
            description = "**Target workspace identifier** from which to remove the member",
            required = true,
            example = "WS_ABC123_XYZ789"
        )
        @PathVariable workspaceId: String,

        @Parameter(
            name = "memberId",
            description = """
            **Member ID to Remove**
            
            The unique identifier of the workspace member to remove permanently.
            
            **Critical Notes:**
            - **OWNER cannot be removed** - only ownership transfer is possible
            - **Removal is permanent** - member will lose all workspace access
            - **Sessions terminated** - all active sessions will be ended immediately
            - **Data transferred** - member's data ownership transferred to admin
            
            **Before Removal:**
            1. Ensure data backup if needed
            2. Transfer any critical ownership or responsibilities
            3. Notify stakeholders of the member removal
            4. Consider temporary role demotion instead of removal
            """,
            required = true,
            example = "MBR_001_JOHN_DOE"
        )
        @PathVariable memberId: String,
    ): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        val result = memberService.removeMember(workspaceId, memberId, userId)
        return ApiResponse.success(result)
    }

    @Operation(
        summary = "Get Current User's Role & Permissions",
        description = """
        ## 🔑 **Retrieve Current User's Workspace Access Information**
        
        Returns comprehensive information about the authenticated user's role,
        permissions, and access rights within the specified workspace context.
        
        ### **Information Provided:**
        
        #### **📜 Role Information**
        - **Current Role**: User's assigned role in the workspace (OWNER, ADMIN, MANAGER, etc.)
        - **Role Hierarchy**: Position in organizational hierarchy
        - **Role Description**: Detailed explanation of role responsibilities
        - **Role Limitations**: Any specific restrictions or temporary limitations
        
        #### **🔐 Permission Matrix**
        - **Workspace Management**: Can modify workspace settings and configuration
        - **Member Management**: Can invite, update, or remove workspace members
        - **Data Operations**: Can create, read, update, delete business data
        - **Module Access**: Available business modules and their access levels
        - **Report Generation**: Can access and generate various business reports
        - **Integration Management**: Can manage external integrations and APIs
        
        #### **📋 Context Information**
        - **Membership Status**: Active, pending, or restricted status
        - **Join Information**: When and how user joined the workspace
        - **Last Activity**: Recent actions and engagement within workspace
        - **Session Details**: Current session information and device context
        
        ### **Permission Categories:**
        
        | Permission Category | Description | Typical Roles |
        |--------------------|-------------|---------------|
        | **WORKSPACE_MANAGE** | Full workspace administration | OWNER, ADMIN |
        | **MEMBER_INVITE** | Invite new members to workspace | ADMIN, MANAGER |
        | **MEMBER_MANAGE** | Modify member roles and permissions | OWNER, ADMIN |
        | **MEMBER_VIEW** | View member directory and details | MANAGER+ |
        | **DATA_MANAGE** | Create/update business data | MEMBER+ |
        | **REPORTS_VIEW** | Access reports and analytics | MEMBER+ |
        
        ### **Use Cases:**
        - **UI Permission Control**: Show/hide UI elements based on user permissions
        - **Feature Availability**: Determine which features are accessible
        - **Navigation Menu**: Customize menu items based on access rights
        - **Security Validation**: Client-side permission validation for better UX
        - **Role-Based Routing**: Direct users to appropriate sections based on role
        
        ### **Integration Benefits:**
        - **Dynamic UI**: Build responsive interfaces that adapt to user permissions
        - **Security Layer**: Add client-side security checks for better user experience
        - **Personalization**: Customize workspace experience based on user role
        - **Compliance**: Support audit and compliance requirements with detailed access logs
        """,
        tags = ["Permission Management"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Successfully retrieved user role and permissions",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "User Role & Permissions Response",
                        value = """{{
  "success": true,
  "data": {{
    "user_id": "USR_JOHN_DOE_123",
    "workspace_id": "WS_ABC123_XYZ789",
    "current_role": "MANAGER",
    "membership_status": "ACTIVE",
    "joined_at": "2025-01-10T09:15:00Z",
    "last_activity": "2025-01-15T14:30:00Z",
    "role_hierarchy": {{
      "is_owner": false,
      "is_admin": false,
      "is_manager": true,
      "is_member": true,
      "is_viewer": true
    }},
    "permissions": {{
      "workspace_management": {{
        "can_manage_workspace": false,
        "can_view_settings": true,
        "can_modify_settings": false,
        "can_delete_workspace": false
      }},
      "member_management": {{
        "can_view_members": true,
        "can_invite_members": true,
        "can_manage_members": false,
        "can_remove_members": false
      }},
      "data_operations": {{
        "can_view_data": true,
        "can_create_data": true,
        "can_update_data": true,
        "can_delete_data": true,
        "can_export_data": true
      }},
      "reporting": {{
        "can_view_reports": true,
        "can_create_reports": true,
        "can_share_reports": false,
        "can_access_analytics": true
      }}
    }},
    "module_access": [
      "customer_management",
      "product_catalog", 
      "sales_pipeline",
      "basic_reporting"
    ],
    "restrictions": {{
      "temporary_limitations": [],
      "access_hours": null,
      "ip_restrictions": [],
      "module_restrictions": []
    }}
  }},
  "timestamp": "2025-01-15T10:30:00Z"
}}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "⛔ Access denied - Not a workspace member"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "🔍 Workspace not found - Invalid workspace ID"
            )
        ]
    )
    @GetMapping("/{workspaceId}/my-role")
    @PreAuthorize("@workspaceAuthorizationService.isWorkspaceMember(authentication, #workspaceId)")
    fun getMyRole(
        @Parameter(
            name = "workspaceId",
            description = """
            **Target Workspace Identifier**
            
            The unique identifier of the workspace for which you want to check
            your current role and permissions.
            
            **Context:** This endpoint provides context-specific permission information.
            Your role and permissions may vary across different workspaces.
            
            **Usage:** Use this endpoint to:
            1. **Customize UI**: Show/hide features based on permissions
            2. **Validate Access**: Check if user can perform specific operations
            3. **Dynamic Navigation**: Build role-appropriate menu systems
            4. **Security Checks**: Implement client-side permission validation
            """,
            required = true,
            example = "WS_ABC123_XYZ789"
        )
        @PathVariable workspaceId: String,
    ): ApiResponse<com.ampairs.workspace.model.dto.UserRoleResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        // Get member details
        val member = memberService.getWorkspaceMember(workspaceId, userId)
            ?: throw IllegalStateException("User is not a member of workspace")

        // Build role hierarchy
        val roleHierarchy = mapOf(
            "OWNER" to memberService.isWorkspaceOwner(workspaceId, userId),
            "ADMIN" to (member.role.name in listOf("OWNER", "ADMIN")),
            "MANAGER" to (member.role.name in listOf("OWNER", "ADMIN", "MANAGER")),
            "MEMBER" to (member.role.name in listOf("OWNER", "ADMIN", "MANAGER", "MEMBER")),
            "VIEWER" to true
        )

        // Build detailed permissions
        val permissions = mapOf(
            "workspace" to mapOf(
                "manage" to memberService.hasPermission(
                    workspaceId,
                    userId,
                    WorkspacePermission.WORKSPACE_MANAGE.permissionName
                ),
                "view" to true,
                "delete" to memberService.hasPermission(
                    workspaceId,
                    userId,
                    WorkspacePermission.WORKSPACE_DELETE.permissionName
                )
            ),
            "members" to mapOf(
                "view" to memberService.hasPermission(
                    workspaceId,
                    userId,
                    WorkspacePermission.MEMBER_VIEW.permissionName
                ),
                "invite" to memberService.hasPermission(
                    workspaceId,
                    userId,
                    WorkspacePermission.MEMBER_INVITE.permissionName
                ),
                "manage" to memberService.hasPermission(
                    workspaceId,
                    userId,
                    WorkspacePermission.MEMBER_MANAGE.permissionName
                ),
                "remove" to memberService.hasPermission(
                    workspaceId,
                    userId,
                    WorkspacePermission.MEMBER_DELETE.permissionName
                )
            )
        )

        // Module access based on role
        val moduleAccess = when (member.role) {
            com.ampairs.workspace.model.enums.WorkspaceRole.OWNER,
            com.ampairs.workspace.model.enums.WorkspaceRole.ADMIN -> listOf("all")

            com.ampairs.workspace.model.enums.WorkspaceRole.MANAGER -> listOf(
                "customer",
                "product",
                "order",
                "invoice",
                "reports"
            )

            com.ampairs.workspace.model.enums.WorkspaceRole.MEMBER -> listOf("customer", "product", "order")
            else -> listOf("customer")
        }

        val result = com.ampairs.workspace.model.dto.UserRoleResponse(
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
        description = """
        ## 🔍 **Advanced Member Search with Filtering**
        
        Search and filter workspace members with comprehensive filtering options including
        role, status, department, and text-based search across member profiles.
        
        ### **Search Capabilities:**
        - **Text Search**: Search across name, email, and profile information
        - **Role Filtering**: Filter by specific workspace roles
        - **Status Filtering**: Filter by member status (ACTIVE, INACTIVE, etc.)
        - **Department Filtering**: Filter by organizational department
        - **Combined Filters**: Use multiple filters simultaneously for precise results
        
        ### **Sorting Options:**
        - **Name**: Alphabetical sorting by member name
        - **Email**: Alphabetical sorting by email address
        - **Role**: Sort by role hierarchy
        - **Join Date**: Sort by when member joined workspace
        - **Last Activity**: Sort by most recent activity
        
        ### **Use Cases:**
        - **Quick Search**: Find specific members by name or email
        - **Role Management**: View all members with specific roles
        - **Department Views**: Filter members by organizational structure
        - **Status Monitoring**: View active, inactive, or pending members
        """,
        tags = ["Member Search"]
    )
    @GetMapping("/{workspaceId}/members/search")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun searchWorkspaceMembers(
        @PathVariable workspaceId: String,
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
        description = """
        ## 📊 **Workspace Member Analytics and Statistics**
        
        Retrieve comprehensive statistics about workspace membership including
        member counts, role distribution, activity metrics, and trend analysis.
        
        ### **Statistical Data:**
        - **Member Counts**: Total, active, inactive member counts
        - **Role Distribution**: Breakdown of members by role hierarchy
        - **Status Analysis**: Member status distribution and health metrics
        - **Activity Trends**: Recent joining patterns and engagement metrics
        - **Department Analytics**: Member distribution across departments
        
        ### **Business Insights:**
        - **Growth Tracking**: Monitor workspace membership growth over time
        - **Role Balance**: Ensure proper distribution of roles and responsibilities
        - **Engagement Analysis**: Track member activity and participation
        - **Compliance Metrics**: Support audit and compliance reporting needs
        """,
        tags = ["Member Analytics"]
    )
    @GetMapping("/{workspaceId}/members/statistics")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun getMemberStatistics(
        @PathVariable workspaceId: String
    ): ApiResponse<Map<String, Any>> {
        val statistics = memberService.getMemberStatistics(workspaceId)
        return ApiResponse.success(statistics)
    }

    @Operation(
        summary = "Get Workspace Departments",
        description = """
        ## 🏢 **Retrieve Available Departments**
        
        Get list of all departments that have members in the workspace.
        Useful for filtering and organizational structure display.
        
        ### **Department Information:**
        - **Active Departments**: Only departments with current members
        - **Alphabetical Sorting**: Departments returned in alphabetical order
        - **Member Count**: Optional member count per department
        
        ### **Use Cases:**
        - **Filter Options**: Populate department filter dropdowns
        - **Organizational View**: Display workspace organizational structure
        - **Admin Tools**: Support HR and management reporting
        """,
        tags = ["Department Management"]
    )
    @GetMapping("/{workspaceId}/members/departments")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun getWorkspaceDepartments(
        @PathVariable workspaceId: String
    ): ApiResponse<List<String>> {
        val departments = memberService.getWorkspaceDepartments(workspaceId)
        return ApiResponse.success(departments)
    }

    @Operation(
        summary = "Bulk Update Members",
        description = """
        ## 🔄 **Bulk Member Operations**
        
        Update multiple workspace members simultaneously with role changes,
        status updates, or department assignments.
        
        ### **Bulk Operations:**
        - **Role Updates**: Change multiple member roles at once
        - **Status Changes**: Activate, deactivate, or suspend multiple members
        - **Department Moves**: Transfer members between departments
        - **Permission Updates**: Apply permission changes across multiple members
        
        ### **Safety Features:**
        - **Transaction Safety**: All updates succeed or all fail
        - **Validation**: Pre-validation of all changes before execution
        - **Audit Trail**: Complete logging of all bulk operations
        - **Notification**: Optional member notification of changes
        """,
        tags = ["Bulk Operations"]
    )
    @PutMapping("/{workspaceId}/members/bulk")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_MANAGE)")
    fun bulkUpdateMembers(
        @PathVariable workspaceId: String,
        @RequestBody request: Map<String, Any>
    ): ApiResponse<Map<String, Any>> {
        val result = memberService.bulkUpdateMembers(workspaceId, request)
        return ApiResponse.success(result)
    }

    @Operation(
        summary = "Bulk Remove Members",
        description = """
        ## 🗑️ **Bulk Member Removal**
        
        Remove multiple members from workspace simultaneously with proper
        cleanup and notification handling.
        
        ### **Bulk Removal Process:**
        - **Multi-Member Selection**: Remove multiple members in single operation
        - **Data Cleanup**: Proper handling of member data and permissions
        - **Audit Logging**: Complete record of removal operations
        - **Notification**: Optional notification to removed members
        """,
        tags = ["Bulk Operations"]
    )
    @DeleteMapping("/{workspaceId}/members/bulk")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_DELETE)")
    fun bulkRemoveMembers(
        @PathVariable workspaceId: String,
        @RequestBody request: Map<String, Any>
    ): ApiResponse<Map<String, Any>> {
        val result = memberService.bulkRemoveMembers(workspaceId, request)
        return ApiResponse.success(result)
    }

    @Operation(
        summary = "Export Members Data",
        description = """
        ## 📤 **Export Member Data**
        
        Export workspace member data in various formats (CSV, Excel) with
        optional filtering and custom field selection.
        
        ### **Export Formats:**
        - **CSV**: Comma-separated values for spreadsheet applications
        - **Excel**: Microsoft Excel format with formatting
        - **Filtered Export**: Apply same filters as member search
        
        ### **Export Data:**
        - **Basic Info**: Name, email, role, status, join date
        - **Extended Info**: Department, last activity, permissions
        - **Custom Fields**: Select specific fields for export
        """,
        tags = ["Data Export"]
    )
    @GetMapping("/{workspaceId}/members/export")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun exportMembers(
        @PathVariable workspaceId: String,
        @RequestParam(defaultValue = "CSV") format: String,
        @RequestParam(required = false) role: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) department: String?,
        @RequestParam(required = false) search_query: String?
    ): ResponseEntity<ByteArray> {
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
        description = """
        ## 🔄 **Update Member Status**
        
        Update a specific member's status (ACTIVE, INACTIVE, SUSPENDED, etc.)
        with optional reason and notification.
        
        ### **Status Options:**
        - **ACTIVE**: Full workspace access and permissions
        - **INACTIVE**: Temporary suspension of access
        - **SUSPENDED**: Administrative suspension with audit trail
        - **PENDING**: Awaiting activation or approval
        
        ### **Status Change Process:**
        - **Immediate Effect**: Status changes take effect immediately
        - **Session Management**: Suspended members are logged out
        - **Audit Trail**: Complete logging of status changes
        - **Notifications**: Optional member and admin notifications
        """,
        tags = ["Member Status"]
    )
    @PatchMapping("/{workspaceId}/members/{memberId}/status")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_MANAGE)")
    fun updateMemberStatus(
        @PathVariable workspaceId: String,
        @PathVariable memberId: String,
        @RequestBody request: Map<String, Any>
    ): ApiResponse<MemberResponse> {
        val status = request["status"] as? String ?: throw IllegalArgumentException("Status is required")
        val reason = request["reason"] as? String

        val updatedMember = memberService.updateMemberStatus(workspaceId, memberId, status, reason)
        return ApiResponse.success(updatedMember)
    }
}