package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.*
import com.ampairs.workspace.model.enums.WorkspaceModuleStatus
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Response for workspace module information
 */
data class WorkspaceModuleResponse(
    @JsonProperty("id")
    var id: String = "",

    @JsonProperty("seq_id")
    var seqId: Long = 0,

    @JsonProperty("workspace_id")
    var workspaceId: String = "",

    @JsonProperty("master_module")
    var masterModule: MasterModuleResponse = MasterModuleResponse(),

    @JsonProperty("status")
    var status: WorkspaceModuleStatus = WorkspaceModuleStatus.INSTALLED,

    @JsonProperty("enabled")
    var enabled: Boolean = true,

    @JsonProperty("installed_version")
    var installedVersion: String = "",

    @JsonProperty("installed_at")
    var installedAt: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("installed_by")
    var installedBy: String? = null,

    @JsonProperty("installed_by_name")
    var installedByName: String? = null,

    @JsonProperty("last_updated_at")
    var lastUpdatedAt: LocalDateTime? = null,

    @JsonProperty("last_updated_by")
    var lastUpdatedBy: String? = null,

    @JsonProperty("category_override")
    var categoryOverride: String? = null,

    @JsonProperty("display_order")
    var displayOrder: Int = 0,

    @JsonProperty("settings")
    var settings: ModuleSettings = ModuleSettings(),

    @JsonProperty("usage_metrics")
    var usageMetrics: ModuleUsageMetrics = ModuleUsageMetrics(),

    @JsonProperty("user_preferences")
    var userPreferences: List<UserModulePreferences> = emptyList(),

    @JsonProperty("license_info")
    var licenseInfo: String? = null,

    @JsonProperty("license_expires_at")
    var licenseExpiresAt: LocalDateTime? = null,

    @JsonProperty("storage_used_mb")
    var storageUsedMb: Int = 0,

    @JsonProperty("configuration_notes")
    var configurationNotes: String? = null,

    @JsonProperty("effective_name")
    var effectiveName: String = "",

    @JsonProperty("effective_description")
    var effectiveDescription: String = "",

    @JsonProperty("effective_icon")
    var effectiveIcon: String = "",

    @JsonProperty("effective_color")
    var effectiveColor: String = "",

    @JsonProperty("effective_category")
    var effectiveCategory: String = "",

    @JsonProperty("is_operational")
    var isOperational: Boolean = false,

    @JsonProperty("has_valid_license")
    var hasValidLicense: Boolean = true,

    @JsonProperty("can_be_updated")
    var canBeUpdated: Boolean = false,

    @JsonProperty("needs_attention")
    var needsAttention: Boolean = false,

    @JsonProperty("health_score")
    var healthScore: Double = 1.0,

    @JsonProperty("engagement_level")
    var engagementLevel: Double = 0.0,

    @JsonProperty("is_popular")
    var isPopular: Boolean = false,

    @JsonProperty("created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
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
                createdAt = workspaceModule.createdAt ?: LocalDateTime.now(),
                updatedAt = workspaceModule.updatedAt ?: LocalDateTime.now()
            )
        }
    }
}

/**
 * Response for master module information
 */
data class MasterModuleResponse(
    @JsonProperty("id")
    var id: String = "",

    @JsonProperty("seq_id")
    var seqId: Long = 0,

    @JsonProperty("module_code")
    var moduleCode: String = "",

    @JsonProperty("name")
    var name: String = "",

    @JsonProperty("description")
    var description: String? = null,

    @JsonProperty("tagline")
    var tagline: String? = null,

    @JsonProperty("category")
    var category: String = "",

    @JsonProperty("status")
    var status: String = "",

    @JsonProperty("required_tier")
    var requiredTier: String = "",

    @JsonProperty("required_role")
    var requiredRole: String = "",

    @JsonProperty("complexity")
    var complexity: String = "",

    @JsonProperty("version")
    var version: String = "",

    @JsonProperty("business_relevance")
    var businessRelevance: List<BusinessRelevance> = emptyList(),

    @JsonProperty("configuration")
    var configuration: ModuleConfiguration = ModuleConfiguration(),

    @JsonProperty("ui_metadata")
    var uiMetadata: ModuleUIMetadata = ModuleUIMetadata(),

    @JsonProperty("provider")
    var provider: String = "",

    @JsonProperty("support_email")
    var supportEmail: String? = null,

    @JsonProperty("documentation_url")
    var documentationUrl: String? = null,

    @JsonProperty("homepage_url")
    var homepageUrl: String? = null,

    @JsonProperty("setup_guide_url")
    var setupGuideUrl: String? = null,

    @JsonProperty("size_mb")
    var sizeMb: Int = 0,

    @JsonProperty("install_count")
    var installCount: Int = 0,

    @JsonProperty("rating")
    var rating: Double = 0.0,

    @JsonProperty("rating_count")
    var ratingCount: Int = 0,

    @JsonProperty("featured")
    var featured: Boolean = false,

    @JsonProperty("display_order")
    var displayOrder: Int = 0,

    @JsonProperty("active")
    var active: Boolean = true,

    @JsonProperty("release_notes")
    var releaseNotes: String? = null,

    @JsonProperty("last_updated_at")
    var lastUpdatedAt: LocalDateTime? = null,

    @JsonProperty("created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
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
                createdAt = masterModule.createdAt ?: LocalDateTime.now(),
                updatedAt = masterModule.updatedAt ?: LocalDateTime.now()
            )
        }
    }
}

/**
 * Response for module search and listing with pagination
 */
