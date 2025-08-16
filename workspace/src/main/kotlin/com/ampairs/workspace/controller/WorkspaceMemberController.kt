package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.user.model.User
import com.ampairs.workspace.model.dto.MemberListResponse
import com.ampairs.workspace.model.dto.MemberResponse
import com.ampairs.workspace.model.dto.UpdateMemberRequest
import com.ampairs.workspace.service.WorkspaceMemberService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
    fun getWorkspaceMembers(
        @PathVariable workspaceId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "joinedAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ApiResponse<Page<MemberListResponse>> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Check if user has access to view members
        if (!memberService.hasPermission(workspaceId, user.uid, "MEMBER_VIEW")) {
            return ApiResponse.error("ACCESS_DENIED", "Insufficient permissions to view workspace members", "workspace")
        }

        val sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val members = memberService.getWorkspaceMembers(workspaceId, pageable)
        return ApiResponse.success(members)
    }

    /**
     * Get member details (requires MEMBER_VIEW permission)
     */
    @GetMapping("/{workspaceId}/members/{memberId}")
    fun getMemberDetails(
        @PathVariable workspaceId: String,
        @PathVariable memberId: String,
    ): ApiResponse<MemberResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Check if user has access to view members
        if (!memberService.hasPermission(workspaceId, user.uid, "MEMBER_VIEW")) {
            return ApiResponse.error("ACCESS_DENIED", "Insufficient permissions to view member details", "workspace")
        }

        val member = memberService.getMemberById(memberId)
        return ApiResponse.success(member)
    }

    /**
     * Update member role and permissions (requires MEMBER_ROLE_MANAGE permission)
     */
    @PutMapping("/{workspaceId}/members/{memberId}")
    fun updateMember(
        @PathVariable workspaceId: String,
        @PathVariable memberId: String,
        @RequestBody @Valid request: UpdateMemberRequest,
    ): ApiResponse<MemberResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Check if user has permission to manage member roles
        if (!memberService.hasPermission(workspaceId, user.uid, "MEMBER_ROLE_MANAGE")) {
            return ApiResponse.error("ACCESS_DENIED", "Insufficient permissions to update member roles", "workspace")
        }

        val updatedMember = memberService.updateMember(workspaceId, memberId, request, user.uid)
        return ApiResponse.success(updatedMember)
    }

    /**
     * Remove member from workspace (requires MEMBER_REMOVE permission)
     */
    @DeleteMapping("/{workspaceId}/members/{memberId}")
    fun removeMember(
        @PathVariable workspaceId: String,
        @PathVariable memberId: String,
    ): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Check if user has permission to remove members
        if (!memberService.hasPermission(workspaceId, user.uid, "MEMBER_REMOVE")) {
            return ApiResponse.error(
                "ACCESS_DENIED",
                "Insufficient permissions to remove workspace members",
                "workspace"
            )
        }

        val result = memberService.removeMember(workspaceId, memberId, user.uid)
        return ApiResponse.success(result)
    }

    /**
     * Get current user's role and permissions in workspace
     */
    @GetMapping("/{workspaceId}/my-role")
    fun getMyRole(@PathVariable workspaceId: String): ApiResponse<Map<String, Any>> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        // Check if user is workspace member first
        if (!memberService.isWorkspaceMember(workspaceId, user.uid)) {
            return ApiResponse.error("ACCESS_DENIED", "User is not a member of this workspace", "workspace")
        }

        // For now, return basic permission check result
        val hasOwnerPermission = memberService.isWorkspaceOwner(workspaceId, user.uid)
        val hasAdminPermission = memberService.hasPermission(workspaceId, user.uid, "WORKSPACE_SETTINGS")
        val hasMemberPermission = memberService.hasPermission(workspaceId, user.uid, "MEMBER_VIEW")

        val result: Map<String, Any> = mapOf(
            "is_owner" to hasOwnerPermission,
            "is_admin" to hasAdminPermission,
            "can_view_members" to hasMemberPermission,
            "can_invite_members" to memberService.hasPermission(workspaceId, user.uid, "MEMBER_INVITE"),
            "can_manage_workspace" to memberService.hasPermission(workspaceId, user.uid, "WORKSPACE_UPDATE")
        )
        return ApiResponse.success(result)
    }
}