package com.ampairs.workspace.model.enums

/**
 * Types of activities that can be tracked in workspace activity logging
 */
enum class WorkspaceActivityType(val displayName: String, val category: String) {

    // Workspace Management Activities
    WORKSPACE_CREATED("Workspace Created", "workspace"),
    WORKSPACE_UPDATED("Workspace Updated", "workspace"),
    WORKSPACE_ARCHIVED("Workspace Archived", "workspace"),
    WORKSPACE_RESTORED("Workspace Restored", "workspace"),
    WORKSPACE_DELETED("Workspace Deleted", "workspace"),

    // Subscription Management Activities
    SUBSCRIPTION_UPDATED("Subscription Updated", "subscription"),
    SUBSCRIPTION_UPGRADED("Subscription Upgraded", "subscription"),
    SUBSCRIPTION_DOWNGRADED("Subscription Downgraded", "subscription"),
    SUBSCRIPTION_CANCELLED("Subscription Cancelled", "subscription"),
    SUBSCRIPTION_RENEWED("Subscription Renewed", "subscription"),

    // Member Management Activities
    MEMBER_ADDED("Member Added", "member"),
    MEMBER_ROLE_CHANGED("Member Role Changed", "member"),
    MEMBER_ACTIVATED("Member Activated", "member"),
    MEMBER_DEACTIVATED("Member Deactivated", "member"),
    MEMBER_REMOVED("Member Removed", "member"),
    MEMBER_PROFILE_UPDATED("Member Profile Updated", "member"),

    // Bulk Member Operations
    BULK_MEMBER_ACTIVATION("Bulk Member Activation", "member"),
    BULK_MEMBER_DEACTIVATION("Bulk Member Deactivation", "member"),
    BULK_MEMBER_REMOVAL("Bulk Member Removal", "member"),
    BULK_ROLE_UPDATE("Bulk Role Update", "member"),

    // Invitation Management Activities
    INVITATION_SENT("Invitation Sent", "invitation"),
    INVITATION_ACCEPTED("Invitation Accepted", "invitation"),
    INVITATION_DECLINED("Invitation Declined", "invitation"),
    INVITATION_RESENT("Invitation Resent", "invitation"),
    INVITATION_CANCELLED("Invitation Cancelled", "invitation"),
    INVITATION_EXPIRED("Invitation Expired", "invitation"),
    INVITATION_REVOKED("Invitation Revoked", "invitation"),

    // Bulk Invitation Operations
    BULK_INVITATION_SENT("Bulk Invitation Sent", "invitation"),
    BULK_INVITATION_RESENT("Bulk Invitation Resent", "invitation"),
    BULK_INVITATION_CANCELLED("Bulk Invitation Cancelled", "invitation"),
    BULK_INVITATION_REVOKED("Bulk Invitation Revoked", "invitation"),

    // Settings Management Activities
    SETTINGS_UPDATED("Settings Updated", "settings"),
    SETTINGS_RESET("Settings Reset", "settings"),
    SETTINGS_EXPORTED("Settings Exported", "settings"),
    SETTINGS_IMPORTED("Settings Imported", "settings"),

    // Security Activities
    SECURITY_POLICY_UPDATED("Security Policy Updated", "security"),
    SECURITY_BREACH_DETECTED("Security Breach Detected", "security"),
    SUSPICIOUS_ACTIVITY_DETECTED("Suspicious Activity Detected", "security"),
    LOGIN_ATTEMPT_FAILED("Login Attempt Failed", "security"),

    // Integration Activities
    INTEGRATION_ENABLED("Integration Enabled", "integration"),
    INTEGRATION_DISABLED("Integration Disabled", "integration"),
    INTEGRATION_CONFIGURED("Integration Configured", "integration"),
    INTEGRATION_ERROR("Integration Error", "integration"),

    // Data Management Activities
    DATA_EXPORT("Data Export", "data"),
    DATA_IMPORT("Data Import", "data"),
    DATA_BACKUP("Data Backup", "data"),
    DATA_CLEANUP("Data Cleanup", "data"),

    // System Activities
    SYSTEM_MAINTENANCE("System Maintenance", "system"),
    SYSTEM_UPGRADE("System Upgrade", "system"),
    SYSTEM_ERROR("System Error", "system"),

    // General Activities
    OTHER("Other Activity", "general");

    companion object {

        /**
         * Get all activity types for a specific category
         */
        fun getByCategory(category: String): List<WorkspaceActivityType> {
            return entries.filter { it.category == category }
        }

        /**
         * Get all available categories
         */
        fun getCategories(): List<String> {
            return entries.map { it.category }.distinct()
        }

        /**
         * Find activity type by display name
         */
        fun findByDisplayName(displayName: String): WorkspaceActivityType? {
            return entries.find { it.displayName.equals(displayName, ignoreCase = true) }
        }
    }
}