data class ModuleSearchResponse(
    @JsonProperty("modules")
    var modules: List<WorkspaceModuleResponse> = emptyList(),

    @JsonProperty("total_elements")
    var totalElements: Long = 0,

    @JsonProperty("total_pages")
    var totalPages: Int = 0,

    @JsonProperty("current_page")
    var currentPage: Int = 0,

    @JsonProperty("page_size")
    var pageSize: Int = 20,

    @JsonProperty("has_next")
    var hasNext: Boolean = false,

    @JsonProperty("has_previous")
    var hasPrevious: Boolean = false,

    @JsonProperty("search_metadata")
    var searchMetadata: ModuleSearchMetadata? = null
)

/**
 * Metadata for module search results
 */
data class ModuleSearchMetadata(
    @JsonProperty("applied_filters")
    var appliedFilters: Map<String, Any> = emptyMap(),

    @JsonProperty("available_categories")
    var availableCategories: List<String> = emptyList(),

    @JsonProperty("available_statuses")
    var availableStatuses: List<String> = emptyList(),

    @JsonProperty("featured_count")
    var featuredCount: Int = 0,

    @JsonProperty("installed_count")
    var installedCount: Int = 0,

    @JsonProperty("enabled_count")
    var enabledCount: Int = 0
)

/**
 * Response for available modules (master modules that can be installed)
 */
data class AvailableModulesResponse(
    @JsonProperty("available_modules")
    var availableModules: List<MasterModuleResponse> = emptyList(),

    @JsonProperty("recommended_modules")
    var recommendedModules: List<MasterModuleResponse> = emptyList(),

    @JsonProperty("essential_modules")
    var essentialModules: List<MasterModuleResponse> = emptyList(),

    @JsonProperty("total_available")
    var totalAvailable: Int = 0,

    @JsonProperty("business_type")
    var businessType: String? = null
)

/**
 * Response for bulk operations
 */
data class BulkOperationResponse(
    @JsonProperty("operation")
    var operation: String = "",

    @JsonProperty("total_requested")
    var totalRequested: Int = 0,

    @JsonProperty("successful_operations")
    var successfulOperations: List<String> = emptyList(),

    @JsonProperty("failed_operations")
    var failedOperations: List<FailedOperationDetail> = emptyList(),

    @JsonProperty("success_count")
    var successCount: Int = 0,

    @JsonProperty("failure_count")
    var failureCount: Int = 0,

    @JsonProperty("warnings")
    var warnings: List<String> = emptyList()
)

data class FailedOperationDetail(
    @JsonProperty("module_id")
    var moduleId: String = "",

    @JsonProperty("error_code")
    var errorCode: String = "",

    @JsonProperty("error_message")
    var errorMessage: String = "",

    @JsonProperty("module_name")
    var moduleName: String? = null
)

/**
 * Response for module analytics and dashboard
 */
data class ModuleDashboardResponse(
    @JsonProperty("total_modules")
    var totalModules: Int = 0,

    @JsonProperty("active_modules")
    var activeModules: Int = 0,

    @JsonProperty("inactive_modules")
    var inactiveModules: Int = 0,

    @JsonProperty("modules_needing_attention")
    var modulesNeedingAttention: Int = 0,

    @JsonProperty("modules_needing_updates")
    var modulesNeedingUpdates: Int = 0,

    @JsonProperty("storage_usage_mb")
    var storageUsageMb: Long = 0,

    @JsonProperty("most_used_modules")
    var mostUsedModules: List<WorkspaceModuleResponse> = emptyList(),

    @JsonProperty("least_used_modules")
    var leastUsedModules: List<WorkspaceModuleResponse> = emptyList(),

    @JsonProperty("category_distribution")
    var categoryDistribution: Map<String, Int> = emptyMap(),

    @JsonProperty("usage_trends")
    var usageTrends: Map<String, Any> = emptyMap(),

    @JsonProperty("health_overview")
    var healthOverview: ModuleHealthOverview = ModuleHealthOverview()
)

data class ModuleHealthOverview(
    @JsonProperty("overall_health_score")
    var overallHealthScore: Double = 1.0,

    @JsonProperty("healthy_modules")
    var healthyModules: Int = 0,

    @JsonProperty("warning_modules")
    var warningModules: Int = 0,

    @JsonProperty("critical_modules")
    var criticalModules: Int = 0,

    @JsonProperty("error_rate")
    var errorRate: Double = 0.0,

    @JsonProperty("user_satisfaction")
    var userSatisfaction: Double = 0.0
)

/**
 * Response for module configuration export
 */
data class ModuleConfigurationExportResponse(
    @JsonProperty("export_data")
    var exportData: Map<String, Any> = emptyMap(),

    @JsonProperty("export_metadata")
    var exportMetadata: ExportMetadata = ExportMetadata(),

    @JsonProperty("export_url")
    var exportUrl: String? = null
)

data class ExportMetadata(
    @JsonProperty("workspace_id")
    var workspaceId: String = "",

    @JsonProperty("workspace_name")
    var workspaceName: String = "",

    @JsonProperty("exported_at")
    var exportedAt: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("exported_by")
    var exportedBy: String = "",

    @JsonProperty("module_count")
    var moduleCount: Int = 0,

    @JsonProperty("format_version")
    var formatVersion: String = "1.0"
)

/**
 * Simple success response for operations
 */
data class ModuleOperationResponse(
    @JsonProperty("operation")
    var operation: String = "",

    @JsonProperty("module_id")
    var moduleId: String? = null,

    @JsonProperty("module_name")
    var moduleName: String? = null,

    @JsonProperty("success")
    var success: Boolean = true,

    @JsonProperty("message")
    var message: String = "",

    @JsonProperty("warnings")
    var warnings: List<String> = emptyList()
)