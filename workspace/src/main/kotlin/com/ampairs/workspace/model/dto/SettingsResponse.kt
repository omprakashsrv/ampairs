package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.WorkspaceSettings
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Response DTO for workspace settings
 */
data class SettingsResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("workspace_id")
    val workspaceId: String,

    @JsonProperty("branding")
    val branding: Map<String, Any>,

    @JsonProperty("notifications")
    val notifications: Map<String, Any>,

    @JsonProperty("integrations")
    val integrations: Map<String, Any>,

    @JsonProperty("security")
    val security: Map<String, Any>,

    @JsonProperty("features")
    val features: Map<String, Any>,

    @JsonProperty("preferences")
    val preferences: Map<String, Any>,

    @JsonProperty("last_modified_by")
    val lastModifiedBy: String?,

    @JsonProperty("last_modified_at")
    val lastModifiedAt: LocalDateTime,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime,
)

/**
 * Response DTO for specific setting sections
 */
data class BrandingResponse(
    @JsonProperty("logo_url")
    val logoUrl: String?,

    @JsonProperty("primary_color")
    val primaryColor: String?,

    @JsonProperty("secondary_color")
    val secondaryColor: String?,

    @JsonProperty("accent_color")
    val accentColor: String?,

    @JsonProperty("custom_css")
    val customCss: String?,

    @JsonProperty("favicon_url")
    val faviconUrl: String?,
)

/**
 * Response DTO for notification settings
 */
data class NotificationResponse(
    @JsonProperty("email_notifications")
    val emailNotifications: Boolean,

    @JsonProperty("push_notifications")
    val pushNotifications: Boolean,

    @JsonProperty("member_joined")
    val memberJoined: Boolean,

    @JsonProperty("member_left")
    val memberLeft: Boolean,

    @JsonProperty("new_invitation")
    val newInvitation: Boolean,

    @JsonProperty("weekly_digest")
    val weeklyDigest: Boolean,
)

/**
 * Response DTO for security settings
 */
data class SecurityResponse(
    @JsonProperty("two_factor_required")
    val twoFactorRequired: Boolean,

    @JsonProperty("session_timeout_minutes")
    val sessionTimeoutMinutes: Int,

    @JsonProperty("allowed_domains")
    val allowedDomains: List<String>,

    @JsonProperty("ip_whitelist")
    val ipWhitelist: List<String>,

    @JsonProperty("require_approval_for_new_members")
    val requireApprovalForNewMembers: Boolean,
)

/**
 * Response DTO for feature settings
 */
data class FeaturesResponse(
    @JsonProperty("file_sharing_enabled")
    val fileSharingEnabled: Boolean,

    @JsonProperty("external_integrations_enabled")
    val externalIntegrationsEnabled: Boolean,

    @JsonProperty("api_access_enabled")
    val apiAccessEnabled: Boolean,

    @JsonProperty("custom_roles_enabled")
    val customRolesEnabled: Boolean,

    @JsonProperty("analytics_enabled")
    val analyticsEnabled: Boolean,
)

/**
 * Extension function to convert WorkspaceSettings entity to SettingsResponse
 */
fun WorkspaceSettings.toResponse(): SettingsResponse {
    return SettingsResponse(
        id = this.uid, // Use uid instead of id
        workspaceId = this.workspaceId,
        branding = this.getBrandingMap(),
        notifications = this.getNotificationsMap(),
        integrations = this.getIntegrationsMap(),
        security = this.getSecurityMap(),
        features = this.getFeaturesMap(),
        preferences = this.getPreferencesMap(),
        lastModifiedBy = this.lastModifiedBy,
        lastModifiedAt = this.lastModifiedAt,
        createdAt = LocalDateTime.parse(this.createdAt ?: "2023-01-01T00:00:00"),
        updatedAt = LocalDateTime.parse(this.updatedAt ?: "2023-01-01T00:00:00")
    )
}