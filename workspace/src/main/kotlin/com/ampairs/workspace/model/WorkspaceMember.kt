package com.ampairs.workspace.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.workspace.model.enums.Permission
import com.ampairs.workspace.model.enums.WorkspaceRole
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * Represents a user's membership in a workspace with their role and permissions.
 * This entity consolidates both active members and invitation/membership history.
 */
@Entity
@Table(
    name = "workspace_members",
    indexes = [
        Index(name = "idx_member_workspace", columnList = "workspace_id"),
        Index(name = "idx_member_user", columnList = "user_id"),
        Index(name = "idx_member_role", columnList = "role"),
        Index(name = "idx_member_active", columnList = "is_active"),
        Index(name = "idx_member_workspace_user", columnList = "workspace_id, user_id", unique = true),
        Index(name = "idx_member_invited_by", columnList = "invited_by"),
        Index(name = "idx_member_joined_at", columnList = "joined_at")
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
     * Display name of the member at time of joining
     */
    @Column(name = "member_name", length = 255)
    var memberName: String? = null

    /**
     * Email of the member at time of joining
     */
    @Column(name = "member_email", length = 255)
    var memberEmail: String? = null

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
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permissions", columnDefinition = "JSON")
    var permissions: Set<Permission> = setOf()

    /**
     * ID of the user who invited this member
     */
    @Column(name = "invited_by", length = 36)
    var invitedBy: String? = null

    /**
     * Name of the user who invited this member
     */
    @Column(name = "invited_by_name", length = 255)
    var invitedByName: String? = null

    /**
     * When the invitation was sent
     */
    @Column(name = "invited_at")
    var invitedAt: LocalDateTime? = null

    /**
     * When the member joined the workspace (accepted invitation)
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
     * Administrative notes about this member
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
     * Name of the user who deactivated this member
     */
    @Column(name = "deactivated_by_name", length = 255)
    var deactivatedByName: String? = null

    /**
     * Reason for deactivation
     */
    @Column(name = "deactivation_reason", length = 500)
    var deactivationReason: String? = null

    /**
     * Department or team this member belongs to
     */
    @Column(name = "department", length = 100)
    var department: String? = null

    /**
     * Job title of this member
     */
    @Column(name = "job_title", length = 100)
    var jobTitle: String? = null

    /**
     * Member's phone number
     */
    @Column(name = "phone", length = 20)
    var phone: String? = null

    /**
     * Access level restrictions (JSON)
     */
    @Column(name = "access_restrictions", columnDefinition = "TEXT")
    var accessRestrictions: String = "{}"

    // JPA Relationships

    /**
     * Reference to the workspace
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    var workspace: Workspace? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.WORKSPACE_MEMBER_PREFIX
    }

    /**
     * Check if this member has a specific permission
     */
    fun hasPermission(permission: Permission): Boolean {
        return permissions.contains(permission)
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

    /**
     * Get member display name (memberName or email or userId)
     */
    fun getDisplayName(): String {
        return when {
            !memberName.isNullOrBlank() -> memberName!!
            !memberEmail.isNullOrBlank() -> memberEmail!!
            else -> userId
        }
    }

    /**
     * Check if member is owner of workspace
     */
    fun isOwner(): Boolean {
        return role == WorkspaceRole.OWNER
    }

    /**
     * Check if member is admin level or higher
     */
    fun isAdminLevel(): Boolean {
        return role.level >= WorkspaceRole.ADMIN.level
    }

    /**
     * Check if member can invite others
     */
    fun canInviteMembers(): Boolean {
        return hasPermission(Permission.MEMBER_INVITE)
    }

    /**
     * Check if member can manage workspace settings
     */
    fun canManageSettings(): Boolean {
        return hasPermission(Permission.WORKSPACE_SETTINGS)
    }

    /**
     * Get days since joined
     */
    fun getDaysSinceJoined(): Long? {
        return joinedAt?.let {
            java.time.Duration.between(it, LocalDateTime.now()).toDays()
        }
    }

    /**
     * Get days since last active
     */
    fun getDaysSinceLastActive(): Long? {
        return lastActiveAt?.let {
            java.time.Duration.between(it, LocalDateTime.now()).toDays()
        }
    }

    /**
     * Check if member is recently active (within last 7 days)
     */
    fun isRecentlyActive(): Boolean {
        return getDaysSinceLastActive()?.let { it <= 7 } ?: false
    }

    /**
     * Accept workspace invitation and activate membership
     */
    fun acceptInvitation(userName: String? = null, userEmail: String? = null) {
        isActive = true
        joinedAt = LocalDateTime.now()
        lastActiveAt = LocalDateTime.now()
        memberName = userName
        memberEmail = userEmail
    }

    /**
     * Update member role with audit info
     */
    fun updateRole(newRole: WorkspaceRole, updatedByUserId: String, updatedByName: String? = null) {
        role = newRole
        // Could log this change in workspace activity
    }

    /**
     * Get member summary for display
     */
    fun getSummary(): String {
        val parts = mutableListOf<String>()
        parts.add(getDisplayName())
        parts.add(role.displayName)
        if (department != null) parts.add(department!!)
        if (jobTitle != null) parts.add(jobTitle!!)
        return parts.joinToString(" • ")
    }
}