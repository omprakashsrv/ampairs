# Tasks: Extract User Module from Auth

**Input**: Design documents from `/specs/007-extract-the-user/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓

**Tests**: Not requested — this is a structural refactor. Validation is done via build commands and existing tests.

**Organization**: Tasks grouped by phase; each phase maps to one or more spec user stories.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel with other [P] tasks in the same phase
- **[Story]**: US1–US4 from spec.md
- Exact file paths included in every task

---

## Phase 1: Setup — Module Scaffold

**Purpose**: Register the new `user` module in the build system before any source is moved.

**Maps to**: All user stories (prerequisite)

- [x] T001 [ALL] Add `include("user")` to `settings.gradle.kts` — insert after `include("auth")` to reflect dependency order
- [x] T002 [ALL] Create `user/build.gradle.kts` — copy the structure from `auth/build.gradle.kts` and reduce dependencies to: `api(project(":core"))`, `api(project(":file"))`, Spring Boot starters (web, data-jpa, security, validation, cache), AWS S3 starter. Disable `bootJar`, enable `jar`. Full content specified in plan.md Step 3.

**Checkpoint**: `./gradlew :user:build` should succeed (empty module, no sources yet)

---

## Phase 2: Foundational — Break the Bidirectional Dependency

**Purpose**: Move `UserDetailsWithId` / `UserDetailsWithRoles` to `core` so that `user` can implement them without depending on `auth`. This is the single blocking prerequisite for all user stories.

**⚠️ CRITICAL**: No source file migration can succeed until T003 + T004 are complete — `User.kt` imports these interfaces from `com.ampairs.auth.service`.

**Maps to**: US2 (auth regression), US3 (no circular deps)

- [x] T003 [US2,US3] Create `core/src/main/kotlin/com/ampairs/core/security/UserDetailsExtensions.kt` with the two interfaces extracted verbatim from `JwtService.kt:292-298`:
  ```kotlin
  package com.ampairs.core.security

  interface UserDetailsWithId {
      fun getId(): String
  }

  interface UserDetailsWithRoles {
      fun getRoles(): List<String>
  }
  ```
- [x] T004 [US2,US3] Update `auth/src/main/kotlin/com/ampairs/auth/service/JwtService.kt` — delete the `UserDetailsWithId` and `UserDetailsWithRoles` interface definitions at lines 292–298. Add import `import com.ampairs.core.security.UserDetailsWithId` and `import com.ampairs.core.security.UserDetailsWithRoles` at the top of the file. No other logic changes.

**Checkpoint**: `./gradlew :core:build` must pass. `./gradlew :auth:build` must fail with "unresolved reference: com.ampairs.user" (expected — user sources not moved yet).

---

## Phase 3: User Story 1 + 3 — Create `user` Module and Move Sources

**Goal (US1)**: Other modules can depend on `user` without pulling in `auth`.
**Goal (US3)**: Dependency graph is acyclic — `user` compiles independently of `auth`.

**Independent Test**: `./gradlew :user:build` succeeds in isolation; `com.ampairs.user.*` classes are on the `user` compile classpath, not `auth`.

- [x] T005 [US1,US3] Create source directory tree for the `user` module:
  ```
  user/src/main/kotlin/com/ampairs/user/controller/
  user/src/main/kotlin/com/ampairs/user/model/dto/
  user/src/main/kotlin/com/ampairs/user/repository/
  user/src/main/kotlin/com/ampairs/user/service/
  user/src/main/resources/db/migration/mysql/
  user/src/main/resources/db/migration/postgresql/
  ```

- [x] T006 [US1,US3] Move all Kotlin source files from `auth/src/main/kotlin/com/ampairs/user/` to `user/src/main/kotlin/com/ampairs/user/` (preserve all sub-paths). Files to move:
  - `controller/UserController.kt`
  - `model/User.kt`
  - `model/dto/UserResponse.kt`
  - `model/dto/UserUpdateRequest.kt`
  - `repository/UserRepository.kt`
  - `service/CachedUserDetailsService.kt`
  - `service/CoreUserServiceImpl.kt`
  - `service/ProfilePictureService.kt`
  - `service/UserService.kt`
  
  **Do not rename packages or change any logic.**

- [x] T007 [US1,US3] Update `user/src/main/kotlin/com/ampairs/user/model/User.kt` — change the two imports:
  ```kotlin
  // Remove:
  import com.ampairs.auth.service.UserDetailsWithId
  import com.ampairs.auth.service.UserDetailsWithRoles
  // Add:
  import com.ampairs.core.security.UserDetailsWithId
  import com.ampairs.core.security.UserDetailsWithRoles
  ```
  No other changes to `User.kt`.

- [x] T008 [P] [US1,US3] Move MySQL migration files — copy then delete from `auth/src/main/resources/db/migration/mysql/` to `user/src/main/resources/db/migration/mysql/`:
  - `V1.0.13__create_app_user_table.sql`
  - `V1.0.15__add_firebase_uid_to_app_user.sql`
  - `V1.0.16__add_user_deletion_fields.sql`
  - `V1.0.23__add_profile_picture_fields.sql`
  
  File contents must be identical (no edits).

- [x] T009 [P] [US1,US3] Move PostgreSQL migration files — copy then delete from `auth/src/main/resources/db/migration/postgresql/` to `user/src/main/resources/db/migration/postgresql/`:
  - `V1.0.13__create_app_user_table.sql`
  - `V1.0.15__add_firebase_uid_to_app_user.sql`
  - `V1.0.16__add_user_deletion_fields.sql`
  - `V1.0.23__add_profile_picture_fields.sql`
  
  File contents must be identical (no edits).

**Checkpoint**: `./gradlew :user:build` must pass. `./gradlew :auth:build` still fails (auth still has no `user` dependency declared).

---

## Phase 4: User Story 2 — Wire Auth → User

**Goal (US2)**: Auth module compiles and all its tests pass after declaring `user` as a dependency. Authentication flows (OTP, Firebase, JWT, device sessions) work without regression.

**Independent Test**: `./gradlew :auth:test` — all existing tests pass unmodified.

- [x] T010 [US2] Update `auth/build.gradle.kts` — in the `dependencies` block, replace:
  ```kotlin
  api(project(mapOf("path" to ":file")))
  ```
  with:
  ```kotlin
  api(project(mapOf("path" to ":user")))
  ```
  Keep `api(project(mapOf("path" to ":core")))` and `api(project(mapOf("path" to ":notification")))` unchanged. The `file` dependency now flows transitively through `user`.

- [x] T011 [US2] Verify `auth` compiles and tests pass:
  ```bash
  ./gradlew :auth:build
  ./gradlew :auth:test
  ```
  If any compilation error occurs, it indicates a missing import update — fix it before proceeding.

**Checkpoint**: `./gradlew :auth:test` green. `./gradlew :user:build` still green. Both modules buildable independently.

---

## Phase 5: User Story 4 — Wire `ampairs_service` → User

**Goal (US4)**: The runnable service includes the `user` module; all existing user-facing API endpoints respond identically. Migration tooling finds `app_user` migrations in the new location.

**Independent Test**: `./gradlew :ampairs_service:build` succeeds; `./gradlew :ampairs_service:flywayInfo` shows V1.0.13–V1.0.23 (user migrations) as Applied.

- [x] T012 [US4] Update `ampairs_service/build.gradle.kts`:
  1. Add to `dependencies`:
     ```kotlin
     implementation(project(mapOf("path" to ":user")))
     ```
  2. Add `"user"` to the `migrationModules` list (alphabetical order):
     ```kotlin
     val migrationModules = listOf(
         "auth", "business", "core", "customer", "event", "form",
         "invoice", "notification", "order", "product", "subscription",
         "tax", "unit", "user", "workspace"
     )
     ```

- [x] T013 [US4] Verify `ampairs_service` compiles:
  ```bash
  ./gradlew :ampairs_service:build
  ```

**Checkpoint**: Full service builds. All four user stories satisfied structurally.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, final validation, and CI gate.

- [x] T014 [P] [ALL] Create `user/CLAUDE.md` documenting the `user` bounded context. Follow the same format as `auth/CLAUDE.md`. Include: what the module owns (User entity, profile, soft-delete), key entities, controllers, services, migration versions, and bounded context summary.

- [x] T015 [P] [ALL] Run the full CI build to confirm zero regressions:
  ```bash
  ./gradlew ciBuild
  ```
  All tests must pass. No new warnings introduced.

- [x] T016 [ALL] Verify dependency graph is acyclic:
  ```bash
  ./gradlew :user:dependencies --configuration compileClasspath | grep "ampairs"
  ```
  Expected output: only `core` and `file` — no `auth`.

**Checkpoint**: All 16 tasks complete. Branch ready for PR.

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)
    └── Phase 2 (Foundational)           ← T003, T004 must complete first
            └── Phase 3 (US1+US3)        ← T005–T009
                    └── Phase 4 (US2)    ← T010, T011
                            └── Phase 5 (US4) ← T012, T013
                                    └── Phase 6 (Polish) ← T014, T015, T016
```

