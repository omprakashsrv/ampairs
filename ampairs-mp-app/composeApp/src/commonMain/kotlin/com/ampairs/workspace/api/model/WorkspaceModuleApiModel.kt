package com.ampairs.workspace.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Workspace Module API Models
 *
 * Data models for module management functionality
 */
object WorkspaceModuleApiModel {

    @Serializable
    data class WorkspaceModule(
        @SerialName("id") val id: String,
        @SerialName("seq_id") val seqId: Long,
        @SerialName("workspace_id") val workspaceId: String,
        @SerialName("master_module") val masterModule: MasterModule,
        @SerialName("status") val status: String,
        @SerialName("enabled") val enabled: Boolean,
        @SerialName("installed_version") val installedVersion: String,
        @SerialName("installed_at") val installedAt: String,
        @SerialName("installed_by") val installedBy: String? = null,
        @SerialName("installed_by_name") val installedByName: String? = null,
        @SerialName("last_updated_at") val lastUpdatedAt: String? = null,
        @SerialName("last_updated_by") val lastUpdatedBy: String? = null,
        @SerialName("category_override") val categoryOverride: String? = null,
        @SerialName("display_order") val displayOrder: Int,
        @SerialName("settings") val settings: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
        @SerialName("usage_metrics") val usageMetrics: ModuleUsageMetrics = ModuleUsageMetrics(),
        @SerialName("user_preferences") val userPreferences: List<UserModulePreferences> = emptyList(),
        @SerialName("license_info") val licenseInfo: String? = null,
        @SerialName("license_expires_at") val licenseExpiresAt: String? = null,
        @SerialName("storage_used_mb") val storageUsedMb: Int,
        @SerialName("configuration_notes") val configurationNotes: String? = null,
        @SerialName("effective_name") val effectiveName: String,
        @SerialName("effective_description") val effectiveDescription: String,
        @SerialName("effective_icon") val effectiveIcon: String,
        @SerialName("effective_color") val effectiveColor: String,
        @SerialName("effective_category") val effectiveCategory: String,
        @SerialName("is_operational") val isOperational: Boolean,
        @SerialName("has_valid_license") val hasValidLicense: Boolean,
        @SerialName("can_be_updated") val canBeUpdated: Boolean,
        @SerialName("needs_attention") val needsAttention: Boolean,
        @SerialName("health_score") val healthScore: Double,
        @SerialName("engagement_level") val engagementLevel: Double,
        @SerialName("is_popular") val isPopular: Boolean,
        @SerialName("created_at") val createdAt: String,
        @SerialName("updated_at") val updatedAt: String,
    )

    @Serializable
    data class MasterModule(
        @SerialName("id") val id: String,
        @SerialName("seq_id") val seqId: Long,
        @SerialName("module_code") val moduleCode: String,
        @SerialName("name") val name: String,
        @SerialName("description") val description: String? = null,
        @SerialName("tagline") val tagline: String? = null,
        @SerialName("category") val category: String,
        @SerialName("status") val status: String,
        @SerialName("required_tier") val requiredTier: String,
        @SerialName("required_role") val requiredRole: String,
        @SerialName("complexity") val complexity: String,
        @SerialName("version") val version: String,
        @SerialName("business_relevance") val businessRelevance: List<BusinessRelevance> = emptyList(),
        @SerialName("configuration") val configuration: ModuleConfiguration = ModuleConfiguration(),
        @SerialName("ui_metadata") val uiMetadata: ModuleUIMetadata = ModuleUIMetadata(),
        @SerialName("provider") val provider: String,
        @SerialName("support_email") val supportEmail: String? = null,
        @SerialName("documentation_url") val documentationUrl: String? = null,
        @SerialName("homepage_url") val homepageUrl: String? = null,
        @SerialName("setup_guide_url") val setupGuideUrl: String? = null,
        @SerialName("size_mb") val sizeMb: Int,
        @SerialName("install_count") val installCount: Int,
        @SerialName("rating") val rating: Double,
        @SerialName("rating_count") val ratingCount: Int,
        @SerialName("featured") val featured: Boolean,
        @SerialName("display_order") val displayOrder: Int,
        @SerialName("active") val active: Boolean,
        @SerialName("release_notes") val releaseNotes: String? = null,
        @SerialName("last_updated_at") val lastUpdatedAt: String? = null,
        @SerialName("created_at") val createdAt: String,
        @SerialName("updated_at") val updatedAt: String,
    )

