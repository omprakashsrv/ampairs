package com.ampairs.workspace.model.enums

/**
 * Granular permissions that can be assigned to workspace roles.
 * Permissions are grouped by functionality and can be combined.
 */
enum class Permission(
    val category: PermissionCategory,
    val description: String,
    val requiredRoleLevel: Int = 0,
) {
    // Workspace Management Permissions
    WORKSPACE_VIEW(PermissionCategory.WORKSPACE, "View workspace details", 10),
    WORKSPACE_UPDATE(PermissionCategory.WORKSPACE, "Update workspace information", 60),
    WORKSPACE_SETTINGS(PermissionCategory.WORKSPACE, "Manage workspace settings", 80),
    WORKSPACE_DELETE(PermissionCategory.WORKSPACE, "Delete workspace", 100),

    // Member Management Permissions
    MEMBER_VIEW(PermissionCategory.MEMBER, "View workspace members", 20),
    MEMBER_INVITE(PermissionCategory.MEMBER, "Invite new members", 60),
    MEMBER_REMOVE(PermissionCategory.MEMBER, "Remove members", 80),
    MEMBER_ROLE_MANAGE(PermissionCategory.MEMBER, "Change member roles", 80),

    // Data Permissions
    DATA_READ(PermissionCategory.DATA, "Read workspace data", 10),
    DATA_CREATE(PermissionCategory.DATA, "Create new data", 40),
    DATA_UPDATE(PermissionCategory.DATA, "Update existing data", 40),
    DATA_DELETE(PermissionCategory.DATA, "Delete data", 60),
    DATA_EXPORT(PermissionCategory.DATA, "Export workspace data", 40),

    // Project Permissions
    PROJECT_VIEW(PermissionCategory.PROJECT, "View projects", 20),
    PROJECT_CREATE(PermissionCategory.PROJECT, "Create new projects", 40),
    PROJECT_UPDATE(PermissionCategory.PROJECT, "Update project details", 40),
    PROJECT_DELETE(PermissionCategory.PROJECT, "Delete projects", 60),
    PROJECT_MANAGE(PermissionCategory.PROJECT, "Manage project settings", 60),

    // Integration Permissions
    INTEGRATION_VIEW(PermissionCategory.INTEGRATION, "View integrations", 40),
    INTEGRATION_MANAGE(PermissionCategory.INTEGRATION, "Manage integrations", 80),
    API_KEY_VIEW(PermissionCategory.INTEGRATION, "View API keys", 60),
    API_KEY_MANAGE(PermissionCategory.INTEGRATION, "Manage API keys", 80),

    // Reporting and Analytics Permissions
    REPORTS_VIEW(PermissionCategory.REPORTING, "View reports", 20),
    REPORTS_CREATE(PermissionCategory.REPORTING, "Create custom reports", 40),
    REPORTS_EXPORT(PermissionCategory.REPORTING, "Export reports", 40),
    ANALYTICS_VIEW(PermissionCategory.REPORTING, "View analytics dashboard", 40),
    ANALYTICS_ADVANCED(PermissionCategory.REPORTING, "Access advanced analytics", 60),

    // Billing and Subscription Permissions
    BILLING_VIEW(PermissionCategory.BILLING, "View billing information", 80),
    BILLING_MANAGE(PermissionCategory.BILLING, "Manage billing and subscriptions", 100),

    // Security Permissions
    SECURITY_VIEW(PermissionCategory.SECURITY, "View security settings", 80),
    SECURITY_MANAGE(PermissionCategory.SECURITY, "Manage security policies", 100),
    AUDIT_LOG_VIEW(PermissionCategory.SECURITY, "View audit logs", 60);

    companion object {
        /**
         * Get default permissions for a workspace role
         */
        fun getDefaultPermissions(role: WorkspaceRole): Set<Permission> {
            return Permission.values()
                .filter { it.requiredRoleLevel <= role.level }
                .toSet()
        }

        /**
         * Get permissions by category
         */
        fun getByCategory(category: PermissionCategory): Set<Permission> {
            return Permission.values()
                .filter { it.category == category }
                .toSet()
        }
    }
}

/**
 * Categories for grouping related permissions
 */
enum class PermissionCategory(val displayName: String) {
    WORKSPACE("Workspace Management"),
    MEMBER("Member Management"),
    DATA("Data Operations"),
    PROJECT("Project Management"),
    INTEGRATION("Integrations & API"),
    REPORTING("Reports & Analytics"),
    BILLING("Billing & Subscriptions"),
    SECURITY("Security & Audit")
}