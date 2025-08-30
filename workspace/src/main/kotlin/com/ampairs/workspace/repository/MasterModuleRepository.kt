package com.ampairs.workspace.repository

import com.ampairs.workspace.model.MasterModule
import com.ampairs.workspace.model.enums.ModuleCategory
import com.ampairs.workspace.model.enums.ModuleComplexity
import com.ampairs.workspace.model.enums.ModuleStatus
import com.ampairs.workspace.model.enums.SubscriptionTier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for managing master module registry.
 * Provides methods for querying and managing the central module catalog.
 */
@Repository
interface MasterModuleRepository : JpaRepository<MasterModule, String>, JpaSpecificationExecutor<MasterModule> {

    /**
     * Find module by unique code
     */
    fun findByModuleCode(moduleCode: String): MasterModule?

    /**
     * Find all active modules
     */
    fun findByActiveTrue(): List<MasterModule>
}