package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.MaterialColors
import com.ampairs.workspace.model.MaterialTheme
import com.ampairs.workspace.model.MaterialTypography
import com.ampairs.workspace.model.WorkspaceSettings
import java.time.Instant

/**
 * Response DTO for workspace settings
 */
data class SettingsResponse(
        val id: String,

        val workspaceId: String,

        val logoUrl: String?,

        val materialTheme: MaterialTheme,

        val materialColors: MaterialColors,

        val materialTypography: MaterialTypography,

        val businessSettings: Map<String, Any>,

        val lastModifiedBy: String?,

        val createdAt: Instant,

        val updatedAt: Instant,
)

/**
 * Response DTO for specific setting sections
 */
data class BrandingResponse(
        val logoUrl: String?,

        val primaryColor: String?,

        val secondaryColor: String?,

        val accentColor: String?,

        val customCss: String?,

        val faviconUrl: String?,
)

/**
 * Response DTO for notification settings
 */
data class NotificationResponse(
        val emailNotifications: Boolean,

        val pushNotifications: Boolean,

        val memberJoined: Boolean,

        val memberLeft: Boolean,

        val newInvitation: Boolean,

        val weeklyDigest: Boolean,
)

/**
 * Response DTO for security settings
 */
data class SecurityResponse(
        val twoFactorRequired: Boolean,

        val sessionTimeoutMinutes: Int,

        val allowedDomains: List<String>,

        val ipWhitelist: List<String>,

        val requireApprovalForNewMembers: Boolean,
)

/**
 * Response DTO for feature settings
 */
data class FeaturesResponse(
        val fileSharingEnabled: Boolean,

        val externalIntegrationsEnabled: Boolean,

        val apiAccessEnabled: Boolean,

        val customRolesEnabled: Boolean,

        val analyticsEnabled: Boolean,
)

/**
 * Extension function to convert WorkspaceSettings entity to SettingsResponse
 */
fun WorkspaceSettings.toResponse(): SettingsResponse {

    return SettingsResponse(
        id = this.uid,
        workspaceId = this.workspaceId,
        logoUrl = this.logoUrl,
        materialTheme = this.materialTheme,
        materialColors = this.materialColors,
        materialTypography = this.materialTypography,
        businessSettings = this.businessSettings,
        lastModifiedBy = this.lastModifiedBy,
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now(),
    )
}