package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.ModuleSettings
import com.ampairs.workspace.model.enums.WorkspaceModuleStatus
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*

/**
 * Request to install a module in workspace
 */
data class ModuleInstallRequest(
    @field:NotBlank(message = "Master module ID is required")
    @JsonProperty("master_module_id")
    var masterModuleId: String = "",

    @JsonProperty("custom_name")
    var customName: String? = null,

    @JsonProperty("custom_description")
    var customDescription: String? = null,

    @JsonProperty("category_override")
    var categoryOverride: String? = null,

    @field:Min(value = 0, message = "Display order must be non-negative")
    @JsonProperty("display_order")
    var displayOrder: Int = 0,

    @JsonProperty("auto_enable")
    var autoEnable: Boolean = true,

    @JsonProperty("initial_settings")
    var initialSettings: ModuleSettings? = null,

    @JsonProperty("configuration_notes")
    var configurationNotes: String? = null
)

/**
 * Request to update module configuration
 */
data class ModuleConfigurationRequest(
    @JsonProperty("custom_name")
    var customName: String? = null,

    @JsonProperty("custom_description")
    var customDescription: String? = null,

    @JsonProperty("custom_icon")
    var customIcon: String? = null,

    @JsonProperty("custom_color")
    var customColor: String? = null,

    @field:Min(value = 0, message = "Display order must be non-negative")
    @JsonProperty("display_order")
    var displayOrder: Int? = null,

    @JsonProperty("visibility")
    @field:Pattern(
        regexp = "^(VISIBLE|HIDDEN|ADMIN_ONLY)$",
        message = "Visibility must be VISIBLE, HIDDEN, or ADMIN_ONLY"
    )
    var visibility: String? = null,

    @JsonProperty("enabled_features")
    var enabledFeatures: List<String>? = null,

    @JsonProperty("disabled_features")
    var disabledFeatures: List<String>? = null,

    @JsonProperty("custom_configuration")
    var customConfiguration: Map<String, Any>? = null,

    @JsonProperty("notifications_enabled")
    var notificationsEnabled: Boolean? = null,

    @JsonProperty("auto_update")
    var autoUpdate: Boolean? = null,

    @JsonProperty("quick_access")
    var quickAccess: Boolean? = null,

    @JsonProperty("category_override")
    var categoryOverride: String? = null,

    @JsonProperty("configuration_notes")
    var configurationNotes: String? = null
)

/**
 * Request to update module status
 */
data class ModuleStatusUpdateRequest(
    @field:NotNull(message = "Status is required")
    @JsonProperty("status")
    var status: WorkspaceModuleStatus = WorkspaceModuleStatus.INSTALLED,

    @JsonProperty("enabled")
    var enabled: Boolean = true,

    @JsonProperty("reason")
    var reason: String? = null
)

/**
 * Request to configure user preferences for a module
 */
data class UserModulePreferencesRequest(
    @JsonProperty("custom_name")
    var customName: String? = null,

    @JsonProperty("custom_icon")
    var customIcon: String? = null,

    @field:Min(value = 0, message = "Display order must be non-negative")
    @JsonProperty("display_order")
    var displayOrder: Int? = null,

    @JsonProperty("quick_access")
    var quickAccess: Boolean? = null,

    @JsonProperty("notifications_enabled")
    var notificationsEnabled: Boolean? = null,

    @JsonProperty("favorited")
    var favorited: Boolean? = null
)

/**
 * Request for bulk module operations
 */
data class BulkModuleOperationRequest(
    @field:NotEmpty(message = "Module IDs list cannot be empty")
    @JsonProperty("module_ids")
    var moduleIds: List<String> = emptyList(),

    @field:NotBlank(message = "Operation is required")
    @field:Pattern(
        regexp = "^(ENABLE|DISABLE|UPDATE|UNINSTALL|SET_QUICK_ACCESS|REMOVE_QUICK_ACCESS)$",
        message = "Operation must be one of: ENABLE, DISABLE, UPDATE, UNINSTALL, SET_QUICK_ACCESS, REMOVE_QUICK_ACCESS"
    )
    @JsonProperty("operation")
    var operation: String = "",

    @JsonProperty("reason")
    var reason: String? = null
)

/**
 * Request to search and filter modules
 */
data class ModuleSearchRequest(
    @JsonProperty("search_query")
    var searchQuery: String? = null,

    @JsonProperty("category_filter")
    var categoryFilter: String? = null,

    @JsonProperty("status_filter")
    var statusFilter: String? = null,

    @JsonProperty("installed_only")
    var installedOnly: Boolean = false,

    @JsonProperty("enabled_only")
    var enabledOnly: Boolean = false,

    @JsonProperty("featured_only")
    var featuredOnly: Boolean = false,

    @JsonProperty("business_relevant_only")
    var businessRelevantOnly: Boolean = false,

    @JsonProperty("sort_by")
    @field:Pattern(
        regexp = "^(NAME|CATEGORY|INSTALL_COUNT|RATING|DISPLAY_ORDER|INSTALLED_AT|LAST_ACCESS)$",
        message = "Sort field must be one of: NAME, CATEGORY, INSTALL_COUNT, RATING, DISPLAY_ORDER, INSTALLED_AT, LAST_ACCESS"
    )
    var sortBy: String = "DISPLAY_ORDER",

    @JsonProperty("sort_direction")
    @field:Pattern(regexp = "^(ASC|DESC)$", message = "Sort direction must be ASC or DESC")
    var sortDirection: String = "ASC",

    @field:Min(value = 0, message = "Page must be non-negative")
    @JsonProperty("page")
    var page: Int = 0,

    @field:Min(value = 1, message = "Size must be positive")
    @field:Max(value = 100, message = "Size cannot exceed 100")
    @JsonProperty("size")
    var size: Int = 20
)

/**
 * Request to rate a module
 */
data class ModuleRatingRequest(
    @field:DecimalMin(value = "1.0", message = "Rating must be at least 1.0")
    @field:DecimalMax(value = "5.0", message = "Rating cannot exceed 5.0")
    @JsonProperty("rating")
    var rating: Double = 1.0,

    @JsonProperty("review")
    var review: String? = null
)

/**
 * Request to reorder modules
 */
data class ModuleReorderRequest(
    @field:NotEmpty(message = "Module order list cannot be empty")
    @JsonProperty("module_orders")
    var moduleOrders: List<ModuleOrderItem> = emptyList()
)

data class ModuleOrderItem(
    @field:NotBlank(message = "Module ID is required")
    @JsonProperty("module_id")
    var moduleId: String = "",

    @field:Min(value = 0, message = "Display order must be non-negative")
    @JsonProperty("display_order")
    var displayOrder: Int = 0
)

/**
 * Request to export/import module configuration
 */
data class ModuleConfigurationExportRequest(
    @JsonProperty("include_user_preferences")
    var includeUserPreferences: Boolean = false,

    @JsonProperty("include_usage_metrics")
    var includeUsageMetrics: Boolean = false,

    @JsonProperty("module_ids")
    var moduleIds: List<String>? = null // null = export all modules
)

/**
 * Request to import module configuration
 */
data class ModuleConfigurationImportRequest(
    @field:NotNull(message = "Configuration data is required")
    @JsonProperty("configuration_data")
    var configurationData: Map<String, Any> = emptyMap(),

    @JsonProperty("overwrite_existing")
    var overwriteExisting: Boolean = false,

    @JsonProperty("preserve_user_preferences")
    var preserveUserPreferences: Boolean = true
)