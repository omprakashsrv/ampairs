package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.*
import com.ampairs.workspace.model.enums.*
import jakarta.validation.constraints.*

/**
 * Request DTO for creating a new master module
 */
data class MasterModuleCreateRequest(
    
    @field:NotBlank(message = "Module code is required")
    @field:Size(min = 3, max = 100, message = "Module code must be between 3 and 100 characters")
    @field:Pattern(
        regexp = "^[a-z0-9-]+$",
        message = "Module code must contain only lowercase letters, numbers, and hyphens"
    )
    val moduleCode: String,
    
    @field:NotBlank(message = "Module name is required")
    @field:Size(min = 3, max = 200, message = "Module name must be between 3 and 200 characters")
    val name: String,
    
    @field:Size(max = 5000, message = "Description must not exceed 5000 characters")
    val description: String? = null,
    
    @field:Size(max = 500, message = "Tagline must not exceed 500 characters")
    val tagline: String? = null,
    
    @field:NotNull(message = "Category is required")
    val category: ModuleCategory,
    
    @field:NotNull(message = "Status is required")
    val status: ModuleStatus = ModuleStatus.ACTIVE,
    
    @field:NotNull(message = "Required tier is required")
    val requiredTier: SubscriptionTier = SubscriptionTier.FREE,
    
    @field:NotNull(message = "Required role is required")
    val requiredRole: UserRole = UserRole.EMPLOYEE,
    
    @field:NotNull(message = "Complexity is required")
    val complexity: ModuleComplexity = ModuleComplexity.STANDARD,
    
    @field:NotBlank(message = "Version is required")
    @field:Size(max = 50, message = "Version must not exceed 50 characters")
    val version: String = "1.0.0",
    
    val businessRelevance: List<BusinessRelevanceRequest> = emptyList(),
    
    val configuration: MasterModuleConfigurationRequest? = null,
    
    val uiMetadata: MasterModuleUIMetadataRequest? = null,
    
    @field:Size(max = 255, message = "Provider must not exceed 255 characters")
    val provider: String = "Ampairs",
    
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
    val sizeMb: Int = 0,
    
    val featured: Boolean = false,
    
    @field:Min(value = 0, message = "Display order must be non-negative")
    val displayOrder: Int = 0,
    
    val active: Boolean = true,
    
    @field:Size(max = 10000, message = "Release notes must not exceed 10000 characters")
    val releaseNotes: String? = null
)

/**
 * Business relevance request DTO
 */
data class BusinessRelevanceRequest(
    @field:NotNull(message = "Business type is required")
    val businessType: BusinessType,
    
    @field:Min(value = 1, message = "Relevance score must be at least 1")
    @field:Max(value = 10, message = "Relevance score must not exceed 10")
    val relevanceScore: Int,
    
    val isEssential: Boolean = false,
    
    @field:Size(max = 500, message = "Recommendation reason must not exceed 500 characters")
    val recommendationReason: String? = null
)

/**
 * Module configuration request DTO
 */
data class MasterModuleConfigurationRequest(
    val requiredPermissions: List<String> = emptyList(),
    val optionalPermissions: List<String> = emptyList(),
    val defaultEnabled: Boolean = true,
    val dependencies: List<String> = emptyList(),
    val conflictsWith: List<String> = emptyList(),
    val customSettings: Map<String, Any> = emptyMap()
)

/**
 * Module UI metadata request DTO  
 */
data class MasterModuleUIMetadataRequest(
    @field:NotBlank(message = "Icon is required")
    val icon: String = "apps",
    
    @field:Pattern(
        regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
        message = "Primary color must be a valid hex color"
    )
    val primaryColor: String = "#6750A4",
    
    @field:Pattern(
        regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
        message = "Secondary color must be a valid hex color"
    )
    val secondaryColor: String? = null,
    
    @field:Pattern(
        regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
        message = "Background color must be a valid hex color"
    )
    val backgroundColor: String? = null,
    
    val screenshots: List<String> = emptyList(),
    val bannerImage: String? = null,
    val demoUrl: String? = null,
    val videoUrl: String? = null,
    val tags: List<String> = emptyList(),
    val keywords: List<String> = emptyList()
)

// Extension functions for conversion
fun MasterModuleCreateRequest.toEntity(): MasterModule {
    return MasterModule().apply {
        moduleCode = this@toEntity.moduleCode
        name = this@toEntity.name
        description = this@toEntity.description
        tagline = this@toEntity.tagline
        category = this@toEntity.category
        status = this@toEntity.status
        requiredTier = this@toEntity.requiredTier
        requiredRole = this@toEntity.requiredRole
        complexity = this@toEntity.complexity
        version = this@toEntity.version
        businessRelevance = this@toEntity.businessRelevance.map { it.toEntity() }
        configuration = this@toEntity.configuration?.toEntity() ?: ModuleConfiguration()
        uiMetadata = this@toEntity.uiMetadata?.toEntity() ?: ModuleUIMetadata()
        provider = this@toEntity.provider
        supportEmail = this@toEntity.supportEmail
        documentationUrl = this@toEntity.documentationUrl
        homepageUrl = this@toEntity.homepageUrl
        setupGuideUrl = this@toEntity.setupGuideUrl
        sizeMb = this@toEntity.sizeMb
        featured = this@toEntity.featured
        displayOrder = this@toEntity.displayOrder
        active = this@toEntity.active
        releaseNotes = this@toEntity.releaseNotes
        lastUpdatedAt = java.time.LocalDateTime.now()
    }
}

fun BusinessRelevanceRequest.toEntity(): BusinessRelevance {
    return BusinessRelevance(
        businessType = this.businessType,
        relevanceScore = this.relevanceScore,
        isEssential = this.isEssential,
        recommendationReason = this.recommendationReason
    )
}

fun MasterModuleConfigurationRequest.toEntity(): ModuleConfiguration {
    return ModuleConfiguration(
        requiredPermissions = this.requiredPermissions,
        optionalPermissions = this.optionalPermissions,
        defaultEnabled = this.defaultEnabled,
        dependencies = this.dependencies,
        conflictsWith = this.conflictsWith,
        customSettings = this.customSettings
    )
}

fun MasterModuleUIMetadataRequest.toEntity(): ModuleUIMetadata {
    return ModuleUIMetadata(
        icon = this.icon,
        primaryColor = this.primaryColor,
        secondaryColor = this.secondaryColor,
        backgroundColor = this.backgroundColor,
        screenshots = this.screenshots,
        bannerImage = this.bannerImage,
        demoUrl = this.demoUrl,
        videoUrl = this.videoUrl,
        tags = this.tags,
        keywords = this.keywords
    )
}