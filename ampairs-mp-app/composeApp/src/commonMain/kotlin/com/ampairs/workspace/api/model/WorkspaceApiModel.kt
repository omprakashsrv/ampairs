package com.ampairs.workspace.api.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Objects

// ===== CORE WORKSPACE MODELS =====

@Serializable
data class WorkspaceApiModel(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String,
    @SerialName("description") val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String = "BUSINESS",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("subscription_plan") val subscriptionPlan: String = "FREE",
    @SerialName("max_members") val maxMembers: Int = 5,
    @SerialName("storage_limit_gb") val storageLimitGb: Int = 1,
    @SerialName("storage_used_gb") val storageUsedGb: Int = 0,
    @SerialName("timezone") val timezone: String = "UTC",
    @SerialName("language") val language: String = "en",
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("last_activity_at") val lastActivityAt: String? = null,
    @SerialName("trial_expires_at") val trialExpiresAt: String? = null,
    @SerialName("member_count") val memberCount: Int? = null,
    @SerialName("is_trial") val isTrial: Boolean? = null,
    @SerialName("storage_percentage") val storagePercentage: Double? = null,
)

@Serializable
data class WorkspaceListApiModel(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String,
    @SerialName("description") val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String = "BUSINESS",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("subscription_plan") val subscriptionPlan: String = "FREE",
    @SerialName("member_count") val memberCount: Int = 1,
    @SerialName("last_activity_at") val lastActivityAt: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class CreateWorkspaceRequest(
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String = "BUSINESS",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("timezone") val timezone: String = "UTC",
    @SerialName("language") val language: String = "en",
)

@Serializable
data class UpdateWorkspaceRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("language") val language: String? = null,
)


@Serializable
data class PagedWorkspaceResponse(
    @SerialName("content") val content: List<WorkspaceListApiModel>,
    @SerialName("page_number") val pageNumber: Int,
    @SerialName("page_size") val pageSize: Int,
    @SerialName("total_elements") val totalElements: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("first") val first: Boolean,
    @SerialName("last") val last: Boolean,
    @SerialName("has_next") val hasNext: Boolean,
    @SerialName("has_previous") val hasPrevious: Boolean,
    @SerialName("empty") val empty: Boolean,
)

// ===== WORKSPACE MEMBER MANAGEMENT MODELS =====

@Serializable
data class MemberApiModel(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("email") val email: String,
    @SerialName("name") val name: String,
    @SerialName("role") val role: String,
    @SerialName("status") val status: String,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("last_activity") val lastActivity: String? = null,
    @SerialName("permissions") val permissions: List<String> = emptyList(),
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
)

@Serializable
data class MemberListResponse(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("email") val email: String,
    @SerialName("name") val name: String,
    @SerialName("role") val role: String,
    @SerialName("status") val status: String,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("last_activity") val lastActivity: String? = null,
    @SerialName("permissions") val permissions: List<String> = emptyList(),
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
)

@Serializable
data class UpdateMemberRequest(
    @SerialName("role") val role: String? = null,
    @SerialName("custom_permissions") val customPermissions: List<String>? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("reason") val reason: String? = null,
    @SerialName("notify_member") val notifyMember: Boolean = true,
)

@Serializable
data class PagedMemberResponse(
    @SerialName("content") val content: List<MemberListResponse>,
    @SerialName("page") val page: Int,
    @SerialName("size") val size: Int,
    @SerialName("total_elements") val totalElements: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("is_first") val isFirst: Boolean,
    @SerialName("is_last") val isLast: Boolean,
)

@Serializable
data class UserRoleResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("current_role") val currentRole: String,
    @SerialName("membership_status") val membershipStatus: String,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("last_activity") val lastActivity: String? = null,
    @SerialName("role_hierarchy") val roleHierarchy: Map<String, Boolean>,
    @SerialName("permissions") val permissions: Map<String, Map<String, Boolean>>,
    @SerialName("module_access") val moduleAccess: List<String>,
)

// ===== WORKSPACE INVITATION MANAGEMENT MODELS =====

