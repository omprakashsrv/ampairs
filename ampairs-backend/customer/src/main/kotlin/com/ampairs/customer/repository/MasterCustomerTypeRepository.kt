package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.MasterCustomerType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for managing master customer type registry.
 * Provides methods for querying and managing the central customer type catalog.
 */
@Repository
interface MasterCustomerTypeRepository : JpaRepository<MasterCustomerType, String>, JpaSpecificationExecutor<MasterCustomerType> {

    /**
     * Find customer type by unique code
     */
    fun findByTypeCode(typeCode: String): MasterCustomerType?

    /**
     * Find all active customer types
     */
    fun findByActiveTrueOrderByDisplayOrderAscNameAsc(): List<MasterCustomerType>

    /**
     * Find all active customer types by name
     */
    fun findByActiveTrueOrderByNameAsc(): List<MasterCustomerType>

    /**
     * Search active customer types by name or description
     */
    @Query("""
        SELECT mct FROM MasterCustomerType mct
        WHERE mct.active = true
        AND (LOWER(mct.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
             OR LOWER(mct.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        ORDER BY mct.displayOrder ASC, mct.name ASC
    """)
    fun searchActiveCustomerTypes(@Param("searchTerm") searchTerm: String): List<MasterCustomerType>

    /**
     * Find customer types that allow credit
     */
    fun findByActiveTrueAndDefaultCreditLimitGreaterThanOrderByDisplayOrderAscNameAsc(creditLimit: Double = 0.0): List<MasterCustomerType>

    /**
     * Check if customer type code exists
     */
    fun existsByTypeCode(typeCode: String): Boolean
}