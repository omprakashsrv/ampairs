package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.enums.*
import jakarta.validation.constraints.*

/**
 * Request DTO for updating an existing master module
 */
data class MasterModuleUpdateRequest(
    
    @field:Size(min = 3, max = 200, message = "Module name must be between 3 and 200 characters")
    val name: String? = null,
    
    @field:Size(max = 5000, message = "Description must not exceed 5000 characters")
    val description: String? = null,
    
    @field:Size(max = 500, message = "Tagline must not exceed 500 characters")
    val tagline: String? = null,
    
    val category: ModuleCategory? = null,
    
    val status: ModuleStatus? = null,
    
    val requiredTier: SubscriptionTier? = null,
    
    val requiredRole: UserRole? = null,
    
    val complexity: ModuleComplexity? = null,
    
    @field:Size(max = 50, message = "Version must not exceed 50 characters")
    val version: String? = null,
    
    val businessRelevance: List<BusinessRelevanceRequest>? = null,
    
    val configuration: MasterModuleConfigurationRequest? = null,
    
    val uiMetadata: MasterModuleUIMetadataRequest? = null,
    
    @field:Size(max = 255, message = "Provider must not exceed 255 characters")
    val provider: String? = null,
    
    @field:Email(message = "Support email must be valid")
    @field:Size(max = 255, message = "Support email must not exceed 255 characters")
    val supportEmail: String? = null,
    
    @field:Size(max = 500, message = "Documentation URL must not exceed 500 characters")
    val documentationUrl: String? = null,
    
    @field:Size(max = 500, message = "Homepage URL must not exceed 500 characters")
    val homepageUrl: String? = null,
    
    @field:Size(max = 500, message = "Setup guide URL must not exceed 500 characters")
    val setupGuideUrl: String? = null,
    
    @field:Min(value = 0, message = "Size MB must be non-negative")
    val sizeMb: Int? = null,
    
    val featured: Boolean? = null,
    
    @field:Min(value = 0, message = "Display order must be non-negative")
    val displayOrder: Int? = null,
    
    val active: Boolean? = null,
    
    @field:Size(max = 10000, message = "Release notes must not exceed 10000 characters")
    val releaseNotes: String? = null
)