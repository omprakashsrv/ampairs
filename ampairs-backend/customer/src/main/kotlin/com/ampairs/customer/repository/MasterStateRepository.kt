package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.MasterState
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
     * Find active states ordered by display order
     */
    fun findByActiveTrueOrderByDisplayOrderAsc(): List<MasterState>

    /**
     * Find featured states (commonly used)
     */
    fun findByActiveTrueAndFeaturedTrueOrderByDisplayOrderAsc(): List<MasterState>

    /**
     * Find states by country with pagination
     */
    fun findByActiveTrueAndCountryCodeOrderByDisplayOrderAsc(
        countryCode: String,
        pageable: Pageable
    ): Page<MasterState>

    /**
     * Search states by name
     */
    fun findByActiveTrueAndNameContainingIgnoreCaseOrderByDisplayOrderAsc(
        nameKeyword: String
    ): List<MasterState>

    /**
     * Find states by region
     */
    fun findByActiveTrueAndRegionOrderByDisplayOrderAsc(region: String): List<MasterState>

    /**
     * Find Indian states (for GST compliance)
     */
    fun findByActiveTrueAndCountryCodeOrderByGstCodeAsc(countryCode: String = "IN"): List<MasterState>

    /**
     * Find states by GST code (Indian states only)
     */
    fun findByActiveTrueAndGstCodeOrderByDisplayOrderAsc(gstCode: String): List<MasterState>

    /**
     * Find states by multiple state codes
     */
    fun findByStateCodeIn(stateCodes: List<String>): List<MasterState>

    /**
     * Search states by name or country
     */
    @Query("""
        SELECT m FROM MasterState m
        WHERE m.active = true
        AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
             OR LOWER(m.countryName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
             OR LOWER(m.shortName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        ORDER BY m.displayOrder ASC
    """)
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
     * Find states by postal code pattern
     */
    @Query("""
        SELECT m FROM MasterState m
        WHERE m.active = true
        AND m.postalCodePattern IS NOT NULL
        AND :postalCode REGEXP m.postalCodePattern
    """)
    fun findByPostalCodePattern(@Param("postalCode") postalCode: String): List<MasterState>

    /**
     * Find popular states (those used in many workspaces)
     */
    @Query("""
        SELECT m, COUNT(s) as usage_count
        FROM MasterState m
        LEFT JOIN m.workspaceStates s
        WHERE m.active = true
        GROUP BY m
        HAVING COUNT(s) >= :minUsage
        ORDER BY COUNT(s) DESC, m.displayOrder ASC
    """)
    fun findPopularStates(@Param("minUsage") minUsage: Long, pageable: Pageable): Page<MasterState>

    /**
     * Find states not used in any workspace
     */
    @Query("""
        SELECT m FROM MasterState m
        WHERE m.active = true
        AND NOT EXISTS (SELECT 1 FROM State s WHERE s.masterState = m)
        ORDER BY m.displayOrder ASC
    """)
    fun findUnusedStates(): List<MasterState>

    /**
     * Batch update featured status
     */
    @Query("UPDATE MasterState m SET m.featured = :featured WHERE m.stateCode IN :stateCodes")
    fun updateFeaturedStatus(@Param("stateCodes") stateCodes: List<String>, @Param("featured") featured: Boolean)
}