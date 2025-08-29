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
     * Find active modules by status
     */
    fun findByActiveTrueAndStatus(status: ModuleStatus): List<MasterModule>

    /**
     * Find active modules by category
     */
    fun findByActiveTrueAndCategory(category: ModuleCategory): List<MasterModule>

    /**
     * Find modules by subscription tier or lower
     */
    @Query("""
        SELECT m FROM MasterModule m 
        WHERE m.active = true 
        AND m.requiredTier IN :allowedTiers
        ORDER BY m.displayOrder ASC
    """)
    fun findBySubscriptionTier(@Param("allowedTiers") allowedTiers: List<SubscriptionTier>): List<MasterModule>

    /**
     * Find modules relevant for business type
     */
    @Query("""
        SELECT m FROM MasterModule m 
        WHERE m.active = true 
        AND JSON_SEARCH(m.businessRelevance, 'one', :businessType, NULL, '$[*].businessType') IS NOT NULL
        ORDER BY JSON_EXTRACT(m.businessRelevance, '$[?(@.businessType == :businessType)].relevanceScore') DESC
    """)
    fun findByBusinessType(@Param("businessType") businessType: String): List<MasterModule>

    /**
     * Find essential modules for business type
     */
    @Query("""
        SELECT m FROM MasterModule m 
        WHERE m.active = true 
        AND JSON_EXTRACT(m.businessRelevance, '$[?(@.businessType == :businessType && @.isEssential == true)]') IS NOT NULL
        ORDER BY m.displayOrder ASC
    """)
    fun findEssentialForBusinessType(@Param("businessType") businessType: String): List<MasterModule>

    /**
     * Find featured modules
     */
    fun findByActiveTrueAndFeaturedTrueOrderByDisplayOrderAsc(): List<MasterModule>

    /**
     * Find modules by complexity level
     */
    fun findByActiveTrueAndComplexity(complexity: ModuleComplexity): List<MasterModule>

    /**
     * Find modules by multiple categories
     */
    fun findByActiveTrueAndCategoryInOrderByDisplayOrderAsc(categories: List<ModuleCategory>): List<MasterModule>

    /**
     * Search modules by name or description
     */
    @Query("""
        SELECT m FROM MasterModule m 
        WHERE m.active = true 
        AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) 
             OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(m.tagline) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(m.moduleCode) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY m.featured DESC, m.rating DESC
    """)
    fun searchByQuery(@Param("query") query: String): List<MasterModule>

    /**
     * Find modules by provider
     */
    fun findByActiveTrueAndProviderIgnoreCase(provider: String): List<MasterModule>

    /**
     * Find modules that have no dependencies
     */
    @Query("""
        SELECT * FROM master_modules m 
        WHERE m.active = true 
        AND JSON_LENGTH(JSON_EXTRACT(m.configuration, '$.dependencies')) = 0
    """, nativeQuery = true
    )
    fun findWithNoDependencies(): List<MasterModule>

    /**
     * Find modules with dependencies
     */
    @Query("""
        SELECT * FROM master_modules m 
        WHERE m.active = true 
        AND JSON_LENGTH(JSON_EXTRACT(m.configuration, '$.dependencies')) > 0
    """, nativeQuery = true
    )
    fun findWithDependencies(): List<MasterModule>

    /**
     * Find modules by multiple codes
     */
    fun findByModuleCodeIn(moduleCodes: List<String>): List<MasterModule>

    /**
     * Check if module code exists
     */
    fun existsByModuleCode(moduleCode: String): Boolean

    /**
     * Count modules by category
     */
    fun countByActiveTrueAndCategory(category: ModuleCategory): Long

    /**
     * Count modules by status
     */
    fun countByStatus(status: ModuleStatus): Long

    /**
     * Find most installed modules
     */
    fun findByActiveTrueOrderByInstallCountDesc(pageable: Pageable): Page<MasterModule>

    /**
     * Find top-rated modules
     */
    @Query("""
        SELECT m FROM MasterModule m 
        WHERE m.active = true 
        AND m.rating >= :minRating 
        AND m.ratingCount >= :minRatingCount 
        ORDER BY m.rating DESC, m.ratingCount DESC
    """)
    fun findTopRatedModules(
        @Param("minRating") minRating: Double,
        @Param("minRatingCount") minRatingCount: Int
    ): List<MasterModule>

    /**
     * Find recently updated modules
     */
    @Query("""
        SELECT m FROM MasterModule m 
        WHERE m.active = true 
        AND m.lastUpdatedAt IS NOT NULL
        ORDER BY m.lastUpdatedAt DESC
    """)
    fun findRecentlyUpdated(pageable: Pageable): Page<MasterModule>

    /**
     * Advanced search with multiple filters
     */
    @Query("""
        SELECT m FROM MasterModule m 
        WHERE m.active = true 
        AND (:category IS NULL OR m.category = :category)
        AND (:status IS NULL OR m.status = :status)
        AND (:requiredTier IS NULL OR m.requiredTier = :requiredTier)
        AND (:complexity IS NULL OR m.complexity = :complexity)
        AND (:provider IS NULL OR LOWER(m.provider) LIKE LOWER(CONCAT('%', :provider, '%')))
        AND (:query IS NULL OR 
             LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) OR 
             LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY 
            CASE WHEN m.featured = true THEN 0 ELSE 1 END,
            m.displayOrder ASC,
            m.rating DESC,
            m.installCount DESC
    """)
    fun advancedSearch(
        @Param("category") category: ModuleCategory?,
        @Param("status") status: ModuleStatus?,
        @Param("requiredTier") requiredTier: SubscriptionTier?,
        @Param("complexity") complexity: ModuleComplexity?,
        @Param("provider") provider: String?,
        @Param("query") query: String?,
        pageable: Pageable
    ): Page<MasterModule>

    /**
     * Find modules suitable for workspace (by subscription and business type)
     */
    @Query("""
        SELECT m FROM MasterModule m 
        WHERE m.active = true 
        AND m.requiredTier IN :allowedTiers
        AND (:businessType IS NULL OR 
             JSON_SEARCH(m.businessRelevance, 'one', :businessType, NULL, '$[*].businessType') IS NOT NULL)
        ORDER BY 
            m.featured DESC,
            CASE WHEN :businessType IS NOT NULL 
                 THEN JSON_EXTRACT(m.businessRelevance, '$[?(@.businessType == :businessType)].relevanceScore') 
                 ELSE m.rating END DESC,
            m.displayOrder ASC
    """)
    fun findSuitableForWorkspace(
        @Param("allowedTiers") allowedTiers: List<SubscriptionTier>,
        @Param("businessType") businessType: String?
    ): List<MasterModule>

    /**
     * Get module statistics
     */
    @Query("""
        SELECT new map(
            COUNT(*) as totalModules,
            COUNT(CASE WHEN m.status = 'ACTIVE' THEN 1 END) as activeModules,
            COUNT(CASE WHEN m.featured = true THEN 1 END) as featuredModules,
            AVG(m.rating) as averageRating,
            SUM(m.installCount) as totalInstalls
        )
        FROM MasterModule m 
        WHERE m.active = true
    """)
    fun getModuleStatistics(): Map<String, Any>

    /**
     * Find modules by size range
     */
    @Query("""
        SELECT m FROM MasterModule m 
        WHERE m.active = true 
        AND m.sizeMb BETWEEN :minSize AND :maxSize
    """)
    fun findBySizeRange(
        @Param("minSize") minSize: Int,
        @Param("maxSize") maxSize: Int
    ): List<MasterModule>

    /**
     * Find recommended modules for business type and subscription
     */
    @Query("""
        SELECT m FROM MasterModule m 
        WHERE m.active = true 
        AND m.requiredTier IN :allowedTiers
        AND JSON_EXTRACT(m.businessRelevance, '$[?(@.businessType == :businessType)].relevanceScore') >= :minRelevanceScore
        AND m.moduleCode NOT IN :excludeModules
        ORDER BY 
            JSON_EXTRACT(m.businessRelevance, '$[?(@.businessType == :businessType)].relevanceScore') DESC,
            m.rating DESC,
            m.installCount DESC
    """)
    fun findRecommendedModules(
        @Param("businessType") businessType: String,
        @Param("allowedTiers") allowedTiers: List<SubscriptionTier>,
        @Param("minRelevanceScore") minRelevanceScore: Int,
        @Param("excludeModules") excludeModules: List<String>,
        pageable: Pageable
    ): Page<MasterModule>

    /**
     * Find modules that depend on a specific module
     */
    @Query("""
        SELECT * FROM master_modules m 
        WHERE m.active = true 
        AND JSON_CONTAINS(JSON_EXTRACT(m.configuration, '$.dependencies'), JSON_QUOTE(:moduleCode))
    """, nativeQuery = true
    )
    fun findModulesWithDependency(@Param("moduleCode") moduleCode: String): List<MasterModule>

    /**
     * Find modules that conflict with a specific module
     */
    @Query("""
        SELECT * FROM master_modules m 
        WHERE m.active = true 
        AND JSON_CONTAINS(JSON_EXTRACT(m.configuration, '$.conflictsWith'), JSON_QUOTE(:moduleCode))
    """, nativeQuery = true
    )
    fun findModulesWithConflict(@Param("moduleCode") moduleCode: String): List<MasterModule>
}