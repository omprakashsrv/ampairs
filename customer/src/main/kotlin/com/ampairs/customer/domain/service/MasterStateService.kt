package com.ampairs.customer.domain.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.customer.domain.model.MasterState
import com.ampairs.customer.domain.model.State
import com.ampairs.customer.repository.MasterStateRepository
import com.ampairs.customer.repository.StateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing master states and workspace state imports
 */
@Service
@Transactional
class MasterStateService(
    private val masterStateRepository: MasterStateRepository,
    private val stateRepository: StateRepository
) {

    private val logger = LoggerFactory.getLogger(MasterStateService::class.java)

    /**
     * Get all active master states
     */
    @Transactional(readOnly = true)
    fun getAllActiveStates(): List<MasterState> {
        return masterStateRepository.findByActiveTrueOrderByNameAsc()
    }

    /**
     * Get master states by country
     */
    @Transactional(readOnly = true)
    fun getStatesByCountry(countryCode: String): List<MasterState> {
        return masterStateRepository.findByActiveTrueAndCountryCode(countryCode)
    }


    /**
     * Search states by keyword
     */
    @Transactional(readOnly = true)
    fun searchStates(searchTerm: String): List<MasterState> {
        return masterStateRepository.searchActiveStates(searchTerm)
    }

    /**
     * Get Indian states with GST codes
     */
    @Transactional(readOnly = true)
    fun getIndianStatesWithGst(): List<MasterState> {
        return masterStateRepository.findStatesWithGstCodes()
    }

    /**
     * Get available countries
     */
    @Transactional(readOnly = true)
    fun getAvailableCountries(): List<Pair<String, String>> {
        return masterStateRepository.findDistinctCountries()
            .map { Pair(it[0], it[1]) }
    }

    /**
     * Find master state by code
     */
    @Transactional(readOnly = true)
    fun findByStateCode(stateCode: String): MasterState? {
        return masterStateRepository.findByStateCode(stateCode)
    }

    /**
     * Import master state to workspace
     */
    @Transactional
    fun importStateToWorkspace(stateCode: String): State? {
        val masterState = masterStateRepository.findByStateCode(stateCode)
            ?: return null

        // Check if already imported
        val existingState = stateRepository.findFirstByMasterStateCode(stateCode)
        if (existingState != null) {
            logger.info("State {} already imported to workspace", stateCode)
            return existingState
        }

        // Create new workspace state
        val workspaceState = State().apply {
            importFromMasterState(masterState)
        }

        val savedState = stateRepository.save(workspaceState)
        logger.info("Imported state {} to workspace", stateCode)

        return savedState
    }

    /**
     * Bulk import multiple states to workspace
     */
    @Transactional
    fun importStatesToWorkspace(stateCodes: List<String>): List<State> {
        val importedStates = mutableListOf<State>()

        stateCodes.forEach { stateCode ->
            importStateToWorkspace(stateCode)?.let { state ->
                importedStates.add(state)
            }
        }

        logger.info("Bulk imported {} states to workspace", importedStates.size)
        return importedStates
    }

    /**
     * Get states available for import (not yet imported to workspace)
     */
    @Transactional(readOnly = true)
    fun getAvailableStatesForImport(): List<MasterState> {
        val currentTenant = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No tenant context available")

        val importedStateCodes = stateRepository.findByOwnerId(currentTenant)
            .mapNotNull { it.masterStateCode }
            .toSet()

        return masterStateRepository.findByActiveTrueOrderByNameAsc()
            .filter { !importedStateCodes.contains(it.stateCode) }
    }

    /**
     * Find states by postal code (pattern matching in service layer)
     */
    @Transactional(readOnly = true)
    fun findStatesByPostalCode(postalCode: String): List<MasterState> {
        val statesWithPatterns = masterStateRepository.findStatesWithPostalCodePatterns()
        return statesWithPatterns.filter { state ->
            state.isValidPostalCode(postalCode)
        }
    }

    /**
     * Get master state statistics
     */
    @Transactional(readOnly = true)
    fun getMasterStateStatistics(): Map<String, Any> {
        val totalStates = masterStateRepository.count()
        val activeStates = masterStateRepository.findByActiveTrue().size
        val countries = masterStateRepository.findDistinctCountries().size
        val indianStates = masterStateRepository.countByActiveTrueAndCountryCode("IN")

        return mapOf(
            "total_states" to totalStates,
            "active_states" to activeStates,
            "countries" to countries,
            "indian_states" to indianStates,
            "gst_enabled_states" to masterStateRepository.findStatesWithGstCodes().size
        )
    }

}