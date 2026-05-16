# workspace module

Manages workspaces (tenants), membership, roles, teams, invitations, and module enablement. Also provides the `SessionUserFilter` that establishes tenant context on every request.

## Responsibilities

- Workspace creation and settings
- Member management (invite, role assignment, remove)
- Team management within a workspace
- Module enablement per workspace (feature flags)
- RBAC — role-based access control via `@WorkspacePermission`
- Workspace avatar / branding
- Activity logging
- `SessionUserFilter` — extracts `X-Workspace-ID`, validates JWT, sets `TenantContextHolder`

## REST Endpoints

### Workspace (`/workspace/v1`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/workspace/v1` | Create workspace |
| GET | `/workspace/v1` | List user's workspaces |
| GET | `/workspace/v1/{workspaceId}` | Get workspace details |
| PUT | `/workspace/v1/{workspaceId}` | Update workspace |
| DELETE | `/workspace/v1/{workspaceId}` | Delete workspace |
| GET | `/workspace/v1/check-slug/{slug}` | Check slug availability |
| GET | `/workspace/v1/search` | Search workspaces |
| GET | `/workspace/v1/{workspaceId}/settings` | Get settings |
| PUT | `/workspace/v1/{workspaceId}/settings` | Update settings |
| GET | `/workspace/v1/{workspaceId}/configuration` | Get full configuration |
| POST | `/workspace/v1/{workspaceId}/avatar` | Upload avatar |
| DELETE | `/workspace/v1/{workspaceId}/avatar` | Remove avatar |

### Members (`/workspace/v1/{workspaceId}/members`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/workspace/v1/{workspaceId}/members` | List members |
| POST | `/workspace/v1/{workspaceId}/members` | Add member |
| PUT | `/workspace/v1/{workspaceId}/members/{userId}` | Update member role |
| DELETE | `/workspace/v1/{workspaceId}/members/{userId}` | Remove member |

### Invitations

| Method | Path | Description |
|--------|------|-------------|
| POST | `/workspace/v1/{workspaceId}/invitations` | Send invitation |
| GET | `/workspace/v1/{workspaceId}/invitations` | List invitations |
| DELETE | `/workspace/v1/{workspaceId}/invitations/{invitationId}` | Cancel invitation |
| POST | `/user/v1/invitations/{token}/accept` | Accept invitation |
| POST | `/user/v1/invitations/{token}/reject` | Reject invitation |
| GET | `/user/v1/invitations/pending` | List pending invitations |

### Teams (`/workspace/v1/{workspaceId}/teams`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/workspace/v1/{workspaceId}/teams` | List teams |
| POST | `/workspace/v1/{workspaceId}/teams` | Create team |
| PUT | `/workspace/v1/{workspaceId}/teams/{teamId}` | Update team |
| DELETE | `/workspace/v1/{workspaceId}/teams/{teamId}` | Delete team |
| POST | `/workspace/v1/{workspaceId}/teams/{teamId}/members` | Add team member |
| DELETE | `/workspace/v1/{workspaceId}/teams/{teamId}/members/{userId}` | Remove team member |

### Modules (`/workspace/v1/{workspaceId}/modules`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/workspace/v1/modules` | List all available modules |
| GET | `/workspace/v1/{workspaceId}/modules` | Get enabled modules |
| POST | `/workspace/v1/{workspaceId}/modules/{moduleId}/enable` | Enable module |
| POST | `/workspace/v1/{workspaceId}/modules/{moduleId}/disable` | Disable module |

## Key Entities

### Workspace

```kotlin
class Workspace : BaseDomain() {
    val name: String
    val slug: String               // unique URL-friendly identifier
    val description: String?
    val status: WorkspaceStatus    // ACTIVE, SUSPENDED, DELETED
    val type: WorkspaceType        // PERSONAL, BUSINESS, ENTERPRISE
    val ownerId: String            // user who created it
    val avatarUrl: String?
    val avatarThumbnailUrl: String?
    val subscriptionPlan: SubscriptionPlan
    val createdAt: Instant
    val updatedAt: Instant
}
```

### WorkspaceMember

```kotlin
class WorkspaceMember : BaseDomain() {
    val workspaceId: String
    val userId: String
    val role: WorkspaceRole        // OWNER, ADMIN, MEMBER, VIEWER
    val active: Boolean
    val joinedAt: Instant
}
```

### WorkspaceInvitation

```kotlin
class WorkspaceInvitation : BaseDomain() {
    val workspaceId: String
    val invitedByUserId: String
    val inviteePhone: String
    val inviteeCountryCode: Int
    val role: WorkspaceRole
    val status: InvitationStatus   // PENDING, ACCEPTED, REJECTED, EXPIRED
    val token: String              // secure invitation token
    val expiresAt: Instant
}
```

### WorkspaceTeam

```kotlin
class WorkspaceTeam : OwnableBaseDomain() {
    val name: String
    val description: String?
    val memberIds: List<String>    // JSON list of user UIDs
    val active: Boolean
}
```

### WorkspaceModule

```kotlin
class WorkspaceModule : OwnableBaseDomain() {
    val masterModuleId: String
    val enabled: Boolean
    val enabledAt: Instant?
    val settings: Map<String, Any> // JSON module-specific settings
}
```

## Roles

| Role | Description |
|------|-------------|
| `OWNER` | Full control, cannot be removed |
| `ADMIN` | Manage members, settings, and all data |
| `MEMBER` | Access to enabled modules |
| `VIEWER` | Read-only access |

## SessionUserFilter

The most critical piece of the workspace module. Runs on every authenticated request:

1. Extracts JWT from `Authorization: Bearer` header
2. Validates JWT signature and expiry
3. Reads `X-Workspace-ID` header
4. Verifies user is a member of that workspace
5. Sets `TenantContextHolder.setCurrentTenant(workspaceId)`
6. Sets `DeviceContextHolder.setCurrentDevice(deviceId)`

Requests without `X-Workspace-ID` on workspace-scoped endpoints are rejected with `400`.

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.5__create_workspace_module_tables.sql` | workspace, member, invitation, team, module tables |
| `V1.0.24__add_workspace_avatar_fields.sql` | Avatar URL columns |
| `V1.0.26__add_subscription_fields.sql` | Subscription plan column |

## Package Structure

```
com.ampairs.workspace
├── config/         — Constants, UserDetailConfiguration, WorkspaceSecurityConfig
├── controller/     — WorkspaceController, WorkspaceMemberController,
│                     WorkspaceInvitationController, UserInvitationController,
│                     WorkspaceTeamController, WorkspaceModuleController
├── exception/      — WorkspaceExceptionHandler
├── filter/         — SessionUserFilter (tenant context setup)
├── model/          — Workspace, WorkspaceMember, WorkspaceInvitation,
│                     WorkspaceTeam, WorkspaceModule, WorkspaceSettings,
│                     WorkspaceActivity, MasterModule, DTOs, enums
├── repository/     — WorkspaceRepository, WorkspaceMemberRepository,
│                     WorkspaceInvitationRepository, WorkspaceTeamRepository,
│                     WorkspaceModuleRepository, WorkspaceSettingsRepository,
│                     WorkspaceActivityRepository, MasterModuleRepository
├── security/       — WorkspaceAuthorizationService, WorkspacePermission
├── service/        — WorkspaceService, WorkspaceMemberService,
│                     WorkspaceInvitationService, WorkspaceTeamService,
│                     WorkspaceModuleService, WorkspaceSettingsService,
│                     WorkspaceAvatarService, WorkspaceActivityService,
│                     WorkspaceNotificationService, MasterModuleSeederService
└── validation/     — ValidContactMethod
```
