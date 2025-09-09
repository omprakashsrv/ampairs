package com.ampairs.workspace.service

import com.ampairs.core.exception.BusinessException
import com.ampairs.core.exception.NotFoundException
import com.ampairs.workspace.model.MasterModule
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.model.enums.ModuleCategory
import com.ampairs.workspace.model.enums.ModuleComplexity
import com.ampairs.workspace.model.enums.ModuleStatus
import com.ampairs.workspace.model.enums.SubscriptionTier
import com.ampairs.workspace.repository.MasterModuleRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Service for Super Admin management of Master Modules.
 * Provides CRUD operations and advanced management features for the module catalog.
 */
@Service
@Transactional
class MasterModuleAdminService(
    private val masterModuleRepository: MasterModuleRepository
) {
    
    private val logger = LoggerFactory.getLogger(MasterModuleAdminService::class.java)
    
    /**
     * Create a new master module
     */
    fun createMasterModule(request: MasterModuleCreateRequest): MasterModuleAdminResponse {
        logger.info("Creating master module: {}", request.moduleCode)
        
        // Check if module code already exists
        if (masterModuleRepository.findByModuleCode(request.moduleCode) != null) {
            throw BusinessException("Module with code '${request.moduleCode}' already exists")
        }
        
        // Validate dependencies if specified
        if (request.configuration?.dependencies?.isNotEmpty() == true) {
            validateDependencies(request.configuration.dependencies)
        }
        
        val masterModule = request.toEntity()
        val savedModule = masterModuleRepository.save(masterModule)
        
        logger.info("Successfully created master module: {} with ID: {}", savedModule.moduleCode, savedModule.uid)
        return savedModule.toResponse()
    }
    
    /**
     * Update an existing master module
     */
    fun updateMasterModule(id: String, request: MasterModuleUpdateRequest): MasterModuleAdminResponse {
        logger.info("Updating master module: {}", id)
        
        val existingModule = masterModuleRepository.findById(id)
            .orElseThrow { NotFoundException("Master module not found with ID: $id") }
        
        // Apply updates
        request.name?.let { existingModule.name = it }
        request.description?.let { existingModule.description = it }
        request.tagline?.let { existingModule.tagline = it }
        request.category?.let { existingModule.category = it }
        request.status?.let { existingModule.status = it }
        request.requiredTier?.let { existingModule.requiredTier = it }
        request.requiredRole?.let { existingModule.requiredRole = it }
        request.complexity?.let { existingModule.complexity = it }
        request.version?.let { existingModule.version = it }
        request.businessRelevance?.let { existingModule.businessRelevance = it.map { br -> br.toEntity() } }
        request.configuration?.let { existingModule.configuration = it.toEntity() }
        request.uiMetadata?.let { existingModule.uiMetadata = it.toEntity() }
        request.provider?.let { existingModule.provider = it }
        request.supportEmail?.let { existingModule.supportEmail = it }
        request.documentationUrl?.let { existingModule.documentationUrl = it }
        request.homepageUrl?.let { existingModule.homepageUrl = it }
        request.setupGuideUrl?.let { existingModule.setupGuideUrl = it }
        request.sizeMb?.let { existingModule.sizeMb = it }
        request.featured?.let { existingModule.featured = it }
        request.displayOrder?.let { existingModule.displayOrder = it }
        request.active?.let { existingModule.active = it }
        request.releaseNotes?.let { existingModule.releaseNotes = it }
        
        existingModule.lastUpdatedAt = LocalDateTime.now()
        
        // Validate dependencies if updated
        existingModule.configuration.dependencies.takeIf { it.isNotEmpty() }?.let { deps ->
            validateDependencies(deps)
        }
        
        val savedModule = masterModuleRepository.save(existingModule)
        logger.info("Successfully updated master module: {}", savedModule.moduleCode)
        
        return savedModule.toResponse()
    }
    
    /**
     * Get master module by ID
     */
    @Transactional(readOnly = true)
    fun getMasterModule(id: String): MasterModuleAdminResponse {
        val masterModule = masterModuleRepository.findById(id)
            .orElseThrow { NotFoundException("Master module not found with ID: $id") }
        
        return masterModule.toResponse()
    }
    
    /**
     * Get master module by code
     */
    @Transactional(readOnly = true)
    fun getMasterModuleByCode(moduleCode: String): MasterModuleAdminResponse {
        val masterModule = masterModuleRepository.findByModuleCode(moduleCode)
            ?: throw NotFoundException("Master module not found with code: $moduleCode")
        
        return masterModule.toResponse()
    }
    
    /**
     * Get all master modules with pagination and filtering
     */
    @Transactional(readOnly = true)
    fun getAllMasterModules(
        category: ModuleCategory? = null,
        status: ModuleStatus? = null,
        complexity: ModuleComplexity? = null,
        requiredTier: SubscriptionTier? = null,
        featured: Boolean? = null,
        active: Boolean? = null,
        pageable: Pageable
    ): Page<MasterModuleAdminListResponse> {
        
        // Build dynamic query based on filters
        val modules = when {
            category != null -> {
                if (active == true) {
                    masterModuleRepository.findByActiveTrueAndCategoryOrderByDisplayOrderAsc(category, pageable)
                } else {
                    // Need custom query for category filtering with other conditions
                    masterModuleRepository.findAll(pageable) // Simplified for now
                }
            }
            active == true && featured == true -> {
                masterModuleRepository.findByActiveTrueAndFeaturedTrueOrderByDisplayOrderAsc()
                    .let { list -> 
                        // Convert list to page - would need custom implementation
                        org.springframework.data.domain.PageImpl(list, pageable, list.size.toLong())
                    }
            }
            active == true -> {
                masterModuleRepository.findAll(pageable) // Would filter in query
            }
            else -> {
                masterModuleRepository.findAll(pageable)
            }
        }
        
        return modules.map { it.toListResponse() }
    }
    
    /**
     * Search master modules by keyword
     */
    @Transactional(readOnly = true)
    fun searchMasterModules(keyword: String): List<MasterModuleAdminListResponse> {
        val modules = masterModuleRepository.findByActiveTrueAndNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByDisplayOrderAsc(
            keyword, keyword
        )
        
        return modules.map { it.toListResponse() }
    }
    
    /**
     * Delete master module
     */
    fun deleteMasterModule(id: String) {
        logger.info("Deleting master module: {}", id)
        
        val masterModule = masterModuleRepository.findById(id)
            .orElseThrow { NotFoundException("Master module not found with ID: $id") }
        
        // Check if module is being used by any workspaces
        if (masterModule.installCount > 0) {
            throw BusinessException("Cannot delete module '${masterModule.moduleCode}' as it is installed in ${masterModule.installCount} workspaces")
        }
        
        masterModuleRepository.delete(masterModule)
        logger.info("Successfully deleted master module: {}", masterModule.moduleCode)
    }
    
    /**
     * Bulk update module status
     */
    fun bulkUpdateStatus(moduleIds: List<String>, status: ModuleStatus): List<MasterModuleAdminResponse> {
        logger.info("Bulk updating status to {} for {} modules", status, moduleIds.size)
        
        val modules = masterModuleRepository.findAllById(moduleIds)
        if (modules.size != moduleIds.size) {
            throw BusinessException("Some modules not found")
        }
        
        modules.forEach { module ->
            module.status = status
            module.lastUpdatedAt = LocalDateTime.now()
        }
        
        val savedModules = masterModuleRepository.saveAll(modules)
        logger.info("Successfully updated status for {} modules", savedModules.size)
        
        return savedModules.map { it.toResponse() }
    }
    
    /**
     * Update display order for multiple modules
     */
    fun updateDisplayOrder(updates: List<Pair<String, Int>>): List<MasterModuleAdminResponse> {
        logger.info("Updating display order for {} modules", updates.size)
        
        val moduleIds = updates.map { it.first }
        val modules = masterModuleRepository.findAllById(moduleIds)
        
        if (modules.size != moduleIds.size) {
            throw BusinessException("Some modules not found")
        }
        
        val moduleMap = modules.associateBy { it.uid }
        
        updates.forEach { (id, order) ->
            moduleMap[id]?.let { module ->
                module.displayOrder = order
                module.lastUpdatedAt = LocalDateTime.now()
            }
        }
        
        val savedModules = masterModuleRepository.saveAll(modules)
        logger.info("Successfully updated display order for {} modules", savedModules.size)
        
        return savedModules.map { it.toResponse() }
    }
    
    /**
     * Get module statistics
     */
    @Transactional(readOnly = true)
    fun getModuleStatistics(): Map<String, Any> {
        val totalModules = masterModuleRepository.count()
        val activeModules = masterModuleRepository.count() // Simplified for now
        val featuredModules = masterModuleRepository.findByActiveTrueAndFeaturedTrueOrderByDisplayOrderAsc().size
        
        val categoryCounts = ModuleCategory.values().associate { category ->
            category.name to masterModuleRepository.countByActiveTrueAndCategory(category)
        }
        
        val statusCounts = ModuleStatus.values().associate { status ->
            status.name to masterModuleRepository.findByStatusOrderByDisplayOrderAsc(status).size
        }
        
        return mapOf(
            "totalModules" to totalModules,
            "activeModules" to activeModules,
            "featuredModules" to featuredModules,
            "categoryCounts" to categoryCounts,
            "statusCounts" to statusCounts
        )
    }
    
    /**
     * Validate module dependencies exist
     */
    private fun validateDependencies(dependencies: List<String>) {
        val existingModules = masterModuleRepository.findByModuleCodeIn(dependencies)
        val existingCodes = existingModules.map { it.moduleCode }.toSet()
        
        val missingDependencies = dependencies.filter { it !in existingCodes }
        if (missingDependencies.isNotEmpty()) {
            throw BusinessException("Dependencies not found: ${missingDependencies.joinToString(", ")}")
        }
    }
}