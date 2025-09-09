package com.ampairs.workspace.controller

import com.ampairs.workspace.service.MasterModuleAdminService
import com.ampairs.workspace.model.dto.MasterModuleCreateRequest
import com.ampairs.workspace.model.dto.MasterModuleUpdateRequest
import com.ampairs.workspace.model.enums.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import jakarta.validation.Valid

/**
 * Web controller for Super Admin MasterModule management interface.
 * Provides a simple HTML interface for CRUD operations.
 */
@Controller
@RequestMapping("/admin/master-modules")
@PreAuthorize("@superAdminAuth.isSuperAdmin(authentication)")
class MasterModuleAdminWebController(
    private val masterModuleAdminService: MasterModuleAdminService
) {
    
    private val logger = LoggerFactory.getLogger(MasterModuleAdminWebController::class.java)
    
    /**
     * Display list of all master modules
     */
    @GetMapping
    fun listModules(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "displayOrder") sortBy: String,
        @RequestParam(defaultValue = "ASC") sortDirection: String,
        @RequestParam(required = false) category: ModuleCategory?,
        @RequestParam(required = false) status: ModuleStatus?,
        @RequestParam(required = false) active: Boolean?,
        model: Model
    ): String {
        
        val pageable = PageRequest.of(
            page, 
            size, 
            Sort.Direction.valueOf(sortDirection.uppercase()), 
            sortBy
        )
        
        val modules = masterModuleAdminService.getAllMasterModules(
            category = category,
            status = status,
            active = active,
            pageable = pageable
        )
        
        val statistics = masterModuleAdminService.getModuleStatistics()
        
        model.addAttribute("modules", modules)
        model.addAttribute("statistics", statistics)
        model.addAttribute("categories", ModuleCategory.values())
        model.addAttribute("statuses", ModuleStatus.values())
        model.addAttribute("currentPage", page)
        model.addAttribute("totalPages", modules.totalPages)
        model.addAttribute("sortBy", sortBy)
        model.addAttribute("sortDirection", sortDirection)
        model.addAttribute("selectedCategory", category)
        model.addAttribute("selectedStatus", status)
        model.addAttribute("selectedActive", active)
        
        return "admin/master-modules/list"
    }
    
    /**
     * Show form to create new master module
     */
    @GetMapping("/create")
    fun createForm(model: Model): String {
        model.addAttribute("module", MasterModuleCreateRequest(
            moduleCode = "",
            name = "",
            category = ModuleCategory.CUSTOMER_MANAGEMENT
        ))
        model.addAttribute("categories", ModuleCategory.values())
        model.addAttribute("statuses", ModuleStatus.values())
        model.addAttribute("tiers", SubscriptionTier.values())
        model.addAttribute("roles", UserRole.values())
        model.addAttribute("complexities", ModuleComplexity.values())
        model.addAttribute("businessTypes", BusinessType.values())
        
        return "admin/master-modules/create"
    }
    
    /**
     * Process create form submission
     */
    @PostMapping("/create")
    fun createModule(
        @Valid @ModelAttribute("module") request: MasterModuleCreateRequest,
        bindingResult: BindingResult,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", ModuleCategory.values())
            model.addAttribute("statuses", ModuleStatus.values())
            model.addAttribute("tiers", SubscriptionTier.values())
            model.addAttribute("roles", UserRole.values())
            model.addAttribute("complexities", ModuleComplexity.values())
            model.addAttribute("businessTypes", BusinessType.values())
            return "admin/master-modules/create"
        }
        
        try {
            val createdModule = masterModuleAdminService.createMasterModule(request)
            redirectAttributes.addFlashAttribute("successMessage", 
                "Master module '${createdModule.name}' created successfully!")
            logger.info("Super Admin created master module: {}", createdModule.moduleCode)
            
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error creating module: ${e.message}")
            logger.error("Error creating master module", e)
        }
        
        return "redirect:/admin/master-modules"
    }
    
    /**
     * Show module details
     */
    @GetMapping("/{id}")
    fun viewModule(@PathVariable id: String, model: Model): String {
        try {
            val module = masterModuleAdminService.getMasterModule(id)
            model.addAttribute("module", module)
            return "admin/master-modules/view"
            
        } catch (e: Exception) {
            logger.error("Error viewing master module: $id", e)
            model.addAttribute("errorMessage", "Module not found: ${e.message}")
            return "redirect:/admin/master-modules"
        }
    }
    
    /**
     * Show form to edit master module
     */
    @GetMapping("/{id}/edit")
    fun editForm(@PathVariable id: String, model: Model): String {
        try {
            val module = masterModuleAdminService.getMasterModule(id)
            
            // Convert to update request
            val updateRequest = MasterModuleUpdateRequest(
                name = module.name,
                description = module.description,
                tagline = module.tagline,
                category = module.category,
                status = module.status,
                requiredTier = module.requiredTier,
                requiredRole = module.requiredRole,
                complexity = module.complexity,
                version = module.version,
                provider = module.provider,
                supportEmail = module.supportEmail,
                documentationUrl = module.documentationUrl,
                homepageUrl = module.homepageUrl,
                setupGuideUrl = module.setupGuideUrl,
                sizeMb = module.sizeMb,
                featured = module.featured,
                displayOrder = module.displayOrder,
                active = module.active,
                releaseNotes = module.releaseNotes
            )
            
            model.addAttribute("module", updateRequest)
            model.addAttribute("moduleId", id)
            model.addAttribute("originalModule", module)
            model.addAttribute("categories", ModuleCategory.values())
            model.addAttribute("statuses", ModuleStatus.values())
            model.addAttribute("tiers", SubscriptionTier.values())
            model.addAttribute("roles", UserRole.values())
            model.addAttribute("complexities", ModuleComplexity.values())
            
            return "admin/master-modules/edit"
            
        } catch (e: Exception) {
            logger.error("Error loading master module for edit: $id", e)
            model.addAttribute("errorMessage", "Module not found: ${e.message}")
            return "redirect:/admin/master-modules"
        }
    }
    
    /**
     * Process edit form submission
     */
    @PostMapping("/{id}/edit")
    fun updateModule(
        @PathVariable id: String,
        @Valid @ModelAttribute("module") request: MasterModuleUpdateRequest,
        bindingResult: BindingResult,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        
        if (bindingResult.hasErrors()) {
            try {
                val originalModule = masterModuleAdminService.getMasterModule(id)
                model.addAttribute("moduleId", id)
                model.addAttribute("originalModule", originalModule)
                model.addAttribute("categories", ModuleCategory.values())
                model.addAttribute("statuses", ModuleStatus.values())
                model.addAttribute("tiers", SubscriptionTier.values())
                model.addAttribute("roles", UserRole.values())
                model.addAttribute("complexities", ModuleComplexity.values())
                return "admin/master-modules/edit"
                
            } catch (e: Exception) {
                redirectAttributes.addFlashAttribute("errorMessage", "Module not found")
                return "redirect:/admin/master-modules"
            }
        }
        
        try {
            val updatedModule = masterModuleAdminService.updateMasterModule(id, request)
            redirectAttributes.addFlashAttribute("successMessage", 
                "Master module '${updatedModule.name}' updated successfully!")
            logger.info("Super Admin updated master module: {}", updatedModule.moduleCode)
            
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error updating module: ${e.message}")
            logger.error("Error updating master module: $id", e)
        }
        
        return "redirect:/admin/master-modules"
    }
    
    /**
     * Delete master module
     */
    @PostMapping("/{id}/delete")
    fun deleteModule(
        @PathVariable id: String, 
        redirectAttributes: RedirectAttributes
    ): String {
        
        try {
            val module = masterModuleAdminService.getMasterModule(id)
            masterModuleAdminService.deleteMasterModule(id)
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Master module '${module.name}' deleted successfully!")
            logger.warn("Super Admin deleted master module: {}", module.moduleCode)
            
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error deleting module: ${e.message}")
            logger.error("Error deleting master module: $id", e)
        }
        
        return "redirect:/admin/master-modules"
    }
    
    /**
     * Bulk update module status
     */
    @PostMapping("/bulk/status")
    fun bulkUpdateStatus(
        @RequestParam moduleIds: List<String>,
        @RequestParam status: ModuleStatus,
        redirectAttributes: RedirectAttributes
    ): String {
        
        try {
            val updatedModules = masterModuleAdminService.bulkUpdateStatus(moduleIds, status)
            redirectAttributes.addFlashAttribute("successMessage", 
                "Updated status for ${updatedModules.size} modules to ${status.displayName}")
            logger.info("Super Admin bulk updated status for {} modules to {}", updatedModules.size, status)
            
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error updating module status: ${e.message}")
            logger.error("Error in bulk status update", e)
        }
        
        return "redirect:/admin/master-modules"
    }
    
    /**
     * Module statistics dashboard
     */
    @GetMapping("/dashboard")
    fun dashboard(model: Model): String {
        val statistics = masterModuleAdminService.getModuleStatistics()
        model.addAttribute("statistics", statistics)
        return "admin/master-modules/dashboard"
    }
}