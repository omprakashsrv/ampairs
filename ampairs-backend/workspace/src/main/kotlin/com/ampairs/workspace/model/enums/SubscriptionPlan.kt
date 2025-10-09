package com.ampairs.workspace.model.enums

/**
 * Subscription plans with different feature sets and limits
 */
enum class SubscriptionPlan(
    val displayName: String,
    val maxMembers: Int,
    val storageLimitGb: Int,
    val maxWorkspaces: Int,
    val hasAdvancedFeatures: Boolean,
    val hasApiAccess: Boolean,
    val hasPrioritySupport: Boolean,
) {
    /**
     * Free tier with basic features
     */
    FREE(
        displayName = "Free",
        maxMembers = 3,
        storageLimitGb = 1,
        maxWorkspaces = 1,
        hasAdvancedFeatures = false,
        hasApiAccess = false,
        hasPrioritySupport = false
    ),

    /**
     * Basic paid tier for small teams
     */
    BASIC(
        displayName = "Basic",
        maxMembers = 10,
        storageLimitGb = 10,
        maxWorkspaces = 3,
        hasAdvancedFeatures = false,
        hasApiAccess = true,
        hasPrioritySupport = false
    ),

    /**
     * Professional tier for growing teams
     */
    PROFESSIONAL(
        displayName = "Professional",
        maxMembers = 50,
        storageLimitGb = 100,
        maxWorkspaces = 10,
        hasAdvancedFeatures = true,
        hasApiAccess = true,
        hasPrioritySupport = true
    ),

    /**
     * Enterprise tier for large organizations
     */
    ENTERPRISE(
        displayName = "Enterprise",
        maxMembers = Int.MAX_VALUE,
        storageLimitGb = Int.MAX_VALUE,
        maxWorkspaces = Int.MAX_VALUE,
        hasAdvancedFeatures = true,
        hasApiAccess = true,
        hasPrioritySupport = true
    );

    /**
     * Check if plan allows a specific number of members
     */
    fun allowsMembers(memberCount: Int): Boolean {
        return memberCount <= maxMembers
    }

    /**
     * Check if plan allows a specific storage amount
     */
    fun allowsStorage(storageGb: Int): Boolean {
        return storageGb <= storageLimitGb
    }

    /**
     * Check if plan allows a specific number of workspaces
     */
    fun allowsWorkspaces(workspaceCount: Int): Boolean {
        return workspaceCount <= maxWorkspaces
    }
}