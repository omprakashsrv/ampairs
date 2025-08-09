package com.ampairs.workspace.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.workspace.model.enums.Permission
import com.ampairs.workspace.model.enums.WorkspaceRole
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Represents a user's membership in a workspace with their role and permissions
 */
@Entity(name = "workspace_members")
@Table(
    name = "workspace_members",
    indexes = [
        Index(name = "idx_member_workspace", columnList = "workspace_id"),
        Index(name = "idx_member_user", columnList = "user_id"),
        Index(name = "idx_member_role", columnList = "role"),
        Index(name = "idx_member_active", columnList = "is_active"),
        Index(name = "idx_member_workspace_user", columnList = "workspace_id, user_id", unique = true)
    ]
)
class WorkspaceMember : BaseDomain() {

    /**
     * ID of the workspace this membership belongs to
     */
    @Column(name = "workspace_id", nullable = false, length = 36)
    var workspaceId: String = ""

    /**
     * ID of the user who is a member
     */
    @Column(name = "user_id", nullable = false, length = 36)
    var userId: String = ""

    /**
     * Role of the user within this workspace
     */
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    var role: WorkspaceRole = WorkspaceRole.MEMBER

    /**
     * Custom permissions specific to this member (JSON array)
     * Used for fine-grained permission overrides beyond role defaults
     */
    @Column(name = "custom_permissions", columnDefinition = "TEXT")
    var customPermissions: String = "[]"

    /**
     * ID of the user who invited this member
     */
    @Column(name = "invited_by", length = 36)
    var invitedBy: String? = null

    /**
     * When the invitation was sent
     */
    @Column(name = "invited_at")
    var invitedAt: LocalDateTime? = null

    /**
     * When the member joined the workspace
     */
    @Column(name = "joined_at")
    var joinedAt: LocalDateTime? = null

    /**
     * Whether this membership is currently active
     */
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    /**
     * When this member was last active in the workspace
     */
    @Column(name = "last_active_at")
    var lastActiveAt: LocalDateTime? = null

    /**
     * Notes about this member (e.g., reason for specific permissions)
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null

    /**
     * When this member was deactivated (if applicable)
     */
    @Column(name = "deactivated_at")
    var deactivatedAt: LocalDateTime? = null

    /**
     * ID of the user who deactivated this member
     */
    @Column(name = "deactivated_by", length = 36)
    var deactivatedBy: String? = null

    /**
     * Reason for deactivation
     */
    @Column(name = "deactivation_reason", length = 255)
    var deactivationReason: String? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.WORKSPACE_MEMBER_PREFIX
    }

    /**
     * Get the combined permissions for this member (role + custom permissions)
     */
    fun getEffectivePermissions(): Set<Permission> {
        val objectMapper = ObjectMapper()
        val rolePermissions = Permission.getDefaultPermissions(role)

        return try {
            val customPerms = objectMapper.readValue(
                customPermissions,
                object : TypeReference<Set<Permission>>() {}
            )
            rolePermissions + customPerms
        } catch (e: Exception) {
            rolePermissions
        }
    }

    /**
     * Add a custom permission to this member
     */
    fun addCustomPermission(permission: Permission) {
        val objectMapper = ObjectMapper()
        try {
            val currentPerms = objectMapper.readValue(
                customPermissions,
                object : TypeReference<MutableSet<Permission>>() {}
            )
            currentPerms.add(permission)
            customPermissions = objectMapper.writeValueAsString(currentPerms)
        } catch (e: Exception) {
            customPermissions = objectMapper.writeValueAsString(setOf(permission))
        }
    }

    /**
     * Remove a custom permission from this member
     */
    fun removeCustomPermission(permission: Permission) {
        val objectMapper = ObjectMapper()
        try {
            val currentPerms = objectMapper.readValue(
                customPermissions,
                object : TypeReference<MutableSet<Permission>>() {}
            )
            currentPerms.remove(permission)
            customPermissions = objectMapper.writeValueAsString(currentPerms)
        } catch (e: Exception) {
            // If parsing fails, reset to empty array
            customPermissions = "[]"
        }
    }

    /**
     * Check if this member has a specific permission
     */
    fun hasPermission(permission: Permission): Boolean {
        return getEffectivePermissions().contains(permission)
    }

    /**
     * Check if this member has a specific permission by string name
     */
    fun hasPermission(permissionName: String): Boolean {
        try {
            val permission = Permission.valueOf(permissionName)
            return hasPermission(permission)
        } catch (e: IllegalArgumentException) {
            return false
        }
    }

    /**
     * Get custom permissions as list of strings
     */
    fun getCustomPermissionsList(): List<String> {
        val objectMapper = ObjectMapper()
        return try {
            val permissions = objectMapper.readValue(
                customPermissions,
                object : TypeReference<Set<Permission>>() {}
            )
            permissions.map { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Alias for lastActiveAt to match DTO expectations
     */
    val lastActivityAt: LocalDateTime?
        get() = lastActiveAt

    /**
     * Alias for joinedAt to match invitation accepted at
     */
    val invitationAcceptedAt: LocalDateTime?
        get() = joinedAt

    /**
     * Update last active timestamp
     */
    fun recordActivity() {
        lastActiveAt = LocalDateTime.now()
    }

    /**
     * Deactivate this member
     */
    fun deactivate(deactivatedByUserId: String, reason: String? = null) {
        isActive = false
        deactivatedAt = LocalDateTime.now()
        deactivatedBy = deactivatedByUserId
        deactivationReason = reason
    }

    /**
     * Reactivate this member
     */
    fun reactivate() {
        isActive = true
        deactivatedAt = null
        deactivatedBy = null
        deactivationReason = null
    }

    /**
     * Check if this member can manage another member based on role hierarchy
     */
    fun canManage(otherMember: WorkspaceMember): Boolean {
        return role.hasPermissionLevel(otherMember.role) && role != otherMember.role
    }
}