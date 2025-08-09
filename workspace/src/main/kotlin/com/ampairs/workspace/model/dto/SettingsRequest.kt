package com.ampairs.workspace.model.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request DTO for updating workspace settings
 */
data class UpdateSettingsRequest(
    @JsonProperty("branding")
    val branding: Map<String, Any>? = null,

    @JsonProperty("notifications")
    val notifications: Map<String, Any>? = null,

    @JsonProperty("integrations")
    val integrations: Map<String, Any>? = null,

    @JsonProperty("security")
    val security: Map<String, Any>? = null,

    @JsonProperty("features")
    val features: Map<String, Any>? = null,

    @JsonProperty("preferences")
    val preferences: Map<String, Any>? = null,
)

/**
 * Request DTO for updating branding settings
 */
data class UpdateBrandingRequest(
    @JsonProperty("logo_url")
    val logoUrl: String? = null,

    @JsonProperty("primary_color")
    val primaryColor: String? = null,

    @JsonProperty("secondary_color")
    val secondaryColor: String? = null,

    @JsonProperty("accent_color")
    val accentColor: String? = null,

    @JsonProperty("custom_css")
    val customCss: String? = null,

    @JsonProperty("favicon_url")
    val faviconUrl: String? = null,
)

/**
 * Request DTO for updating notification settings
 */
data class UpdateNotificationRequest(
    @JsonProperty("email_notifications")
    val emailNotifications: Boolean? = null,

    @JsonProperty("push_notifications")
    val pushNotifications: Boolean? = null,

    @JsonProperty("member_joined")
    val memberJoined: Boolean? = null,

    @JsonProperty("member_left")
    val memberLeft: Boolean? = null,

    @JsonProperty("new_invitation")
    val newInvitation: Boolean? = null,

    @JsonProperty("weekly_digest")
    val weeklyDigest: Boolean? = null,
)

/**
 * Request DTO for updating security settings
 */
data class UpdateSecurityRequest(
    @JsonProperty("two_factor_required")
    val twoFactorRequired: Boolean? = null,

    @JsonProperty("session_timeout_minutes")
    val sessionTimeoutMinutes: Int? = null,

    @JsonProperty("allowed_domains")
    val allowedDomains: List<String>? = null,

    @JsonProperty("ip_whitelist")
    val ipWhitelist: List<String>? = null,

    @JsonProperty("require_approval_for_new_members")
    val requireApprovalForNewMembers: Boolean? = null,
)

/**
 * Request DTO for updating feature settings
 */
data class UpdateFeaturesRequest(
    @JsonProperty("file_sharing_enabled")
    val fileSharingEnabled: Boolean? = null,

    @JsonProperty("external_integrations_enabled")
    val externalIntegrationsEnabled: Boolean? = null,

    @JsonProperty("api_access_enabled")
    val apiAccessEnabled: Boolean? = null,

    @JsonProperty("custom_roles_enabled")
    val customRolesEnabled: Boolean? = null,

    @JsonProperty("analytics_enabled")
    val analyticsEnabled: Boolean? = null,
)