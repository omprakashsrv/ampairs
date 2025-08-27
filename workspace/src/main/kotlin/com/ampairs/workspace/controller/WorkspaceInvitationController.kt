package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.user.model.User
import com.ampairs.workspace.model.dto.CreateInvitationRequest
import com.ampairs.workspace.model.dto.InvitationListResponse
import com.ampairs.workspace.model.dto.InvitationResponse
import com.ampairs.workspace.model.dto.ResendInvitationRequest
import com.ampairs.workspace.security.WorkspacePermission
import com.ampairs.workspace.service.WorkspaceInvitationService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * REST controller for workspace invitation management operations
 */
@RestController
@RequestMapping("/workspace/v1")
class WorkspaceInvitationController(
    private val invitationService: WorkspaceInvitationService,
) {

    /**
     * Get workspace invitations (requires MEMBER_INVITE permission)
     */
    @GetMapping("/{workspaceId}/invitations")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_INVITE)")
    fun getWorkspaceInvitations(
        @PathVariable workspaceId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ApiResponse<PageResponse<InvitationListResponse>> {
        val sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val invitations = invitationService.getWorkspaceInvitations(workspaceId, null, pageable)
        return ApiResponse.success(PageResponse.from(invitations))
    }

    /**
     * Create/send invitation to join workspace (requires MEMBER_INVITE permission)
     */
    @PostMapping("/{workspaceId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_INVITE)")
    fun createInvitation(
        @PathVariable workspaceId: String,
        @RequestBody @Valid request: CreateInvitationRequest,
    ): ApiResponse<InvitationResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val invitation = invitationService.createInvitation(workspaceId, request, user.uid)
        return ApiResponse.success(invitation)
    }

    /**
     * Accept workspace invitation (public endpoint)
     */
    @PostMapping("/invitations/{token}/accept")
    fun acceptInvitation(@PathVariable token: String): ApiResponse<InvitationResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val invitation = invitationService.acceptInvitation(token, user.uid)
        return ApiResponse.success(invitation)
    }

    /**
     * Resend invitation email (requires MEMBER_INVITE permission)
     */
    @PostMapping("/{workspaceId}/invitations/{invitationId}/resend")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_INVITE)")
    fun resendInvitation(
        @PathVariable workspaceId: String,
        @PathVariable invitationId: String,
        @RequestBody request: ResendInvitationRequest = ResendInvitationRequest(),
    ): ApiResponse<InvitationResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val result = invitationService.resendInvitation(invitationId, request, user.uid)
        return ApiResponse.success(result)
    }

    /**
     * Cancel/revoke invitation (requires MEMBER_INVITE permission)
     */
    @DeleteMapping("/{workspaceId}/invitations/{invitationId}")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_INVITE)")
    fun cancelInvitation(
        @PathVariable workspaceId: String,
        @PathVariable invitationId: String,
    ): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val result = invitationService.cancelInvitation(invitationId, null, user.uid)
        return ApiResponse.success(result)
    }

}