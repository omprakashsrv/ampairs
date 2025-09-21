package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.customer.domain.model.MasterState
import com.ampairs.customer.domain.service.MasterStateService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
     * Get all active master states
     */
    @GetMapping("")
    fun getAllStates(): ApiResponse<List<MasterState>> {
        val states = masterStateService.getAllActiveStates()
        return ApiResponse.success(states)
    }

    /**
     * Get states by country
     */
    @GetMapping("/country/{countryCode}")
    fun getStatesByCountry(@PathVariable countryCode: String): ApiResponse<List<MasterState>> {
        val states = masterStateService.getStatesByCountry(countryCode.uppercase())
        return ApiResponse.success(states)
    }


    /**
     * Search states by keyword
     */
    @GetMapping("/search")
    fun searchStates(@RequestParam("q") searchTerm: String): ApiResponse<List<MasterState>> {
        val states = masterStateService.searchStates(searchTerm)
        return ApiResponse.success(states)
    }

    /**
     * Get Indian states with GST codes
     */
    @GetMapping("/indian-gst")
    fun getIndianStatesWithGst(): ApiResponse<List<MasterState>> {
        val states = masterStateService.getIndianStatesWithGst()
        return ApiResponse.success(states)
    }

    /**
     * Get available countries
     */
    @GetMapping("/countries")
    fun getAvailableCountries(): ApiResponse<List<Map<String, String>>> {
        val countries = masterStateService.getAvailableCountries()
            .map { mapOf("code" to it.first, "name" to it.second) }
        return ApiResponse.success(countries)
    }

    /**
     * Get master state by code
     */
    @GetMapping("/{stateCode}")
    fun getStateByCode(@PathVariable stateCode: String): ApiResponse<MasterState> {
        val state = masterStateService.findByStateCode(stateCode.uppercase())
            ?: return ApiResponse.error("Master state not found", "MASTER_STATE_NOT_FOUND")

        return ApiResponse.success(state)
    }

    /**
     * Import master state to workspace
     */
    @PostMapping("/{stateCode}/import")
    fun importStateToWorkspace(
        @PathVariable stateCode: String,
    ): ApiResponse<String> {
        val importedState = masterStateService.importStateToWorkspace(stateCode.uppercase())
            ?: return ApiResponse.error("Failed to import state or state not found", "IMPORT_FAILED")

        return ApiResponse.success("State imported successfully with ID: ${importedState.uid}")
    }

    /**
     * Bulk import multiple states to workspace
     */
    @PostMapping("/bulk-import")
    fun bulkImportStates(
        @RequestBody request: BulkImportRequest
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
    fun getAvailableStatesForImport(
        @RequestParam("workspace_id") workspaceId: String
    ): ApiResponse<List<MasterState>> {
        val states = masterStateService.getAvailableStatesForImport(workspaceId)
        return ApiResponse.success(states)
    }

    /**
     * Find states by postal code
     */
    @GetMapping("/by-postal-code")
    fun findStatesByPostalCode(@RequestParam("postal_code") postalCode: String): ApiResponse<List<MasterState>> {
        val states = masterStateService.findStatesByPostalCode(postalCode)
        return ApiResponse.success(states)
    }

    /**
     * Get master state statistics
     */
    @GetMapping("/statistics")
    fun getMasterStateStatistics(): ApiResponse<Map<String, Any>> {
        val stats = masterStateService.getMasterStateStatistics()
        return ApiResponse.success(stats)
    }

}

/**
 * Request DTO for bulk import
 */
data class BulkImportRequest(
    val stateCodes: List<String>,
)