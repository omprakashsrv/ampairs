package com.ampairs.workspace.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.workspace.security.WorkspacePermission
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * Represents a team within a workspace that can have its own permissions and members
 */
@Entity
@Table(
    name = "workspace_teams",
    indexes = [
        Index(name = "idx_team_workspace", columnList = "workspace_id"),
        Index(name = "idx_team_code", columnList = "team_code"),
        Index(name = "idx_team_department", columnList = "department"),
        Index(name = "idx_team_workspace_code", columnList = "workspace_id, team_code", unique = true)
    ]
)
class WorkspaceTeam : BaseDomain() {

    @Column(name = "workspace_id", nullable = false, length = 36)
    var workspaceId: String = ""

    @Column(name = "team_code", nullable = false, length = 50)
    var teamCode: String = ""

    @Column(name = "name", nullable = false, length = 100)
    var name: String = ""

    @Column(name = "description", length = 500)
    var description: String? = null

    @Column(name = "department", length = 100)
    var department: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permissions")
    var permissions: Set<WorkspacePermission> = setOf()

    @Column(name = "team_lead_id", length = 36)
    var teamLeadId: String? = null

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @Column(name = "max_members")
    var maxMembers: Int? = null

    @Column(name = "created_by", length = 36)
    var createdBy: String? = null

    @Column(name = "updated_by", length = 36)
    var updatedBy: String? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.WORKSPACE_TEAM_PREFIX
    }

    /**
     * Check if team has specific permission
     */
    fun hasPermission(permission: WorkspacePermission): Boolean {
        return permissions.contains(permission)
    }

    /**
     * Check if team has capacity for new members
     */
    fun hasCapacity(currentMemberCount: Int): Boolean {
        return maxMembers?.let { currentMemberCount < it } ?: true
    }
}
