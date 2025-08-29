package com.ampairs.workspace.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.workspace.model.enums.WorkspaceRole
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity(name = "user_workspace")
class UserWorkspace : BaseDomain() {

    @Column(name = "workspace_id", length = 200, updatable = false, nullable = false)
    var companyId: String = ""

    @Column(name = "user_id", length = 200, updatable = false, nullable = false)
    var userId: String = ""

    @Column(name = "role", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    var role: WorkspaceRole = WorkspaceRole.MEMBER

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @Column(name = "joined_at")
    var joinedAt: LocalDateTime = LocalDateTime.now()

    @OneToOne
    @JoinColumn(name = "workspace_id", referencedColumnName = "uid", updatable = false, insertable = false)
    lateinit var company: Workspace

    override fun obtainSeqIdPrefix(): String {
        return Constants.USER_WORKSPACE_PREFIX
    }

    /**
     * Convert to new WorkspaceMember entity
     */
    fun toWorkspaceMember(): WorkspaceMember {
        val member = WorkspaceMember()
        member.workspaceId = this.companyId
        member.userId = this.userId
        member.role = this.role
        member.isActive = this.isActive
        member.joinedAt = this.joinedAt
        member.createdAt = this.createdAt
        member.updatedAt = this.updatedAt
        return member
    }
}