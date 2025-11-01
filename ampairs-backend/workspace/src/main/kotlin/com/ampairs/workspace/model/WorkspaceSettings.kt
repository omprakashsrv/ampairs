package com.ampairs.workspace.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * Material Design 3 theme configuration
 */
data class MaterialTheme(
    var mode: String = "light", // light, dark, auto
    var colorScheme: String = "static", // static, dynamic (Material You)
    var seedColor: String? = null, // For dynamic theming
    var highContrast: Boolean = false
)

/**
 * Material Design 3 color palette
 */
data class MaterialColors(
    var primary: String = "#6750A4",
    var onPrimary: String = "#FFFFFF",
    var primaryContainer: String = "#EADDFF",
    var onPrimaryContainer: String = "#21005D",

    var secondary: String = "#625B71",
    var onSecondary: String = "#FFFFFF",
    var secondaryContainer: String = "#E8DEF8",
    var onSecondaryContainer: String = "#1D192B",

    var tertiary: String = "#7D5260",
    var onTertiary: String = "#FFFFFF",
    var tertiaryContainer: String = "#FFD8E4",
    var onTertiaryContainer: String = "#31111D",

    var error: String = "#BA1A1A",
    var onError: String = "#FFFFFF",
    var errorContainer: String = "#FFDAD6",
    var onErrorContainer: String = "#410002",

    var background: String = "#FFFBFE",
    var onBackground: String = "#1C1B1F",
    var surface: String = "#FFFBFE",
    var onSurface: String = "#1C1B1F",
    var surfaceVariant: String = "#E7E0EC",
    var onSurfaceVariant: String = "#49454F",

    var outline: String = "#79747E",
    var outlineVariant: String = "#CAC4D0",
    var scrim: String = "#000000"
)

/**
 * Material Design 3 typography scale
 */
data class MaterialTypography(
    var displayFont: String = "Roboto",
    var headlineFont: String = "Roboto",
    var titleFont: String = "Roboto",
    var bodyFont: String = "Roboto",
    var labelFont: String = "Roboto",

    var displayLarge: TypographyStyle = TypographyStyle(size = 57, weight = 400, lineHeight = 64),
    var displayMedium: TypographyStyle = TypographyStyle(size = 45, weight = 400, lineHeight = 52),
    var displaySmall: TypographyStyle = TypographyStyle(size = 36, weight = 400, lineHeight = 44),

    var headlineLarge: TypographyStyle = TypographyStyle(size = 32, weight = 400, lineHeight = 40),
    var headlineMedium: TypographyStyle = TypographyStyle(size = 28, weight = 400, lineHeight = 36),
    var headlineSmall: TypographyStyle = TypographyStyle(size = 24, weight = 400, lineHeight = 32),

    var titleLarge: TypographyStyle = TypographyStyle(size = 22, weight = 400, lineHeight = 28),
    var titleMedium: TypographyStyle = TypographyStyle(size = 16, weight = 500, lineHeight = 24),
    var titleSmall: TypographyStyle = TypographyStyle(size = 14, weight = 500, lineHeight = 20),

    var bodyLarge: TypographyStyle = TypographyStyle(size = 16, weight = 400, lineHeight = 24),
    var bodyMedium: TypographyStyle = TypographyStyle(size = 14, weight = 400, lineHeight = 20),
    var bodySmall: TypographyStyle = TypographyStyle(size = 12, weight = 400, lineHeight = 16),

    var labelLarge: TypographyStyle = TypographyStyle(size = 14, weight = 500, lineHeight = 20),
    var labelMedium: TypographyStyle = TypographyStyle(size = 12, weight = 500, lineHeight = 16),
    var labelSmall: TypographyStyle = TypographyStyle(size = 11, weight = 500, lineHeight = 16)
)

/**
 * Typography style definition
 */
data class TypographyStyle(
    var size: Int = 14,
    var weight: Int = 400,
    var lineHeight: Int = 20,
    var letterSpacing: Double = 0.0
)

/**
 * Workspace-specific settings and customizations.
 * Simplified structure with commonly used settings as direct fields.
 */
@Entity
@Table(
    name = "workspace_settings",
    indexes = [
        Index(name = "idx_settings_workspace", columnList = "workspace_id", unique = true)
    ]
)
class WorkspaceSettings : BaseDomain() {

    /**
     * ID of the workspace these settings belong to
     */
    @Column(name = "workspace_id", nullable = false, unique = true, length = 36)
    var workspaceId: String = ""

    // Material Design 3 Theme Settings

    /**
     * Company logo URL
     */
    @Column(name = "logo_url", length = 500)
    var logoUrl: String? = null

    /**
     * Material Design 3 theme configuration (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "material_theme")
    var materialTheme: MaterialTheme = MaterialTheme()

    /**
     * Material Design 3 color palette (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "material_colors")
    var materialColors: MaterialColors = MaterialColors()

    /**
     * Material Design 3 typography configuration (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "material_typography")
    var materialTypography: MaterialTypography = MaterialTypography()

    // Business Settings

    /**
     * Business operation settings (JSON)
     * Contains: working hours, document prefixes, auto-generation settings
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "business_settings")
    var businessSettings: Map<String, Any> = mapOf(
        "workingHours" to mapOf(
            "start" to "09:00",
            "end" to "17:00",
            "days" to listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        ),
        "prefixes" to mapOf(
            "invoice" to "INV",
            "order" to "ORD"
        ),
        "autoGenerate" to mapOf(
            "invoices" to true,
            "orders" to true
        )
    )

    /**
     * ID of user who last modified settings
     */
    @Column(name = "last_modified_by", length = 36)
    var lastModifiedBy: String? = null

