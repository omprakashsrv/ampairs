package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.*
import com.ampairs.workspace.model.enums.WorkspaceModuleStatus
import java.time.Instant

/**
 * Response for workspace module information
 */
data class WorkspaceModuleResponse(
    var id: String = "",

    var seqId: Long = 0,

    var workspaceId: String = "",

    var masterModule: MasterModuleResponse = MasterModuleResponse(),

    var status: WorkspaceModuleStatus = WorkspaceModuleStatus.INSTALLED,

    var enabled: Boolean = true,

    var installedVersion: String = "",

    var installedAt: Instant = Instant.now(),

    var installedBy: String? = null,

    var installedByName: String? = null,

    var lastUpdatedAt: Instant? = null,

    var lastUpdatedBy: String? = null,

    var categoryOverride: String? = null,

    var displayOrder: Int = 0,

    var settings: ModuleSettings = ModuleSettings(),

    var usageMetrics: ModuleUsageMetrics = ModuleUsageMetrics(),

    var userPreferences: List<UserModulePreferences> = emptyList(),

    var licenseInfo: String? = null,

    var licenseExpiresAt: Instant? = null,

    var storageUsedMb: Int = 0,

    var configurationNotes: String? = null,

    var effectiveName: String = "",

    var effectiveDescription: String = "",

    var effectiveIcon: String = "",

    var effectiveColor: String = "",

    var effectiveCategory: String = "",

    var isOperational: Boolean = false,

    var hasValidLicense: Boolean = true,

    var canBeUpdated: Boolean = false,

    var needsAttention: Boolean = false,

    var healthScore: Double = 1.0,

    var engagementLevel: Double = 0.0,

    var isPopular: Boolean = false,

    var createdAt: Instant = Instant.now(),

    var updatedAt: Instant = Instant.now()
) {
    companion object {
        fun from(workspaceModule: WorkspaceModule): WorkspaceModuleResponse {
            return WorkspaceModuleResponse(
                id = workspaceModule.uid,
                seqId = workspaceModule.id,
                workspaceId = workspaceModule.workspaceId,
                masterModule = MasterModuleResponse.from(workspaceModule.masterModule),
                status = workspaceModule.status,
                enabled = workspaceModule.enabled,
                installedVersion = workspaceModule.installedVersion,
                installedAt = workspaceModule.installedAt,
                installedBy = workspaceModule.installedBy,
                installedByName = workspaceModule.installedByName,
                lastUpdatedAt = workspaceModule.lastUpdatedAt,
                lastUpdatedBy = workspaceModule.lastUpdatedBy,
                categoryOverride = workspaceModule.categoryOverride,
                displayOrder = workspaceModule.displayOrder,
                settings = workspaceModule.settings,
                usageMetrics = workspaceModule.usageMetrics,
                userPreferences = workspaceModule.userPreferences,
                licenseInfo = workspaceModule.licenseInfo,
                licenseExpiresAt = workspaceModule.licenseExpiresAt,
                storageUsedMb = workspaceModule.storageUsedMb,
                configurationNotes = workspaceModule.configurationNotes,
                effectiveName = workspaceModule.getEffectiveName(),
                effectiveDescription = workspaceModule.getEffectiveDescription(),
                effectiveIcon = workspaceModule.getEffectiveIcon(),
                effectiveColor = workspaceModule.getEffectiveColor(),
                effectiveCategory = workspaceModule.getEffectiveCategory(),
                isOperational = workspaceModule.isOperational(),
                hasValidLicense = workspaceModule.hasValidLicense(),
                canBeUpdated = workspaceModule.canBeUpdated(),
                needsAttention = workspaceModule.needsAttention(),
                healthScore = workspaceModule.getHealthScore(),
                engagementLevel = workspaceModule.getUserEngagementLevel(),
                isPopular = workspaceModule.isPopular(),
                createdAt = workspaceModule.createdAt ?: Instant.now(),
                updatedAt = workspaceModule.updatedAt ?: Instant.now()
            )
        }
    }
}

/**
 * Response for master module information
 */
