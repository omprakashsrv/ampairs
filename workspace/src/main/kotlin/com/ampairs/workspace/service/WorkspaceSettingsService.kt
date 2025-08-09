package com.ampairs.workspace.service

import com.ampairs.core.exception.NotFoundException
import com.ampairs.workspace.model.WorkspaceSettings
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.repository.WorkspaceSettingsRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Service for workspace settings management operations
 */
@Service
@Transactional
class WorkspaceSettingsService(
    private val settingsRepository: WorkspaceSettingsRepository,
    private val activityService: WorkspaceActivityService,
    private val objectMapper: ObjectMapper,
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
            this.branding = getDefaultBranding()
            this.notifications = getDefaultNotifications()
            this.integrations = "{}"
            this.security = getDefaultSecurity()
            this.features = getDefaultFeatures()
            this.preferences = getDefaultPreferences()
            this.lastModifiedAt = LocalDateTime.now()
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
        request.branding?.let {
            settings.branding = objectMapper.writeValueAsString(it)
            activityService.logSettingsUpdated(workspaceId, "branding", updatedBy, "Unknown User")
        }

        request.notifications?.let {
            settings.notifications = objectMapper.writeValueAsString(it)
            activityService.logSettingsUpdated(workspaceId, "notifications", updatedBy, "Unknown User")
        }

        request.integrations?.let {
            settings.integrations = objectMapper.writeValueAsString(it)
            activityService.logSettingsUpdated(workspaceId, "integrations", updatedBy, "Unknown User")
        }

        request.security?.let {
            settings.security = objectMapper.writeValueAsString(it)
            activityService.logSettingsUpdated(workspaceId, "security", updatedBy, "Unknown User")
        }

        request.features?.let {
            settings.features = objectMapper.writeValueAsString(it)
            activityService.logSettingsUpdated(workspaceId, "features", updatedBy, "Unknown User")
        }

        request.preferences?.let {
            settings.preferences = objectMapper.writeValueAsString(it)
            activityService.logSettingsUpdated(workspaceId, "preferences", updatedBy, "Unknown User")
        }

        settings.lastModifiedBy = updatedBy
        settings.lastModifiedAt = LocalDateTime.now()

        val updatedSettings = settingsRepository.save(settings)
        return updatedSettings.toResponse()
    }

    /**
     * Update branding settings
     */
    fun updateBranding(workspaceId: String, request: UpdateBrandingRequest, updatedBy: String): BrandingResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)
        val currentBranding = settings.getBrandingMap().toMutableMap()

        // Update branding fields
        request.logoUrl?.let { currentBranding["logo_url"] = it }
        request.primaryColor?.let { currentBranding["primary_color"] = it }
        request.secondaryColor?.let { currentBranding["secondary_color"] = it }
        request.accentColor?.let { currentBranding["accent_color"] = it }
        request.customCss?.let { currentBranding["custom_css"] = it }
        request.faviconUrl?.let { currentBranding["favicon_url"] = it }

        settings.branding = objectMapper.writeValueAsString(currentBranding)
        settings.lastModifiedBy = updatedBy
        settings.lastModifiedAt = LocalDateTime.now()

        settingsRepository.save(settings)

        // Log activity
        activityService.logSettingsUpdated(workspaceId, "branding", updatedBy, "Unknown User")

        return BrandingResponse(
            logoUrl = currentBranding["logo_url"] as? String,
            primaryColor = currentBranding["primary_color"] as? String,
            secondaryColor = currentBranding["secondary_color"] as? String,
            accentColor = currentBranding["accent_color"] as? String,
            customCss = currentBranding["custom_css"] as? String,
            faviconUrl = currentBranding["favicon_url"] as? String
        )
    }

    /**
     * Update notification settings
     */
    fun updateNotifications(
        workspaceId: String,
        request: UpdateNotificationRequest,
        updatedBy: String,
    ): NotificationResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)
        val currentNotifications = settings.getNotificationsMap().toMutableMap()

        // Update notification fields
        request.emailNotifications?.let { currentNotifications["email_notifications"] = it }
        request.pushNotifications?.let { currentNotifications["push_notifications"] = it }
        request.memberJoined?.let { currentNotifications["member_joined"] = it }
        request.memberLeft?.let { currentNotifications["member_left"] = it }
        request.newInvitation?.let { currentNotifications["new_invitation"] = it }
        request.weeklyDigest?.let { currentNotifications["weekly_digest"] = it }

        settings.notifications = objectMapper.writeValueAsString(currentNotifications)
        settings.lastModifiedBy = updatedBy
        settings.lastModifiedAt = LocalDateTime.now()

        settingsRepository.save(settings)

        // Log activity
        activityService.logSettingsUpdated(workspaceId, "notifications", updatedBy, "Unknown User")

        return NotificationResponse(
            emailNotifications = currentNotifications["email_notifications"] as? Boolean ?: true,
            pushNotifications = currentNotifications["push_notifications"] as? Boolean ?: true,
            memberJoined = currentNotifications["member_joined"] as? Boolean ?: true,
            memberLeft = currentNotifications["member_left"] as? Boolean ?: true,
            newInvitation = currentNotifications["new_invitation"] as? Boolean ?: true,
            weeklyDigest = currentNotifications["weekly_digest"] as? Boolean ?: false
        )
    }

    /**
     * Update security settings
     */
    fun updateSecurity(workspaceId: String, request: UpdateSecurityRequest, updatedBy: String): SecurityResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)
        val currentSecurity = settings.getSecurityMap().toMutableMap()

        // Update security fields
        request.twoFactorRequired?.let { currentSecurity["two_factor_required"] = it }
        request.sessionTimeoutMinutes?.let { currentSecurity["session_timeout_minutes"] = it }
        request.allowedDomains?.let { currentSecurity["allowed_domains"] = it }
        request.ipWhitelist?.let { currentSecurity["ip_whitelist"] = it }
        request.requireApprovalForNewMembers?.let { currentSecurity["require_approval_for_new_members"] = it }

        settings.security = objectMapper.writeValueAsString(currentSecurity)
        settings.lastModifiedBy = updatedBy
        settings.lastModifiedAt = LocalDateTime.now()

        settingsRepository.save(settings)

        // Log activity
        activityService.logSettingsUpdated(workspaceId, "security", updatedBy, "Unknown User")

        return SecurityResponse(
            twoFactorRequired = currentSecurity["two_factor_required"] as? Boolean ?: false,
            sessionTimeoutMinutes = currentSecurity["session_timeout_minutes"] as? Int ?: 30,
            allowedDomains = (currentSecurity["allowed_domains"] as? List<*>)?.filterIsInstance<String>()
                ?: emptyList(),
            ipWhitelist = (currentSecurity["ip_whitelist"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            requireApprovalForNewMembers = currentSecurity["require_approval_for_new_members"] as? Boolean ?: false
        )
    }

    /**
     * Update feature settings
     */
    fun updateFeatures(workspaceId: String, request: UpdateFeaturesRequest, updatedBy: String): FeaturesResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)
        val currentFeatures = settings.getFeaturesMap().toMutableMap()

        // Update feature fields
        request.fileSharingEnabled?.let { currentFeatures["file_sharing_enabled"] = it }
        request.externalIntegrationsEnabled?.let { currentFeatures["external_integrations_enabled"] = it }
        request.apiAccessEnabled?.let { currentFeatures["api_access_enabled"] = it }
        request.customRolesEnabled?.let { currentFeatures["custom_roles_enabled"] = it }
        request.analyticsEnabled?.let { currentFeatures["analytics_enabled"] = it }

        settings.features = objectMapper.writeValueAsString(currentFeatures)
        settings.lastModifiedBy = updatedBy
        settings.lastModifiedAt = LocalDateTime.now()

        settingsRepository.save(settings)

        // Log activity
        activityService.logSettingsUpdated(workspaceId, "features", updatedBy, "Unknown User")

        return FeaturesResponse(
            fileSharingEnabled = currentFeatures["file_sharing_enabled"] as? Boolean ?: true,
            externalIntegrationsEnabled = currentFeatures["external_integrations_enabled"] as? Boolean ?: true,
            apiAccessEnabled = currentFeatures["api_access_enabled"] as? Boolean ?: false,
            customRolesEnabled = currentFeatures["custom_roles_enabled"] as? Boolean ?: false,
            analyticsEnabled = currentFeatures["analytics_enabled"] as? Boolean ?: true
        )
    }

    /**
     * Reset settings to defaults
     */
    fun resetSettings(workspaceId: String, section: String?, resetBy: String): SettingsResponse {
        val settings = findSettingsByWorkspaceId(workspaceId)

        when (section) {
            "branding" -> {
                settings.branding = getDefaultBranding()
                activityService.logSettingsReset(workspaceId, "branding", resetBy, "Unknown User")
            }

            "notifications" -> {
                settings.notifications = getDefaultNotifications()
                activityService.logSettingsReset(workspaceId, "notifications", resetBy, "Unknown User")
            }

            "security" -> {
                settings.security = getDefaultSecurity()
                activityService.logSettingsReset(workspaceId, "security", resetBy, "Unknown User")
            }

            "features" -> {
                settings.features = getDefaultFeatures()
                activityService.logSettingsReset(workspaceId, "features", resetBy, "Unknown User")
            }

            "preferences" -> {
                settings.preferences = getDefaultPreferences()
                activityService.logSettingsReset(workspaceId, "preferences", resetBy, "Unknown User")
            }

            null -> {
                // Reset all settings
                settings.branding = getDefaultBranding()
                settings.notifications = getDefaultNotifications()
                settings.integrations = "{}"
                settings.security = getDefaultSecurity()
                settings.features = getDefaultFeatures()
                settings.preferences = getDefaultPreferences()
                activityService.logSettingsReset(workspaceId, "all", resetBy, "Unknown User")
            }

            else -> throw IllegalArgumentException("Invalid settings section: $section")
        }

        settings.lastModifiedBy = resetBy
        settings.lastModifiedAt = LocalDateTime.now()

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

    private fun getDefaultBranding(): String {
        val branding = mapOf(
            "logo_url" to null,
            "primary_color" to "#007bff",
            "secondary_color" to "#6c757d",
            "accent_color" to "#28a745",
            "custom_css" to null,
            "favicon_url" to null
        )
        return objectMapper.writeValueAsString(branding)
    }

    private fun getDefaultNotifications(): String {
        val notifications = mapOf(
            "email_notifications" to true,
            "push_notifications" to true,
            "member_joined" to true,
            "member_left" to true,
            "new_invitation" to true,
            "weekly_digest" to false
        )
        return objectMapper.writeValueAsString(notifications)
    }

    private fun getDefaultSecurity(): String {
        val security = mapOf(
            "two_factor_required" to false,
            "session_timeout_minutes" to 30,
            "allowed_domains" to emptyList<String>(),
            "ip_whitelist" to emptyList<String>(),
            "require_approval_for_new_members" to false
        )
        return objectMapper.writeValueAsString(security)
    }

    private fun getDefaultFeatures(): String {
        val features = mapOf(
            "file_sharing_enabled" to true,
            "external_integrations_enabled" to true,
            "api_access_enabled" to false,
            "custom_roles_enabled" to false,
            "analytics_enabled" to true
        )
        return objectMapper.writeValueAsString(features)
    }

    private fun getDefaultPreferences(): String {
        val preferences = mapOf(
            "default_view" to "dashboard",
            "theme" to "light",
            "language" to "en",
            "timezone" to "UTC",
            "date_format" to "MM/dd/yyyy",
            "time_format" to "12h"
        )
        return objectMapper.writeValueAsString(preferences)
    }
}