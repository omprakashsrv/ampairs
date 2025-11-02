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

    /**
     * Find active modules by category
     */
    fun findByActiveTrueAndCategory(category: ModuleCategory): List<MasterModule>

    /**
     * Find active modules ordered by display order
     */
    fun findByActiveTrueOrderByDisplayOrderAsc(): List<MasterModule>

    /**
     * Find featured modules
     */
    fun findByActiveTrueAndFeaturedTrueOrderByDisplayOrderAsc(): List<MasterModule>

    /**
     * Find modules by status
     */
    fun findByStatusOrderByDisplayOrderAsc(status: ModuleStatus): List<MasterModule>

    /**
     * Find modules available for subscription tier
     */
    @Query("SELECT m FROM MasterModule m WHERE m.active = true AND m.requiredTier <= :tier ORDER BY m.displayOrder ASC")
    fun findByActiveTrueAndRequiredTierLessThanEqualOrderByDisplayOrderAsc(@Param("tier") tier: SubscriptionTier): List<MasterModule>

    /**
     * Search modules by name or description
     */
    fun findByActiveTrueAndNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByDisplayOrderAsc(
        nameKeyword: String,
        descriptionKeyword: String,
    ): List<MasterModule>

    /**
     * Find modules by complexity level
     */
    fun findByActiveTrueAndComplexity(complexity: ModuleComplexity): List<MasterModule>

    /**
     * Find modules by category with pagination
     */
    fun findByActiveTrueAndCategoryOrderByDisplayOrderAsc(
        category: ModuleCategory,
        pageable: Pageable,
    ): Page<MasterModule>

    /**
     * Find most popular modules (high install count and rating)
     */
    @Query("SELECT m FROM MasterModule m WHERE m.active = true AND m.installCount > :minInstalls AND m.rating >= :minRating ORDER BY m.installCount DESC, m.rating DESC")
    fun findPopularModules(
        @Param("minInstalls") minInstalls: Int,
        @Param("minRating") minRating: Double,
        pageable: Pageable,
    ): Page<MasterModule>

    /**
     * Find modules by tags or keywords (search in UI metadata)
     * Note: Full-text search moved to service layer due to JSON field limitations
     */
    fun findByActiveTrueAndNameContainingIgnoreCase(searchTerm: String): List<MasterModule>

    /**
     * Count modules by category
     */
    fun countByActiveTrueAndCategory(category: ModuleCategory): Long

    /**
     * Get module provider statistics
     */
    fun countByActiveTrueAndProvider(provider: String): Long

    /**
     * Find modules that exist in master registry by codes
     */
    fun findByModuleCodeIn(moduleCodes: List<String>): List<MasterModule>
}