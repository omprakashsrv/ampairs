package com.ampairs.customer.domain.service

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
    fun importStateToWorkspace(stateCode: String, workspaceId: String): State? {
        val masterState = masterStateRepository.findByStateCode(stateCode)
            ?: return null

        // Check if already imported
        val existingState = stateRepository.findByMasterStateCodeAndOwnerId(stateCode, workspaceId)
        if (existingState != null) {
            logger.info("State {} already imported to workspace {}", stateCode, workspaceId)
            return existingState
        }

        // Create new workspace state
        val workspaceState = State().apply {
            importFromMasterState(masterState)
            ownerId = workspaceId
        }

        val savedState = stateRepository.save(workspaceState)
        logger.info("Imported state {} to workspace {}", stateCode, workspaceId)

        return savedState
    }

    /**
     * Bulk import multiple states to workspace
     */
    @Transactional
    fun importStatesToWorkspace(stateCodes: List<String>, workspaceId: String): List<State> {
        val importedStates = mutableListOf<State>()

        stateCodes.forEach { stateCode ->
            importStateToWorkspace(stateCode, workspaceId)?.let { state ->
                importedStates.add(state)
            }
        }

        logger.info("Bulk imported {} states to workspace {}", importedStates.size, workspaceId)
        return importedStates
    }

    /**
     * Get states available for import (not yet imported to workspace)
     */
    @Transactional(readOnly = true)
    fun getAvailableStatesForImport(workspaceId: String): List<MasterState> {
        val importedStateCodes = stateRepository.findByOwnerId(workspaceId)
            .mapNotNull { it.masterStateCode }
            .toSet()

        return masterStateRepository.findByActiveTrueOrderByNameAsc()
            .filter { !importedStateCodes.contains(it.stateCode) }
    }

    /**
     * Sync workspace state with master state
     */
    @Transactional
    fun syncWorkspaceStateWithMaster(workspaceStateId: String): State? {
        val workspaceState = stateRepository.findByUid(workspaceStateId)
            ?: return null

        val masterState = workspaceState.masterState
            ?: return null

        // Update workspace state with latest master data
        workspaceState.importFromMasterState(masterState)

        val updatedState = stateRepository.save(workspaceState)
        logger.info("Synced workspace state {} with master state {}", workspaceStateId, masterState.stateCode)

        return updatedState
    }

    /**
     * Get states that need syncing (out of date with master)
     */
    @Transactional(readOnly = true)
    fun getStatesNeedingSync(workspaceId: String): List<State> {
        return stateRepository.findByOwnerId(workspaceId)
            .filter { !it.isSyncedWithMaster() }
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

    /**
     * Create or update master state
     */
    @Transactional
    fun saveOrUpdateMasterState(masterState: MasterState): MasterState {
        val existing = masterStateRepository.findByStateCode(masterState.stateCode)

        return if (existing != null) {
            // Update existing
            existing.apply {
                name = masterState.name
                shortName = masterState.shortName
                countryCode = masterState.countryCode
                countryName = masterState.countryName
                region = masterState.region
                timezone = masterState.timezone
                localName = masterState.localName
                capital = masterState.capital
                population = masterState.population
                areaSqKm = masterState.areaSqKm
                gstCode = masterState.gstCode
                postalCodePattern = masterState.postalCodePattern
                active = masterState.active
                metadata = masterState.metadata
            }
            masterStateRepository.save(existing)
        } else {
            // Create new
            masterStateRepository.save(masterState)
        }
    }

    /**
     * Deactivate master state and handle workspace implications
     */
    @Transactional
    fun deactivateMasterState(stateCode: String): Boolean {
        val masterState = masterStateRepository.findByStateCode(stateCode)
            ?: return false

        masterState.active = false
        masterStateRepository.save(masterState)

        // Optionally handle workspace states that reference this master state
        // For now, we'll just log the impact
        val affectedWorkspaceStates = stateRepository.findByMasterStateCode(stateCode)
        logger.warn("Deactivated master state {} affects {} workspace states",
                   stateCode, affectedWorkspaceStates.size)

        return true
    }
}