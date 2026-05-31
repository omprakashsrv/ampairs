package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.exception.NotFoundException
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.service.WorkspaceModuleService
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/workspace/v1/modules")
@SecurityRequirement(name = "BearerAuth")
@SecurityRequirement(name = "WorkspaceContext")
class WorkspaceModuleController(
    private val workspaceModuleService: WorkspaceModuleService
) {

    @GetMapping
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getModules(): ApiResponse<List<InstalledModuleResponse>> {
        return ApiResponse.success(workspaceModuleService.getInstalledModules())
    }

    @GetMapping("/{moduleCode}")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getModule(@PathVariable moduleCode: String): ApiResponse<ModuleDetailResponse> {
        return ApiResponse.success(
            workspaceModuleService.getModuleInfo(moduleCode)
                ?: throw NotFoundException("Module not found: $moduleCode")
        )
    }

    @GetMapping("/available")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getAvailableModules(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false, defaultValue = "false") featured: Boolean,
    ): ApiResponse<List<AvailableModuleResponse>> {
        return ApiResponse.success(workspaceModuleService.getAvailableModules(category, featured))
    }

    @GetMapping("/catalog")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantMember(authentication)")
    fun getModuleCatalog(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false, defaultValue = "false") includeDisabled: Boolean,
    ): ApiResponse<ModuleCatalogResponse> {
        return ApiResponse.success(workspaceModuleService.getModuleCatalog(category, includeDisabled))
    }

    @PostMapping("/install/{moduleCode}")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantAdmin(authentication) || @workspaceAuthorizationService.isCurrentTenantOwner(authentication)")
    fun installModule(@PathVariable moduleCode: String): ApiResponse<ModuleInstallationResponse> {
        val result = workspaceModuleService.installModule(
            moduleCode = moduleCode,
            installedBy = null,
            installedByName = null
        )
        if (!result.success) throw IllegalArgumentException(result.message)
        return ApiResponse.success(result)
    }

    @DeleteMapping("/{moduleCode}")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantAdmin(authentication) || @workspaceAuthorizationService.isCurrentTenantOwner(authentication)")
    fun uninstallModule(@PathVariable moduleCode: String): ApiResponse<ModuleUninstallationResponse> {
        val result = workspaceModuleService.uninstallModule(moduleCode)
        if (!result.success) throw IllegalArgumentException(result.message)
        return ApiResponse.success(result)
    }

    @PatchMapping("/reorder")
    @PreAuthorize("@workspaceAuthorizationService.isCurrentTenantAdmin(authentication) || @workspaceAuthorizationService.isCurrentTenantOwner(authentication)")
    fun reorderModules(@RequestBody request: ModuleReorderRequest): ApiResponse<Unit> {
        workspaceModuleService.reorderModules(request.orders)
        return ApiResponse.success(Unit)
    }
}
