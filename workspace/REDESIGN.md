# Workspace Module Redesign

## Overview

Redesigning the workspace module to implement a modern SaaS workspace concept - a logical, collaborative environment
where defined groups of users can access shared resources, data, tools, and configurations.

## Key Principles

### 1. Data Isolation

- Complete separation between workspaces
- No cross-workspace data visibility unless explicitly shared
- Row-level security implementation
- Tenant-scoped queries and operations

### 2. Collaboration Hub

- Shared resources and tools within workspace
- Team-based project management
- Collaborative access to datasets and documents
- Real-time collaboration features

### 3. Role-Based Access Control

- Granular permission system
- Hierarchical role structure
- Custom role definitions
- Permission inheritance

### 4. Customization Scope

- Workspace-level settings and preferences
- Custom integrations per workspace
- Branding and appearance customization
- API keys and external service configuration

### 5. Scalability

- Multi-workspace user membership
- Cross-project collaboration
- Client/team segregation
- Resource sharing controls

## Proposed Architecture

### Core Entities

#### 1. Workspace

```kotlin
@Entity(name = "workspaces")
class Workspace : OwnableBaseDomain() {
    @Column(name = "name", nullable = false)
    var name: String = ""
    
    @Column(name = "slug", unique = true, nullable = false)
    var slug: String = ""
    
    @Column(name = "description")
    var description: String? = null
    
    @Column(name = "workspace_type")
    @Enumerated(EnumType.STRING)
    var workspaceType: WorkspaceType = WorkspaceType.TEAM
    
    @Column(name = "avatar_url")
    var avatarUrl: String? = null
    
    @Column(name = "settings", columnDefinition = "TEXT")
    var settings: String = "{}" // JSON settings
    
    @Column(name = "is_active")
    var isActive: Boolean = true
    
    @Column(name = "subscription_plan")
    @Enumerated(EnumType.STRING)
    var subscriptionPlan: SubscriptionPlan = SubscriptionPlan.FREE
    
    @Column(name = "max_members")
    var maxMembers: Int = 10
    
    @Column(name = "storage_limit_gb")
    var storageLimitGb: Int = 5
}
```

#### 2. WorkspaceMember

```kotlin
@Entity(name = "workspace_members")
class WorkspaceMember : BaseDomain() {
    @Column(name = "workspace_id", nullable = false)
    var workspaceId: String = ""
    
    @Column(name = "user_id", nullable = false) 
    var userId: String = ""
    
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    var role: WorkspaceRole = WorkspaceRole.MEMBER
    
    @Column(name = "permissions", columnDefinition = "TEXT")
    var permissions: String = "[]" // JSON array of permissions
    
    @Column(name = "invited_by")
    var invitedBy: String? = null
    
    @Column(name = "invited_at")
    var invitedAt: LocalDateTime? = null
    
    @Column(name = "joined_at")
    var joinedAt: LocalDateTime? = null
    
    @Column(name = "is_active")
    var isActive: Boolean = true
    
    @Column(name = "last_active_at")
    var lastActiveAt: LocalDateTime? = null
}
```

#### 3. WorkspaceInvitation

```kotlin
@Entity(name = "workspace_invitations")
class WorkspaceInvitation : BaseDomain() {
    @Column(name = "workspace_id", nullable = false)
    var workspaceId: String = ""

    @Column(name = "email", nullable = false)
    var email: String = ""

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    var role: WorkspaceRole = WorkspaceRole.MEMBER

    @Column(name = "invited_by", nullable = false)
    var invitedBy: String = ""

    @Column(name = "invitation_token", unique = true)
    var invitationToken: String = ""

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: InvitationStatus = InvitationStatus.PENDING

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime = LocalDateTime.now().plusDays(7)

    @Column(name = "accepted_at")
    var acceptedAt: LocalDateTime? = null
}
```

#### 4. WorkspaceSettings

```kotlin
@Entity(name = "workspace_settings")
class WorkspaceSettings : BaseDomain() {
    @Column(name = "workspace_id", nullable = false, unique = true)
    var workspaceId: String = ""

    @Column(name = "branding", columnDefinition = "TEXT")
    var branding: String = "{}" // Logo, colors, theme

    @Column(name = "notifications", columnDefinition = "TEXT")
    var notifications: String = "{}" // Notification preferences

    @Column(name = "integrations", columnDefinition = "TEXT")
    var integrations: String = "{}" // Third-party integrations

    @Column(name = "security", columnDefinition = "TEXT")
    var security: String = "{}" // Security policies

    @Column(name = "features", columnDefinition = "TEXT")
    var features: String = "{}" // Enabled features
}
```

### Enums

#### WorkspaceType

```kotlin
enum class WorkspaceType {
    PERSONAL,    // Individual workspace
    TEAM,        // Team collaboration
    ORGANIZATION,// Large organization
    CLIENT,      // Client-specific workspace
    PROJECT      // Project-specific workspace
}
```

#### WorkspaceRole

```kotlin
enum class WorkspaceRole {
    OWNER,       // Full workspace control
    ADMIN,       // Administrative access
    MANAGER,     // Management permissions
    MEMBER,      // Standard member access
    GUEST,       // Limited guest access
    VIEWER       // Read-only access
}
```

