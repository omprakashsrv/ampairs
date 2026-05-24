package com.ampairs.workspace.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.GenericSuccessResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.service.WorkspaceTeamService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(
    name = "Team Management",
    description = """
    ## 👥 Team Management APIs
    
    Manage teams, team members, and team-specific permissions within workspaces.
    Teams provide an additional layer of organization and access control beyond workspace-level roles.
    
    ### Key Features:
    - Create and manage teams with specific permissions
    - Organize teams by department
    - Assign team leads and members
    - Set team-specific access controls
    - Track team membership and activities
    """
)
@RestController
@RequestMapping("/workspace/v1/workspaces/{workspaceId}/teams")
@SecurityRequirement(name = "BearerAuth")
class WorkspaceTeamController(
    private val teamService: WorkspaceTeamService
) {

    @Operation(
        summary = "Create New Team",
        description = """
        ## 🏗️ **Create Workspace Team**
        
        Create a new team within the workspace for better organization and access control.
        Teams provide department-level organization with specialized permissions.
        
        ### **Team Creation Features:**
        - **Department Organization**: Group members by department or function
        - **Permission Sets**: Define team-specific access controls
        - **Member Capacity**: Set maximum team size limits
        - **Team Leadership**: Assign team leads and managers
        
        ### **Use Cases:**
        - **Department Teams**: Sales, Marketing, Engineering, Support
        - **Project Teams**: Cross-functional teams for specific projects
        - **Location Teams**: Teams organized by office location
        - **Skill Teams**: Specialized teams based on expertise
        """,
        tags = ["Team Management"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "201",
                description = "✅ Team successfully created",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Team Creation Success",
                        value = """{
  "success": true,
  "data": {
    "id": "TEAM_SALES_001",
    "name": "Sales Team",
    "description": "Customer acquisition and sales management team",
    "team_code": "SALES001",
    "department": "Sales",
    "team_lead": {
      "id": "MBR_JANE_DOE_123",
      "name": "Jane Doe",
      "email": "jane.doe@company.com",
      "role": "MANAGER"
    },
    "member_count": 5,
    "max_members": 15,
    "permissions": [
      "VIEW_CUSTOMERS",
      "MANAGE_ORDERS",
      "VIEW_REPORTS"
    ],
    "is_active": true,
    "created_at": "2025-01-15T10:30:00Z"
  },
  "timestamp": "2025-01-15T10:30:00Z"
}"""
                    )]
                )]
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "❌ Bad request - Invalid team data"
            ),
            SwaggerApiResponse(
                responseCode = "403",
                description = "🚫 Access denied - Missing MANAGE_TEAMS permission"
            )
        ]
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuthorizationService.hasPermission(#workspaceId, 'MANAGE_TEAMS')")
    fun createTeam(
        @PathVariable workspaceId: String,
        @Valid @RequestBody request: CreateTeamRequest
    ): ApiResponse<TeamResponse> {
        return ApiResponse.success(teamService.createTeam(workspaceId, request))
    }

    @Operation(
        summary = "List/Search Teams",
        description = """
        ## 🔍 **Search Workspace Teams**
        
        Retrieve and search teams within the workspace with comprehensive filtering and pagination.
        
        ### **Search Capabilities:**
        - **Text Search**: Find teams by name or description
        - **Department Filter**: Filter by specific departments
        - **Status Filter**: Show active or inactive teams
        - **Advanced Sorting**: Multiple sort options for better organization
        
        ### **Response Data:**
        - **Team Overview**: Name, description, department, and status
        - **Member Information**: Team lead, member count, and capacity
        - **Permission Summary**: Team-specific access controls
        - **Activity Metrics**: Creation date and last activity
        """,
        tags = ["Team Management"]
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "✅ Teams retrieved successfully",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ApiResponse::class),
                    examples = [ExampleObject(
                        name = "Teams List Response",
                        value = """{
  "success": true,
  "data": {
    "content": [
      {
        "id": "TEAM_SALES_001",
        "name": "Sales Team",
        "description": "Customer acquisition and sales management",
        "team_code": "SALES001",
        "department": "Sales",
        "member_count": 8,
        "max_members": 15,
        "team_lead": {
          "name": "Jane Doe",
          "email": "jane.doe@company.com"
        },
        "is_active": true,
        "created_at": "2025-01-10T09:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "total_elements": 5,
    "total_pages": 1
  },
  "timestamp": "2025-01-15T10:30:00Z"
}"""
                    )]
                )]
            )
        ]
    )
    @GetMapping
    @PreAuthorize("@workspaceAuthorizationService.hasPermission(#workspaceId, 'VIEW_TEAMS')")
    fun searchTeams(
        @PathVariable workspaceId: String,
        @Parameter(description = "Search text to filter teams by name or description")
        @RequestParam(required = false) query: String?,
        @Parameter(description = "Filter teams by department")
        @RequestParam(required = false) department: String?,
        @Parameter(description = "Filter by team status (active/inactive)")
        @RequestParam(required = false) status: Boolean?,
        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort field (name, created_at, etc)")
        @RequestParam(defaultValue = "name") sort: String,
        @Parameter(description = "Sort direction (asc/desc)")
        @RequestParam(defaultValue = "asc") direction: String
    ): ApiResponse<PageResponse<TeamListResponse>> {
        val pageable = PageRequest.of(
            page, size,
            Sort.by(Sort.Direction.fromString(direction), sort)
        )
        return ApiResponse.success(PageResponse.from(teamService.searchTeams(workspaceId, query, department, status, pageable)))
    }

    @Operation(
        summary = "Get Team Details",
        description = "Get detailed information about a specific team including its members and permissions"
    )
    @GetMapping("/{teamId}")
    @PreAuthorize("@workspaceAuthorizationService.hasPermission(#workspaceId, 'VIEW_TEAMS')")
    fun getTeam(
        @PathVariable workspaceId: String,
        @PathVariable teamId: String
    ): ApiResponse<TeamResponse> {
        return ApiResponse.success(teamService.getTeamById(workspaceId, teamId))
    }

    @Operation(
        summary = "Update Team",
        description = "Update team details, permissions, and settings"
    )
    @PutMapping("/{teamId}")
    @PreAuthorize("@workspaceAuthorizationService.hasPermission(#workspaceId, 'MANAGE_TEAMS')")
    fun updateTeam(
        @PathVariable workspaceId: String,
        @PathVariable teamId: String,
        @Valid @RequestBody request: UpdateTeamRequest
    ): ApiResponse<TeamResponse> {
        return ApiResponse.success(teamService.updateTeam(workspaceId, teamId, request))
    }

    @Operation(
        summary = "Delete Team",
        description = "Soft delete a team by deactivating it"
    )
    @DeleteMapping("/{teamId}")
    @PreAuthorize("@workspaceAuthorizationService.hasPermission(#workspaceId, 'MANAGE_TEAMS')")
    fun deleteTeam(
        @PathVariable workspaceId: String,
        @PathVariable teamId: String
    ): ApiResponse<GenericSuccessResponse> {
        teamService.deleteTeam(workspaceId, teamId)
        return ApiResponse.success(GenericSuccessResponse())
    }

    @Operation(
        summary = "Add Team Members",
        description = "Add multiple members to a team"
    )
    @PostMapping("/{teamId}/members")
    @PreAuthorize("@workspaceAuthorizationService.hasPermission(#workspaceId, 'MANAGE_TEAM_MEMBERS')")
    fun addTeamMembers(
        @PathVariable workspaceId: String,
        @PathVariable teamId: String,
        @Valid @RequestBody request: AddTeamMembersRequest
    ): ApiResponse<TeamResponse> {
        return ApiResponse.success(teamService.addMembers(workspaceId, teamId, request))
    }

    @Operation(
        summary = "Remove Team Members",
        description = "Remove multiple members from a team"
    )
    @PostMapping("/{teamId}/members/remove")
    @PreAuthorize("@workspaceAuthorizationService.hasPermission(#workspaceId, 'MANAGE_TEAM_MEMBERS')")
    fun removeTeamMembers(
        @PathVariable workspaceId: String,
        @PathVariable teamId: String,
        @Valid @RequestBody request: RemoveTeamMembersRequest
    ): ApiResponse<TeamResponse> {
        return ApiResponse.success(teamService.removeMembers(workspaceId, teamId, request))
    }

    @Operation(
        summary = "Update Team Member Settings",
        description = "Update settings for a specific member within a team"
    )
    @PutMapping("/{teamId}/members/{memberId}")
    @PreAuthorize("@workspaceAuthorizationService.hasPermission(#workspaceId, 'MANAGE_TEAM_MEMBERS')")
    fun updateTeamMember(
        @PathVariable workspaceId: String,
        @PathVariable teamId: String,
        @PathVariable memberId: String,
        @Valid @RequestBody request: UpdateTeamMemberRequest
    ): ApiResponse<TeamMemberSummary> {
        return ApiResponse.success(teamService.updateTeamMember(workspaceId, teamId, memberId, request))
    }
}
