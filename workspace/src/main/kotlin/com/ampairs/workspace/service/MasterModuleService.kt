package com.ampairs.workspace.service

import com.ampairs.workspace.model.MasterModule
import com.ampairs.workspace.model.enums.*
import com.ampairs.workspace.repository.MasterModuleRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing the master module registry.
 * Handles the central catalog of business modules available in the Ampairs ecosystem.
 */
@Service
@Transactional
class MasterModuleService(
    private val masterModuleRepository: MasterModuleRepository
) {

    /**
     * Get all active modules
     */
    @Transactional(readOnly = true)
    fun getAllActiveModules(): List<MasterModule> {
        return masterModuleRepository.findByActiveTrue()
    }

    /**
     * Get module by code
     */
    @Transactional(readOnly = true)
    fun getModuleByCode(moduleCode: String): MasterModule? {
        return masterModuleRepository.findByModuleCode(moduleCode)
    }

    /**
     * Get modules by category
     */
    @Transactional(readOnly = true)
    fun getModulesByCategory(category: ModuleCategory): List<MasterModule> {
        return masterModuleRepository.findByActiveTrueAndCategory(category)
    }

    /**
     * Get modules available for subscription tier
     */
    @Transactional(readOnly = true)
    fun getModulesForSubscription(subscriptionTier: SubscriptionTier): List<MasterModule> {
        val allowedTiers = SubscriptionTier.values().filter { 
            subscriptionTier.supportsModule(it) 
        }
        return masterModuleRepository.findBySubscriptionTier(allowedTiers)
    }

    /**
     * Get modules relevant for business type
     */
    @Transactional(readOnly = true)
    fun getModulesForBusinessType(businessType: BusinessType): List<MasterModule> {
        return masterModuleRepository.findByBusinessType(businessType.name)
            .sortedByDescending { it.getRelevanceScore(businessType) }
    }

    /**
     * Get essential modules for business type
     */
    @Transactional(readOnly = true)
    fun getEssentialModulesForBusiness(businessType: BusinessType): List<MasterModule> {
        return masterModuleRepository.findEssentialForBusinessType(businessType.name)
    }

    /**
     * Get featured modules
     */
    @Transactional(readOnly = true)
    fun getFeaturedModules(): List<MasterModule> {
        return masterModuleRepository.findByActiveTrueAndFeaturedTrueOrderByDisplayOrderAsc()
    }

    /**
     * Get modules by complexity level
     */
    @Transactional(readOnly = true)
    fun getModulesByComplexity(complexity: ModuleComplexity): List<MasterModule> {
        return masterModuleRepository.findByActiveTrueAndComplexity(complexity)
    }

    /**
     * Search modules by query
     */
    @Transactional(readOnly = true)
    fun searchModules(query: String): List<MasterModule> {
        return masterModuleRepository.searchByQuery(query)
    }

    /**
     * Get modules suitable for workspace
     */
    @Transactional(readOnly = true)
    fun getModulesForWorkspace(
        subscriptionTier: SubscriptionTier,
        businessType: BusinessType?
    ): List<MasterModule> {
        val allowedTiers = SubscriptionTier.values().filter { 
            subscriptionTier.supportsModule(it) 
        }
        return masterModuleRepository.findSuitableForWorkspace(
            allowedTiers, 
            businessType?.name
        )
    }

    /**
     * Get modules by provider
     */
    @Transactional(readOnly = true)
    fun getModulesByProvider(provider: String): List<MasterModule> {
        return masterModuleRepository.findByActiveTrueAndProviderIgnoreCase(provider)
    }

    /**
     * Get top-rated modules
     */
    @Transactional(readOnly = true)
    fun getTopRatedModules(minRating: Double = 4.0, minRatingCount: Int = 10): List<MasterModule> {
        return masterModuleRepository.findTopRatedModules(minRating, minRatingCount)
    }

    /**
     * Get most installed modules
     */
    @Transactional(readOnly = true)
    fun getMostInstalledModules(pageable: Pageable): Page<MasterModule> {
        return masterModuleRepository.findByActiveTrueOrderByInstallCountDesc(pageable)
    }

    /**
     * Get modules with no dependencies
     */
    @Transactional(readOnly = true)
    fun getModulesWithNoDependencies(): List<MasterModule> {
        return masterModuleRepository.findWithNoDependencies()
    }

    /**
     * Advanced search with multiple filters
     */
    @Transactional(readOnly = true)
    fun advancedSearch(
        category: ModuleCategory?,
        status: ModuleStatus?,
        requiredTier: SubscriptionTier?,
        complexity: ModuleComplexity?,
        provider: String?,
        query: String?,
        pageable: Pageable
    ): Page<MasterModule> {
        return masterModuleRepository.advancedSearch(
            category, status, requiredTier, complexity, provider, query, pageable
        )
    }

    /**
     * Get module statistics
     */
    @Transactional(readOnly = true)
    fun getModuleStatistics(): Map<String, Any> {
        return masterModuleRepository.getModuleStatistics()
    }

    /**
     * Get modules by size range
     */
    @Transactional(readOnly = true)
    fun getModulesBySizeRange(minSizeMb: Int, maxSizeMb: Int): List<MasterModule> {
        return masterModuleRepository.findBySizeRange(minSizeMb, maxSizeMb)
    }

    /**
     * Get recently updated modules
     */
    @Transactional(readOnly = true)
    fun getRecentlyUpdatedModules(pageable: Pageable): Page<MasterModule> {
        return masterModuleRepository.findRecentlyUpdated(pageable)
    }

    /**
     * Get recommended modules
     */
    @Transactional(readOnly = true)
    fun getRecommendedModules(
        businessType: BusinessType,
        subscriptionTier: SubscriptionTier,
        excludeModules: List<String> = emptyList(),
        minRelevanceScore: Int = 5,
        pageable: Pageable
    ): Page<MasterModule> {
        val allowedTiers = SubscriptionTier.values().filter { 
            subscriptionTier.supportsModule(it) 
        }
        return masterModuleRepository.findRecommendedModules(
            businessType.name, allowedTiers, minRelevanceScore, excludeModules, pageable
        )
    }

    /**
     * Check if module code exists
     */
    @Transactional(readOnly = true)
    fun doesModuleExist(moduleCode: String): Boolean {
        return masterModuleRepository.existsByModuleCode(moduleCode)
    }

    /**
     * Count modules by category
     */
    @Transactional(readOnly = true)
    fun countModulesByCategory(category: ModuleCategory): Long {
        return masterModuleRepository.countByActiveTrueAndCategory(category)
    }

    /**
     * Get modules by multiple codes
     */
    @Transactional(readOnly = true)
    fun getModulesByCodes(moduleCodes: List<String>): List<MasterModule> {
        return masterModuleRepository.findByModuleCodeIn(moduleCodes)
    }

    /**
     * Find modules that depend on a specific module
     */
    @Transactional(readOnly = true)
    fun getModulesWithDependency(moduleCode: String): List<MasterModule> {
        return masterModuleRepository.findModulesWithDependency(moduleCode)
    }

    /**
     * Find modules that conflict with a specific module
     */
    @Transactional(readOnly = true)
    fun getModulesWithConflict(moduleCode: String): List<MasterModule> {
        return masterModuleRepository.findModulesWithConflict(moduleCode)
    }

    /**
     * Create new master module
     */
    fun createModule(module: MasterModule): MasterModule {
        require(!masterModuleRepository.existsByModuleCode(module.moduleCode)) {
            "Module with code '${module.moduleCode}' already exists"
        }
        return masterModuleRepository.save(module)
    }

    /**
     * Update master module
     */
    fun updateModule(moduleCode: String, module: MasterModule): MasterModule {
        val existingModule = masterModuleRepository.findByModuleCode(moduleCode)
            ?: throw IllegalArgumentException("Module with code '$moduleCode' not found")
        
        // Preserve ID and creation metadata
        module.uid = existingModule.uid
        module.createdAt = existingModule.createdAt
        module.moduleCode = existingModule.moduleCode
        
        return masterModuleRepository.save(module)
    }

    /**
     * Update module status
     */
    fun updateModuleStatus(moduleCode: String, status: ModuleStatus): MasterModule {
        val module = masterModuleRepository.findByModuleCode(moduleCode)
            ?: throw IllegalArgumentException("Module with code '$moduleCode' not found")
        
        module.status = status
        return masterModuleRepository.save(module)
    }

    /**
     * Toggle module active status
     */
    fun toggleModuleActive(moduleCode: String): MasterModule {
        val module = masterModuleRepository.findByModuleCode(moduleCode)
            ?: throw IllegalArgumentException("Module with code '$moduleCode' not found")
        
        module.active = !module.active
        return masterModuleRepository.save(module)
    }

    /**
     * Update module rating
     */
    fun updateModuleRating(moduleCode: String, rating: Double): MasterModule {
        val module = masterModuleRepository.findByModuleCode(moduleCode)
            ?: throw IllegalArgumentException("Module with code '$moduleCode' not found")
        
        module.updateRating(rating)
        return masterModuleRepository.save(module)
    }

    /**
     * Increment install count
     */
    fun incrementInstallCount(moduleCode: String): MasterModule {
        val module = masterModuleRepository.findByModuleCode(moduleCode)
            ?: throw IllegalArgumentException("Module with code '$moduleCode' not found")
        
        module.incrementInstallCount()
        return masterModuleRepository.save(module)
    }

    /**
     * Decrement install count
     */
    fun decrementInstallCount(moduleCode: String): MasterModule {
        val module = masterModuleRepository.findByModuleCode(moduleCode)
            ?: throw IllegalArgumentException("Module with code '$moduleCode' not found")
        
        module.decrementInstallCount()
        return masterModuleRepository.save(module)
    }

    /**
     * Set module as featured
     */
    fun setModuleFeatured(moduleCode: String, featured: Boolean): MasterModule {
        val module = masterModuleRepository.findByModuleCode(moduleCode)
            ?: throw IllegalArgumentException("Module with code '$moduleCode' not found")
        
        module.featured = featured
        return masterModuleRepository.save(module)
    }

    /**
     * Delete master module (soft delete by setting inactive)
     */
    fun deleteModule(moduleCode: String): MasterModule {
        val module = masterModuleRepository.findByModuleCode(moduleCode)
            ?: throw IllegalArgumentException("Module with code '$moduleCode' not found")
        
        module.active = false
        module.status = ModuleStatus.INACTIVE
        return masterModuleRepository.save(module)
    }

    /**
     * Validate module dependencies
     */
    @Transactional(readOnly = true)
    fun validateModuleDependencies(moduleCode: String, availableModules: Set<String>): List<String> {
        val module = masterModuleRepository.findByModuleCode(moduleCode)
            ?: throw IllegalArgumentException("Module with code '$moduleCode' not found")
        
        return module.getMissingDependencies(availableModules)
    }

    /**
     * Check for module conflicts
     */
    @Transactional(readOnly = true)
    fun checkModuleConflicts(moduleCode: String, installedModules: Set<String>): List<String> {
        val module = masterModuleRepository.findByModuleCode(moduleCode)
            ?: throw IllegalArgumentException("Module with code '$moduleCode' not found")
        
        return module.hasConflicts(installedModules)
    }

    /**
     * Check if module can be installed in workspace
     */
    @Transactional(readOnly = true)
    fun canInstallInWorkspace(
        moduleCode: String,
        subscriptionTier: SubscriptionTier,
        userRole: UserRole
    ): Boolean {
        val module = masterModuleRepository.findByModuleCode(moduleCode) ?: return false
        
        return module.isProductionReady() &&
                module.isAvailableForTier(subscriptionTier) &&
                module.hasRequiredRole(userRole)
    }

    /**
     * Get module categories with counts
     */
    @Transactional(readOnly = true)
    fun getModuleCategoriesWithCounts(): Map<ModuleCategory, Long> {
        return ModuleCategory.values().associateWith { category ->
            masterModuleRepository.countByActiveTrueAndCategory(category)
        }.filter { it.value > 0 }
    }

    /**
     * Get startup modules (essential for basic operation)
     */
    @Transactional(readOnly = true)
    fun getStartupModules(businessType: BusinessType): List<MasterModule> {
        return getEssentialModulesForBusiness(businessType)
            .filter { it.complexity == ModuleComplexity.ESSENTIAL }
            .sortedBy { it.displayOrder }
    }

    /**
     * Get onboarding modules (recommended for new workspaces)
     */
    @Transactional(readOnly = true)
    fun getOnboardingModules(
        businessType: BusinessType,
        subscriptionTier: SubscriptionTier
    ): List<MasterModule> {
        return getModulesForWorkspace(subscriptionTier, businessType)
            .filter { 
                it.complexity in listOf(ModuleComplexity.ESSENTIAL, ModuleComplexity.STANDARD) &&
                it.getRelevanceScore(businessType) >= 7
            }
            .sortedByDescending { it.getRelevanceScore(businessType) }
            .take(8) // Limit to top 8 for onboarding
    }

    /**
     * Bulk update display orders
     */
    fun updateDisplayOrders(moduleOrderMap: Map<String, Int>) {
        moduleOrderMap.forEach { (moduleCode, order) ->
            masterModuleRepository.findByModuleCode(moduleCode)?.let { module ->
                module.displayOrder = order
                masterModuleRepository.save(module)
            }
        }
    }
}