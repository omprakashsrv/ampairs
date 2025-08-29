package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.workspace.service.WorkspaceModuleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

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
  "data": {
    "workspaceId": "WS_ABC123_XYZ789",
    "message": "Module management is available",
    "totalModules": 8,
    "activeModules": 6,
    "moduleCategories": [
      "CUSTOMER_MANAGEMENT",
      "SALES_MANAGEMENT", 
      "INVENTORY_MANAGEMENT"
    ],
    "recentActivity": {
      "lastInstalled": "Product Catalog Module",
      "lastConfigured": "Customer CRM Module",
      "lastAccessed": "2025-01-15T10:30:00Z"
    },
    "quickActions": [
      "Browse Available Modules",
      "Configure Existing Modules",
      "View Module Analytics"
    ]
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
    @GetMapping
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getModules(): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val result = workspaceModuleService.getBasicModuleInfo()
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
        @PathVariable moduleId: String
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val result = workspaceModuleService.getModuleInfo(moduleId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @Operation(
        summary = "Perform Module Action",
        description = """
        ## ⚡ **Execute Module Management Operations**
        
        Performs various management actions on workspace modules including activation,
        deactivation, configuration updates, and maintenance operations.
        
        ### **Available Actions:**
        
        #### **🔧 Configuration Actions**
        - **`configure`**: Update module settings and preferences
        - **`reset`**: Reset module to default configuration
        - **`backup`**: Create configuration backup
        - **`restore`**: Restore from configuration backup
        
        #### **🔄 State Management Actions**  
        - **`enable`**: Activate module for workspace usage
        - **`disable`**: Temporarily deactivate module
        - **`restart`**: Restart module services
        - **`refresh`**: Refresh module data and cache
        
        #### **📊 Analytics Actions**
        - **`analyze`**: Generate detailed usage analytics
        - **`report`**: Create module performance report
        - **`audit`**: Perform security and compliance audit
        
        #### **🛠️ Maintenance Actions**
        - **`update`**: Update module to latest version
        - **`diagnose`**: Run module health diagnostics
        - **`optimize`**: Optimize module performance
        - **`cleanup`**: Clean unused data and cache
        
        ### **Action Results:**
        Each action returns detailed status information including:
        - **Operation Status**: Success/failure indication
        - **Action Details**: Specific action performed and parameters
        - **Impact Summary**: What changed as a result of the action
        - **Next Steps**: Recommended follow-up actions if any
        
        ### **Permission Requirements:**
        - **Basic Actions** (enable/disable): **MANAGER** role required
        - **Advanced Actions** (update/reset): **ADMIN** role required
        - **Analytics Actions** (analyze/report): **MANAGER** role required
        """,
        tags = ["Module Actions"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Action completed successfully",
                content = [Content(
                    mediaType = "application/json", 
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Successful Action Response",
                        value = """{
  "success": true,
  "data": {
    "moduleId": "MOD_CUSTOMER_CRM_001",
    "action": "enable",
    "workspaceId": "WS_ABC123_XYZ789",
    "success": true,
    "message": "Action enable completed for module MOD_CUSTOMER_CRM_001",
    "actionDetails": {
      "executedAt": "2025-01-15T10:30:00Z",
      "executedBy": "john.doe@example.com",
      "duration": "2.3 seconds",
      "affectedComponents": ["data-sync", "user-interface", "notifications"]
    },
    "impact": {
      "usersAffected": 12,
      "dataChanged": false,
      "requiresRestart": false,
      "immediatelyAvailable": true
    },
    "nextSteps": [
      "Verify module functionality in user interface",
      "Check integration with dependent modules",
      "Monitor performance metrics"
    ]
  },
  "timestamp": "2025-01-15T10:30:00Z"
}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "❌ Bad request - Invalid action or missing parameters"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "⛔ Access denied - Insufficient permissions for this action"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "🔍 Module not found - Invalid module ID"
            ),
            SwaggerApiResponse(
                responseCode = "409",
                description = "⚠️ Conflict - Action cannot be performed in current module state"
            )
        ]
    )
    @PostMapping("/{moduleId}/action")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantManager(authentication)")
    fun performModuleAction(
        @Parameter(
            name = "moduleId",
            description = """
            **Target Module Identifier**
            
            The unique identifier of the module on which to perform the action.
            
            **Module ID Format:** Usually follows pattern 'MOD_CATEGORY_NAME_###'
            **Example:** 'MOD_CUSTOMER_CRM_001'
            """,
            required = true,
            example = "MOD_CUSTOMER_CRM_001"
        )
        @PathVariable moduleId: String,
        
        @Parameter(
            name = "action", 
            description = """
            **Action to Perform**
            
            The specific management action to execute on the module.
            
            **Common Actions:**
            - `enable` - Activate module
            - `disable` - Deactivate module  
            - `configure` - Update settings
            - `reset` - Reset to defaults
            - `update` - Update version
            - `analyze` - Generate analytics
            - `diagnose` - Health check
            - `optimize` - Performance optimization
            
            **Action Categories:**
            - **State**: enable, disable, restart
            - **Config**: configure, reset, backup, restore  
            - **Maintenance**: update, diagnose, optimize, cleanup
            - **Analytics**: analyze, report, audit
            """,
            required = true,
            example = "enable"
        )
        @RequestParam action: String
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        val result = workspaceModuleService.performAction(moduleId, action)
        return ResponseEntity.ok(ApiResponse.success(result))
    }
}