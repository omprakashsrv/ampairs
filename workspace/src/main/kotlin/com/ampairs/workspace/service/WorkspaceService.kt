package com.ampairs.workspace.service

import com.ampairs.core.exception.BusinessException
import com.ampairs.core.exception.NotFoundException
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.workspace.model.Workspace
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.model.enums.WorkspaceStatus
import com.ampairs.workspace.model.enums.WorkspaceType
import com.ampairs.workspace.repository.WorkspaceMemberRepository
import com.ampairs.workspace.repository.WorkspaceRepository
import com.ampairs.workspace.security.WorkspacePermission
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
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
    private val activityService: WorkspaceActivityService
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
            // Note: avatarUrl is managed by WorkspaceAvatarService, not set from request
            this.timezone = request.timezone
            this.language = request.language
            this.createdBy = createdBy
            this.status = WorkspaceStatus.ACTIVE
            this.subscriptionPlan = SubscriptionPlan.FREE
            this.active = true
            this.lastActivityAt = Instant.now()

            // Business address details
            this.addressLine1 = request.addressLine1
            this.addressLine2 = request.addressLine2
            this.city = request.city
            this.state = request.state
            this.postalCode = request.postalCode
            this.country = request.country

            // Contact information
            this.phone = request.phone
            this.email = request.email
            this.website = request.website

            // Legal/Tax details
            this.taxId = request.taxId ?: "GSTIN"
            this.registrationNumber = request.registrationNumber

            // Business operations
            this.currency = request.currency
            this.dateFormat = request.dateFormat
            this.timeFormat = request.timeFormat
            this.businessHoursStart = request.businessHoursStart
            this.businessHoursEnd = request.businessHoursEnd
        }

        val savedWorkspace = workspaceRepository.save(workspace)

        // Add creator as owner - switch to new workspace tenant context for @TenantId validation
        TenantContextHolder.withTenant(savedWorkspace.uid) {
            memberService.addMemberAsOwner(savedWorkspace.uid, createdBy)
        }

        // Initialize workspace settings - also requires tenant context
        TenantContextHolder.withTenant(savedWorkspace.uid) {
            settingsService.initializeDefaultSettings(savedWorkspace.uid)
        }

        // Note: FREE subscription creation is handled by subscription module
        // via lazy initialization when the subscription is first accessed

        // Log activity - also requires tenant context
        TenantContextHolder.withTenant(savedWorkspace.uid) {
            activityService.logWorkspaceCreated(savedWorkspace.uid, createdBy, "Unknown User")
        }

        logger.info("Successfully created workspace: ${savedWorkspace.uid}")
        return savedWorkspace.toResponse(memberCount = 1)
    }

    /**
     * Get workspace by ID
     */
    fun getWorkspaceById(workspaceId: String, userId: String): WorkspaceResponse {
        val workspace = findWorkspaceById(workspaceId)

        // Check if user has access to this workspace
        if (!memberService.isWorkspaceMember(userId)) {
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
        if (!memberService.isWorkspaceMember(userId)) {
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
        if (!memberService.hasPermission(updatedBy, WorkspacePermission.WORKSPACE_MANAGE)) {
            throw BusinessException("INSUFFICIENT_PERMISSIONS", "You don't have permission to update this workspace")
        }

        // Update fields
        // Note: avatarUrl and avatarThumbnailUrl are managed by WorkspaceAvatarService
        // and should not be updated via the regular update endpoint
        request.name?.let { workspace.name = it }
        request.description?.let { workspace.description = it }
        request.workspaceType?.let { workspace.workspaceType = it }
        request.timezone?.let { workspace.timezone = it }
        request.language?.let { workspace.language = it }

        // Update business address details
        request.addressLine1?.let { workspace.addressLine1 = it }
        request.addressLine2?.let { workspace.addressLine2 = it }
        request.city?.let { workspace.city = it }
        request.state?.let { workspace.state = it }
        request.postalCode?.let { workspace.postalCode = it }
        request.country?.let { workspace.country = it }

        // Update contact information
        request.phone?.let { workspace.phone = it }
        request.email?.let { workspace.email = it }
        request.website?.let { workspace.website = it }

        // Update legal/Tax details
        request.taxId?.let { workspace.taxId = it }
        request.registrationNumber?.let { workspace.registrationNumber = it }

        // Update business operations
        request.currency?.let { workspace.currency = it }
        request.dateFormat?.let { workspace.dateFormat = it }
        request.timeFormat?.let { workspace.timeFormat = it }
        request.businessHoursStart?.let { workspace.businessHoursStart = it }
        request.businessHoursEnd?.let { workspace.businessHoursEnd = it }

        workspace.lastActivityAt = Instant.now()

        val updatedWorkspace = workspaceRepository.save(workspace)

        // Log activity
        activityService.logWorkspaceUpdated(workspaceId, updatedBy, "Unknown User")

        // Get member count with fallback handling for database mapping issues
        val memberCount = try {
            memberService.getActiveMemberCount(workspace.uid)
        } catch (e: Exception) {
            logger.warn("Failed to get member count for workspace ${workspace.uid}: ${e.message}")
            // Fallback: use simple repository count or default value
            try {
                workspaceMemberRepository.countByWorkspaceIdAndIsActiveTrue(workspace.uid).toInt()
            } catch (ex: Exception) {
                logger.warn("Fallback member count also failed for workspace ${workspace.uid}: ${ex.message}")
                1 // Default fallback value
            }
        }

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
        if (!memberService.isWorkspaceOwner(archivedBy)) {
            throw BusinessException("INSUFFICIENT_PERMISSIONS", "Only workspace owners can archive workspaces")
        }

        workspace.status = WorkspaceStatus.ARCHIVED
        workspace.active = false
        workspaceRepository.save(workspace)

        // Cancel all pending invitations
        invitationService.cancelAllPendingInvitations(workspaceId, archivedBy, "Workspace archived")

        // Log activity
        activityService.logWorkspaceArchived(workspaceId, archivedBy, "Unknown User")

        logger.info("Workspace archived: $workspaceId by user: $archivedBy")
        return "Workspace archived successfully"
    }

    /**
     * Delete workspace permanently
     */
    fun deleteWorkspace(workspaceId: String, deletedBy: String): String {
        val workspace = findWorkspaceById(workspaceId)

        // Check permissions - only owners can delete
        if (!memberService.isWorkspaceOwner(deletedBy)) {
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
     * Get workspace entity by ID (for internal use by other services)
     */
    fun getWorkspaceEntity(workspaceId: String): Workspace {
        return workspaceRepository.findByUid(workspaceId)
            .orElseThrow { NotFoundException("Workspace not found: $workspaceId") }
    }

    private fun findWorkspaceById(workspaceId: String): Workspace {
        return getWorkspaceEntity(workspaceId)
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