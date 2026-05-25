# Research: Extract User Module from Auth

## Decision 1: Where to put `UserDetailsWithId` / `UserDetailsWithRoles`

**Decision**: Move to `core/src/main/kotlin/com/ampairs/core/security/`

**Rationale**: These are simple marker interfaces with no auth-specific logic (`getId()`, `getRoles()`). They exist only so that `JwtService` can inspect extra claims from a `UserDetails` instance. `core` already owns shared utilities and base domains. Placing them there breaks the bidirectional cycle (`user` → `auth` → `user`) cleanly: `user` depends on `core`, `auth` depends on `user` + `core`, and neither creates a cycle.

**Alternatives considered**:
- Keep in `auth`, make `user` depend on `auth` for these two interfaces → rejected: preserves the cycle
- Duplicate the interfaces in `user` → rejected: two definitions of the same contract will diverge
- New `security-contracts` module → rejected: over-engineering for two one-method interfaces

---

## Decision 2: Which migration files move to the `user` module

**Decision**: Move only `app_user`-related migrations; leave auth-session migrations in `auth`.

| File | Moves to |
|------|----------|
| `V1.0.1__create_auth_module_tables.sql` | Stays in `auth` (login_session, token, device_session) |
| `V1.0.13__create_app_user_table.sql` | Moves to `user` |
| `V1.0.15__add_firebase_uid_to_app_user.sql` | Moves to `user` |
| `V1.0.16__add_user_deletion_fields.sql` | Moves to `user` |
| `V1.0.23__add_profile_picture_fields.sql` | Moves to `user` |
| `V1.0.54__fix_device_session_timestamp_types.sql` (pg only) | Stays in `auth` |
| `V1.0.55__fix_login_session_timestamp_types.sql` (pg only) | Stays in `auth` |

Both mysql/ and postgresql/ variants follow the same split. Flyway tracks migrations by version+checksum, not location path, so moving files does not invalidate already-applied migrations in existing environments.

**Rationale**: `app_user` is owned by the `user` bounded context. Auth-session tables (`login_session`, `token`, `device_session`) are owned by `auth`. Split follows bounded context ownership.

---

## Decision 3: Dependencies for the new `user` build.gradle.kts

**Decision**: `user` depends on `:core` and `:file`.

- `:core` — `BaseDomain`, `ApiResponse`, `CoreUser` interface
- `:file` — `ProfilePictureService` calls `ObjectStorageService`, `ImageResizingService`, `StorageProperties` from the `file` module

Spring Boot starters needed: `web`, `data-jpa`, `security`, `validation`, `cache`.

**Rationale**: `ProfilePictureService` directly imports from `com.ampairs.file.*`. Moving it to `user` requires `file` on the classpath. The current `auth/build.gradle.kts` already has `api(project(":file"))`, so this dependency is pre-validated.

---

## Decision 4: `auth/build.gradle.kts` — keep `file` or drop it?

**Decision**: Remove `api(project(":file"))` from `auth` and add `api(project(":user"))`. The `file` dependency flows transitively through `user` → `file`. Auth itself does not directly reference any `com.ampairs.file.*` classes.

**Rationale**: Removing direct `file` from `auth` reflects the true dependency graph. `user` now owns the `ProfilePictureService` and declares `file` directly.

---

## Decision 5: `ampairs_service` migration modules list

**Decision**: Add `"user"` to the `migrationModules` list in `ampairs_service/build.gradle.kts` at the same time as the module is added to `settings.gradle.kts`.

**Rationale**: Flyway's `locations` array is derived from `migrationModules`. Without adding `user`, the `app_user` migrations would not be found after they move out of `auth`.

---

## Decision 6: `ampairs_service` — no duplicate `user` dependency

**Decision**: `ampairs_service` already transitively receives `user` classes via `auth` (since `auth` will declare `api(project(":user"))`). However, it also directly references `com.ampairs.user.*` classes in `AccountDeletionService` and `CustomJwtAuthenticationConverter`. Add an explicit `implementation(project(":user"))` for clarity and to avoid transitive leakage fragility.

**Rationale**: Explicit is better than relying on transitive `api()` chains for classes referenced directly.