@Serializable
data class InvitationApiModel(
    @SerialName("id") val id: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("recipient_email") val recipientEmail: String,
    @SerialName("recipient_name") val recipientName: String? = null,
    @SerialName("invited_role") val invitedRole: String,
    @SerialName("status") val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("accepted_at") val acceptedAt: String? = null,
    @SerialName("sent_by") val sentBy: InvitationSenderInfo,
    @SerialName("delivery_status") val deliveryStatus: InvitationDeliveryStatus,
    @SerialName("invitation_message") val invitationMessage: String? = null,
    @SerialName("resend_count") val resendCount: Int = 0,
    @SerialName("last_activity") val lastActivity: String? = null,
)

@Serializable
data class InvitationListResponse(
    @SerialName("id") val id: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("recipient_email") val recipientEmail: String,
    @SerialName("recipient_name") val recipientName: String? = null,
    @SerialName("invited_role") val invitedRole: String,
    @SerialName("status") val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("sent_by") val sentBy: InvitationSenderInfo,
    @SerialName("delivery_status") val deliveryStatus: InvitationDeliveryStatus,
    @SerialName("resend_count") val resendCount: Int = 0,
)

@Serializable
data class InvitationSenderInfo(
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
)

@Serializable
data class InvitationDeliveryStatus(
    @SerialName("email_sent") val emailSent: Boolean = false,
    @SerialName("email_delivered") val emailDelivered: Boolean = false,
    @SerialName("email_opened") val emailOpened: Boolean = false,
    @SerialName("link_clicked") val linkClicked: Boolean = false,
)

@Serializable
data class CreateInvitationRequest(
    @SerialName("recipient_email") val recipientEmail: String,
    @SerialName("recipient_name") val recipientName: String? = null,
    @SerialName("invited_role") val invitedRole: String,
    @SerialName("custom_message") val customMessage: String? = null,
    @SerialName("expires_in_days") val expiresInDays: Int = 7,
    @SerialName("send_notification") val sendNotification: Boolean = true,
    @SerialName("permissions") val permissions: List<String>? = null,
    @SerialName("department") val department: String? = null,
    @SerialName("welcome_tour") val welcomeTour: Boolean = true,
)

@Serializable
data class ResendInvitationRequest(
    @SerialName("updated_message") val updatedMessage: String? = null,
    @SerialName("priority_delivery") val priorityDelivery: Boolean = false,
    @SerialName("send_time") val sendTime: String? = null,
    @SerialName("include_reminder") val includeReminder: Boolean = true,
    @SerialName("extend_expiration_days") val extendExpirationDays: Int? = null,
)

@Serializable
data class PagedInvitationResponse(
    @SerialName("content") val content: List<InvitationListResponse>,
    @SerialName("page") val page: Int,
    @SerialName("size") val size: Int,
    @SerialName("total_elements") val totalElements: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("is_first") val isFirst: Boolean,
    @SerialName("is_last") val isLast: Boolean,
)

@Serializable
data class AcceptInvitationResponse(
    @SerialName("id") val id: String,
    @SerialName("status") val status: String,
    @SerialName("accepted_at") val acceptedAt: String,
    @SerialName("workspace_info") val workspaceInfo: AcceptedWorkspaceInfo,
    @SerialName("member_details") val memberDetails: AcceptedMemberDetails,
    @SerialName("onboarding") val onboarding: OnboardingInfo,
    @SerialName("immediate_access") val immediateAccess: ImmediateAccessInfo,
)

@Serializable
data class AcceptedWorkspaceInfo(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("your_role") val yourRole: String,
)

@Serializable
data class AcceptedMemberDetails(
    @SerialName("member_id") val memberId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("email") val email: String,
    @SerialName("name") val name: String,
    @SerialName("role") val role: String,
    @SerialName("status") val status: String,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("permissions") val permissions: List<String>,
)

@Serializable
data class OnboardingInfo(
    @SerialName("welcome_tour_available") val welcomeTourAvailable: Boolean,
    @SerialName("profile_completion_required") val profileCompletionRequired: Boolean,
    @SerialName("setup_tasks") val setupTasks: List<String>,
    @SerialName("welcome_message") val welcomeMessage: String? = null,
)

@Serializable
data class ImmediateAccessInfo(
    @SerialName("dashboard_url") val dashboardUrl: String,
    @SerialName("available_modules") val availableModules: List<String>,
    @SerialName("team_members") val teamMembers: Int,
    @SerialName("recent_activity_available") val recentActivityAvailable: Boolean,
)