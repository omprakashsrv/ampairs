package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.*
import com.ampairs.workspace.model.enums.*
import java.time.LocalDateTime

/**
 * Response DTO for master module information
 */
data class MasterModuleAdminResponse(
    val id: String,
    val moduleCode: String,
    val name: String,
    val description: String?,
    val tagline: String?,
    val category: ModuleCategory,
    val status: ModuleStatus,
    val requiredTier: SubscriptionTier,
    val requiredRole: UserRole,
    val complexity: ModuleComplexity,
    val version: String,
    val businessRelevance: List<BusinessRelevanceResponse>,
    val configuration: MasterModuleConfigurationResponse,
    val uiMetadata: MasterModuleUIMetadataResponse,
    val provider: String,
    val supportEmail: String?,
    val documentationUrl: String?,
    val homepageUrl: String?,
    val setupGuideUrl: String?,
    val sizeMb: Int,
    val installCount: Int,
    val rating: Double,
    val ratingCount: Int,
    val featured: Boolean,
    val displayOrder: Int,
    val active: Boolean,
    val releaseNotes: String?,
    val lastUpdatedAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

/**
 * Simplified response DTO for module listings
 */
data class MasterModuleAdminListResponse(
    val id: String,
    val moduleCode: String,
    val name: String,
    val tagline: String?,
    val category: ModuleCategory,
    val status: ModuleStatus,
    val requiredTier: SubscriptionTier,
    val complexity: ModuleComplexity,
    val version: String,
    val provider: String,
    val sizeMb: Int,
    val installCount: Int,
    val rating: Double,
    val ratingCount: Int,
    val featured: Boolean,
    val displayOrder: Int,
    val active: Boolean,
    val icon: String,
    val primaryColor: String,
    val tags: List<String>
)

/**
 * Business relevance response DTO
 */
data class BusinessRelevanceResponse(
    val businessType: BusinessType,
    val relevanceScore: Int,
    val isEssential: Boolean,
    val recommendationReason: String?
)

/**
 * Module configuration response DTO
 */
data class MasterModuleConfigurationResponse(
    val requiredPermissions: List<String>,
    val optionalPermissions: List<String>,
    val defaultEnabled: Boolean,
    val dependencies: List<String>,
    val conflictsWith: List<String>,
    val customSettings: Map<String, Any>
)

/**
 * Module UI metadata response DTO
 */
data class MasterModuleUIMetadataResponse(
    val icon: String,
    val primaryColor: String,
    val secondaryColor: String?,
    val backgroundColor: String?,
    val screenshots: List<String>,
    val bannerImage: String?,
    val demoUrl: String?,
    val videoUrl: String?,
    val tags: List<String>,
    val keywords: List<String>
)

// Extension functions for conversion
fun MasterModule.toResponse(): MasterModuleAdminResponse {
    return MasterModuleAdminResponse(
        id = this.uid,
        moduleCode = this.moduleCode,
        name = this.name,
        description = this.description,
        tagline = this.tagline,
        category = this.category,
        status = this.status,
        requiredTier = this.requiredTier,
        requiredRole = this.requiredRole,
        complexity = this.complexity,
        version = this.version,
        businessRelevance = this.businessRelevance.map { it.toResponse() },
        configuration = this.configuration.toResponse(),
        uiMetadata = this.uiMetadata.toResponse(),
        provider = this.provider,
        supportEmail = this.supportEmail,
        documentationUrl = this.documentationUrl,
        homepageUrl = this.homepageUrl,
        setupGuideUrl = this.setupGuideUrl,
        sizeMb = this.sizeMb,
        installCount = this.installCount,
        rating = this.rating,
        ratingCount = this.ratingCount,
        featured = this.featured,
        displayOrder = this.displayOrder,
        active = this.active,
        releaseNotes = this.releaseNotes,
        lastUpdatedAt = this.lastUpdatedAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun MasterModule.toListResponse(): MasterModuleAdminListResponse {
    return MasterModuleAdminListResponse(
        id = this.uid,
        moduleCode = this.moduleCode,
        name = this.name,
        tagline = this.tagline,
        category = this.category,
        status = this.status,
        requiredTier = this.requiredTier,
        complexity = this.complexity,
        version = this.version,
        provider = this.provider,
        sizeMb = this.sizeMb,
        installCount = this.installCount,
        rating = this.rating,
        ratingCount = this.ratingCount,
        featured = this.featured,
        displayOrder = this.displayOrder,
        active = this.active,
        icon = this.uiMetadata.icon,
        primaryColor = this.uiMetadata.primaryColor,
        tags = this.uiMetadata.tags
    )
}

fun BusinessRelevance.toResponse(): BusinessRelevanceResponse {
    return BusinessRelevanceResponse(
        businessType = this.businessType,
        relevanceScore = this.relevanceScore,
        isEssential = this.isEssential,
        recommendationReason = this.recommendationReason
    )
}

fun ModuleConfiguration.toResponse(): MasterModuleConfigurationResponse {
    return MasterModuleConfigurationResponse(
        requiredPermissions = this.requiredPermissions,
        optionalPermissions = this.optionalPermissions,
        defaultEnabled = this.defaultEnabled,
        dependencies = this.dependencies,
        conflictsWith = this.conflictsWith,
        customSettings = this.customSettings
    )
}

fun ModuleUIMetadata.toResponse(): MasterModuleUIMetadataResponse {
    return MasterModuleUIMetadataResponse(
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