package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.ModuleSettings
import com.ampairs.workspace.model.enums.WorkspaceModuleStatus
import jakarta.validation.constraints.*

/**
 * Request to install a module in workspace
 */
data class ModuleInstallRequest(
    @field:NotBlank(message = "Master module ID is required")
        var masterModuleId: String = "",

        var customName: String? = null,

        var customDescription: String? = null,

        var categoryOverride: String? = null,

    @field:Min(value = 0, message = "Display order must be non-negative")
        var displayOrder: Int = 0,

        var autoEnable: Boolean = true,

        var initialSettings: ModuleSettings? = null,

        var configurationNotes: String? = null
)

/**
 * Request to update module configuration
 */
data class ModuleConfigurationRequest(
        var customName: String? = null,

        var customDescription: String? = null,

        var customIcon: String? = null,

        var customColor: String? = null,

    @field:Min(value = 0, message = "Display order must be non-negative")
        var displayOrder: Int? = null,

        @field:Pattern(
        regexp = "^(VISIBLE|HIDDEN|ADMIN_ONLY)$",
        message = "Visibility must be VISIBLE, HIDDEN, or ADMIN_ONLY"
    )
    var visibility: String? = null,

        var enabledFeatures: List<String>? = null,

        var disabledFeatures: List<String>? = null,

        var customConfiguration: Map<String, Any>? = null,

        var notificationsEnabled: Boolean? = null,

        var autoUpdate: Boolean? = null,

        var quickAccess: Boolean? = null,

        var categoryOverride: String? = null,

        var configurationNotes: String? = null
)

/**
 * Request to update module status
 */
data class ModuleStatusUpdateRequest(
    @field:NotNull(message = "Status is required")
        var status: WorkspaceModuleStatus = WorkspaceModuleStatus.INSTALLED,

        var enabled: Boolean = true,

        var reason: String? = null
)

/**
 * Request to configure user preferences for a module
 */
data class UserModulePreferencesRequest(
        var customName: String? = null,

        var customIcon: String? = null,

    @field:Min(value = 0, message = "Display order must be non-negative")
        var displayOrder: Int? = null,

        var quickAccess: Boolean? = null,

        var notificationsEnabled: Boolean? = null,

        var favorited: Boolean? = null
)

/**
 * Request for bulk module operations
 */
data class BulkModuleOperationRequest(
    @field:NotEmpty(message = "Module IDs list cannot be empty")
        var moduleIds: List<String> = emptyList(),

    @field:NotBlank(message = "Operation is required")
    @field:Pattern(
        regexp = "^(ENABLE|DISABLE|UPDATE|UNINSTALL|SET_QUICK_ACCESS|REMOVE_QUICK_ACCESS)$",
        message = "Operation must be one of: ENABLE, DISABLE, UPDATE, UNINSTALL, SET_QUICK_ACCESS, REMOVE_QUICK_ACCESS"
    )
        var operation: String = "",

        var reason: String? = null
)

/**
 * Request to search and filter modules
 */
data class ModuleSearchRequest(
        var searchQuery: String? = null,

        var categoryFilter: String? = null,

        var statusFilter: String? = null,

        var installedOnly: Boolean = false,

        var enabledOnly: Boolean = false,

        var featuredOnly: Boolean = false,

        var businessRelevantOnly: Boolean = false,

        @field:Pattern(
        regexp = "^(NAME|CATEGORY|INSTALL_COUNT|RATING|DISPLAY_ORDER|INSTALLED_AT|LAST_ACCESS)$",
        message = "Sort field must be one of: NAME, CATEGORY, INSTALL_COUNT, RATING, DISPLAY_ORDER, INSTALLED_AT, LAST_ACCESS"
    )
    var sortBy: String = "DISPLAY_ORDER",

        @field:Pattern(regexp = "^(ASC|DESC)$", message = "Sort direction must be ASC or DESC")
    var sortDirection: String = "ASC",

    @field:Min(value = 0, message = "Page must be non-negative")
        var page: Int = 0,

    @field:Min(value = 1, message = "Size must be positive")
    @field:Max(value = 100, message = "Size cannot exceed 100")
        var size: Int = 20
)

/**
 * Request to rate a module
 */
data class ModuleRatingRequest(
    @field:DecimalMin(value = "1.0", message = "Rating must be at least 1.0")
    @field:DecimalMax(value = "5.0", message = "Rating cannot exceed 5.0")
        var rating: Double = 1.0,

        var review: String? = null
)

/**
 * Request to reorder modules
 */
data class ModuleReorderRequest(
    @field:NotEmpty(message = "Module order list cannot be empty")
        var moduleOrders: List<ModuleOrderItem> = emptyList()
)

data class ModuleOrderItem(
    @field:NotBlank(message = "Module ID is required")
        var moduleId: String = "",

    @field:Min(value = 0, message = "Display order must be non-negative")
        var displayOrder: Int = 0
)

/**
 * Request to export/import module configuration
 */
data class ModuleConfigurationExportRequest(
        var includeUserPreferences: Boolean = false,

        var includeUsageMetrics: Boolean = false,

        var moduleIds: List<String>? = null // null = export all modules
)

/**
 * Request to import module configuration
 */
data class ModuleConfigurationImportRequest(
    @field:NotNull(message = "Configuration data is required")
        var configurationData: Map<String, Any> = emptyMap(),

        var overwriteExisting: Boolean = false,

        var preserveUserPreferences: Boolean = true
)