data class MasterModuleResponse(
    var id: String = "",

    var seqId: Long = 0,

    var moduleCode: String = "",

    var name: String = "",

    var description: String? = null,

    var tagline: String? = null,

    var category: String = "",

    var status: String = "",

    var requiredTier: String = "",

    var requiredRole: String = "",

    var complexity: String = "",

    var version: String = "",

    var businessRelevance: List<BusinessRelevance> = emptyList(),

    var configuration: ModuleConfiguration = ModuleConfiguration(),

    var uiMetadata: ModuleUIMetadata = ModuleUIMetadata(),

    var provider: String = "",

    var supportEmail: String? = null,

    var documentationUrl: String? = null,

    var homepageUrl: String? = null,

    var setupGuideUrl: String? = null,

    var sizeMb: Int = 0,

    var installCount: Int = 0,

    var rating: Double = 0.0,

    var ratingCount: Int = 0,

    var featured: Boolean = false,

    var displayOrder: Int = 0,

    var active: Boolean = true,

    var releaseNotes: String? = null,

    var lastUpdatedAt: Instant? = null,

    var createdAt: Instant = Instant.now(),

    var updatedAt: Instant = Instant.now()
) {
    companion object {
        fun from(masterModule: MasterModule): MasterModuleResponse {
            return MasterModuleResponse(
                id = masterModule.uid,
                seqId = masterModule.id,
                moduleCode = masterModule.moduleCode,
                name = masterModule.name,
                description = masterModule.description,
                tagline = masterModule.tagline,
                category = masterModule.category.name,
                status = masterModule.status.name,
                requiredTier = masterModule.requiredTier.name,
                requiredRole = masterModule.requiredRole.name,
                complexity = masterModule.complexity.name,
                version = masterModule.version,
                businessRelevance = masterModule.businessRelevance,
                configuration = masterModule.configuration,
                uiMetadata = masterModule.uiMetadata,
                provider = masterModule.provider,
                supportEmail = masterModule.supportEmail,
                documentationUrl = masterModule.documentationUrl,
                homepageUrl = masterModule.homepageUrl,
                setupGuideUrl = masterModule.setupGuideUrl,
                sizeMb = masterModule.sizeMb,
                installCount = masterModule.installCount,
                rating = masterModule.rating,
                ratingCount = masterModule.ratingCount,
                featured = masterModule.featured,
                displayOrder = masterModule.displayOrder,
                active = masterModule.active,
                releaseNotes = masterModule.releaseNotes,
                lastUpdatedAt = masterModule.lastUpdatedAt,
                createdAt = masterModule.createdAt ?: Instant.now(),
                updatedAt = masterModule.updatedAt ?: Instant.now()
            )
        }
    }
}

/**
 * Response for module search and listing with pagination
 */
data class ModuleSearchResponse(
    var modules: List<WorkspaceModuleResponse> = emptyList(),

    var totalElements: Long = 0,

    var totalPages: Int = 0,

    var currentPage: Int = 0,

    var pageSize: Int = 20,

    var hasNext: Boolean = false,

    var hasPrevious: Boolean = false,

    var searchMetadata: ModuleSearchMetadata? = null
)

/**
 * Metadata for module search results
 */
data class ModuleSearchMetadata(
    var appliedFilters: Map<String, Any> = emptyMap(),

    var availableCategories: List<String> = emptyList(),

    var availableStatuses: List<String> = emptyList(),

    var featuredCount: Int = 0,

    var installedCount: Int = 0,

    var enabledCount: Int = 0
)

/**
 * Response for available modules (master modules that can be installed)
 */
data class AvailableModulesResponse(
    var availableModules: List<MasterModuleResponse> = emptyList(),

    var recommendedModules: List<MasterModuleResponse> = emptyList(),

    var essentialModules: List<MasterModuleResponse> = emptyList(),

    var totalAvailable: Int = 0,

    var businessType: String? = null
)

/**
 * Response for bulk operations
 */
data class BulkOperationResponse(
    var operation: String = "",

    var totalRequested: Int = 0,

    var successfulOperations: List<String> = emptyList(),

    var failedOperations: List<FailedOperationDetail> = emptyList(),

    var successCount: Int = 0,

    var failureCount: Int = 0,

    var warnings: List<String> = emptyList()
)

data class FailedOperationDetail(
    var moduleId: String = "",

    var errorCode: String = "",

    var errorMessage: String = "",

    var moduleName: String? = null
)

/**
 * Response for module analytics and dashboard
 */
data class ModuleDashboardResponse(
    var totalModules: Int = 0,

    var activeModules: Int = 0,

    var inactiveModules: Int = 0,

    var modulesNeedingAttention: Int = 0,

    var modulesNeedingUpdates: Int = 0,

    var storageUsageMb: Long = 0,

    var mostUsedModules: List<WorkspaceModuleResponse> = emptyList(),

    var leastUsedModules: List<WorkspaceModuleResponse> = emptyList(),

    var categoryDistribution: Map<String, Int> = emptyMap(),

    var usageTrends: Map<String, Any> = emptyMap(),

    var healthOverview: ModuleHealthOverview = ModuleHealthOverview()
)

data class ModuleHealthOverview(
    var overallHealthScore: Double = 1.0,

    var healthyModules: Int = 0,

    var warningModules: Int = 0,

    var criticalModules: Int = 0,

    var errorRate: Double = 0.0,

    var userSatisfaction: Double = 0.0
)

/**
 * Response for module configuration export
 */
data class ModuleConfigurationExportResponse(
    var exportData: Map<String, Any> = emptyMap(),

    var exportMetadata: ExportMetadata = ExportMetadata(),

    var exportUrl: String? = null
)

data class ExportMetadata(
    var workspaceId: String = "",

    var workspaceName: String = "",

    var exportedAt: Instant = Instant.now(),

    var exportedBy: String = "",

    var moduleCount: Int = 0,

    var formatVersion: String = "1.0"
)

/**
 * Simple success response for operations
 */
data class ModuleOperationResponse(
    var operation: String = "",

    var moduleId: String? = null,

    var moduleName: String? = null,

    var success: Boolean = true,

    var message: String = "",

    var warnings: List<String> = emptyList()
)