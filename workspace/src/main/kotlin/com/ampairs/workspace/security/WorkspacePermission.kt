package com.ampairs.workspace.security

/**
 * Enum representing workspace permissions for type-safe authorization
 * This provides compile-time safety and better IDE support compared to string constants
 */
enum class WorkspacePermission(val permissionName: String) {
    
    // Workspace Management Permissions (WORKSPACE_ACTION pattern)
    WORKSPACE_MANAGE("WORKSPACE_MANAGE"),        // Manage workspace details, settings, etc.
    WORKSPACE_DELETE("WORKSPACE_DELETE"),        // Delete workspace (includes archive/soft delete)
    
    // Member Management Permissions (MEMBER_ACTION pattern) 
    MEMBER_VIEW("MEMBER_VIEW"),                  // View workspace members
    MEMBER_INVITE("MEMBER_INVITE"),              // Invite new members
    MEMBER_MANAGE("MEMBER_MANAGE"),              // Manage member roles and permissions
    MEMBER_DELETE("MEMBER_DELETE");              // Delete/remove members from workspace

    /**
     * Get the string representation of the permission
     */
    override fun toString(): String = permissionName
    
    companion object {
        /**
         * Get permission enum from string name
         */
        fun fromString(permissionName: String): WorkspacePermission? {
            return entries.find { it.permissionName == permissionName }
        }
    }
}