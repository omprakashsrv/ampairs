package com.ampairs.workspace.model.enums

/**
 * Hierarchical roles within a workspace with different permission levels.
 * Each role inherits permissions from lower levels.
 */
enum class WorkspaceRole(
    val displayName: String,
    val level: Int,
    val description: String,
) {
    /**
     * Full workspace control - can delete workspace and manage all aspects
     */
    OWNER("Owner", 100, "Full workspace control including deletion and billing"),

    /**
     * Administrative access - can manage members, settings, and most workspace features
     */
    ADMIN("Administrator", 80, "Administrative access with member and settings management"),

    /**
     * Management permissions - can oversee projects and team activities
     */
    MANAGER("Manager", 60, "Project and team management with limited administrative access"),

    /**
     * Standard member access - can contribute to workspace activities
     */
    MEMBER("Member", 40, "Standard access to workspace features and collaboration"),

    /**
     * Limited guest access - temporary or external user access
     */
    GUEST("Guest", 20, "Limited access for external collaborators"),

    /**
     * Read-only access - can view but not modify workspace content
     */
    VIEWER("Viewer", 10, "Read-only access to workspace content");

    /**
     * Check if this role has higher or equal permissions than another role
     */
    fun hasPermissionLevel(otherRole: WorkspaceRole): Boolean {
        return this.level >= otherRole.level
    }

    /**
     * Get all roles that this role can manage (lower level roles)
     */
    fun getManageableRoles(): Set<WorkspaceRole> {
        return WorkspaceRole.values().filter { it.level < this.level }.toSet()
    }
}