package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.user.model.User
import com.ampairs.workspace.model.dto.MemberListResponse
import com.ampairs.workspace.model.dto.MemberResponse
import com.ampairs.workspace.model.dto.UpdateMemberRequest
import com.ampairs.workspace.security.WorkspacePermission
import com.ampairs.workspace.service.WorkspaceMemberService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * REST controller for workspace member management operations
 */
@RestController
@RequestMapping("/workspace/v1")
class WorkspaceMemberController(
    private val memberService: WorkspaceMemberService,
) {

    /**
     * Get workspace members (requires MEMBER_VIEW permission)
     */
    @GetMapping("/{workspaceId}/members")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun getWorkspaceMembers(
        @PathVariable workspaceId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "joinedAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ApiResponse<PageResponse<MemberListResponse>> {
        val sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val members = memberService.getWorkspaceMembers(workspaceId, pageable)
        return ApiResponse.success(PageResponse.from(members))
    }

    /**
     * Get member details (requires MEMBER_VIEW permission)
     */
    @GetMapping("/{workspaceId}/members/{memberId}")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_VIEW)")
    fun getMemberDetails(
        @PathVariable workspaceId: String,
        @PathVariable memberId: String,
    ): ApiResponse<MemberResponse> {
        val member = memberService.getMemberById(memberId)
        return ApiResponse.success(member)
    }

    /**
     * Update member role and permissions (requires MEMBER_MANAGE permission)
     */
    @PutMapping("/{workspaceId}/members/{memberId}")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_MANAGE)")
    fun updateMember(
        @PathVariable workspaceId: String,
        @PathVariable memberId: String,
        @RequestBody @Valid request: UpdateMemberRequest,
    ): ApiResponse<MemberResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val updatedMember = memberService.updateMember(workspaceId, memberId, request, user.uid)
        return ApiResponse.success(updatedMember)
    }

    /**
     * Remove member from workspace (requires MEMBER_DELETE permission)
     */
    @DeleteMapping("/{workspaceId}/members/{memberId}")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_DELETE)")
    fun removeMember(
        @PathVariable workspaceId: String,
        @PathVariable memberId: String,
    ): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val result = memberService.removeMember(workspaceId, memberId, user.uid)
        return ApiResponse.success(result)
    }

    /**
     * Get current user's role and permissions in workspace (requires workspace membership)
     */
    @GetMapping("/{workspaceId}/my-role")
    @PreAuthorize("@workspaceAuthorizationService.isWorkspaceMember(authentication, #workspaceId)")
    fun getMyRole(@PathVariable workspaceId: String): ApiResponse<Map<String, Any>> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Return user's permissions in workspace
        val hasOwnerPermission = memberService.isWorkspaceOwner(workspaceId, user.uid)
        val hasAdminPermission = memberService.hasPermission(workspaceId, user.uid, WorkspacePermission.WORKSPACE_MANAGE.permissionName)
        val hasMemberPermission = memberService.hasPermission(workspaceId, user.uid, WorkspacePermission.MEMBER_VIEW.permissionName)

        val result: Map<String, Any> = mapOf(
            "is_owner" to hasOwnerPermission,
            "is_admin" to hasAdminPermission,
            "can_view_members" to hasMemberPermission,
            "can_invite_members" to memberService.hasPermission(workspaceId, user.uid, WorkspacePermission.MEMBER_INVITE.permissionName),
            "can_manage_workspace" to memberService.hasPermission(workspaceId, user.uid, WorkspacePermission.WORKSPACE_MANAGE.permissionName)
        )
        return ApiResponse.success(result)
    }
}