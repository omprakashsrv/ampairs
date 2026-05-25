# Feature Specification: Extract User Module from Auth

**Feature Branch**: `007-extract-the-user`
**Created**: 2026-05-25
**Status**: Draft
**Input**: Extract the `user` bounded context out of the `auth` Gradle module into its own first-class `user` module.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Other modules reference User identity without depending on auth (Priority: P1)

A developer working on `workspace`, `order`, or `invoice` needs to reference a `User` (e.g., assign ownership, look up a member) without adding the `auth` module as a dependency and pulling in JWT, OTP, and session logic.

**Why this priority**: This is the primary motivation for the split. Until this works, the module boundary violation remains, and every module that touches users carries unrelated auth dependencies.

**Independent Test**: Add `user` as a dependency in the `workspace` module build file. Confirm it compiles and can reference `User` entity and `UserService` without `auth` appearing on the classpath.

**Acceptance Scenarios**:

1. **Given** the `workspace` module, **When** its build file declares a dependency on `user`, **Then** it can resolve the `User` entity, `UserService`, and `UserRepository` without any `auth` classes on the compile classpath.
2. **Given** any new module being created, **When** it needs to look up or reference a user, **Then** it only needs `user` as a dependency, not `auth`.

---

### User Story 2 — Auth module continues to work without regression (Priority: P1)

The authentication flows (OTP, Firebase, JWT, device sessions, refresh tokens) continue to work exactly as before. Auth now depends on `user` rather than owning it.

**Why this priority**: This refactor must be zero-regression. Any break in auth flows is a critical production incident.

**Independent Test**: Run the full `auth` integration test suite (`AuthIntegrationTest`, `JwtAuthenticationTest`, `JwtRS256IntegrationTest`). All tests must pass.

**Acceptance Scenarios**:

1. **Given** the refactored codebase, **When** the auth integration tests run, **Then** all existing tests pass with no modifications to test logic.
2. **Given** a POST to `/api/auth/v1/init` followed by `/api/auth/v1/verify-otp`, **When** valid OTP and device ID are submitted, **Then** the system returns JWT and refresh tokens as before.
3. **Given** a Firebase auth request, **When** a valid Firebase ID token is submitted, **Then** the system creates or retrieves the user and returns tokens as before.

---

### User Story 3 — No circular dependencies between modules (Priority: P2)

The dependency graph is acyclic: `user` depends on `core`, `auth` depends on `user` and `core`. No module depends on `auth` to get user data.

**Why this priority**: Circular dependencies prevent independent compilation and test isolation. This criterion validates that the structural goals of the refactor are met.

**Independent Test**: Build the `user` module in isolation (`./gradlew :user:build`). It must compile and pass tests without `auth` on its classpath.

**Acceptance Scenarios**:

1. **Given** the `user` module, **When** built independently, **Then** it compiles successfully with only `core` (and standard framework) dependencies.
2. **Given** the full project build, **When** the dependency graph is inspected, **Then** there is no cycle involving `auth`, `user`, or `core`.
3. **Given** the `core` module, **When** built independently, **Then** it has no dependency on either `auth` or `user`.

---

### User Story 4 — User-facing APIs are unchanged (Priority: P1)

All existing user-facing HTTP endpoints (`/api/user/v1/...`, `/api/auth/v1/...`) continue to respond with the same request/response shapes, same URL paths, and same HTTP status codes.

**Why this priority**: API clients (web, mobile) must not require any changes as a result of this refactor.

**Independent Test**: Replay a set of representative API calls against the refactored service. Response shapes and status codes must be identical.

**Acceptance Scenarios**:

1. **Given** the refactored service, **When** a client calls `GET /api/user/v1/profile`, **Then** the response shape is identical to the pre-refactor response.
2. **Given** the refactored service, **When** a client calls `PATCH /api/user/v1/profile`, **Then** the request is accepted and processed identically to before.
3. **Given** any auth or user endpoint, **When** called with valid credentials, **Then** HTTP status codes, response structures, and error messages are unchanged.

---

### Edge Cases

- What happens when a module that previously imported `auth` to get `User` is not updated to use `user` instead? The build must fail with a clear compilation error, not a runtime surprise.
- How does the system handle the `UserDetails` Spring Security contract after the split? The `User` entity must still satisfy Spring Security's `UserDetails` interface; the implementing interfaces (`UserDetailsWithId`, `UserDetailsWithRoles`) must be reachable from `core` so that neither `auth` nor `user` creates a cycle.
- What if a migration is needed for the new module? Since this is a code-only structural change (no schema changes), no new Flyway migration should be required. The `user` module reuses the existing `app_user` table; migration files move from `auth` to `user` resources.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The `user` Gradle module MUST contain all classes under `com.ampairs.user.*` (entity, repository, service, controller, DTOs).
- **FR-002**: The `auth` Gradle module MUST depend on the `user` module; it MUST NOT contain any `com.ampairs.user.*` source files.
- **FR-003**: The `UserDetailsWithId` and `UserDetailsWithRoles` interfaces MUST reside in the `core` module so that `user` can implement them without depending on `auth`.
- **FR-004**: The `user` module MUST NOT have a compile-time dependency on the `auth` module.
- **FR-005**: The `user` module MUST be listed in the root `settings.gradle.kts` so it participates in the unified build.
- **FR-006**: The `ampairs_service` aggregator MUST include `user` so it is packaged in the runnable service artifact.
- **FR-007**: Flyway migration files for the `app_user` table MUST be moved to the `user` module resources; no migration file content may be modified.
- **FR-008**: All existing unit and integration tests for `com.ampairs.user.*` and `com.ampairs.auth.*` MUST continue to pass without changes to test assertions.
- **FR-009**: The `user` module MUST have its own `CLAUDE.md` documenting its bounded context.

### Key Entities

- **User**: The platform identity record (phone, email, name, Firebase UID, profile picture, soft-delete state). After the split, owned exclusively by the `user` module.
- **UserDetailsWithId / UserDetailsWithRoles**: Spring Security extension interfaces that `User` implements. After the split, these live in `core` and carry no auth-specific logic.
- **Module dependency graph**: `core` ← `user` ← `auth`. No other direction is permitted.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The `user` module builds and its tests pass in complete isolation from the `auth` module.
- **SC-002**: The `auth` module builds and all its integration tests pass after declaring `user` as a dependency.
- **SC-003**: Zero compilation errors or test failures in any other module (`workspace`, `order`, `invoice`, etc.) after the refactor.
- **SC-004**: No changes to any HTTP API contract — endpoint paths, request shapes, response shapes, and HTTP status codes are identical to pre-refactor behavior.
- **SC-005**: The full project CI build (`./gradlew ciBuild`) passes without any new errors or warnings introduced by this change.
- **SC-006**: No new Flyway migrations are required — the structural change produces zero schema delta.

## Assumptions

- Flyway migration files will be moved from `auth/src/main/resources/db/migration/mysql/` to `user/src/main/resources/db/migration/mysql/` as the `app_user` table is now owned by `user`. This is a file move only — migration content is unchanged.
- The `workspace` and other dependent modules currently do not have a direct compile dependency on `auth` for user access (they go through service interfaces). If any do, those dependencies will be updated to point to `user` instead.
- The `CachedUserDetailsService` (Spring Security `UserDetailsService` implementation) moves to `user` because it loads `User` entities; `auth` wires it via the Spring context, not a direct package import.
- The existing `CoreUserServiceImpl` already uses the `CoreUser` interface from `core` — this pattern is preserved in the new structure.
