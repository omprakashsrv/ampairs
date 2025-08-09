package com.ampairs.workspace.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Workspace-specific settings and customizations
 */
@Entity(name = "workspace_settings")
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

    /**
     * Branding settings (JSON)
     * Contains: logo_url, primary_color, secondary_color, theme, etc.
     */
    @Column(name = "branding", columnDefinition = "TEXT")
    var branding: String = "{}"

    /**
     * Notification preferences (JSON)
     * Contains: email_notifications, in_app_notifications, notification_types, etc.
     */
    @Column(name = "notifications", columnDefinition = "TEXT")
    var notifications: String = "{}"

    /**
     * Third-party integrations configuration (JSON)
     * Contains: enabled_integrations, api_keys, webhook_urls, etc.
     */
    @Column(name = "integrations", columnDefinition = "TEXT")
    var integrations: String = "{}"

    /**
     * Security policies and settings (JSON)
     * Contains: password_policy, session_timeout, ip_restrictions, etc.
     */
    @Column(name = "security", columnDefinition = "TEXT")
    var security: String = "{}"

    /**
     * Feature flags and enabled features (JSON)
     * Contains: enabled_features, feature_limits, beta_features, etc.
     */
    @Column(name = "features", columnDefinition = "TEXT")
    var features: String = "{}"

    /**
     * General workspace preferences (JSON)
     * Contains: default_language, timezone, date_format, number_format, etc.
     */
    @Column(name = "preferences", columnDefinition = "TEXT")
    var preferences: String = "{}"

    /**
     * Custom fields and metadata (JSON)
     * Contains: custom_fields, metadata, tags, etc.
     */
    @Column(name = "custom_data", columnDefinition = "TEXT")
    var customData: String = "{}"

    /**
     * When settings were last modified
     */
    @Column(name = "last_modified_at")
    var lastModifiedAt: LocalDateTime = LocalDateTime.now()

    /**
     * ID of user who last modified settings
     */
    @Column(name = "last_modified_by", length = 36)
    var lastModifiedBy: String? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.WORKSPACE_SETTINGS_PREFIX
    }

    private val objectMapper = ObjectMapper()

    /**
     * Get a branding setting value
     */
    fun getBrandingSetting(key: String): String? {
        return getJsonValue(branding, key)
    }

    /**
     * Set a branding setting value
     */
    fun setBrandingSetting(key: String, value: Any, modifiedBy: String? = null) {
        branding = setJsonValue(branding, key, value)
        updateModification(modifiedBy)
    }

    /**
     * Get a notification setting value
     */
    fun getNotificationSetting(key: String): String? {
        return getJsonValue(notifications, key)
    }

    /**
     * Set a notification setting value
     */
    fun setNotificationSetting(key: String, value: Any, modifiedBy: String? = null) {
        notifications = setJsonValue(notifications, key, value)
        updateModification(modifiedBy)
    }

    /**
     * Get an integration setting value
     */
    fun getIntegrationSetting(key: String): String? {
        return getJsonValue(integrations, key)
    }

    /**
     * Set an integration setting value
     */
    fun setIntegrationSetting(key: String, value: Any, modifiedBy: String? = null) {
        integrations = setJsonValue(integrations, key, value)
        updateModification(modifiedBy)
    }

    /**
     * Get a security setting value
     */
    fun getSecuritySetting(key: String): String? {
        return getJsonValue(security, key)
    }

    /**
     * Set a security setting value
     */
    fun setSecuritySetting(key: String, value: Any, modifiedBy: String? = null) {
        security = setJsonValue(security, key, value)
        updateModification(modifiedBy)
    }

    /**
     * Get a feature setting value
     */
    fun getFeatureSetting(key: String): String? {
        return getJsonValue(features, key)
    }

    /**
     * Set a feature setting value
     */
    fun setFeatureSetting(key: String, value: Any, modifiedBy: String? = null) {
        features = setJsonValue(features, key, value)
        updateModification(modifiedBy)
    }

    /**
     * Get a preference setting value
     */
    fun getPreferenceSetting(key: String): String? {
        return getJsonValue(preferences, key)
    }

    /**
     * Set a preference setting value
     */
    fun setPreferenceSetting(key: String, value: Any, modifiedBy: String? = null) {
        preferences = setJsonValue(preferences, key, value)
        updateModification(modifiedBy)
    }

    /**
     * Check if a feature is enabled
     */
    fun isFeatureEnabled(feature: String): Boolean {
        return try {
            val featuresMap = objectMapper.readValue(features, Map::class.java)
            featuresMap[feature] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Enable or disable a feature
     */
    fun setFeatureEnabled(feature: String, enabled: Boolean, modifiedBy: String? = null) {
        setFeatureSetting(feature, enabled, modifiedBy)
    }

    /**
     * Get all branding settings as a map
     */
    fun getAllBrandingSettings(): Map<String, Any> {
        return getJsonAsMap(branding)
    }

    /**
     * Get all notification settings as a map
     */
    fun getAllNotificationSettings(): Map<String, Any> {
        return getJsonAsMap(notifications)
    }

    /**
     * Get all security settings as a map
     */
    fun getAllSecuritySettings(): Map<String, Any> {
        return getJsonAsMap(security)
    }

    /**
     * Helper method to get value from JSON string
     */
    private fun getJsonValue(jsonString: String, key: String): String? {
        return try {
            val map = objectMapper.readValue(jsonString, Map::class.java)
            map[key]?.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Helper method to set value in JSON string
     */
    private fun setJsonValue(jsonString: String, key: String, value: Any): String {
        return try {
            val map = objectMapper.readValue(jsonString, HashMap::class.java) as HashMap<String, Any>
            map[key] = value
            objectMapper.writeValueAsString(map)
        } catch (e: Exception) {
            objectMapper.writeValueAsString(mapOf(key to value))
        }
    }

    /**
     * Helper method to get JSON as Map
     */
    private fun getJsonAsMap(jsonString: String): Map<String, Any> {
        return try {
            objectMapper.readValue(jsonString, Map::class.java) as Map<String, Any>
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Update modification tracking
     */
    private fun updateModification(modifiedBy: String?) {
        lastModifiedAt = LocalDateTime.now()
        lastModifiedBy = modifiedBy
    }

    /**
     * Get branding settings as map
     */
    fun getBrandingMap(): Map<String, Any> {
        return getJsonAsMap(branding)
    }

    /**
     * Get notification settings as map
     */
    fun getNotificationsMap(): Map<String, Any> {
        return getJsonAsMap(notifications)
    }

    /**
     * Get integration settings as map
     */
    fun getIntegrationsMap(): Map<String, Any> {
        return getJsonAsMap(integrations)
    }

    /**
     * Get security settings as map
     */
    fun getSecurityMap(): Map<String, Any> {
        return getJsonAsMap(security)
    }

    /**
     * Get features settings as map
     */
    fun getFeaturesMap(): Map<String, Any> {
        return getJsonAsMap(features)
    }

    /**
     * Get preferences settings as map
     */
    fun getPreferencesMap(): Map<String, Any> {
        return getJsonAsMap(preferences)
    }
}