#### Permission

```kotlin
enum class Permission {
    // Workspace management
    WORKSPACE_MANAGE,
    WORKSPACE_SETTINGS,
    WORKSPACE_DELETE,

    // Member management
    MEMBER_INVITE,
    MEMBER_REMOVE,
    MEMBER_ROLE_MANAGE,

    // Data permissions
    DATA_READ,
    DATA_CREATE,
    DATA_UPDATE,
    DATA_DELETE,

    // Project permissions
    PROJECT_CREATE,
    PROJECT_MANAGE,
    PROJECT_DELETE,

    // Integration permissions
    INTEGRATION_MANAGE,
    API_KEY_MANAGE,

    // Reporting permissions
    REPORTS_VIEW,
    REPORTS_EXPORT,
    ANALYTICS_VIEW
}
```

#### InvitationStatus

```kotlin
enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    CANCELLED
}
```

#### SubscriptionPlan

```kotlin
enum class SubscriptionPlan {
    FREE,
    BASIC,
    PROFESSIONAL,
    ENTERPRISE
}
```

## Services Architecture

### WorkspaceService

```kotlin
@Service
class WorkspaceService {

    // Workspace CRUD
    fun createWorkspace(request: CreateWorkspaceRequest): WorkspaceResponse
    fun getWorkspace(workspaceId: String): WorkspaceResponse
    fun updateWorkspace(workspaceId: String, request: UpdateWorkspaceRequest): WorkspaceResponse
    fun deleteWorkspace(workspaceId: String)
    fun getUserWorkspaces(userId: String): List<WorkspaceResponse>

    // Member management
    fun inviteUser(workspaceId: String, request: InviteUserRequest): InvitationResponse
    fun acceptInvitation(token: String): WorkspaceMemberResponse
    fun declineInvitation(token: String)
    fun removeUser(workspaceId: String, userId: String)
    fun updateUserRole(workspaceId: String, userId: String, role: WorkspaceRole)
    fun getWorkspaceMembers(workspaceId: String): List<WorkspaceMemberResponse>

    // Permission management
    fun hasPermission(userId: String, workspaceId: String, permission: Permission): Boolean
    fun getUserPermissions(userId: String, workspaceId: String): Set<Permission>
    fun grantPermissions(workspaceId: String, userId: String, permissions: Set<Permission>)
    fun revokePermissions(workspaceId: String, userId: String, permissions: Set<Permission>)

    // Settings management
    fun getWorkspaceSettings(workspaceId: String): WorkspaceSettingsResponse
    fun updateWorkspaceSettings(workspaceId: String, request: UpdateSettingsRequest): WorkspaceSettingsResponse
}
```

### WorkspaceSecurityService

```kotlin
@Service
class WorkspaceSecurityService {

    fun validateWorkspaceAccess(userId: String, workspaceId: String): Boolean
    fun validatePermission(userId: String, workspaceId: String, permission: Permission): Boolean
    fun enforceWorkspaceIsolation(workspaceId: String, query: String): String
    fun getWorkspaceDataFilter(workspaceId: String): Specification<*>
}
```

## API Endpoints

### Workspace Management

```
GET    /workspace/v1/workspaces                    # Get user's workspaces
POST   /workspace/v1/workspaces                    # Create workspace
GET    /workspace/v1/workspaces/{id}               # Get workspace details
PUT    /workspace/v1/workspaces/{id}               # Update workspace
DELETE /workspace/v1/workspaces/{id}               # Delete workspace
```

### Member Management

```
GET    /workspace/v1/workspaces/{id}/members       # Get workspace members
POST   /workspace/v1/workspaces/{id}/invitations   # Invite user
GET    /workspace/v1/workspaces/{id}/invitations   # Get pending invitations
POST   /workspace/v1/invitations/{token}/accept    # Accept invitation
POST   /workspace/v1/invitations/{token}/decline   # Decline invitation
DELETE /workspace/v1/workspaces/{id}/members/{userId} # Remove member
PUT    /workspace/v1/workspaces/{id}/members/{userId}/role # Update member role
```

### Settings Management

```
GET    /workspace/v1/workspaces/{id}/settings      # Get workspace settings
PUT    /workspace/v1/workspaces/{id}/settings      # Update workspace settings
GET    /workspace/v1/workspaces/{id}/permissions   # Get permission matrix
```

## Implementation Benefits

1. **True Data Isolation**: Each workspace maintains complete data separation
2. **Scalable Collaboration**: Support for multiple workspace types and use cases
3. **Flexible Permissions**: Granular role-based access control
4. **Customizable Experience**: Per-workspace settings and branding
5. **Multi-tenancy Ready**: Built for SaaS scalability
6. **Integration Friendly**: Support for workspace-scoped integrations

## Migration Strategy

1. **Phase 1**: Create new entities alongside existing ones
2. **Phase 2**: Migrate existing company data to workspace model
3. **Phase 3**: Update all dependent modules to use new workspace APIs
4. **Phase 4**: Remove legacy company-based entities
5. **Phase 5**: Implement advanced features (settings, integrations, etc.)

This redesign transforms the workspace module from a simple company management system into a comprehensive SaaS
workspace platform that supports modern collaboration patterns.