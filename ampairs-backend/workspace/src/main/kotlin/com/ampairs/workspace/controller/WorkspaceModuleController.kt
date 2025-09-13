package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.service.WorkspaceModuleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

/**
 * **Workspace Module Management Controller**
 *
 * Provides comprehensive module management functionality for workspaces.
 * Workspace managers and admins can discover, install, configure, and manage
 * business modules based on their specific business needs and category.
 *
 * **Key Features:**
 * - Module discovery and browsing
 * - Installation and removal management
 * - Configuration and customization
 * - Role-based access control
 * - Multi-tenant workspace isolation
 */
@Tag(
    name = "Workspace Module Management",
    description = """
    ## 🧩 Comprehensive Module Management System
    
    **Enable workspace administrators to dynamically manage business functionality modules.**
    
    ### 🎯 **Core Capabilities**
    - **Module Discovery**: Browse available business modules by category
    - **Installation Management**: Install and uninstall modules in workspaces
    - **Configuration Control**: Customize module settings per workspace needs
    - **Activity Tracking**: Monitor module usage and performance
    - **Role-Based Access**: Secure operations based on user permissions
    
    ### 📋 **Module Categories**
    - **Customer Management**: CRM and customer relationship tools
    - **Sales & Marketing**: Sales pipeline and marketing automation
    - **Inventory Management**: Stock tracking and inventory control
    - **Financial Management**: Accounting and financial reporting
    - **Project Management**: Task and project coordination tools
    - **Analytics & Reporting**: Business intelligence and insights
    
    ### 🔐 **Permission Requirements**
    - **View Operations**: Workspace **MEMBER** or higher
    - **Configuration Operations**: Workspace **MANAGER** or higher  
    - **Installation Operations**: Workspace **ADMIN** or higher
    
    ### 🏗️ **API Usage Workflow**
    1. **Authenticate** with JWT token
    2. **Set Workspace Context** using X-Workspace-ID header
    3. **Browse Available Modules** to discover functionality
    4. **Install Required Modules** for your business needs
    5. **Configure Module Settings** to match workflows
    6. **Monitor Module Activity** for optimization
    """
)
@RestController
@RequestMapping("/workspace/v1/modules")
@SecurityRequirement(name = "BearerAuth")
@SecurityRequirement(name = "WorkspaceContext")
class WorkspaceModuleController(
    private val workspaceModuleService: WorkspaceModuleService
) {

    @Operation(
        summary = "Get Workspace Module Overview",
        description = """
        ## 📊 **Get Comprehensive Module Information**
        
        Retrieves an overview of the workspace's current module configuration and status.
        
        ### **Response Information:**
        - **Workspace Context**: Current workspace identification
        - **Module Statistics**: Total and active module counts
        - **Status Overview**: Module health and activity summary
        - **Quick Access**: Key module management actions available
        
        ### **Use Cases:**
        - **Dashboard Display**: Show module overview in admin panels
        - **Health Monitoring**: Check workspace module status
        - **Quick Navigation**: Provide shortcuts to common actions
        - **Audit Trail**: Track module management activities
        
        ### **Business Value:**
        - Provides instant visibility into workspace capabilities
        - Enables proactive module management and optimization
        - Supports informed decision-making for module additions
        """,
        tags = ["Module Overview"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Successfully retrieved module overview",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Module Overview Response",
                        value = """{
  "success": true,
  "data": [
      {
        "id": "MOD_CUSTOMER_CRM_001",
        "moduleCode": "customer-management",
        "name": "Customer CRM",
        "category": "CUSTOMER_MANAGEMENT",
        "version": "2.1.0",
        "status": "ACTIVE",
        "enabled": true,
        "installedAt": "2025-01-10T09:15:00Z",
        "icon": "people",
        "primaryColor": "#2196F3",
        "healthScore": 0.95,
        "needsAttention": false
      },
      {
        "id": "MOD_INVENTORY_MGT_002",
        "moduleCode": "inventory-management",
        "name": "Inventory Manager",
        "category": "INVENTORY_MANAGEMENT",
        "version": "1.5.2",
        "status": "ACTIVE",
        "enabled": true,
        "installedAt": "2025-01-12T14:20:00Z",
        "icon": "inventory",
        "primaryColor": "#4CAF50",
        "healthScore": 0.88,
        "needsAttention": false
      },
      {
        "id": "MOD_SALES_PIPELINE_003",
        "moduleCode": "sales-management",
        "name": "Sales Pipeline",
        "category": "SALES_MANAGEMENT",
        "version": "3.0.1",
        "status": "INSTALLED",
        "enabled": false,
        "installedAt": "2025-01-14T16:45:00Z",
        "icon": "trending_up",
        "primaryColor": "#FF9800",
        "healthScore": 0.72,
        "needsAttention": true
      }
    ],
  "timestamp": "2025-01-15T10:30:00Z"
}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "⛔ Access denied - Not a workspace member or missing workspace header"
            ),
            SwaggerApiResponse(
                responseCode = "500",
                description = "💥 Internal server error - System unavailable"
            )
        ]
    )
    @GetMapping
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getModules(): ResponseEntity<ApiResponse<List<InstalledModuleResponse>>> {
        val result = workspaceModuleService.getInstalledModules()
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @Operation(
        summary = "Get Specific Module Details",
        description = """
        ## 🔍 **Retrieve Detailed Module Information**
        
        Fetches comprehensive information about a specific workspace module including
        configuration, status, usage metrics, and available actions.
        
        ### **Detailed Information Provided:**
        - **Module Metadata**: Name, description, category, and version
        - **Installation Status**: Current state and installation timestamp  
        - **Configuration Settings**: Current module configuration parameters
        - **Usage Analytics**: Access patterns and performance metrics
        - **Available Actions**: Operations permitted for current user role
        
        ### **Use Cases:**
        - **Module Configuration**: Access current settings for modifications
        - **Troubleshooting**: Debug module issues with detailed status information
        - **Analytics Review**: Analyze module usage patterns and performance
        - **Security Audit**: Review module permissions and access history
        
        ### **Business Benefits:**
        - Enables precise module management and optimization
        - Provides detailed insights for business decision-making
        - Supports compliance and audit requirements
        """,
        tags = ["Module Details"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Successfully retrieved module details",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Module Details Response",
                        value = """{
  "success": true,
  "data": {
    "moduleId": "MOD_CUSTOMER_CRM_001",
    "workspaceId": "WS_ABC123_XYZ789", 
    "message": "Module info placeholder - implementation pending",
    "moduleInfo": {
      "name": "Customer CRM Module",
      "category": "CUSTOMER_MANAGEMENT",
      "description": "Comprehensive customer relationship management",
      "version": "2.1.0",
      "status": "ACTIVE",
      "enabled": true,
      "installedAt": "2025-01-10T09:15:00Z",
      "lastUpdated": "2025-01-14T16:20:00Z"
    },
    "configuration": {
      "autoSync": true,
      "notificationsEnabled": true,
      "customFields": ["priority", "source", "tags"]
    },
    "analytics": {
      "dailyActiveUsers": 12,
      "monthlyAccess": 245,
      "averageSessionDuration": "8.5 minutes"
    },
    "permissions": {
      "canConfigure": true,
      "canUninstall": false,
      "canViewAnalytics": true
    }
  },
  "timestamp": "2025-01-15T10:30:00Z"
}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "🔍 Module not found - Invalid module ID or not installed in workspace"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "⛔ Access denied - Not a workspace member or missing workspace header"
            )
        ]
    )
    @GetMapping("/{moduleId}")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getModule(
        @Parameter(
            name = "moduleId",
            description = """
            **Unique Module Identifier**
            
            The specific identifier for the workspace module you want to retrieve.
            This can be either:
            - **Module UID**: Unique identifier (e.g., 'MOD_CUSTOMER_CRM_001')
            - **Module Code**: Short code identifier (e.g., 'customer-crm')
            
            **How to find Module ID:**
            1. Call `GET /workspace/v1/modules` to list all modules
            2. Use the `id` field from the module list response
            3. Module IDs are consistent across API calls
            """,
            required = true,
            example = "MOD_CUSTOMER_CRM_001"
        )
        @PathVariable moduleId: String,
    ): ResponseEntity<ApiResponse<ModuleDetailResponse>> {
        val result = workspaceModuleService.getModuleInfo(moduleId)
        return if (result != null) {
            ResponseEntity.ok(ApiResponse.success(result))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(
        summary = "Browse Available Modules",
        description = """
        ## 🔍 **Discover Available Modules for Installation**
        
        Browse the catalog of available business modules that can be installed in the workspace.
        Modules are filtered based on subscription tier and what's already installed.
        
        ### **Response Information:**
        - **Available Modules**: Modules not yet installed in workspace
        - **Module Details**: Name, description, category, rating, and requirements
        - **Installation Requirements**: Dependencies, conflicts, and prerequisites
        - **Filtering Options**: Category-based filtering and featured modules
        
        ### **Use Cases:**
        - **Module Discovery**: Find new functionality to add to workspace
        - **Business Growth**: Explore modules as business needs expand
        - **Feature Planning**: Research available capabilities before implementation
        - **Competitive Analysis**: Compare module features and ratings
        
        ### **Business Value:**
        - Enables informed decisions about workspace functionality
        - Supports scalable business growth through modular expansion
        - Provides visibility into ecosystem capabilities
        """,
        tags = ["Module Discovery"]
    )
    @GetMapping("/available")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getAvailableModules(
        @Parameter(
            name = "category",
            description = """
            """,
            required = false,
            example = "CUSTOMER_MANAGEMENT"
        )
        @RequestParam(required = false) category: String?,

        @Parameter(
            name = "featured",
            description = """
            **Show Featured Modules Only**

            When true, returns only modules marked as featured/recommended.
            Featured modules are typically popular, well-rated, or essential for most businesses.
            """,
            required = false,
            example = "false"
        )
        @RequestParam(required = false, defaultValue = "false") featured: Boolean,
    ): ResponseEntity<ApiResponse<List<AvailableModuleResponse>>> {
        val result = workspaceModuleService.getAvailableModules(category, featured)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @Operation(
        summary = "Get Module Catalog with Actions",
        description = """
        ## 📚 **Get Unified Module Catalog with Install/Uninstall Options**

        Retrieves a comprehensive view of all modules - both installed and available -
        with their respective action options (install, uninstall, enable, disable, configure).
        This endpoint is designed for frontend module management interfaces.

        ### **Response Information:**
        - **Installed Modules**: Currently installed modules with management actions
        - **Available Modules**: Modules available for installation
        - **Action Options**: Available actions per module based on user permissions
        - **Installation Status**: Current status and health information
        - **User Permissions**: What actions the current user can perform
        - **Statistics**: Overview of module installation state

        ### **Action Types Provided:**
        - **INSTALL**: Install an available module
        - **UNINSTALL**: Remove an installed module
        - **ENABLE**: Activate a disabled module
        - **DISABLE**: Deactivate an active module
        - **CONFIGURE**: Access module configuration settings
        - **UPDATE**: Update module to newer version

        ### **Use Cases:**
        - **Frontend Module Manager**: Display modules with action buttons
        - **Workspace Dashboard**: Show module overview with management options
        - **Admin Panel**: Comprehensive module management interface
        - **Mobile App**: Module management with touch-friendly actions

        ### **Business Benefits:**
        - Unified interface for all module operations
        - Clear visibility of available actions per module
        - Permission-aware action presentation
        - Streamlined module management workflow
        """,
        tags = ["Module Catalog"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Successfully retrieved module catalog with actions",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Module Catalog Response",
                        value = """{
  "success": true,
  "data": {
    "installed_modules": [
      {
        "module_code": "customer-management",
        "name": "Customer CRM",
        "description": "Comprehensive customer relationship management",
        "category": "CUSTOMER_MANAGEMENT",
        "version": "2.1.0",
        "icon": "people",
        "primary_color": "#2196F3",
        "featured": true,
        "rating": 4.8,
        "install_count": 1250,
        "complexity": "MEDIUM",
        "size_mb": 45,
        "required_tier": "STANDARD",
        "installation_status": {
          "is_installed": true,
          "workspace_module_id": "MOD_CUSTOMER_CRM_001",
          "status": "ACTIVE",
          "enabled": true,
          "installed_at": "2025-01-10T09:15:00Z",
          "health_score": 0.95,
          "needs_attention": false
        },
        "available_actions": [
          {
            "action_type": "UNINSTALL",
            "label": "Uninstall",
            "description": "Remove module from workspace",
            "enabled": true,
            "requires_confirmation": true,
            "confirmation_message": "This will remove all customer data. Continue?"
          },
          {
            "action_type": "CONFIGURE",
            "label": "Configure",
            "description": "Modify module settings",
            "enabled": true,
            "requires_confirmation": false
          },
          {
            "action_type": "DISABLE",
            "label": "Disable",
            "description": "Temporarily disable module",
            "enabled": true,
            "requires_confirmation": false
          }
        ],
        "permissions": {
          "can_install": false,
          "can_uninstall": true,
          "can_configure": true,
          "can_enable": false,
          "can_disable": true
        }
      }
    ],
    "available_modules": [
      {
        "module_code": "inventory-management",
        "name": "Inventory Manager",
        "description": "Track stock levels and manage inventory",
        "category": "INVENTORY_MANAGEMENT",
        "version": "1.8.0",
        "icon": "inventory",
        "primary_color": "#4CAF50",
        "featured": false,
        "rating": 4.3,
        "install_count": 850,
        "complexity": "EASY",
        "size_mb": 32,
        "required_tier": "STANDARD",
        "installation_status": {
          "is_installed": false
        },
        "available_actions": [
          {
            "action_type": "INSTALL",
            "label": "Install",
            "description": "Add to workspace",
            "enabled": true,
            "requires_confirmation": false
          }
        ],
        "permissions": {
          "can_install": true,
          "can_uninstall": false,
          "can_configure": false,
          "can_enable": false,
          "can_disable": false
        }
      }
    ],
    "categories": [
      {
        "code": "CUSTOMER_MANAGEMENT",
        "display_name": "Customer Management",
        "description": "CRM and customer relationship tools",
        "icon": "people"
      }
    ],
    "statistics": {
      "total_installed": 3,
      "total_available": 12,
      "enabled_modules": 2,
      "disabled_modules": 1,
      "modules_needing_attention": 0
    }
  },
  "timestamp": "2025-01-15T10:30:00Z"
}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "⛔ Access denied - Not a workspace member or missing workspace header"
            ),
            SwaggerApiResponse(
                responseCode = "500",
                description = "💥 Internal server error - System unavailable"
            )
        ]
    )
    @GetMapping("/catalog")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getModuleCatalog(
        @Parameter(
            name = "category",
            description = """
            **Filter by Module Category**

            Optional filter to show only modules from specific categories.
            When provided, both installed and available modules will be filtered.

            **Available Categories:**
            - CUSTOMER_MANAGEMENT
            - SALES_MANAGEMENT
            - INVENTORY_MANAGEMENT
            - FINANCIAL_MANAGEMENT
            - PROJECT_MANAGEMENT
            - ANALYTICS_REPORTING
            """,
            required = false,
            example = "CUSTOMER_MANAGEMENT"
        )
        @RequestParam(required = false) category: String?,

        @Parameter(
            name = "include_disabled",
            description = """
            **Include Disabled Modules**

            When true, includes disabled/inactive installed modules in the response.
            When false (default), only shows active installed modules.
            """,
            required = false,
            example = "false"
        )
        @RequestParam(required = false, defaultValue = "false") includeDisabled: Boolean,
    ): ResponseEntity<ApiResponse<ModuleCatalogResponse>> {
        val result = workspaceModuleService.getModuleCatalog(category, includeDisabled)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @Operation(
        summary = "Install Module",
        description = """
        ## ⚡ **Install New Module in Workspace**
        
        Installs a business module from the master catalog into the current workspace.
        Handles dependency checking, conflict resolution, and proper initialization.
        
        ### **Installation Process:**
        1. **Validation**: Check module exists and is installable
        2. **Dependencies**: Verify all required modules are installed
        3. **Conflicts**: Ensure no conflicting modules are active
        4. **Installation**: Create workspace module configuration
        5. **Activation**: Enable module for immediate use
        
        ### **Use Cases:**
        - **Business Expansion**: Add new functionality as business grows
        - **Feature Adoption**: Install specific modules for new processes
        - **Compliance Requirements**: Install modules for regulatory needs
        - **Integration Setup**: Install modules for third-party connections
        
        ### **Business Impact:**
        - Immediate access to new business capabilities
        - Structured approach to functionality expansion
        - Maintained system integrity through validation
        """,
        tags = ["Module Installation"]
    )
    @PostMapping("/install/{moduleCode}")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantAdmin(authentication) || @workspaceAuthorizationService.isCurrentTenantOwner(authentication)")
    fun installModule(
        @Parameter(
            name = "moduleCode",
            description = """
            **Module Code to Install**
            
            The unique code identifier of the module to install from the master catalog.
            
            **Format:** Usually lowercase with hyphens (e.g., 'customer-management')
            **How to Find:** Use the /available endpoint to browse module codes
            """,
            required = true,
            example = "customer-management"
        )
        @PathVariable moduleCode: String,
    ): ResponseEntity<ApiResponse<ModuleInstallationResponse>> {
        val result = workspaceModuleService.installModule(moduleCode)
        return if (result.success) {
            ResponseEntity.ok(ApiResponse.success(result))
        } else {
            ResponseEntity.badRequest().body(ApiResponse.error(ErrorCodes.BAD_REQUEST, result.message))
        }
    }

    @Operation(
        summary = "Uninstall Module",
        description = """
        ## 🗑️ **Remove Module from Workspace**
        
        Safely uninstalls a module from the workspace after checking for dependencies.
        Ensures data integrity and prevents breaking other installed modules.
        
        ### **Uninstallation Process:**
        1. **Dependency Check**: Verify no other modules depend on this one
        2. **Data Backup**: Option to backup module data before removal
        3. **Deactivation**: Disable module functionality
        4. **Cleanup**: Remove module configuration and settings
        5. **Statistics Update**: Update master module usage statistics
        
        ### **Safety Features:**
        - **Dependency Protection**: Prevents removal if other modules depend on it
        - **Data Preservation**: Option to retain data for future reinstallation
        - **Rollback Support**: Ability to reinstall and restore previous state
        
        ### **Use Cases:**
        - **Cost Optimization**: Remove unused modules to reduce overhead
        - **Simplification**: Streamline workspace by removing unnecessary features
        - **Migration**: Remove modules before switching to alternatives
        - **Troubleshooting**: Temporarily remove problematic modules
        """,
        tags = ["Module Management"]
    )
    @DeleteMapping("/{moduleId}")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantAdmin(authentication) || @workspaceAuthorizationService.isCurrentTenantOwner(authentication)")
    fun uninstallModule(
        @Parameter(
            name = "moduleId",
            description = """
            **Module Identifier to Uninstall**
            
            The unique identifier of the installed module to remove from workspace.
            Can be either the workspace module UID or the master module code.
            """,
            required = true,
            example = "MOD_CUSTOMER_CRM_001"
        )
        @PathVariable moduleId: String,
    ): ResponseEntity<ApiResponse<ModuleUninstallationResponse>> {
        val result = workspaceModuleService.uninstallModule(moduleId)
        return if (result.success) {
            ResponseEntity.ok(ApiResponse.success(result))
        } else {
            ResponseEntity.badRequest().body(ApiResponse.error(ErrorCodes.BAD_REQUEST, result.message))
        }
    }
}