### Within-Phase Parallel Opportunities

| Phase | Parallel tasks |
|-------|----------------|
| Phase 1 | T001 and T002 [P] |
| Phase 2 | T003 and T004 are sequential (T004 modifies a file that uses T003's output) |
| Phase 3 | T005 first; then T006+T007 sequential; T008 [P] T009 after T005 |
| Phase 4 | T010 then T011 (sequential) |
| Phase 5 | T012 then T013 (sequential) |
| Phase 6 | T014 [P] T015; T016 after T015 |

---

## Parallel Execution Examples

```bash
# Phase 1 — both can run in parallel:
Task T001: "Add include('user') to settings.gradle.kts"
Task T002: "Create user/build.gradle.kts"

# Phase 3 — migrations can be moved in parallel after T005:
Task T008: "Move MySQL migration files to user module"
Task T009: "Move PostgreSQL migration files to user module"

# Phase 6 — documentation and CI can start together:
Task T014: "Create user/CLAUDE.md"
Task T015: "Run ./gradlew ciBuild"
```

---

## Implementation Strategy

### MVP First (Minimal Shippable Refactor)

1. Complete Phase 1: Setup (T001–T002)
2. Complete Phase 2: Foundational (T003–T004) — **blocks everything**
3. Complete Phase 3: Move sources (T005–T009)
4. Complete Phase 4: Wire auth (T010–T011)
5. **STOP and VALIDATE**: `./gradlew :auth:test` must be green
6. Complete Phase 5: Wire service (T012–T013)
7. Complete Phase 6: Polish and CI gate

### Commit Strategy

Commit after each phase completes to keep the history readable:
- `refactor: create user module scaffold and build config`
- `refactor: move UserDetailsWithId/UserDetailsWithRoles to core`
- `refactor: move com.ampairs.user.* sources and migrations to user module`
- `refactor: wire auth → user dependency`
- `refactor: wire ampairs_service → user, update flyway migration modules`
- `docs: add user/CLAUDE.md`

---

## Notes

- T008 and T009 are file moves, not copies — delete originals from `auth` after confirming user module has them
- `auth/build.gradle.kts` `testImplementation(project(":ampairs_service"))` stays unchanged — test classpath already includes `user` transitively
- No package renames anywhere — all moved files keep `com.ampairs.user.*` package declarations unchanged
- The `event` module imports `com.ampairs.auth.service.JwtService` — this is unaffected; JwtService stays in `auth`
- `ampairs_service/AccountDeletionService.kt` already imports from `com.ampairs.user.*` — after the move it will resolve from the `user` module automatically via the `implementation(project(":user"))` dependency in Phase 5
