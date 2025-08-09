package com.ampairs.workspace.model.enums

/**
 * Represents different types of workspaces in the system.
 * Each type defines the intended use case and collaboration model.
 */
enum class WorkspaceType {
    /**
     * Individual workspace for personal projects and data
     */
    PERSONAL,

    /**
     * Team-based workspace for collaborative projects
     */
    TEAM,

    /**
     * Organization-wide workspace for large enterprises
     */
    ORGANIZATION,

    /**
     * Client-specific workspace for external stakeholder collaboration
     */
    CLIENT,

    /**
     * Project-specific workspace with defined scope and timeline
     */
    PROJECT
}