    @Serializable
    data class ModuleUsageMetrics(
        @SerialName("daily_active_users") val dailyActiveUsers: Int? = null,
        @SerialName("monthly_access") val monthlyAccess: Int? = null,
        @SerialName("average_session_duration") val averageSessionDuration: String? = null,
        @SerialName("last_accessed") val lastAccessed: String? = null,
        @SerialName("total_operations") val totalOperations: Int? = null,
        @SerialName("error_count") val errorCount: Int? = null,
    )

    @Serializable
    data class UserModulePreferences(
        @SerialName("user_id") val userId: String,
        @SerialName("preferences") val preferences: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
        @SerialName("created_at") val createdAt: String,
        @SerialName("updated_at") val updatedAt: String,
    )

    @Serializable
    data class BusinessRelevance(
        @SerialName("industry") val industry: String,
        @SerialName("use_case") val useCase: String,
        @SerialName("priority") val priority: Int,
    )

    @Serializable
    data class ModuleConfiguration(
        @SerialName("default_settings") val defaultSettings: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
        @SerialName("required_fields") val requiredFields: List<String> = emptyList(),
        @SerialName("optional_fields") val optionalFields: List<String> = emptyList(),
        @SerialName("validation_rules") val validationRules: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
    )

    @Serializable
    data class ModuleUIMetadata(
        @SerialName("icon") val icon: String = "",
        @SerialName("color") val color: String = "",
        @SerialName("theme") val theme: String? = null,
        @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
        @SerialName("screenshots") val screenshots: List<String> = emptyList(),
        @SerialName("category_icon") val categoryIcon: String? = null,
        @SerialName("category_color") val categoryColor: String? = null,
    )

    // Response Models
    @Serializable
    data class ModuleOverviewResponse(
        @SerialName("workspace_id") val workspaceId: String,
        @SerialName("message") val message: String,
        @SerialName("total_modules") val totalModules: Int,
        @SerialName("active_modules") val activeModules: Int,
        @SerialName("module_categories") val moduleCategories: List<String>,
        @SerialName("recent_activity") val recentActivity: RecentActivity = RecentActivity(),
        @SerialName("quick_actions") val quickActions: List<String> = emptyList(),
    )

    @Serializable
    data class RecentActivity(
        @SerialName("last_installed") val lastInstalled: String? = null,
        @SerialName("last_configured") val lastConfigured: String? = null,
        @SerialName("last_accessed") val lastAccessed: String? = null,
    )

    @Serializable
    data class ModuleDetailResponse(
        @SerialName("module_id") val moduleId: String,
        @SerialName("workspace_id") val workspaceId: String,
        @SerialName("message") val message: String,
        @SerialName("module_info") val moduleInfo: WorkspaceModule,
        @SerialName("configuration") val configuration: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
        @SerialName("analytics") val analytics: ModuleAnalytics = ModuleAnalytics(),
        @SerialName("permissions") val permissions: ModulePermissions = ModulePermissions(),
    )

    @Serializable
    data class ModuleAnalytics(
        @SerialName("daily_active_users") val dailyActiveUsers: Int = 0,
        @SerialName("monthly_access") val monthlyAccess: Int = 0,
        @SerialName("average_session_duration") val averageSessionDuration: String = "0 minutes",
    )

    @Serializable
    data class ModulePermissions(
        @SerialName("can_configure") val canConfigure: Boolean = false,
        @SerialName("can_uninstall") val canUninstall: Boolean = false,
        @SerialName("can_view_analytics") val canViewAnalytics: Boolean = false,
    )

    @Serializable
    data class ModuleSearchResponse(
        @SerialName("modules") val modules: List<WorkspaceModule>,
        @SerialName("total_elements") val totalElements: Long,
        @SerialName("total_pages") val totalPages: Int,
        @SerialName("current_page") val currentPage: Int,
        @SerialName("page_size") val pageSize: Int,
        @SerialName("has_next") val hasNext: Boolean,
        @SerialName("has_previous") val hasPrevious: Boolean,
        @SerialName("search_metadata") val searchMetadata: ModuleSearchMetadata? = null,
    )

