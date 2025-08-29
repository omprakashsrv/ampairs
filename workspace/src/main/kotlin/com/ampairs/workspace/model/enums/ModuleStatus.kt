package com.ampairs.workspace.model.enums

/**
 * Status of a module in the master registry or workspace configuration
 */
enum class ModuleStatus(val displayName: String, val description: String) {
    ACTIVE("Active", "Module is active and available for use"),
    INACTIVE("Inactive", "Module is temporarily disabled"),
    BETA("Beta", "Module is in beta testing phase"),
    DEPRECATED("Deprecated", "Module is deprecated but still functional"),
    MAINTENANCE("Maintenance", "Module is under maintenance"),
    COMING_SOON("Coming Soon", "Module is planned for future release")
}

/**
 * Subscription plans that determine module availability
 */
enum class SubscriptionTier(
    val displayName: String,
    val description: String,
    val maxUsers: Int,
    val storageGb: Int,
    val priority: Int
) {
    FREE("Free", "Basic features for small teams", 3, 1, 1),
    BASIC("Basic", "Essential features for growing businesses", 10, 5, 2),
    STANDARD("Standard", "Standard features for established businesses", 25, 15, 3),
    PREMIUM("Premium", "Premium features for growing organizations", 50, 25, 4),
    ENTERPRISE("Enterprise", "Full features for large organizations", -1, -1, 5); // -1 = unlimited
    
    fun isUnlimited(field: String): Boolean {
        return when (field) {
            "users" -> maxUsers == -1
            "storage" -> storageGb == -1
            else -> false
        }
    }
    
    fun supportsModule(requiredTier: SubscriptionTier): Boolean {
        return this.priority >= requiredTier.priority
    }
}

/**
 * User roles within workspace for module access control
 */
enum class UserRole(
    val displayName: String,
    val description: String,
    val priority: Int,
    val canManageModules: Boolean
) {
    VIEWER("Viewer", "View-only access to assigned modules", 1, false),
    EMPLOYEE("Employee", "Standard user with module access", 2, false),
    MANAGER("Manager", "Manage team and some modules", 3, false),
    ADMIN("Admin", "Full module management except billing", 4, true),
    OWNER("Owner", "Complete workspace control", 5, true);
    
    fun hasAccessLevel(requiredRole: UserRole): Boolean {
        return this.priority >= requiredRole.priority
    }
}

/**
 * Module installation and usage status
 */
enum class WorkspaceModuleStatus(val displayName: String, val description: String) {
    INSTALLING("Installing", "Module is being installed"),
    INSTALLED("Installed", "Module is installed and ready to use"),
    CONFIGURING("Configuring", "Module is being configured"),
    ACTIVE("Active", "Module is actively being used"),
    SUSPENDED("Suspended", "Module access is temporarily suspended"),
    ARCHIVED("Archived", "Module is archived but data is retained"),
    INSTALLATION_FAILED("Installation Failed", "Module installation failed"),
    UNINSTALLING("Uninstalling", "Module is being uninstalled")
}