    /**
     * Name of user who last modified settings
     */
    @Column(name = "last_modified_by_name", length = 255)
    var lastModifiedByName: String? = null

    // JPA Relationships

    // JPA Relationships
    // Note: Removed workspace relationship mapping to avoid column conflict
    // The workspaceId string field above is used instead of entity relationship

    override fun obtainSeqIdPrefix(): String {
        return Constants.WORKSPACE_SETTINGS_PREFIX
    }


    /**
     * Update modification tracking
     */
    fun updateModification(modifiedBy: String?, modifiedByName: String? = null) {
        lastModifiedBy = modifiedBy
        lastModifiedByName = modifiedByName
    }

    // Business Settings Helper Methods

    /**
     * Get business setting value
     */
    fun getBusinessSetting(key: String): Any? {
        return businessSettings[key]
    }

    /**
     * Set business setting value
     */
    fun setBusinessSetting(key: String, value: Any, modifiedBy: String? = null, modifiedByName: String? = null) {
        businessSettings = businessSettings.toMutableMap().apply { put(key, value) }
        updateModification(modifiedBy, modifiedByName)
    }

    /**
     * Get working hours start time
     */
    fun getWorkingHoursStart(): String {
        return try {
            val workingHours = businessSettings["workingHours"] as? Map<String, Any>
            workingHours?.get("start") as? String ?: "09:00"
        } catch (e: Exception) {
            "09:00"
        }
    }

    /**
     * Get working hours end time
     */
    fun getWorkingHoursEnd(): String {
        return try {
            val workingHours = businessSettings["workingHours"] as? Map<String, Any>
            workingHours?.get("end") as? String ?: "17:00"
        } catch (e: Exception) {
            "17:00"
        }
    }

    /**
     * Get working days
     */
    fun getWorkingDays(): List<String> {
        return try {
            val workingHours = businessSettings["workingHours"] as? Map<String, Any>
            workingHours?.get("days") as? List<String> ?: listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        } catch (e: Exception) {
            listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        }
    }

    /**
     * Get invoice prefix
     */
    fun getInvoicePrefix(): String {
        return try {
            val prefixes = businessSettings["prefixes"] as? Map<String, Any>
            prefixes?.get("invoice") as? String ?: "INV"
        } catch (e: Exception) {
            "INV"
        }
    }

    /**
     * Get order prefix
     */
    fun getOrderPrefix(): String {
        return try {
            val prefixes = businessSettings["prefixes"] as? Map<String, Any>
            prefixes?.get("order") as? String ?: "ORD"
        } catch (e: Exception) {
            "ORD"
        }
    }

    /**
     * Check if auto-generate invoices is enabled
     */
    fun isAutoGenerateInvoicesEnabled(): Boolean {
        return try {
            val autoGenerate = businessSettings["autoGenerate"] as? Map<String, Any>
            autoGenerate?.get("invoices") as? Boolean ?: true
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Check if auto-generate orders is enabled
     */
    fun isAutoGenerateOrdersEnabled(): Boolean {
        return try {
            val autoGenerate = businessSettings["autoGenerate"] as? Map<String, Any>
            autoGenerate?.get("orders") as? Boolean ?: true
        } catch (e: Exception) {
            true
        }
    }

    // Note: JSON helper methods no longer needed with direct object mapping

    // Material Design 3 Helper Methods

    /**
     * Set Material Theme configuration
     */
    fun setMaterialTheme(theme: MaterialTheme, modifiedBy: String? = null, modifiedByName: String? = null) {
        materialTheme = theme
        updateModification(modifiedBy, modifiedByName)
    }

    /**
     * Set Material Colors configuration
     */
    fun setMaterialColors(colors: MaterialColors, modifiedBy: String? = null, modifiedByName: String? = null) {
        materialColors = colors
        updateModification(modifiedBy, modifiedByName)
    }

    /**
     * Set Material Typography configuration
     */
    fun setMaterialTypography(
        typography: MaterialTypography,
        modifiedBy: String? = null,
        modifiedByName: String? = null
    ) {
        materialTypography = typography
        updateModification(modifiedBy, modifiedByName)
    }

    /**
     * Update theme mode only
     */
    fun updateThemeMode(mode: String, modifiedBy: String? = null, modifiedByName: String? = null) {
        materialTheme.mode = mode
        updateModification(modifiedBy, modifiedByName)
    }

    /**
     * Update color scheme preference
     */
    fun updateColorScheme(
        colorScheme: String,
        seedColor: String? = null,
        modifiedBy: String? = null,
        modifiedByName: String? = null
    ) {
        materialTheme.colorScheme = colorScheme
        materialTheme.seedColor = seedColor
        updateModification(modifiedBy, modifiedByName)
    }

    /**
     * Update primary color
     */
    fun updatePrimaryColor(primaryColor: String, modifiedBy: String? = null, modifiedByName: String? = null) {
        materialColors.primary = primaryColor
        updateModification(modifiedBy, modifiedByName)
    }

    /**
     * Check if dark theme is enabled
     */
    fun isDarkTheme(): Boolean {
        return materialTheme.mode == "dark"
    }

    /**
     * Check if Material You (dynamic colors) is enabled
     */
    fun isMaterialYouEnabled(): Boolean {
        return materialTheme.colorScheme == "dynamic"
    }

    /**
     * Get primary color for current theme
     */
    fun getPrimaryColor(): String {
        return materialColors.primary
    }

    /**
     * Get surface color for current theme
     */
    fun getSurfaceColor(): String {
        return materialColors.surface
    }
}