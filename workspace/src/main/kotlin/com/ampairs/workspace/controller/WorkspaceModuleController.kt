package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.service.WorkspaceModuleService
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

    @GetMapping("/{moduleCode}")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getModule(
        @PathVariable moduleCode: String,
    ): ResponseEntity<ApiResponse<ModuleDetailResponse>> {
        val result = workspaceModuleService.getModuleInfo(moduleCode)
        return if (result != null) {
            ResponseEntity.ok(ApiResponse.success(result))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/available")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getAvailableModules(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false, defaultValue = "false") featured: Boolean,
    ): ResponseEntity<ApiResponse<List<AvailableModuleResponse>>> {
        val result = workspaceModuleService.getAvailableModules(category, featured)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/catalog")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getModuleCatalog(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false, defaultValue = "false") includeDisabled: Boolean,
    ): ResponseEntity<ApiResponse<ModuleCatalogResponse>> {
        val result = workspaceModuleService.getModuleCatalog(category, includeDisabled)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @PostMapping("/install/{moduleCode}")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantAdmin(authentication) || @workspaceAuthorizationService.isCurrentTenantOwner(authentication)")
    fun installModule(
        @PathVariable moduleCode: String,
    ): ResponseEntity<ApiResponse<ModuleInstallationResponse>> {
        val result = workspaceModuleService.installModule(
            moduleCode = moduleCode,
            installedBy = null, // Could be extracted from authentication if needed
            installedByName = null // Could be extracted from authentication if needed
        )
        return if (result.success) {
            ResponseEntity.ok(ApiResponse.success(result))
        } else {
            ResponseEntity.badRequest().body(ApiResponse.error(ErrorCodes.BAD_REQUEST, result.message))
        }
    }

    @DeleteMapping("/{moduleCode}")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantAdmin(authentication) || @workspaceAuthorizationService.isCurrentTenantOwner(authentication)")
    fun uninstallModule(
        @PathVariable moduleCode: String,
    ): ResponseEntity<ApiResponse<ModuleUninstallationResponse>> {
        val result = workspaceModuleService.uninstallModule(moduleCode)
        return if (result.success) {
            ResponseEntity.ok(ApiResponse.success(result))
        } else {
            ResponseEntity.badRequest().body(ApiResponse.error(ErrorCodes.BAD_REQUEST, result.message))
        }
    }
}