    @Serializable
    data class ModuleSearchMetadata(
        @SerialName("applied_filters") val appliedFilters: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
        @SerialName("available_categories") val availableCategories: List<String> = emptyList(),
        @SerialName("available_statuses") val availableStatuses: List<String> = emptyList(),
        @SerialName("featured_count") val featuredCount: Int = 0,
        @SerialName("installed_count") val installedCount: Int = 0,
        @SerialName("enabled_count") val enabledCount: Int = 0,
    )

    @Serializable
    data class AvailableModulesResponse(
        @SerialName("available_modules") val availableModules: List<MasterModule>,
        @SerialName("recommended_modules") val recommendedModules: List<MasterModule>,
        @SerialName("essential_modules") val essentialModules: List<MasterModule>,
        @SerialName("total_available") val totalAvailable: Int,
        @SerialName("business_type") val businessType: String? = null,
    )

    @Serializable
    data class MasterModuleSearchResponse(
        @SerialName("modules") val modules: List<MasterModule>,
        @SerialName("total") val total: Int,
    )

    @Serializable
    data class ModuleDashboardResponse(
        @SerialName("total_modules") val totalModules: Int,
        @SerialName("active_modules") val activeModules: Int,
        @SerialName("inactive_modules") val inactiveModules: Int,
        @SerialName("modules_needing_attention") val modulesNeedingAttention: Int,
        @SerialName("modules_needing_updates") val modulesNeedingUpdates: Int,
        @SerialName("storage_usage_mb") val storageUsageMb: Long,
        @SerialName("most_used_modules") val mostUsedModules: List<WorkspaceModule>,
        @SerialName("least_used_modules") val leastUsedModules: List<WorkspaceModule>,
        @SerialName("category_distribution") val categoryDistribution: Map<String, Int>,
        @SerialName("usage_trends") val usageTrends: Map<String, kotlinx.serialization.json.JsonElement>,
        @SerialName("health_overview") val healthOverview: ModuleHealthOverview,
    )

    @Serializable
    data class ModuleHealthOverview(
        @SerialName("overall_health_score") val overallHealthScore: Double = 1.0,
        @SerialName("healthy_modules") val healthyModules: Int = 0,
        @SerialName("warning_modules") val warningModules: Int = 0,
        @SerialName("critical_modules") val criticalModules: Int = 0,
        @SerialName("error_rate") val errorRate: Double = 0.0,
        @SerialName("user_satisfaction") val userSatisfaction: Double = 0.0,
    )

    // Request Models
    @Serializable
    data class ModuleActionRequest(
        @SerialName("action") val action: String,
        @SerialName("parameters") val parameters: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    )

    @Serializable
    data class ModuleInstallationRequest(
        @SerialName("master_module_id") val masterModuleId: String,
        @SerialName("display_order") val displayOrder: Int? = null,
        @SerialName("initial_settings") val initialSettings: Map<String, kotlinx.serialization.json.JsonElement>? = null,
        @SerialName("category_override") val categoryOverride: String? = null,
    )

    @Serializable
    data class ModuleConfigurationRequest(
        @SerialName("settings") val settings: Map<String, kotlinx.serialization.json.JsonElement>,
        @SerialName("user_preferences") val userPreferences: Map<String, kotlinx.serialization.json.JsonElement>? = null,
        @SerialName("notes") val notes: String? = null,
    )

    @Serializable
    data class BulkOperationRequest(
        @SerialName("operation") val operation: String,
        @SerialName("module_ids") val moduleIds: List<String>,
        @SerialName("parameters") val parameters: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    )

    // Response Models
    @Serializable
    data class ModuleActionResponse(
        @SerialName("module_id") val moduleId: String,
        @SerialName("action") val action: String,
        @SerialName("workspace_id") val workspaceId: String,
        @SerialName("success") val success: Boolean,
        @SerialName("message") val message: String,
        @SerialName("action_details") val actionDetails: ActionDetails = ActionDetails(),
        @SerialName("impact") val impact: ActionImpact = ActionImpact(),
        @SerialName("next_steps") val nextSteps: List<String> = emptyList(),
    )

    @Serializable
    data class ActionDetails(
        @SerialName("executed_at") val executedAt: String = "",
        @SerialName("executed_by") val executedBy: String = "",
        @SerialName("duration") val duration: String = "",
        @SerialName("affected_components") val affectedComponents: List<String> = emptyList(),
    )

