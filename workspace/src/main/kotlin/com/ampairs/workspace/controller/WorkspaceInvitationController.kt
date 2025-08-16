package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.user.model.User
import com.ampairs.workspace.model.dto.CreateInvitationRequest
import com.ampairs.workspace.model.dto.InvitationListResponse
import com.ampairs.workspace.model.dto.InvitationResponse
import com.ampairs.workspace.model.dto.ResendInvitationRequest
import com.ampairs.workspace.service.WorkspaceInvitationService
import com.ampairs.workspace.service.WorkspaceMemberService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
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
    private val memberService: WorkspaceMemberService,
) {

    /**
     * Get workspace invitations (requires MEMBER_INVITE permission)
     */
    @GetMapping("/{workspaceId}/invitations")
    fun getWorkspaceInvitations(
        @PathVariable workspaceId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ApiResponse<Page<InvitationListResponse>> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Check if user has access to view invitations
        if (!memberService.hasPermission(workspaceId, user.uid, "MEMBER_INVITE")) {
            return ApiResponse.error(
                "ACCESS_DENIED",
                "Insufficient permissions to view workspace invitations",
                "workspace"
            )
        }

        val sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val invitations = invitationService.getWorkspaceInvitations(workspaceId, null, pageable)
        return ApiResponse.success(invitations)
    }

    /**
     * Create/send invitation to join workspace (requires MEMBER_INVITE permission)
     */
    @PostMapping("/{workspaceId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    fun createInvitation(
        @PathVariable workspaceId: String,
        @RequestBody @Valid request: CreateInvitationRequest,
    ): ApiResponse<InvitationResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Check if user has permission to invite members
        if (!memberService.hasPermission(workspaceId, user.uid, "MEMBER_INVITE")) {
            return ApiResponse.error(
                "ACCESS_DENIED",
                "Insufficient permissions to invite workspace members",
                "workspace"
            )
        }

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
    fun resendInvitation(
        @PathVariable workspaceId: String,
        @PathVariable invitationId: String,
        @RequestBody request: ResendInvitationRequest = ResendInvitationRequest(),
    ): ApiResponse<InvitationResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Check if user has permission to invite members
        if (!memberService.hasPermission(workspaceId, user.uid, "MEMBER_INVITE")) {
            return ApiResponse.error("ACCESS_DENIED", "Insufficient permissions to resend invitations", "workspace")
        }

        val result = invitationService.resendInvitation(invitationId, request, user.uid)
        return ApiResponse.success(result)
    }

    /**
     * Cancel/revoke invitation (requires MEMBER_INVITE permission)
     */
    @DeleteMapping("/{workspaceId}/invitations/{invitationId}")
    fun cancelInvitation(
        @PathVariable workspaceId: String,
        @PathVariable invitationId: String,
    ): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Check if user has permission to manage invitations
        if (!memberService.hasPermission(workspaceId, user.uid, "MEMBER_INVITE")) {
            return ApiResponse.error("ACCESS_DENIED", "Insufficient permissions to cancel invitations", "workspace")
        }

        val result = invitationService.cancelInvitation(invitationId, null, user.uid)
        return ApiResponse.success(result)
    }

}