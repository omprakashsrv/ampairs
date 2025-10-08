package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.MasterState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for managing master state registry.
 * Provides methods for querying and managing the central state catalog.
 */
@Repository
interface MasterStateRepository : JpaRepository<MasterState, String>, JpaSpecificationExecutor<MasterState> {

    /**
     * Find state by unique code
     */
    fun findByStateCode(stateCode: String): MasterState?

    /**
     * Find all active states
     */
    fun findByActiveTrue(): List<MasterState>

    /**
     * Find active states by country
     */
    fun findByActiveTrueAndCountryCode(countryCode: String): List<MasterState>

    /**
     * Find active states ordered by name
     */
    fun findByActiveTrueOrderByNameAsc(): List<MasterState>

    /**
     * Search states by name or country
     */
    @Query(
        """
        SELECT m FROM MasterState m
        WHERE m.active = true
        AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
             OR LOWER(m.countryName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
             OR LOWER(m.shortName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        ORDER BY m.name ASC
    """
    )
    fun searchActiveStates(@Param("searchTerm") searchTerm: String): List<MasterState>

    /**
     * Count states by country
     */
    fun countByActiveTrueAndCountryCode(countryCode: String): Long

    /**
     * Get all available countries
     */
    @Query("SELECT DISTINCT m.countryCode, m.countryName FROM MasterState m WHERE m.active = true ORDER BY m.countryName")
    fun findDistinctCountries(): List<Array<String>>

    /**
     * Find states with GST codes (Indian states with GST setup)
     */
    @Query("SELECT m FROM MasterState m WHERE m.active = true AND m.gstCode IS NOT NULL ORDER BY m.gstCode ASC")
    fun findStatesWithGstCodes(): List<MasterState>

    /**
     * Find states with postal code patterns (pattern matching moved to service layer)
     */
    @Query("SELECT m FROM MasterState m WHERE m.active = true AND m.postalCodePattern IS NOT NULL")
    fun findStatesWithPostalCodePatterns(): List<MasterState>


}