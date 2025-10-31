package com.ampairs.workspace.service

import com.ampairs.core.exception.NotFoundException
import com.ampairs.workspace.model.MaterialColors
import com.ampairs.workspace.model.MaterialTheme
import com.ampairs.workspace.model.MaterialTypography
import com.ampairs.workspace.model.WorkspaceSettings
import com.ampairs.workspace.model.dto.SettingsResponse
import com.ampairs.workspace.model.dto.UpdateSettingsRequest
import com.ampairs.workspace.model.dto.toResponse
import com.ampairs.workspace.repository.WorkspaceSettingsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime

/**
 * Service for workspace settings management operations
 */
@Service
@Transactional
class WorkspaceSettingsService(
    private val settingsRepository: WorkspaceSettingsRepository,
    private val activityService: WorkspaceActivityService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceSettingsService::class.java)
    }

    /**
     * Initialize default settings for a new workspace
     */
    fun initializeDefaultSettings(workspaceId: String): WorkspaceSettings {
        val defaultSettings = WorkspaceSettings().apply {
            this.workspaceId = workspaceId
            this.logoUrl = null
        }

        val savedSettings = settingsRepository.save(defaultSettings)
        logger.info("Initialized default settings for workspace: $workspaceId")

        return savedSettings
    }

    /**
     * Get workspace settings
     */
    fun getWorkspaceSettings(workspaceId: String): SettingsResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)
        return settings.toResponse()
    }

    /**
     * Update workspace settings
     */
    fun updateSettings(workspaceId: String, request: UpdateSettingsRequest, updatedBy: String): SettingsResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)

        // Update individual setting sections
        request.logoUrl?.let {
            settings.logoUrl = it
        }

        request.materialTheme?.let {
            settings.materialTheme = it
        }

        request.materialColors?.let {
            settings.materialColors = it
        }

        request.materialTypography?.let {
            settings.materialTypography = it
        }

        request.businessSettings?.let {
            settings.businessSettings = it
        }

        settings.updateModification(updatedBy, "Unknown User")

        val updatedSettings = settingsRepository.save(settings)
        return updatedSettings.toResponse()
    }

    /**
     * Update Material Design theme
     */
    fun updateMaterialTheme(workspaceId: String, theme: MaterialTheme, updatedBy: String): SettingsResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)
        settings.materialTheme = theme
        settings.updateModification(updatedBy, "Unknown User")

        val updatedSettings = settingsRepository.save(settings)
        activityService.logSettingsUpdated(workspaceId, "material_theme", updatedBy, "Unknown User")

        return updatedSettings.toResponse()
    }

    /**
     * Update Material Design colors
     */
    fun updateMaterialColors(workspaceId: String, colors: MaterialColors, updatedBy: String): SettingsResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)
        settings.materialColors = colors
        settings.updateModification(updatedBy, "Unknown User")

        val updatedSettings = settingsRepository.save(settings)
        activityService.logSettingsUpdated(workspaceId, "material_colors", updatedBy, "Unknown User")

        return updatedSettings.toResponse()
    }

    /**
     * Update Material Design typography
     */
    fun updateMaterialTypography(
        workspaceId: String,
        typography: MaterialTypography,
        updatedBy: String,
    ): SettingsResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)
        settings.materialTypography = typography
        settings.updateModification(updatedBy, "Unknown User")

        val updatedSettings = settingsRepository.save(settings)
        activityService.logSettingsUpdated(workspaceId, "material_typography", updatedBy, "Unknown User")

        return updatedSettings.toResponse()
    }

    /**
     * Update business settings
     */
    fun updateBusinessSettings(
        workspaceId: String,
        businessSettings: Map<String, Any>,
        updatedBy: String,
    ): SettingsResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)
        settings.businessSettings = businessSettings
        settings.updateModification(updatedBy, "Unknown User")

        val updatedSettings = settingsRepository.save(settings)
        activityService.logSettingsUpdated(workspaceId, "business_settings", updatedBy, "Unknown User")

        return updatedSettings.toResponse()
    }

    /**
     * Update logo URL
     */
    fun updateLogo(workspaceId: String, logoUrl: String?, updatedBy: String): SettingsResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)
        settings.logoUrl = logoUrl
        settings.updateModification(updatedBy, "Unknown User")

        val updatedSettings = settingsRepository.save(settings)
        activityService.logSettingsUpdated(workspaceId, "logo", updatedBy, "Unknown User")

        return updatedSettings.toResponse()
    }

    /**
     * Update business setting value
     */
    fun updateBusinessSetting(workspaceId: String, key: String, value: Any, updatedBy: String): SettingsResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)
        settings.setBusinessSetting(key, value, updatedBy, "Unknown User")

        val updatedSettings = settingsRepository.save(settings)
        activityService.logSettingsUpdated(workspaceId, "business_setting_$key", updatedBy, "Unknown User")

        return updatedSettings.toResponse()
    }

    /**
     * Reset settings to defaults
     */
    fun resetSettings(workspaceId: String, section: String?, resetBy: String): SettingsResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)

        when (section) {
            "material_theme" -> {
                settings.materialTheme = MaterialTheme()
                activityService.logSettingsReset(workspaceId, "material_theme", resetBy, "Unknown User")
            }

            "material_colors" -> {
                settings.materialColors = MaterialColors()
                activityService.logSettingsReset(workspaceId, "material_colors", resetBy, "Unknown User")
            }

            "material_typography" -> {
                settings.materialTypography = MaterialTypography()
                activityService.logSettingsReset(workspaceId, "material_typography", resetBy, "Unknown User")
            }

            "business_settings" -> {
                settings.businessSettings = mapOf(
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
                activityService.logSettingsReset(workspaceId, "business_settings", resetBy, "Unknown User")
            }

            null -> {
                // Reset all settings to defaults
                settings.logoUrl = null
                settings.materialTheme = MaterialTheme()
                settings.materialColors = MaterialColors()
                settings.materialTypography = MaterialTypography()
                settings.businessSettings = mapOf(
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
                activityService.logSettingsReset(workspaceId, "all", resetBy, "Unknown User")
            }

            else -> throw IllegalArgumentException("Invalid settings section: $section")
        }

        settings.updateModification(resetBy, "Unknown User")

        val updatedSettings = settingsRepository.save(settings)
        return updatedSettings.toResponse()
    }

    /**
     * Delete settings (used during workspace deletion)
     */
    fun deleteSettings(workspaceId: String) {
        settingsRepository.deleteByWorkspaceId(workspaceId)
        logger.info("Deleted settings for workspace: $workspaceId")
    }

    // Private helper methods

    private fun findSettingsByWorkspaceId(workspaceId: String): WorkspaceSettings {
        return settingsRepository.findByWorkspaceId(workspaceId)
            .orElseThrow { NotFoundException("Settings not found for workspace: $workspaceId") }
    }
}