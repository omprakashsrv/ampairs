package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.exception.NotFoundException
import com.ampairs.customer.domain.dto.BulkImportRequest
import com.ampairs.customer.domain.dto.MasterStateResponse
import com.ampairs.customer.domain.dto.asMasterStateResponses
import com.ampairs.customer.domain.service.MasterStateService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/**
 * Controller for managing master states catalog and workspace imports
 */
@RestController
@RequestMapping("/customer/v1/master-states")
class MasterStateController(
    private val masterStateService: MasterStateService
) {

    /**
     * Import master state to workspace
     */
    @PostMapping("/{stateCode}/import")
    fun importStateToWorkspace(
        @PathVariable stateCode: String,
    ): ApiResponse<String> {
        val importedState = masterStateService.importStateToWorkspace(stateCode.uppercase())
            ?: throw NotFoundException("State not found: $stateCode")
        return ApiResponse.success("State imported successfully with ID: ${importedState.uid}")
    }

    /**
     * Bulk import multiple states to workspace
     */
    @PostMapping("/bulk-import")
    fun bulkImportStates(
        @Valid @RequestBody request: BulkImportRequest
    ): ApiResponse<Map<String, Any>> {
        val importedStates = masterStateService.importStatesToWorkspace(
            request.stateCodes.map { it.uppercase() },
        )

        val response = mapOf(
            "imported_count" to importedStates.size,
            "imported_states" to importedStates.map { mapOf(
                "uid" to it.uid,
                "name" to it.name,
                "master_state_code" to it.masterStateCode
            )}
        )

        return ApiResponse.success(response)
    }

    /**
     * Get states available for import to workspace
     */
    @GetMapping("/available-for-import")
    fun getAvailableStatesForImport(): ApiResponse<List<MasterStateResponse>> {
        // Remove hardcoded workspace_id parameter - use tenant context instead
        val states = masterStateService.getAvailableStatesForImport()
        return ApiResponse.success(states.asMasterStateResponses())
    }
}
