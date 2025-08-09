package com.ampairs.workspace.service

import com.ampairs.core.exception.BusinessException
import com.ampairs.core.exception.NotFoundException
import com.ampairs.workspace.model.Workspace
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.model.enums.WorkspaceStatus
import com.ampairs.workspace.model.enums.WorkspaceType
import com.ampairs.workspace.repository.WorkspaceMemberRepository
import com.ampairs.workspace.repository.WorkspaceRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Service for workspace management operations
 */
@Service
@Transactional
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceMemberRepository: WorkspaceMemberRepository,
    private val memberService: WorkspaceMemberService,
    private val invitationService: WorkspaceInvitationService,
    private val settingsService: WorkspaceSettingsService,
    private val activityService: WorkspaceActivityService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
        private const val DEFAULT_SLUG_LENGTH = 8
        private const val MAX_SLUG_ATTEMPTS = 10
    }

    /**
     * Create a new workspace
     */
    fun createWorkspace(request: CreateWorkspaceRequest, createdBy: String): WorkspaceResponse {
        logger.info("Creating workspace: ${request.name} for user: $createdBy")

        // Generate or validate slug
        val slug = generateUniqueSlug(request.slug ?: generateSlugFromName(request.name))

        // Create workspace entity
        val workspace = Workspace().apply {
            this.name = request.name
            this.slug = slug
            this.description = request.description
            this.workspaceType = request.workspaceType
            this.avatarUrl = request.avatarUrl
            this.timezone = request.timezone
            this.language = request.language
            this.ownerId = createdBy // Use ownerId from OwnableBaseDomain
            this.status = WorkspaceStatus.ACTIVE
            this.subscriptionPlan = SubscriptionPlan.FREE
            this.active = true
            this.lastActivityAt = LocalDateTime.now()
        }

        val savedWorkspace = workspaceRepository.save(workspace)

        // Add creator as owner
        memberService.addMemberAsOwner(savedWorkspace.uid, createdBy)

        // Initialize workspace settings
        settingsService.initializeDefaultSettings(savedWorkspace.uid)

        // Log activity
        activityService.logWorkspaceCreated(savedWorkspace.uid, createdBy)

        logger.info("Successfully created workspace: ${savedWorkspace.uid}")
        return savedWorkspace.toResponse(memberCount = 1)
    }

    /**
     * Get workspace by ID
     */
    fun getWorkspaceById(workspaceId: String, userId: String): WorkspaceResponse {
        val workspace = findWorkspaceById(workspaceId)

        // Check if user has access to this workspace
        if (!memberService.isWorkspaceMember(workspace.uid, userId)) {
            throw BusinessException("ACCESS_DENIED", "You don't have access to this workspace")
        }

        val memberCount = memberService.getActiveMemberCount(workspace.uid)
        return workspace.toResponse(memberCount = memberCount)
    }

    /**
     * Get workspace by slug
     */
    fun getWorkspaceBySlug(slug: String, userId: String): WorkspaceResponse {
        val workspace = workspaceRepository.findBySlug(slug)
            .orElseThrow { NotFoundException("Workspace not found with slug: $slug") }

        // Check if user has access to this workspace
        if (!memberService.isWorkspaceMember(workspace.uid, userId)) {
            throw BusinessException("ACCESS_DENIED", "You don't have access to this workspace")
        }

        val memberCount = memberService.getActiveMemberCount(workspace.uid)
        return workspace.toResponse(memberCount = memberCount)
    }

    /**
     * Update workspace information
     */
    fun updateWorkspace(workspaceId: String, request: UpdateWorkspaceRequest, updatedBy: String): WorkspaceResponse {
        val workspace = findWorkspaceById(workspaceId)

        // Check permissions
        if (!memberService.hasPermission(workspaceId, updatedBy, "MANAGE_WORKSPACE")) {
            throw BusinessException("INSUFFICIENT_PERMISSIONS", "You don't have permission to update this workspace")
        }

        // Update fields
        request.name?.let { workspace.name = it }
        request.description?.let { workspace.description = it }
        request.workspaceType?.let { workspace.workspaceType = it }
        request.avatarUrl?.let { workspace.avatarUrl = it }
        request.timezone?.let { workspace.timezone = it }
        request.language?.let { workspace.language = it }

        workspace.updatedBy = updatedBy
        workspace.lastActivityAt = LocalDateTime.now()

        val updatedWorkspace = workspaceRepository.save(workspace)

        // Log activity
        activityService.logWorkspaceUpdated(workspaceId, updatedBy)

        val memberCount = memberService.getActiveMemberCount(workspace.uid)
        return updatedWorkspace.toResponse(memberCount = memberCount)
    }

    /**
     * Get workspaces for a user
     */
    fun getUserWorkspaces(userId: String, pageable: Pageable): Page<WorkspaceListResponse> {
        val workspaces = workspaceRepository.findWorkspacesByUserId(userId, pageable)

        return workspaces.map { workspace ->
            val memberCount = memberService.getActiveMemberCount(workspace.uid)
            workspace.toListResponse(memberCount)
        }
    }

    /**
     * Search workspaces
     */
    fun searchWorkspaces(
        query: String,
        workspaceType: WorkspaceType?,
        subscriptionPlan: SubscriptionPlan?,
        userId: String,
        pageable: Pageable,
    ): Page<WorkspaceListResponse> {
        val workspaces = workspaceRepository.searchWorkspaces(query, pageable)

        return workspaces.map { workspace ->
            val memberCount = memberService.getActiveMemberCount(workspace.uid)
            workspace.toListResponse(memberCount)
        }
    }

    /**
     * Archive workspace (soft delete)
     */
    fun archiveWorkspace(workspaceId: String, archivedBy: String): String {
        val workspace = findWorkspaceById(workspaceId)

        // Check permissions - only owners can archive
        if (!memberService.isWorkspaceOwner(workspaceId, archivedBy)) {
            throw BusinessException("INSUFFICIENT_PERMISSIONS", "Only workspace owners can archive workspaces")
        }

        workspace.status = WorkspaceStatus.ARCHIVED
        workspace.active = false
        workspace.updatedBy = archivedBy
        workspaceRepository.save(workspace)

        // Cancel all pending invitations
        invitationService.cancelAllPendingInvitations(workspaceId, archivedBy, "Workspace archived")

        // Log activity
        activityService.logWorkspaceArchived(workspaceId, archivedBy)

        logger.info("Workspace archived: $workspaceId by user: $archivedBy")
        return "Workspace archived successfully"
    }

    /**
     * Delete workspace permanently
     */
    fun deleteWorkspace(workspaceId: String, deletedBy: String): String {
        val workspace = findWorkspaceById(workspaceId)

        // Check permissions - only owners can delete
        if (!memberService.isWorkspaceOwner(workspaceId, deletedBy)) {
            throw BusinessException("INSUFFICIENT_PERMISSIONS", "Only workspace owners can delete workspaces")
        }

        // Check if workspace has active subscription
        if (workspace.subscriptionPlan != SubscriptionPlan.FREE) {
            throw BusinessException("ACTIVE_SUBSCRIPTION", "Cannot delete workspace with active subscription")
        }

        // Delete all related data
        memberService.removeAllMembers(workspaceId)
        invitationService.deleteAllInvitations(workspaceId)
        settingsService.deleteSettings(workspaceId)
        activityService.deleteActivities(workspaceId)

        // Delete workspace
        workspaceRepository.delete(workspace)

        logger.info("Workspace permanently deleted: $workspaceId by user: $deletedBy")
        return "Workspace deleted permanently"
    }

    /**
     * Check workspace availability (for slug)
     */
    fun checkSlugAvailability(slug: String): Map<String, Boolean> {
        val isAvailable = !workspaceRepository.existsBySlug(slug)
        return mapOf("available" to isAvailable)
    }

    /**
     * Get workspace member count
     */
    fun getWorkspaceMemberCount(workspaceId: String): Int {
        return memberService.getActiveMemberCount(workspaceId)
    }

    /**
     * Update workspace activity timestamp
     */
    fun updateLastActivity(workspaceId: String) {
        workspaceRepository.updateLastActivity(workspaceId, LocalDateTime.now())
    }

    // Private helper methods

    private fun findWorkspaceById(workspaceId: String): Workspace {
        return workspaceRepository.findById(workspaceId)
            .orElseThrow { NotFoundException("Workspace not found: $workspaceId") }
    }

    private fun generateSlugFromName(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), "-")
            .take(30)
    }

    private fun generateUniqueSlug(preferredSlug: String): String {
        var slug = preferredSlug
        var attempt = 0

        while (workspaceRepository.existsBySlug(slug) && attempt < MAX_SLUG_ATTEMPTS) {
            attempt++
            slug = "$preferredSlug-${UUID.randomUUID().toString().substring(0, DEFAULT_SLUG_LENGTH)}"
        }

        if (workspaceRepository.existsBySlug(slug)) {
            throw BusinessException("SLUG_GENERATION_FAILED", "Unable to generate unique slug")
        }

        return slug
    }
}