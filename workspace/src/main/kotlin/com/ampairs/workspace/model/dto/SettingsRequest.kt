package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.MaterialColors
import com.ampairs.workspace.model.MaterialTheme
import com.ampairs.workspace.model.MaterialTypography

/**
 * Request DTO for updating workspace settings
 */
data class UpdateSettingsRequest(
        val logoUrl: String? = null,

        val materialTheme: MaterialTheme? = null,

        val materialColors: MaterialColors? = null,

        val materialTypography: MaterialTypography? = null,

        val businessSettings: Map<String, Any>? = null,
)

/**
 * Request DTO for updating branding settings
 */
data class UpdateBrandingRequest(
        val logoUrl: String? = null,

        val primaryColor: String? = null,

        val secondaryColor: String? = null,

        val accentColor: String? = null,

        val customCss: String? = null,

        val faviconUrl: String? = null,
)

/**
 * Request DTO for updating notification settings
 */
data class UpdateNotificationRequest(
        val emailNotifications: Boolean? = null,

        val pushNotifications: Boolean? = null,

        val memberJoined: Boolean? = null,

        val memberLeft: Boolean? = null,

        val newInvitation: Boolean? = null,

        val weeklyDigest: Boolean? = null,
)

/**
 * Request DTO for updating security settings
 */
data class UpdateSecurityRequest(
        val twoFactorRequired: Boolean? = null,

        val sessionTimeoutMinutes: Int? = null,

        val allowedDomains: List<String>? = null,

        val ipWhitelist: List<String>? = null,

        val requireApprovalForNewMembers: Boolean? = null,
)

/**
 * Request DTO for updating feature settings
 */
data class UpdateFeaturesRequest(
        val fileSharingEnabled: Boolean? = null,

        val externalIntegrationsEnabled: Boolean? = null,

        val apiAccessEnabled: Boolean? = null,

        val customRolesEnabled: Boolean? = null,

        val analyticsEnabled: Boolean? = null,
)