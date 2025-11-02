package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.workspace.model.dto.InvitationResponse
import com.ampairs.workspace.service.WorkspaceInvitationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * User-scoped invitation controller that doesn't require workspace context.
 * This controller handles cross-workspace invitation operations.
 */
@Tag(
    name = "User Invitations",
    description = "User-scoped invitation management endpoints"
)
@RestController
@RequestMapping("/user/v1/invitation")
class UserInvitationController(
    private val invitationService: WorkspaceInvitationService,
) {

    @Operation(
        summary = "Get My Pending Invitations",
        description = """
        ## 📨 **Get Current User's Pending Workspace Invitations**

        Retrieves all pending workspace invitations for the currently authenticated user.
        This endpoint allows users to see all workspaces they've been invited to join.

        **No workspace context required** - this is a user-scoped endpoint that works
        across all workspaces. No X-Workspace-ID header needed.

        ### **Response Information:**
        - **Invitation Details**: Complete invitation information including workspace details
        - **Workspace Info**: Workspace name and ID for each invitation
        - **Expiration Status**: Only returns non-expired invitations
        - **Role Information**: The role they would be assigned upon acceptance
        - **Invitation Message**: Any custom message from the inviter

        ### **Use Cases:**
        - **User Dashboard**: Display pending invitations in user's dashboard
        - **Notification Center**: Show invitation notifications
        - **Mobile App**: List invitations for acceptance/rejection
        - **Email Integration**: Show invitations from email links
        - **First-time Users**: Users who haven't joined any workspace yet

        ### **Authentication:**
        - Requires valid JWT authentication only
        - Uses current user's email/phone to find invitations
        - No X-Workspace-ID header required

        ### **Business Value:**
        - Centralized invitation management for users
        - Easy discovery of workspace opportunities
        - Streamlined onboarding process for new users
        """,
        tags = ["User Invitations"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Successfully retrieved pending invitations",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "My Pending Invitations Response",
                        value = """{
  "success": true,
  "data": [
    {
      "id": "INV_ABC123_XYZ789",
      "workspace_id": "WSP_MARKETING_001",
      "workspace_name": "Marketing Team Workspace",
      "email": "user@example.com",
      "role": "MEMBER",
      "status": "PENDING",
      "message": "Welcome to our marketing team!",
      "token": "secure_invitation_token_here",
      "expires_at": "2025-01-20T10:00:00Z",
      "created_at": "2025-01-13T10:00:00Z",
      "invited_by": "USER_123",
      "inviter_name": "John Manager"
    }
  ],
  "timestamp": "2025-01-16T12:00:00Z"
}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "🔍 User contact required - User must have email or phone for invitation lookup"
            ),
            SwaggerApiResponse(
                responseCode = "500",
                description = "💥 Internal server error - System unavailable"
            )
        ]
    )
    @GetMapping("/pending")
    fun getMyPendingInvitations(): ResponseEntity<ApiResponse<List<InvitationResponse>>> {
        val pendingInvitations = invitationService.getUserPendingInvitations()
        return ResponseEntity.ok(ApiResponse.success(pendingInvitations))
    }

    @Operation(
        summary = "Accept Workspace Invitation",
        description = """
        ## ✅ **Accept Workspace Invitation via Token**

        Public endpoint for accepting workspace invitations using the secure invitation token.
        This completes the invitation workflow by adding the user to the workspace with
        the pre-assigned role and permissions.

        **No workspace context required** - this is a user-scoped endpoint that uses the
        token to determine workspace context automatically.

        ### **Acceptance Process:**

        #### **🔐 Token Validation**
        - **Token Verification**: Validate invitation token authenticity and integrity
        - **Expiration Check**: Ensure invitation has not expired
        - **Single Use**: Confirm token hasn't been used previously
        - **Workspace Validation**: Verify target workspace still exists and is active

        #### **👤 User Integration**
        - **Account Linking**: Associate invitation with authenticated user account
        - **Role Assignment**: Apply pre-configured role and permissions
        - **Workspace Access**: Grant immediate access to workspace resources
        - **Profile Integration**: Merge any provided profile information

        #### **🔔 Notification & Communication**
        - **Welcome Email**: Send personalized welcome message to new member
        - **Team Notification**: Notify workspace admins of successful acceptance
        - **Onboarding Trigger**: Initialize guided tour and onboarding workflow
        - **Activity Log**: Record acceptance in audit trail

        ### **Post-Acceptance Setup:**

        **Immediate Access:**
        - **Workspace Dashboard**: Access to workspace overview and navigation
        - **Role-Specific Features**: Immediate availability of role-appropriate tools
        - **Team Directory**: Access to member directory and contact information
        - **Resource Library**: Access to shared documents and resources

        **Onboarding Experience:**
        - **Welcome Tour**: Guided introduction to workspace features
        - **Profile Completion**: Prompts to complete member profile
        - **Preference Setup**: Configure notification and communication preferences
        - **Integration Setup**: Connect with relevant business tools and modules

        ### **Security Considerations:**
        - **Authentication Required**: User must be logged in to accept invitation
        - **Account Verification**: Confirm account email matches invitation recipient
        - **IP Tracking**: Log acceptance IP address for security audit
        - **Session Management**: Establish secure workspace session context

        ### **Error Handling:**
        Common acceptance failures and resolutions:
        - **Expired Token**: Invitation has passed expiration date (request new invitation)
        - **Invalid Token**: Token is malformed or doesn't exist (verify invitation link)
        - **Already Used**: Invitation already accepted (check existing workspace access)
        - **Account Mismatch**: User email doesn't match invitation recipient (contact administrator)

        ### **Use Cases:**
        - **New Employee Onboarding**: First-time workspace access for new hires
        - **External Collaboration**: Partner or consultant joining project workspace
        - **Role Migration**: User accepting invitation to different workspace or role
        - **Account Recovery**: Re-establishing workspace access after account issues
        """,
        tags = ["Invitation Acceptance"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Invitation successfully accepted, user added to workspace",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Invitation Acceptance Success",
                        value = """{
  "success": true,
  "data": {
    "id": "INV_003_ALEX_TAYLOR",
    "status": "ACCEPTED",
    "accepted_at": "2025-01-15T10:30:00Z",
    "workspace_info": {
      "id": "WS_ABC123_XYZ789",
      "name": "Acme Corp - Project Management",
      "description": "Collaborative workspace for project coordination",
      "member_count": 16,
      "your_role": "MEMBER"
    },
    "member_details": {
      "member_id": "MBR_003_ALEX_TAYLOR",
      "user_id": "USR_ALEX_TAYLOR_456",
      "email": "alex.taylor@example.com",
      "name": "Alex Taylor",
      "role": "MEMBER",
      "status": "ACTIVE",
      "joined_at": "2025-01-15T10:30:00Z",
      "permissions": [
        "VIEW_DATA",
        "CREATE_DATA",
        "UPDATE_DATA",
        "BASIC_REPORTS"
      ]
    }
  },
  "timestamp": "2025-01-15T10:30:00Z"
}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "❌ Bad request - Invalid invitation token format"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Must be logged in to accept invitation"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "🔍 Invitation not found - Invalid or expired invitation token"
            ),
            SwaggerApiResponse(
                responseCode = "409",
                description = "⚠️ Conflict - Invitation already accepted or user already workspace member"
            ),
            SwaggerApiResponse(
                responseCode = "410",
                description = "⏰ Gone - Invitation has expired and is no longer valid"
            ),
            SwaggerApiResponse(
                responseCode = "422",
                description = "🚫 Unprocessable Entity - Account email doesn't match invitation recipient"
            )
        ]
    )
    @PostMapping("/{id}/accept")
    fun acceptInvitation(
        @Parameter(
            name = "id",
            description = """
            **Invitation ID**

            The unique identifier of the invitation obtained from the pending invitations list.
            This ID is used to accept a specific invitation that the user has received.

            **ID Format:** Alphanumeric string (UUID or similar)
            **Example:** 'INV_ABC123_XYZ789' or 'uuid-format-string'

            **Properties:**
            - **User-Specific**: Only the invitation recipient can use this ID
            - **Time-Limited**: Invitation expires after configured period (default: 7 days)
            - **Secure**: Validates recipient ownership before processing
            - **Workspace-Specific**: Tied to specific workspace and role assignment

            **Where to Find ID:**
            1. **Pending Invitations API**: Primary source via GET /user/v1/invitations/pending
            2. **User Dashboard**: Listed in user's invitation management interface
            3. **Mobile App**: Available in invitation list view

            **Security Features:**
            - Only invitation recipient can use the ID
            - Validates user ownership before processing
            - Single-use operation (cannot accept twice)
            """,
            required = true,
            example = "INV_ABC123_XYZ789"
        )
        @PathVariable id: String,
    ): ApiResponse<InvitationResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        // First, get the invitation to determine workspace context
        val invitation = invitationService.findInvitationById(id)

        // Set tenant context to invitation's workspace before service call
        val currentTenant = TenantContextHolder.getCurrentTenant()
        try {
            TenantContextHolder.setCurrentTenant(invitation.workspaceId)
            val acceptedInvitation = invitationService.acceptInvitationById(id, userId)
            return ApiResponse.success(acceptedInvitation)
        } finally {
            // Restore original tenant context
            TenantContextHolder.setCurrentTenant(currentTenant)
        }
    }

    @Operation(
        summary = "Reject Workspace Invitation",
        description = """
        ## ❌ **Reject Workspace Invitation via ID**

        User-scoped endpoint for rejecting workspace invitations using the invitation ID.
        This allows users to decline workspace invitations they don't wish to accept.

        **No workspace context required** - this is a user-scoped endpoint that uses the
        invitation ID to determine workspace context automatically.

        ### **Rejection Process:**

        #### **🔐 Invitation Validation**
        - **ID Verification**: Validate invitation ID exists and is accessible to user
        - **Expiration Check**: Ensure invitation has not expired
        - **Single Use**: Confirm invitation hasn't been processed previously
        - **Workspace Validation**: Verify target workspace still exists and is active
        - **Recipient Validation**: Ensure authenticated user is the invitation recipient

        #### **👤 User Authorization**
        - **Account Verification**: Confirm account email/phone matches invitation recipient
        - **Authentication Required**: User must be logged in to reject invitation
        - **Ownership Check**: Only the intended recipient can reject the invitation

        #### **📝 Rejection Recording**
        - **Status Update**: Mark invitation as DECLINED in database
        - **Reason Capture**: Optionally record rejection reason for analytics
        - **Timestamp**: Record exact rejection time for audit trail
        - **Activity Log**: Log rejection event in workspace activity feed

        ### **Security Features:**
        - **Recipient Validation**: Only invitation recipient can reject
        - **ID Security**: Only accessible to invitation recipient
        - **Single Action**: Each invitation can only be processed once
        - **Audit Trail**: Complete rejection history maintained

        ### **Use Cases:**
        - **Privacy Protection**: Users declining unwanted workspace invitations
        - **Role Mismatch**: When offered role doesn't match user expectations
        - **Availability**: User not available to join workspace at this time
        - **External Users**: Partners/consultants declining internal workspace access
        - **Mistaken Invitations**: Correcting invitations sent to wrong recipients

        ### **Response Information:**
        - **Rejection Confirmation**: Confirmation that invitation was successfully rejected
        - **Invitation Details**: Basic information about what was rejected
        - **Status Update**: Current invitation status after rejection
        - **Workspace Info**: Limited workspace information for context
        """,
        tags = ["Invitation Rejection"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Invitation successfully rejected",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Invitation Rejection Success",
                        value = """
{
  "success": true,
  "data": {
    "id": "INV_003_REJECTED_TOKEN",
    "workspace_id": "WSP_MARKETING_001",
    "workspace_name": "Marketing Team Workspace",
    "email": "user@example.com",
    "role": "MEMBER",
    "status": "DECLINED",
    "message": "Welcome to our marketing team!",
    "token": "secure_invitation_token_here",
    "expires_at": "2025-01-20T10:00:00Z",
    "created_at": "2025-01-13T10:00:00Z",
    "rejected_at": "2025-01-16T14:30:00Z",
    "rejection_reason": "Not available to join at this time",
    "invited_by": "USER_123",
    "inviter_name": "John Manager"
  },
  "timestamp": "2025-01-16T14:30:00Z"
}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "❌ Bad request - Invalid invitation token format"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Must be logged in to reject invitation"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "🚫 Forbidden - You are not the intended recipient of this invitation"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "🔍 Invitation not found - Invalid or expired invitation token"
            ),
            SwaggerApiResponse(
                responseCode = "409",
                description = "⚠️ Conflict - Invitation already processed (accepted or rejected)"
            ),
            SwaggerApiResponse(
                responseCode = "410",
                description = "⏰ Gone - Invitation has expired and is no longer valid"
            )
        ]
    )
    @PostMapping("/{id}/reject")
    fun rejectInvitation(
        @Parameter(
            name = "id",
            description = """
            **Invitation ID**

            The unique identifier of the invitation obtained from the pending invitations list.
            This ID is used to reject a specific invitation that the user has received.

            **ID Format:** Alphanumeric string (UUID or similar)
            **Example:** 'INV_ABC123_XYZ789' or 'uuid-format-string'

            **Properties:**
            - **User-Specific**: Only the invitation recipient can use this ID
            - **Time-Limited**: Invitation expires after configured period (default: 7 days)
            - **Secure**: Validates recipient ownership before processing
            - **Single Action**: Can only be used once for any action (accept or reject)

            **Where to Find ID:**
            1. **Pending Invitations API**: Primary source via GET /user/v1/invitations/pending
            2. **User Dashboard**: Listed in user's invitation management interface
            3. **Mobile App**: Available in invitation list view

            **Security Requirements:**
            - User must be authenticated and match invitation recipient
            - ID must be valid and invitation must not be expired
            - Only invitation recipient can perform this action
            """,
            required = true,
            example = "INV_ABC123_XYZ789"
        )
        @PathVariable id: String,
        @Parameter(
            name = "reason",
            description = """
            **Optional Rejection Reason**

            Provide a reason for declining the workspace invitation. This information
            helps workspace administrators understand member preferences and improve
            their invitation process.

            **Common Reasons:**
            - "Not available at this time"
            - "Role doesn't match my expertise"
            - "Already part of similar workspace"
            - "Personal schedule conflicts"
            - "Received invitation by mistake"

            **Benefits of Providing Reason:**
            - Helps administrators improve invitation targeting
            - Provides valuable feedback for workspace management
            - Maintains professional communication
            - Assists with analytics and reporting
            """,
            required = false,
            example = "Not available to join at this time"
        )
        @RequestParam(required = false) reason: String?
    ): ApiResponse<InvitationResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val userId = AuthenticationHelper.getCurrentUserId(auth)
            ?: throw IllegalStateException("User not authenticated")

        val invitation = invitationService.declineInvitationById(id, userId, reason)
        return ApiResponse.success(invitation)
    }
}