package com.ampairs.workspace.repository

import com.ampairs.workspace.model.WorkspaceTeam
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface WorkspaceTeamRepository : JpaRepository<WorkspaceTeam, String> {

    fun findByWorkspaceId(workspaceId: String, pageable: Pageable): Page<WorkspaceTeam>

    fun findByWorkspaceIdAndIsActiveTrue(workspaceId: String, pageable: Pageable): Page<WorkspaceTeam>

    fun findByWorkspaceIdAndTeamCode(workspaceId: String, teamCode: String): WorkspaceTeam?

    fun findByWorkspaceIdAndDepartment(workspaceId: String, department: String, pageable: Pageable): Page<WorkspaceTeam>

    fun findByWorkspaceIdAndTeamLeadId(workspaceId: String, teamLeadId: String): List<WorkspaceTeam>

    @Query("""
        SELECT t FROM WorkspaceTeam t 
        WHERE t.workspaceId = :workspaceId
        AND (:searchText IS NULL OR 
            LOWER(t.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR
            LOWER(t.description) LIKE LOWER(CONCAT('%', :searchText, '%')))
        AND (:department IS NULL OR t.department = :department)
        AND (:isActive IS NULL OR t.isActive = :isActive)
    """)
    fun searchTeams(
        workspaceId: String,
        searchText: String?,
        department: String?,
        isActive: Boolean?,
        pageable: Pageable
    ): Page<WorkspaceTeam>

    @Query("""
        SELECT COUNT(t) FROM WorkspaceTeam t
        WHERE t.workspaceId = :workspaceId AND t.isActive = true
    """)
    fun countActiveTeams(workspaceId: String): Long

    fun existsByWorkspaceIdAndTeamCode(workspaceId: String, teamCode: String): Boolean
}
