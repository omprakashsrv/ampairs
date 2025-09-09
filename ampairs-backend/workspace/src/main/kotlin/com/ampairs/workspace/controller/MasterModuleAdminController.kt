package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.model.enums.*
import com.ampairs.workspace.service.MasterModuleAdminService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Super Admin controller for managing Master Modules.
 * Only accessible to users with SUPER_ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/v1/master-modules")
@Tag(name = "Master Module Administration", description = "Super Admin operations for managing the master module catalog")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@superAdminAuth.isSuperAdmin(authentication)")
class MasterModuleAdminController(
    private val masterModuleAdminService: MasterModuleAdminService
) {
    
    private val logger = LoggerFactory.getLogger(MasterModuleAdminController::class.java)
    
    @Operation(
        summary = "Create a new master module",
        description = "Creates a new module in the master catalog that can be installed by workspaces"
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "201", description = "Module created successfully"),
        SwaggerApiResponse(responseCode = "400", description = "Invalid request data"),
        SwaggerApiResponse(responseCode = "409", description = "Module code already exists"),
        SwaggerApiResponse(responseCode = "403", description = "Access denied - Super Admin required")
    )
    @PostMapping
    fun createMasterModule(
        @Valid @RequestBody request: MasterModuleCreateRequest
    ): ResponseEntity<ApiResponse<MasterModuleAdminResponse>> {
        
        logger.info("Super Admin creating master module: {}", request.moduleCode)
        
        val response = masterModuleAdminService.createMasterModule(request)
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(data = response)
        )
    }
    
    @Operation(
        summary = "Update an existing master module",
        description = "Updates module information in the master catalog"
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "Module updated successfully"),
        SwaggerApiResponse(responseCode = "400", description = "Invalid request data"),
        SwaggerApiResponse(responseCode = "404", description = "Module not found"),
        SwaggerApiResponse(responseCode = "403", description = "Access denied - Super Admin required")
    )
    @PutMapping("/{id}")
    fun updateMasterModule(
        @Parameter(description = "Master module ID")
        @PathVariable id: String,
        @Valid @RequestBody request: MasterModuleUpdateRequest
    ): ResponseEntity<ApiResponse<MasterModuleAdminResponse>> {
        
        logger.info("Super Admin updating master module: {}", id)
        
        val response = masterModuleAdminService.updateMasterModule(id, request)
        
        return ResponseEntity.ok(
            ApiResponse.success(data = response)
        )
    }
    
    @Operation(
        summary = "Get master module by ID",
        description = "Retrieves detailed information about a specific master module"
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "Module found"),
        SwaggerApiResponse(responseCode = "404", description = "Module not found"),
        SwaggerApiResponse(responseCode = "403", description = "Access denied - Super Admin required")
    )
    @GetMapping("/{id}")
    fun getMasterModule(
        @Parameter(description = "Master module ID")
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<MasterModuleAdminResponse>> {
        
        val response = masterModuleAdminService.getMasterModule(id)
        
        return ResponseEntity.ok(
            ApiResponse.success(data = response)
        )
    }
    
    @Operation(
        summary = "Get master module by code",
        description = "Retrieves detailed information about a specific master module by its unique code"
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "Module found"),
        SwaggerApiResponse(responseCode = "404", description = "Module not found"),
        SwaggerApiResponse(responseCode = "403", description = "Access denied - Super Admin required")
    )
    @GetMapping("/code/{moduleCode}")
    fun getMasterModuleByCode(
        @Parameter(description = "Master module code")
        @PathVariable moduleCode: String
    ): ResponseEntity<ApiResponse<MasterModuleAdminResponse>> {
        
        val response = masterModuleAdminService.getMasterModuleByCode(moduleCode)
        
        return ResponseEntity.ok(
            ApiResponse.success(data = response)
        )
    }
    
    @Operation(
        summary = "Get all master modules",
        description = "Retrieves a paginated list of master modules with optional filtering"
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "Modules retrieved successfully"),
        SwaggerApiResponse(responseCode = "403", description = "Access denied - Super Admin required")
    )
    @GetMapping
    fun getAllMasterModules(
        @Parameter(description = "Filter by category")
        @RequestParam(required = false) category: ModuleCategory?,
        
        @Parameter(description = "Filter by status")
        @RequestParam(required = false) status: ModuleStatus?,
        
        @Parameter(description = "Filter by complexity")
        @RequestParam(required = false) complexity: ModuleComplexity?,
        
        @Parameter(description = "Filter by required tier")
        @RequestParam(required = false) requiredTier: SubscriptionTier?,
        
        @Parameter(description = "Filter by featured status")
        @RequestParam(required = false) featured: Boolean?,
        
        @Parameter(description = "Filter by active status")
        @RequestParam(required = false) active: Boolean?,
        
        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") page: Int,
        
        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int,
        
        @Parameter(description = "Sort field")
        @RequestParam(defaultValue = "displayOrder") sortBy: String,
        
        @Parameter(description = "Sort direction")
        @RequestParam(defaultValue = "ASC") sortDirection: String
    ): ResponseEntity<ApiResponse<Page<MasterModuleAdminListResponse>>> {
        
        val pageable = PageRequest.of(
            page, 
            size, 
            Sort.Direction.valueOf(sortDirection.uppercase()), 
            sortBy
        )
        
        val response = masterModuleAdminService.getAllMasterModules(
            category = category,
            status = status,
            complexity = complexity,
            requiredTier = requiredTier,
            featured = featured,
            active = active,
            pageable = pageable
        )
        
        return ResponseEntity.ok(
            ApiResponse.success(data = response)
        )
    }
    
    @Operation(
        summary = "Search master modules",
        description = "Search modules by name, description, or tags"
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "Search completed successfully"),
        SwaggerApiResponse(responseCode = "403", description = "Access denied - Super Admin required")
    )
    @GetMapping("/search")
    fun searchMasterModules(
        @Parameter(description = "Search keyword")
        @RequestParam keyword: String
    ): ResponseEntity<ApiResponse<List<MasterModuleAdminListResponse>>> {
        
        val response = masterModuleAdminService.searchMasterModules(keyword)
        
        return ResponseEntity.ok(
            ApiResponse.success(data = response)
        )
    }
    
    @Operation(
        summary = "Delete a master module",
        description = "Permanently removes a module from the master catalog (only if not installed anywhere)"
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "Module deleted successfully"),
        SwaggerApiResponse(responseCode = "404", description = "Module not found"),
        SwaggerApiResponse(responseCode = "409", description = "Cannot delete - module is in use"),
        SwaggerApiResponse(responseCode = "403", description = "Access denied - Super Admin required")
    )
    @DeleteMapping("/{id}")
    fun deleteMasterModule(
        @Parameter(description = "Master module ID")
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Unit>> {
        
        logger.warn("Super Admin deleting master module: {}", id)
        
        masterModuleAdminService.deleteMasterModule(id)
        
        return ResponseEntity.ok(
            ApiResponse.success(data = Unit)
        )
    }
    
    @Operation(
        summary = "Bulk update module status",
        description = "Updates the status of multiple modules at once"
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "Modules updated successfully"),
        SwaggerApiResponse(responseCode = "400", description = "Invalid request data"),
        SwaggerApiResponse(responseCode = "403", description = "Access denied - Super Admin required")
    )
    @PatchMapping("/bulk/status")
    fun bulkUpdateStatus(
        @Parameter(description = "List of module IDs to update")
        @RequestParam moduleIds: List<String>,
        
        @Parameter(description = "New status to apply")
        @RequestParam status: ModuleStatus
    ): ResponseEntity<ApiResponse<List<MasterModuleAdminResponse>>> {
        
        logger.info("Super Admin bulk updating status to {} for {} modules", status, moduleIds.size)
        
        val response = masterModuleAdminService.bulkUpdateStatus(moduleIds, status)
        
        return ResponseEntity.ok(
            ApiResponse.success(data = response)
        )
    }
    
    @Operation(
        summary = "Update display order",
        description = "Updates the display order for multiple modules"
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "Display order updated successfully"),
        SwaggerApiResponse(responseCode = "400", description = "Invalid request data"),
        SwaggerApiResponse(responseCode = "403", description = "Access denied - Super Admin required")
    )
    @PatchMapping("/display-order")
    fun updateDisplayOrder(
        @Parameter(description = "List of module ID and display order pairs")
        @RequestBody updates: List<DisplayOrderUpdateRequest>
    ): ResponseEntity<ApiResponse<List<MasterModuleAdminResponse>>> {
        
        logger.info("Super Admin updating display order for {} modules", updates.size)
        
        val updatePairs = updates.map { it.moduleId to it.displayOrder }
        val response = masterModuleAdminService.updateDisplayOrder(updatePairs)
        
        return ResponseEntity.ok(
            ApiResponse.success(data = response)
        )
    }
    
    @Operation(
        summary = "Get module statistics",
        description = "Returns statistics about the master module catalog"
    )
    @ApiResponses(
        SwaggerApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        SwaggerApiResponse(responseCode = "403", description = "Access denied - Super Admin required")
    )
    @GetMapping("/statistics")
    fun getModuleStatistics(): ResponseEntity<ApiResponse<Map<String, Any>>> {
        
        val response = masterModuleAdminService.getModuleStatistics()
        
        return ResponseEntity.ok(
            ApiResponse.success(data = response)
        )
    }
}

/**
 * Request DTO for display order updates
 */
data class DisplayOrderUpdateRequest(
    val moduleId: String,
    val displayOrder: Int
)