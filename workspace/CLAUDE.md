# workspace module

Multi-tenant workspaces, RBAC, members, teams, invitations, module enablement.

## Critical: SessionUserFilter
Runs on every authenticated request. Reads `X-Workspace-ID` header → validates JWT → sets `TenantContextHolder`. Requests without this header on workspace-scoped endpoints are rejected.

## Roles
`OWNER` > `ADMIN` > `MEMBER` > `VIEWER`

## Key entities
- `Workspace` — name, slug, status, subscriptionPlan, avatarUrl
- `WorkspaceMember` — userId, role, active, joinedAt
- `WorkspaceInvitation` — inviteePhone, role, token, status, expiresAt
- `WorkspaceTeam` — name, memberIds (JSON)
- `WorkspaceModule` — moduleCode, enabled, settings (JSON)

## Controllers
`WorkspaceController`, `WorkspaceMemberController`, `WorkspaceInvitationController`, `WorkspaceTeamController`, `WorkspaceModuleController`, `UserInvitationController`

## Base paths
`/workspace/v1/**`

## Migrations
`V1.0.5`, `V1.0.24` (avatars), `V1.0.26` (subscription fields)

## Full docs
`docs/modules/workspace.md`
