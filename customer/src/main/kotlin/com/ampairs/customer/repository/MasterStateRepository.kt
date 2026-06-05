package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.MasterState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
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
     * Find active states ordered by name
     */
    fun findByActiveTrueOrderByNameAsc(): List<MasterState>
}