    @Serializable
    data class ActionImpact(
        @SerialName("users_affected") val usersAffected: Int = 0,
        @SerialName("data_changed") val dataChanged: Boolean = false,
        @SerialName("requires_restart") val requiresRestart: Boolean = false,
        @SerialName("immediately_available") val immediatelyAvailable: Boolean = true,
    )

    @Serializable
    data class BulkOperationResponse(
        @SerialName("operation") val operation: String,
        @SerialName("total_requested") val totalRequested: Int,
        @SerialName("successful_operations") val successfulOperations: List<String>,
        @SerialName("failed_operations") val failedOperations: List<FailedOperationDetail>,
        @SerialName("success_count") val successCount: Int,
        @SerialName("failure_count") val failureCount: Int,
        @SerialName("warnings") val warnings: List<String>,
    )

    @Serializable
    data class FailedOperationDetail(
        @SerialName("module_id") val moduleId: String,
        @SerialName("error_code") val errorCode: String,
        @SerialName("error_message") val errorMessage: String,
        @SerialName("module_name") val moduleName: String? = null,
    )

    @Serializable
    data class ModuleOperationResponse(
        @SerialName("operation") val operation: String,
        @SerialName("module_id") val moduleId: String? = null,
        @SerialName("module_name") val moduleName: String? = null,
        @SerialName("success") val success: Boolean,
        @SerialName("message") val message: String,
        @SerialName("warnings") val warnings: List<String> = emptyList(),
    )

    @Serializable
    data class ModuleAnalyticsResponse(
        @SerialName("module_id") val moduleId: String,
        @SerialName("period") val period: String,
        @SerialName("analytics_data") val analyticsData: Map<String, kotlinx.serialization.json.JsonElement>,
    )

    @Serializable
    data class ModuleConfigurationExportResponse(
        @SerialName("export_data") val exportData: Map<String, kotlinx.serialization.json.JsonElement>,
        @SerialName("export_metadata") val exportMetadata: ExportMetadata = ExportMetadata(),
        @SerialName("export_url") val exportUrl: String? = null,
    )

    @Serializable
    data class ExportMetadata(
        @SerialName("workspace_id") val workspaceId: String = "",
        @SerialName("workspace_name") val workspaceName: String = "",
        @SerialName("exported_at") val exportedAt: String = "",
        @SerialName("exported_by") val exportedBy: String = "",
        @SerialName("module_count") val moduleCount: Int = 0,
        @SerialName("format_version") val formatVersion: String = "1.0",
    )

    @Serializable
    data class ModuleUpdatesResponse(
        @SerialName("total_updates_available") val totalUpdatesAvailable: Int,
        @SerialName("modules_with_updates") val modulesWithUpdates: List<WorkspaceModule>,
        @SerialName("update_details") val updateDetails: Map<String, kotlinx.serialization.json.JsonElement>,
    )

    @Serializable
    data class ModuleHealthResponse(
        @SerialName("module_id") val moduleId: String,
        @SerialName("health_score") val healthScore: Double,
        @SerialName("status") val status: String,
        @SerialName("diagnostics") val diagnostics: Map<String, kotlinx.serialization.json.JsonElement>,
        @SerialName("recommendations") val recommendations: List<String>,
    )

    // Enums
    enum class WorkspaceModuleStatus(val value: String) {
        INSTALLED("INSTALLED"),
        PENDING("PENDING"),
        FAILED("FAILED"),
        UPDATING("UPDATING"),
        DISABLED("DISABLED"),
        UNINSTALLING("UNINSTALLING")
    }

    enum class ModuleCategory(val value: String) {
        CUSTOMER_MANAGEMENT("CUSTOMER_MANAGEMENT"),
        SALES_MANAGEMENT("SALES_MANAGEMENT"),
        INVENTORY_MANAGEMENT("INVENTORY_MANAGEMENT"),
        FINANCIAL_MANAGEMENT("FINANCIAL_MANAGEMENT"),
        PROJECT_MANAGEMENT("PROJECT_MANAGEMENT"),
        ANALYTICS_REPORTING("ANALYTICS_REPORTING"),
        MARKETING_AUTOMATION("MARKETING_AUTOMATION"),
        HUMAN_RESOURCES("HUMAN_RESOURCES"),
        OPERATIONS_MANAGEMENT("OPERATIONS_MANAGEMENT"),
        COMMUNICATION("COMMUNICATION"),
        INTEGRATION("INTEGRATION"),
        UTILITIES("UTILITIES")
    }
}