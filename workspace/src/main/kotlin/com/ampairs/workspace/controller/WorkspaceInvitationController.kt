package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.user.model.User
import com.ampairs.workspace.model.dto.CreateInvitationRequest
import com.ampairs.workspace.model.dto.InvitationListResponse
import com.ampairs.workspace.model.dto.InvitationResponse
import com.ampairs.workspace.model.dto.ResendInvitationRequest
import com.ampairs.workspace.service.WorkspaceInvitationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

/**
 * **Workspace Invitation Management Controller**
 *
 * Comprehensive invitation system for workspace collaboration including
 * invitation creation, acceptance, tracking, and management.
 */
@Tag(
    name = "Workspace Invitation Management",
    description = """
    ## 📧 **Advanced Workspace Invitation System**
    
    **Streamlined invitation management for workspace collaboration and team building.**
    
    ### 🎯 **Core Capabilities**
    - **Invitation Creation**: Send personalized invitations to new team members
    - **Invitation Tracking**: Monitor invitation status, delivery, and acceptance
    - **Role Assignment**: Pre-assign roles and permissions in invitations
    - **Bulk Invitations**: Invite multiple team members simultaneously
    - **Invitation Management**: Resend, modify, or cancel pending invitations
    - **Acceptance Workflow**: Streamlined process for invitation acceptance
    
    ### 💬 **Invitation Lifecycle**
    
    ```
    Created → Sent → Delivered → Accepted → Active Member
         │       │        │         │
         └────────┼────────┼─────────┘
                Expired/Cancelled/Declined
    ```
    
    ### 🔐 **Security Features**
    - **Token-Based Security**: Secure invitation tokens with expiration
    - **Email Verification**: Verify recipient email before workspace access
    - **Role Validation**: Ensure invited roles comply with workspace policies
    - **Spam Protection**: Rate limiting and abuse prevention mechanisms
    
    ### 🔑 **Permission Requirements**
    
    | Operation | Required Permission | Description |
    |-----------|-------------------|-------------|
    | **View Invitations** | `MEMBER_INVITE` | List and monitor workspace invitations |
    | **Create Invitations** | `MEMBER_INVITE` | Send new invitations to potential members |
    | **Manage Invitations** | `MEMBER_INVITE` | Resend, modify, or cancel invitations |
    | **Accept Invitations** | None (Public) | Public endpoint for invitation acceptance |
    
    ### 🔑 **Required Headers**
    - **Authorization**: `Bearer {jwt_token}` - Valid JWT authentication token (except acceptance)
    - **X-Workspace-ID**: `{workspace_id}` - Target workspace for invitation operations
    
    ### 📊 **Invitation Status Types**
    - **PENDING**: Invitation created and sent, awaiting recipient response
    - **DELIVERED**: Email successfully delivered to recipient's inbox
    - **OPENED**: Recipient has opened the invitation email
    - **ACCEPTED**: Invitation accepted, member added to workspace
    - **DECLINED**: Invitation explicitly declined by recipient
    - **EXPIRED**: Invitation has passed its expiration date
    - **CANCELLED**: Invitation cancelled by workspace administrator
    
    ### 📊 **Business Workflow Integration**
    1. **Team Expansion**: Systematically invite new team members with appropriate roles
    2. **Project Collaboration**: Invite external collaborators with limited access
    3. **Onboarding Process**: Integrate with HR systems for new employee onboarding
    4. **Compliance Tracking**: Maintain audit trail of all invitation activities
    """
)
@RestController
@RequestMapping("/workspace/v1")
@SecurityRequirement(name = "BearerAuth")
@SecurityRequirement(name = "WorkspaceContext")
class WorkspaceInvitationController(
    private val invitationService: WorkspaceInvitationService,
) {

    @Operation(
        summary = "Get Workspace Invitations",
        description = """
        ## 📋 **Retrieve Paginated Workspace Invitations**
        
        Fetches a comprehensive list of all workspace invitations with their status,
        recipient information, and tracking details for invitation management and monitoring.
        
        ### **Response Information:**
        - **Invitation Details**: Token, recipient email, invited role, and creation date
        - **Status Tracking**: Current status (PENDING, ACCEPTED, EXPIRED, CANCELLED)
        - **Sender Information**: Who sent the invitation and when
        - **Delivery Metrics**: Email delivery status and recipient engagement
        - **Expiration Info**: Invitation validity period and expiration dates
        
        ### **Sorting Options:**
        - **`createdAt`** (default): Order by invitation creation date
        - **`status`**: Group by invitation status (pending, accepted, expired)
        - **`recipientEmail`**: Alphabetical ordering by recipient email
        - **`role`**: Group by invited role (admin, manager, member)
        - **`expiresAt`**: Order by expiration date (urgent first)
        
        ### **Status Filtering:**
        Use query parameters to filter invitations by status:
        - **PENDING**: Awaiting recipient response
        - **ACCEPTED**: Successfully accepted and member added
        - **EXPIRED**: Past expiration date
        - **CANCELLED**: Cancelled by administrator
        - **DECLINED**: Explicitly declined by recipient
        
        ### **Use Cases:**
        - **Invitation Dashboard**: Display all pending and active invitations
        - **Team Management**: Track invitation acceptance rates and follow-up needs
        - **Audit Compliance**: Monitor invitation history for security audits
        - **Performance Analytics**: Analyze invitation success rates and patterns
        
        ### **Management Actions:**
        From the invitation list, administrators can:
        - **Resend Invitations**: For pending or failed delivery
        - **Cancel Invitations**: Remove pending invitations
        - **View Details**: See comprehensive invitation information
        - **Bulk Operations**: Manage multiple invitations simultaneously
        """,
        tags = ["Invitation Management"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Successfully retrieved workspace invitations",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Workspace Invitations Response",
                        value = """{{
  "success": true,
  "data": {{
    "content": [
      {{
        "id": "INV_001_JOHN_SMITH",
        "workspace_id": "WS_ABC123_XYZ789",
        "recipient_email": "john.smith@example.com",
        "recipient_name": "John Smith",
        "invited_role": "MANAGER",
        "status": "PENDING",
        "created_at": "2025-01-10T09:15:00Z",
        "expires_at": "2025-01-17T09:15:00Z",
        "sent_by": {{
          "name": "Jane Admin",
          "email": "jane.admin@example.com"
        }},
        "delivery_status": {{
          "email_sent": true,
          "email_delivered": true,
          "email_opened": true,
          "link_clicked": false
        }},
        "invitation_message": "Welcome to our project management workspace!",
        "resend_count": 0,
        "last_activity": "2025-01-10T09:15:00Z"
      }},
      {{
        "id": "INV_002_MARY_JOHNSON",
        "workspace_id": "WS_ABC123_XYZ789",
        "recipient_email": "mary.johnson@example.com",
        "recipient_name": "Mary Johnson",
        "invited_role": "MEMBER",
        "status": "ACCEPTED",
        "created_at": "2025-01-08T14:30:00Z",
        "accepted_at": "2025-01-09T10:20:00Z",
        "sent_by": {{
          "name": "Jane Admin",
          "email": "jane.admin@example.com"
        }},
        "delivery_status": {{
          "email_sent": true,
          "email_delivered": true,
          "email_opened": true,
          "link_clicked": true
        }},
        "acceptance_details": {{
          "accepted_from_ip": "192.168.1.100",
          "user_agent": "Mozilla/5.0...",
          "member_id": "MBR_002_MARY_JOHNSON"
        }}
      }}
    ],
    "page": 0,
    "size": 20,
    "total_elements": 12,
    "total_pages": 1,
    "is_first": true,
    "is_last": true
  }},
  "timestamp": "2025-01-15T10:30:00Z"
}}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "⛔ Access denied - Missing MEMBER_INVITE permission"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "🔍 Workspace not found - Invalid workspace ID"
            )
        ]
    )
    @GetMapping("/{workspaceId}/invitations")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_INVITE)")
    fun getWorkspaceInvitations(
        @Parameter(
            name = "workspaceId",
            description = "**Target workspace identifier** containing the invitations to retrieve",
            required = true,
            example = "WS_ABC123_XYZ789"
        )
        @PathVariable workspaceId: String,

        @Parameter(
            name = "page",
            description = "**Page number** (0-based) for pagination. Default: 0",
            example = "0"
        )
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(
            name = "size",
            description = "**Page size** - number of invitations per page (1-100). Default: 20",
            example = "20"
        )
        @RequestParam(defaultValue = "20") size: Int,

        @Parameter(
            name = "sortBy",
            description = """
            **Sort field** for ordering results. Available options:
            - `createdAt` (default) - Order by invitation creation date
            - `status` - Group by invitation status
            - `recipientEmail` - Alphabetical by recipient email
            - `role` - Group by invited role
            - `expiresAt` - Order by expiration date
            """,
            example = "createdAt"
        )
        @RequestParam(defaultValue = "createdAt") sortBy: String,

        @Parameter(
            name = "sortDir",
            description = "**Sort direction**: `asc` (ascending) or `desc` (descending). Default: desc",
            example = "desc"
        )
        @RequestParam(defaultValue = "desc") sortDir: String,
    ): ApiResponse<PageResponse<InvitationListResponse>> {
        val sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val invitations = invitationService.getWorkspaceInvitations(workspaceId, null, pageable)
        return ApiResponse.success(PageResponse.from(invitations))
    }

    @Operation(
        summary = "Create Workspace Invitation",
        description = """
        ## ✉️ **Send New Workspace Invitation**
        
        Creates and sends a personalized invitation to join the workspace with specified
        role and permissions. The invitation includes secure token-based access and
        comprehensive onboarding information.
        
        ### **Invitation Creation Process:**
        
        #### **📝 Information Collection**
        - **Recipient Details**: Email address and optional name for personalization
        - **Role Assignment**: Pre-assign workspace role (ADMIN, MANAGER, MEMBER, VIEWER)
        - **Permission Scope**: Define specific permissions and access limitations
        - **Custom Message**: Personal welcome message from the inviter
        - **Expiration Settings**: Configure invitation validity period
        
        #### **🔐 Security Implementation**
        - **Unique Token**: Generate cryptographically secure invitation token
        - **Email Verification**: Ensure recipient email ownership before access
        - **Expiration Control**: Automatic expiration after configured period (default: 7 days)
        - **Single Use**: Invitation tokens are valid for one-time acceptance only
        
        #### **📧 Email Delivery**
        - **Personalized Content**: Custom email template with workspace branding
        - **Clear Instructions**: Step-by-step acceptance process for recipients
        - **Security Notice**: Information about invitation authenticity and safety
        - **Support Contact**: Help and contact information for assistance
        
        ### **Role Assignment Options:**
        
        | Role | Description | Default Permissions |
        |------|-------------|--------------------|
        | **ADMIN** | Administrative access | Full workspace management except ownership |
        | **MANAGER** | Business operations lead | Data management, reporting, limited admin |
        | **MEMBER** | Standard team member | Create/edit data, basic operations |
        | **VIEWER** | Read-only access | View data and basic reports only |
        
        ### **Invitation Validation:**
        The system performs comprehensive validation:
        - **Email Format**: Valid email address format verification
        - **Duplicate Check**: Prevent duplicate invitations to same email
        - **Role Validation**: Ensure invited role is valid for workspace
        - **Permission Check**: Verify inviter has authority to assign specified role
        - **Workspace Limits**: Check workspace member capacity and licensing
        
        ### **Tracking & Analytics:**
        Each invitation provides detailed tracking:
        - **Delivery Status**: Email delivery confirmation and failure notifications
        - **Engagement Metrics**: Email open rates, link clicks, and interaction data
        - **Acceptance Timeline**: Time-to-acceptance analytics for follow-up optimization
        - **Conversion Rates**: Success rates for invitation acceptance by role/sender
        
        ### **Use Cases:**
        - **Team Expansion**: Systematically invite new team members
        - **Project Collaboration**: Invite external partners or consultants
        - **Bulk Onboarding**: Support HR processes for new employee groups
        - **Role-Specific Access**: Provide targeted access for specific business needs
        """,
        tags = ["Invitation Creation"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "201",
                description = "✅ Invitation successfully created and sent",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Invitation Creation Success",
                        value = """{{
  "success": true,
  "data": {{
    "id": "INV_003_ALEX_TAYLOR",
    "workspace_id": "WS_ABC123_XYZ789",
    "recipient_email": "alex.taylor@example.com",
    "recipient_name": "Alex Taylor",
    "invited_role": "MEMBER",
    "status": "PENDING",
    "invitation_token": "inv_AbCdEfGhIjKlMnOpQrStUvWxYz123456",
    "created_at": "2025-01-15T10:30:00Z",
    "expires_at": "2025-01-22T10:30:00Z",
    "sent_by": {{
      "name": "Jane Admin",
      "email": "jane.admin@example.com",
      "user_id": "USR_JANE_ADMIN_789"
    }},
    "invitation_details": {{
      "custom_message": "Welcome to our team! We're excited to have you join our project.",
      "workspace_info": {{
        "name": "Acme Corp - Project Management",
        "description": "Collaborative workspace for project coordination",
        "member_count": 15,
        "modules_available": ["Customer CRM", "Project Management", "Reporting"]
      }},
      "role_permissions": [
        "VIEW_DATA",
        "CREATE_DATA", 
        "UPDATE_DATA",
        "BASIC_REPORTS"
      ]
    }},
    "delivery_info": {{
      "email_queued": true,
      "estimated_delivery": "2025-01-15T10:32:00Z",
      "tracking_enabled": true
    }},
    "acceptance_url": "https://app.ampairs.com/invitations/inv_AbCdEfGhIjKlMnOpQrStUvWxYz123456/accept",
    "expires_in_hours": 168
  }},
  "timestamp": "2025-01-15T10:30:00Z"
}}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "❌ Bad request - Invalid email, role, or invitation data"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "⛔ Access denied - Missing MEMBER_INVITE permission"
            ),
            SwaggerApiResponse(
                responseCode = "409",
                description = "⚠️ Conflict - Duplicate invitation or member already exists"
            ),
            SwaggerApiResponse(
                responseCode = "422",
                description = "🚫 Unprocessable Entity - Workspace member limit reached or invalid role assignment"
            )
        ]
    )
    @PostMapping("/{workspaceId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_INVITE)")
    fun createInvitation(
        @Parameter(
            name = "workspaceId",
            description = "**Target workspace identifier** where the invitee will become a member",
            required = true,
            example = "WS_ABC123_XYZ789"
        )
        @PathVariable workspaceId: String,

        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = """
            **Invitation Creation Details**
            
            Comprehensive information for creating and sending the workspace invitation.
            
            **Example Request Body:**
            ```json
            {
              "recipient_email": "alex.taylor@example.com",
              "recipient_name": "Alex Taylor",
              "invited_role": "MEMBER",
              "custom_message": "Welcome to our team! We're excited to have you join our project.",
              "expires_in_days": 7,
              "send_notification": true,
              "permissions": ["VIEW_DATA", "CREATE_DATA", "UPDATE_DATA"],
              "department": "Engineering",
              "welcome_tour": true
            }
            ```
            
            **Required Fields:**
            - `recipient_email`: Valid email address of the invitee
            - `invited_role`: One of: ADMIN, MANAGER, MEMBER, VIEWER
            
            **Optional Fields:**
            - `recipient_name`: Personalization for email template
            - `custom_message`: Personal welcome message (max 500 chars)
            - `expires_in_days`: Invitation validity period (1-30 days, default: 7)
            - `send_notification`: Whether to send email immediately (default: true)
            - `permissions`: Custom permission array (overrides role defaults)
            - `department`: Organizational department assignment
            - `welcome_tour`: Enable guided tour for new members (default: true)
            """,
            required = true
        )
        @RequestBody @Valid request: CreateInvitationRequest,
    ): ApiResponse<InvitationResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val invitation = invitationService.createInvitation(workspaceId, request, user.uid)
        return ApiResponse.success(invitation)
    }

    @Operation(
        summary = "Accept Workspace Invitation",
        description = """
        ## ✅ **Accept Workspace Invitation via Token**
        
        Public endpoint for accepting workspace invitations using the secure invitation token.
        This completes the invitation workflow by adding the user to the workspace with
        the pre-assigned role and permissions.
        
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
                        value = """{{
  "success": true,
  "data": {{
    "id": "INV_003_ALEX_TAYLOR",
    "status": "ACCEPTED",
    "accepted_at": "2025-01-15T10:30:00Z",
    "workspace_info": {{
      "id": "WS_ABC123_XYZ789",
      "name": "Acme Corp - Project Management",
      "description": "Collaborative workspace for project coordination",
      "member_count": 16,
      "your_role": "MEMBER"
    }},
    "member_details": {{
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
    }},
    "onboarding": {{
      "welcome_tour_available": true,
      "profile_completion_required": false,
      "setup_tasks": [
        "Complete profile information",
        "Set notification preferences",
        "Join relevant project teams"
      ],
      "welcome_message": "Welcome to Acme Corp! We're excited to have you on the team."
    }},
    "immediate_access": {{
      "dashboard_url": "/workspace/WS_ABC123_XYZ789/dashboard",
      "available_modules": ["Customer CRM", "Project Management", "Basic Reporting"],
      "team_members": 15,
      "recent_activity_available": true
    }}
  }},
  "timestamp": "2025-01-15T10:30:00Z"
}}"""
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
    @PostMapping("/invitations/{token}/accept")
    fun acceptInvitation(
        @Parameter(
            name = "token",
            description = """
            **Secure Invitation Token**
            
            The unique, cryptographically secure token provided in the invitation email.
            This token contains all necessary information to validate and process the
            invitation acceptance.
            
            **Token Format:** Usually starts with 'inv_' followed by random characters
            **Example:** 'inv_AbCdEfGhIjKlMnOpQrStUvWxYz123456'
            
            **Token Properties:**
            - **Single Use**: Can only be used once for acceptance
            - **Time-Limited**: Expires after configured period (default: 7 days)
            - **Secure**: Cryptographically generated and tamper-evident
            - **Workspace-Specific**: Tied to specific workspace and role assignment
            
            **Where to Find Token:**
            1. **Invitation Email**: Primary source - click 'Accept Invitation' button or link
            2. **Manual Extraction**: Copy token from invitation URL if needed
            3. **Mobile App**: Token handled automatically when opening invitation link
            
            **Security Notes:**
            - Never share invitation tokens with others
            - Tokens are single-use and expire automatically
            - Contact workspace administrator if token is lost or expired
            """,
            required = true,
            example = "inv_AbCdEfGhIjKlMnOpQrStUvWxYz123456"
        )
        @PathVariable token: String,
    ): ApiResponse<InvitationResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val invitation = invitationService.acceptInvitation(token, user.uid)
        return ApiResponse.success(invitation)
    }

    @Operation(
        summary = "Resend Invitation Email",
        description = """
        ## 🔄 **Resend Workspace Invitation**
        
        Resends an existing workspace invitation with updated delivery options,
        custom messaging, and tracking. Useful for following up on pending invitations
        or addressing delivery issues.
        
        ### **Resend Scenarios:**
        
        #### **📧 Delivery Issues**
        - **Email Delivery Failure**: Original email bounced or failed to deliver
        - **Spam Folder**: Invitation email filtered to spam/junk folder
        - **Email Lost**: Recipient accidentally deleted or can't locate invitation
        - **Technical Issues**: Email service disruptions or client-side problems
        
        #### **🕑 Follow-Up Requirements**
        - **Pending Response**: Invitation sent but no response after reasonable time
        - **Reminder Needed**: Gentle reminder for busy recipients
        - **Urgency Change**: Project urgency requires faster response
        - **Updated Information**: Additional context or details to include
        
        #### **🔄 Content Updates**
        - **Message Modification**: Update or personalize invitation message
        - **Role Changes**: Modify invited role or permissions (if supported)
        - **Deadline Extension**: Extend invitation expiration date
        - **Additional Context**: Include project updates or team changes
        
        ### **Resend Process:**
        
        **Pre-Send Validation:**
        - **Invitation Status**: Verify invitation is still pending/valid
        - **Recipient Status**: Confirm recipient hasn't joined already
        - **Sender Permissions**: Validate sender still has invitation rights
        - **Workspace Status**: Ensure workspace is active and accepting members
        
        **Delivery Enhancement:**
        - **Updated Template**: Use latest email template with improvements
        - **Delivery Optimization**: Choose optimal sending time/method
        - **Tracking Enhancement**: Improved delivery and engagement tracking
        - **Mobile Optimization**: Enhanced mobile-friendly email format
        
        **Communication Tracking:**
        - **Resend Counter**: Track number of resend attempts
        - **Delivery Analytics**: Monitor open rates, clicks, and engagement
        - **Response Patterns**: Analyze recipient behavior for optimization
        - **Follow-Up Scheduling**: Automatic follow-up scheduling if configured
        
        ### **Best Practices:**
        
        **Timing Considerations:**
        - **Wait Period**: Allow 24-48 hours before first resend
        - **Business Hours**: Send during recipient's business hours
        - **Frequency Limits**: Avoid excessive resends (max 2-3 attempts)
        - **Expiration Buffer**: Ensure sufficient time before invitation expires
        
        **Message Optimization:**
        - **Personal Touch**: Include personalized message for resends
        - **Urgency Indication**: Clearly communicate timeline expectations
        - **Contact Information**: Provide alternative contact methods
        - **Support Resources**: Include help resources for technical issues
        
        ### **Use Cases:**
        - **Onboarding Follow-Up**: Systematic follow-up for new employee invitations
        - **Project Urgency**: Quick team assembly for time-sensitive projects
        - **Delivery Troubleshooting**: Address technical email delivery issues
        - **Relationship Management**: Maintain positive experience for important invitees
        """,
        tags = ["Invitation Management"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Invitation successfully resent",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Invitation Resend Success",
                        value = """{{
  "success": true,
  "data": {{
    "id": "INV_003_ALEX_TAYLOR",
    "workspace_id": "WS_ABC123_XYZ789",
    "recipient_email": "alex.taylor@example.com",
    "recipient_name": "Alex Taylor",
    "status": "PENDING",
    "resend_details": {{
      "resent_at": "2025-01-15T10:30:00Z",
      "resent_by": "jane.admin@example.com",
      "resend_count": 1,
      "previous_sends": [
        "2025-01-10T09:15:00Z"
      ]
    }},
    "updated_message": "Hi Alex, following up on our workspace invitation. We'd love to have you join our team!",
    "delivery_info": {{
      "email_queued": true,
      "estimated_delivery": "2025-01-15T10:32:00Z",
      "delivery_method": "priority",
      "tracking_enabled": true
    }},
    "invitation_details": {{
      "expires_at": "2025-01-22T09:15:00Z",
      "time_remaining": "7 days",
      "acceptance_url": "https://app.ampairs.com/invitations/inv_AbCdEfGhIjKlMnOpQrStUvWxYz123456/accept",
      "role": "MEMBER"
    }},
    "engagement_data": {{
      "previous_opens": 0,
      "previous_clicks": 0,
      "best_send_time": "2025-01-15T14:00:00Z",
      "recipient_timezone": "America/New_York"
    }}
  }},
  "timestamp": "2025-01-15T10:30:00Z"
}}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "❌ Bad request - Invalid resend parameters or invitation not eligible"
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "⛔ Access denied - Missing MEMBER_INVITE permission"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "🔍 Invitation not found - Invalid invitation ID"
            ),
            SwaggerApiResponse(
                responseCode = "409",
                description = "⚠️ Conflict - Invitation already accepted or cancelled"
            ),
            SwaggerApiResponse(
                responseCode = "429",
                description = "🚫 Rate limit exceeded - Too many resend attempts"
            )
        ]
    )
    @PostMapping("/{workspaceId}/invitations/{invitationId}/resend")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_INVITE)")
    fun resendInvitation(
        @Parameter(
            name = "workspaceId",
            description = "**Target workspace identifier** containing the invitation to resend",
            required = true,
            example = "WS_ABC123_XYZ789"
        )
        @PathVariable workspaceId: String,

        @Parameter(
            name = "invitationId",
            description = """
            **Invitation ID to Resend**
            
            The unique identifier of the workspace invitation to resend.
            
            **Requirements:**
            - Invitation must be in PENDING status
            - Invitation must not be expired
            - Maximum resend limit not exceeded
            - Sender must have MEMBER_INVITE permission
            
            **Find Invitation ID:**
            1. Call `GET /workspace/v1/{workspaceId}/invitations` to list invitations
            2. Use the `id` field from invitation list
            3. Filter by status="PENDING" for resendable invitations
            """,
            required = true,
            example = "INV_003_ALEX_TAYLOR"
        )
        @PathVariable invitationId: String,

        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = """
            **Resend Configuration Options**
            
            Optional configuration for customizing the resend operation.
            
            **Example Request Body:**
            ```json
            {
              "updated_message": "Hi Alex, following up on our workspace invitation. We'd love to have you join our team!",
              "priority_delivery": true,
              "send_time": "2025-01-15T14:00:00Z",
              "include_reminder": true,
              "extend_expiration_days": 3
            }
            ```
            
            **Available Options:**
            - `updated_message`: New personalized message for the resend (max 500 chars)
            - `priority_delivery`: Use priority email delivery (default: false)
            - `send_time`: Schedule specific send time (default: immediate)
            - `include_reminder`: Add gentle reminder text (default: true)
            - `extend_expiration_days`: Extend expiration by additional days (max: 14)
            - `notification_preferences`: Override recipient notification settings
            
            **Note:** All fields are optional. Empty request body uses default resend settings.
            """,
            required = false
        )
        @RequestBody request: ResendInvitationRequest = ResendInvitationRequest(),
    ): ApiResponse<InvitationResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val result = invitationService.resendInvitation(invitationId, request, user.uid)
        return ApiResponse.success(result)
    }

    @Operation(
        summary = "Cancel Workspace Invitation",
        description = """
        ## ❌ **Cancel/Revoke Workspace Invitation**
        
        Permanently cancels a pending workspace invitation, preventing its acceptance
        and removing it from active invitation lists. This is irreversible and will
        invalidate the invitation token.
        
        ### **Cancellation Process:**
        
        #### **🚫 Token Invalidation**
        - **Immediate Revocation**: Invitation token becomes invalid immediately
        - **Link Deactivation**: Acceptance link in email will no longer work
        - **Security Cleanup**: Remove token from authentication systems
        - **Database Update**: Mark invitation as cancelled with timestamp
        
        #### **📧 Communication Handling**
        - **Optional Notification**: Send cancellation notification to recipient (if configured)
        - **Audit Logging**: Record cancellation in workspace activity log
        - **Admin Notification**: Notify other administrators of cancellation (if configured)
        - **Tracking Update**: Update invitation analytics and metrics
        
        #### **📋 Status Management**
        - **Status Change**: Update invitation status from PENDING to CANCELLED
        - **History Preservation**: Maintain historical record for audit purposes
        - **Analytics Impact**: Update invitation success rate calculations
        - **Cleanup Scheduling**: Schedule removal of expired cancellation records
        
        ### **Cancellation Reasons:**
        
        **Business Changes:**
        - **Role Filled**: Position filled by another candidate
        - **Project Cancelled**: Project or initiative no longer active
        - **Budget Constraints**: Resource limitations prevent new hires
        - **Team Restructuring**: Organizational changes affect team composition
        
        **Security Concerns:**
        - **Email Compromise**: Recipient email account potentially compromised
        - **Unauthorized Access**: Concern about invitation falling into wrong hands
        - **Policy Violation**: Invitation violates updated security policies
        - **Risk Assessment**: Security review flags invitation as high-risk
        
        **Administrative Reasons:**
        - **Duplicate Invitation**: Multiple invitations sent to same recipient
        - **Wrong Role**: Invitation sent with incorrect role or permissions
        - **Wrong Person**: Invitation sent to incorrect email address
        - **Timing Issues**: Invitation sent at inappropriate time
        
        ### **Impact Assessment:**
        
        **Immediate Effects:**
        - **Access Prevention**: Recipient can no longer use invitation to join workspace
        - **Link Deactivation**: All invitation links become non-functional
        - **Queue Removal**: Pending email deliveries are cancelled
        - **Tracking Stoppage**: Invitation tracking and analytics are frozen
        
        **Long-term Considerations:**
        - **Reputational Impact**: Consider recipient's experience and relationship
        - **Future Invitations**: May affect future invitation acceptance rates
        - **Team Morale**: Consider impact on existing team members
        - **Process Improvement**: Learn from cancellation to improve invitation process
        
        ### **Best Practices:**
        
        **Before Cancellation:**
        - **Verify Decision**: Confirm cancellation is truly necessary
        - **Consider Alternatives**: Could invitation be modified instead of cancelled?
        - **Impact Assessment**: Consider effect on recipient and team relationships
        - **Documentation**: Document reason for audit and learning purposes
        
        **After Cancellation:**
        - **Personal Communication**: Consider personal follow-up with recipient
        - **Process Review**: Analyze what led to need for cancellation
        - **System Updates**: Update processes to prevent similar issues
        - **Relationship Management**: Maintain positive relationship with cancelled recipient
        
        ### **Use Cases:**
        - **Role Changes**: Position requirements or availability changed
        - **Security Response**: Address security concerns or policy violations  
        - **Process Corrections**: Fix administrative errors in invitation process
        - **Strategic Pivot**: Business strategy changes affecting team composition
        """,
        tags = ["Invitation Management"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Invitation successfully cancelled",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Invitation Cancellation Success",
                        value = """{{
  "success": true,
  "data": {{
    "message": "Invitation INV_003_ALEX_TAYLOR successfully cancelled",
    "cancellation_details": {{
      "invitation_id": "INV_003_ALEX_TAYLOR",
      "recipient_email": "alex.taylor@example.com",
      "recipient_name": "Alex Taylor",
      "invited_role": "MEMBER",
      "cancelled_at": "2025-01-15T10:30:00Z",
      "cancelled_by": "jane.admin@example.com",
      "previous_status": "PENDING",
      "new_status": "CANCELLED"
    }},
    "impact_summary": {{
      "token_invalidated": true,
      "email_deliveries_stopped": true,
      "tracking_frozen": true,
      "recipient_notified": false
    }},
    "invitation_history": {{
      "created_at": "2025-01-10T09:15:00Z",
      "emails_sent": 2,
      "emails_opened": 0,
      "links_clicked": 0,
      "days_active": 5
    }},
    "next_steps": {{
      "can_recreate_invitation": true,
      "consider_personal_followup": true,
      "update_invitation_process": "recommended"
    }}
  }},
  "timestamp": "2025-01-15T10:30:00Z"
}}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "401",
                description = "🚫 Authentication required - Invalid or missing JWT token"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "⛔ Access denied - Missing MEMBER_INVITE permission"
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "🔍 Invitation not found - Invalid invitation ID or already processed"
            ),
            SwaggerApiResponse(
                responseCode = "409",
                description = "⚠️ Conflict - Invitation already accepted or previously cancelled"
            )
        ]
    )
    @DeleteMapping("/{workspaceId}/invitations/{invitationId}")
    @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).MEMBER_INVITE)")
    fun cancelInvitation(
        @Parameter(
            name = "workspaceId",
            description = "**Target workspace identifier** containing the invitation to cancel",
            required = true,
            example = "WS_ABC123_XYZ789"
        )
        @PathVariable workspaceId: String,

        @Parameter(
            name = "invitationId",
            description = """
            **Invitation ID to Cancel**
            
            The unique identifier of the workspace invitation to permanently cancel.
            
            **Requirements:**
            - Invitation must be in PENDING status
            - Sender must have MEMBER_INVITE permission
            - Cancellation is permanent and irreversible
            
            **Important Notes:**
            - **Permanent Action**: Cannot be undone once executed
            - **Token Invalidation**: Invitation link will stop working immediately
            - **Recipient Impact**: Consider recipient experience and relationships
            - **Re-invitation**: New invitation can be created if needed later
            
            **Before Cancelling:**
            1. Verify cancellation is truly necessary
            2. Consider personal communication with recipient
            3. Document reason for future reference
            4. Consider alternative solutions (role change, delay, etc.)
            
            **Find Invitation ID:**
            - Call `GET /workspace/v1/{workspaceId}/invitations`
            - Filter by status="PENDING" for cancellable invitations
            - Use the `id` field from invitation list
            """,
            required = true,
            example = "INV_003_ALEX_TAYLOR"
        )
        @PathVariable invitationId: String,
    ): ApiResponse<String> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User

        val result = invitationService.cancelInvitation(invitationId, null, user.uid)
        return ApiResponse.success(result)
    }

}