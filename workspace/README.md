# Workspace Module

## Overview
The workspace module defines tenant boundaries for the Ampairs platform. It provisions workspaces, keeps member and team rosters in sync with permissions, drives invitation flows, and tracks which functional modules are enabled per tenant. Every controller enforces the multi-tenant context supplied by the `X-Workspace-ID` header and publishes audit activity that downstream services can consume.

## Architecture
### Package Structure
```
com.ampairs.workspace/
├── config/       # Bean configuration (security adapters, constants, user details)
├── controller/   # REST endpoints for workspaces, members, teams, modules, invitations
├── exception/    # Module-specific exceptions and handlers
├── filter/       # Request filters (session enrichment)
├── model/        # Entities, DTOs, and enums
├── repository/   # Spring Data repositories
├── security/     # Permission checks and authorization helpers
├── service/      # Workspace, member, team, module, settings, activity services
└── validation/   # Custom validators and request guards
```

## Core Controllers
- **`WorkspaceController`** – Create, update, list, and search workspaces; check slugs; expose subscription metadata.
- **`WorkspaceMemberController`** – Paginate members, inspect role/permission assignments, update or remove members, and surface the caller’s role.
- **`WorkspaceInvitationController`** – Create/resend/cancel invitations, accept or decline tokens, and return invitation lists with status filters.
- **`UserInvitationController`** – Public acceptance endpoints used by invite links (no workspace header required).
- **`WorkspaceTeamController`** – CRUD for teams, member assignments, and team-specific permissions inside a workspace.
- **`WorkspaceModuleController`** – Toggle module availability, introspect module catalog entries, and manage module lifecycle metadata.

## Feature Highlights
- Tenant-scoped repositories and services built on `TenantContextHolder`.
- Role and permission checks powered by `WorkspacePermission` plus `@PreAuthorize` guards.
- Invitation lifecycle with optional email/SMS notifications and expiry windows.
- Team organisation layered on top of workspace roles for departmental/group access control.
- Activity logging hooks (`WorkspaceActivityService`) for monitoring invitations, membership changes, and module operations.
- Module catalog seeding (`MasterModuleSeederService`) and per-workspace module enablement tracking.

## API Highlights
| Endpoint | Purpose |
|----------|---------|
| `POST /workspace/v1` | Provision a workspace and assign the authenticated user as owner. |
| `GET /workspace/v1? page&size` | List workspaces for the caller with pagination and sorting. |
| `GET /workspace/v1/member?page=&size=` | Inspect members within the active workspace (requires `MEMBER_VIEW`). |
| `POST /workspace/v1/invitation` | Send a workspace invitation; can request notification delivery. |
| `POST /workspace/v1/workspaces/{id}/teams` | Create a team within a workspace and define team permissions. |
| `PUT /workspace/v1/modules/{moduleId}` | Enable/disable a module or update module configuration for the tenant. |

All responses are wrapped in `ApiResponse<T>` (with `PageResponse<T>` on paginated endpoints).

## Integration Points
- **Core** – Supplies tenant context utilities, API envelopes, security helpers, and activity logging support.
- **Auth** – Provides the authenticated principal; role resolution reuses JWT claims through `AuthenticationHelper`.
- **Notification** – Invitation workflows can trigger notification delivery when `sendNotification` is true.
- **Event** – Member, invitation, and module operations publish activity logs consumed by the event module.

## Build & Test
```bash
# From ampairs-backend/
./gradlew :workspace:build
./gradlew :workspace:test
```

Execute `./gradlew :ampairs_service:bootRun` to run the module within the assembled application.
