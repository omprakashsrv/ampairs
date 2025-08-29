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
import com.ampairs.workspace.service.WorkspaceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
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
) {

    @Operation(
        summary = "Create New Workspace",
        description = """
        ## 🏢 **Create Multi-Tenant Business Workspace**
        
        Establishes a new business workspace with complete multi-tenant isolation,
        role-based access control, and comprehensive business functionality setup.
        
        ### **Workspace Creation Process:**
        
        #### **🏗️ Workspace Initialization**
        - **Tenant Setup**: Create isolated tenant context with unique identifier
        - **Owner Assignment**: Assign authenticated user as workspace OWNER
        - **Default Configuration**: Initialize workspace with standard settings
        - **Security Setup**: Establish access control and permission framework
        
        #### **📋 Business Setup**
        - **Company Profile**: Configure business information and branding
        - **Module Installation**: Set up core business modules (CRM, Sales, etc.)
        - **Data Structure**: Initialize database schemas and data relationships
        - **Integration Points**: Prepare for external service integrations
        
        #### **👥 Team Foundation**
        - **Owner Permissions**: Grant full administrative rights to creator
        - **Invitation System**: Set up member invitation and onboarding workflows
        - **Role Framework**: Initialize role hierarchy and permission structure
        - **Collaboration Tools**: Enable team communication and collaboration features
        
        ### **Workspace Types & Features:**
        
        **STARTUP Workspace:**
        - Basic CRM and customer management
        - Essential sales pipeline tracking
        - Core reporting and analytics
        - Up to 10 team members
        - Standard integrations
        
        **BUSINESS Workspace:**
        - Advanced CRM with automation
        - Full sales and marketing suite
        - Comprehensive inventory management
        - Advanced reporting and analytics
        - Up to 50 team members
        - Premium integrations
        
        **ENTERPRISE Workspace:**
        - Complete business management suite
        - Advanced workflow automation
        - Custom module development
        - Enterprise-grade security
        - Unlimited team members
        - Custom integrations and API access
        
        ### **Subscription Plans:**
        
        | Plan | Features | Team Size | Storage | Support |
        |------|----------|-----------|---------|----------|
        | **FREE** | Core features | 3 members | 1GB | Community |
        | **STARTER** | Extended features | 10 members | 10GB | Email |
        | **PROFESSIONAL** | Advanced features | 50 members | 100GB | Priority |
        | **ENTERPRISE** | All features | Unlimited | 1TB+ | Dedicated |
        
        ### **Post-Creation Setup:**
        - **Onboarding Workflow**: Guided setup process for new workspace
        - **Team Invitations**: Invite initial team members with appropriate roles
        - **Data Import**: Import existing business data from external systems
        - **Integration Configuration**: Connect with existing business tools
        - **Customization**: Configure workspace appearance and workflows
        
        ### **Use Cases:**
        - **New Business Setup**: Complete business management system for startups
        - **Department Isolation**: Separate workspaces for different business units
        - **Project Management**: Dedicated workspace for specific projects or clients
        - **Multi-Location Management**: Separate workspaces for different office locations
        """,
        tags = ["Workspace Management"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "201",
                description = "✅ Workspace successfully created",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Workspace Creation Success",
                        value = """{{
  "success": true,
  "data": {{
    "id": "WS_NEW_ACME_001",
    "name": "Acme Corporation",
    "slug": "acme-corp",
    "description": "Complete business management workspace for Acme Corp",
    "workspace_type": "BUSINESS",
    "subscription_plan": "PROFESSIONAL",
    "created_at": "2025-01-15T10:30:00Z",
    "owner": {{
      "user_id": "USR_JOHN_DOE_123",
      "name": "John Doe",
      "email": "john.doe@acme.com",
      "role": "OWNER"
    }},
    "settings": {{
      "timezone": "America/New_York",
      "currency": "USD",
      "language": "en-US",
      "business_type": "Technology",
      "employee_count": "11-50"
    }},
    "features": {{
      "modules_included": [
        "Customer CRM",
        "Sales Pipeline",
        "Product Catalog",
        "Order Management",
        "Invoice Generation",
        "Advanced Reporting"
      ],
      "integrations_available": [
        "Email Marketing",
        "Accounting Software",
        "E-commerce Platforms",
        "Communication Tools"
      ],
      "member_limit": 50,
      "storage_limit_gb": 100
    }},
    "onboarding": {{
      "setup_progress": 20,
      "next_steps": [
        "Complete business profile",
        "Invite team members",
        "Import existing data",
        "Configure integrations"
      ],
      "guided_tour_available": true
    }},
    "access_info": {{
      "workspace_url": "https://app.ampairs.com/workspace/acme-corp",
      "dashboard_url": "https://app.ampairs.com/workspace/acme-corp/dashboard",
      "admin_panel_url": "https://app.ampairs.com/workspace/acme-corp/admin"
    }}
  }},
  "timestamp": "2025-01-15T10:30:00Z"
}}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "❌ Bad request - Invalid workspace data or validation errors"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "409",
                description = "⚠️ Conflict - Workspace name or slug already exists"
            ),
            SwaggerApiResponse(
                responseCode = "422",
                description = "🚫 Unprocessable Entity - User already owns maximum number of workspaces"
            )
        ]
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createWorkspace(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = """
            **New Workspace Configuration**
            
            Complete configuration for creating a new business workspace.
            
            **Example Request Body:**
            ```json
            {
              "name": "Acme Corporation",
              "slug": "acme-corp",
              "description": "Complete business management workspace for Acme Corp",
              "workspace_type": "BUSINESS",
              "subscription_plan": "PROFESSIONAL",
              "settings": {
                "timezone": "America/New_York",
                "currency": "USD",
                "language": "en-US",
                "business_type": "Technology",
                "employee_count": "11-50"
              },
              "branding": {
                "logo_url": "https://example.com/logo.png",
                "primary_color": "#1976d2",
                "secondary_color": "#424242"
              },
              "initial_modules": [
                "CUSTOMER_MANAGEMENT",
                "SALES_PIPELINE",
                "PRODUCT_CATALOG"
              ]
            }
            ```
            
            **Required Fields:**
            - `name`: Workspace display name (3-100 characters)
            - `workspace_type`: One of: STARTUP, BUSINESS, ENTERPRISE
            
            **Optional Fields:**
            - `slug`: URL-friendly identifier (auto-generated if not provided)
            - `description`: Workspace description (max 500 characters)
            - `subscription_plan`: Billing plan selection
            - `settings`: Business configuration options
            - `branding`: Visual customization options
            - `initial_modules`: Pre-install specific business modules
            """,
            required = true
        )
        @RequestBody @Valid request: CreateWorkspaceRequest,
    ): ApiResponse<WorkspaceResponse> {
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