package com.ampairs.workspace.model.enums

/**
 * Enumeration representing different states of a workspace
 */
enum class WorkspaceStatus(val displayName: String, val description: String) {
    /**
     * Workspace is active and operational
     */
    ACTIVE("Active", "Workspace is fully operational and accessible to members"),

    /**
     * Workspace is temporarily suspended
     */
    SUSPENDED("Suspended", "Workspace access is temporarily restricted"),

    /**
     * Workspace is archived but data is preserved
     */
    ARCHIVED("Archived", "Workspace is archived and read-only, but data is preserved"),

    /**
     * Workspace is soft deleted - can be restored
     */
    DELETED("Deleted", "Workspace is marked for deletion but can be restored"),

    /**
     * Workspace is being set up/initialized
     */
    INITIALIZING("Initializing", "Workspace is being set up and configured")
}