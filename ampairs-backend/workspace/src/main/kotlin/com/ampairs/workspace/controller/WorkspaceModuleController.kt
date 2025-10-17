package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.service.WorkspaceModuleService
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
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
@RestController
@RequestMapping("/workspace/v1/modules")
@SecurityRequirement(name = "BearerAuth")
@SecurityRequirement(name = "WorkspaceContext")
class WorkspaceModuleController(
    private val workspaceModuleService: WorkspaceModuleService
) {

    @GetMapping
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getModules(): ResponseEntity<ApiResponse<List<InstalledModuleResponse>>> {
        val result = workspaceModuleService.getInstalledModules()
        return ResponseEntity.ok(ApiResponse.success(result))
    }

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