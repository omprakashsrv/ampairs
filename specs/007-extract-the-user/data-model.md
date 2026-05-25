# Data Model: Extract User Module from Auth

No schema changes. This is a structural code refactor only.

## Module Dependency Graph (after refactor)

```
core
 └── user          (depends on: core, file)
      └── auth     (depends on: user, core, notification, file transitively)
```

Other modules that may declare `user` as a direct dependency:
- `ampairs_service` (already references com.ampairs.user.* directly)
- Any future module needing user identity (workspace, order, invoice can add user without auth)

## Source File Moves

### From `auth/src/main/kotlin/com/ampairs/user/` → `user/src/main/kotlin/com/ampairs/user/`

| File | Package unchanged |
|------|-------------------|
| `model/User.kt` | `com.ampairs.user.model` |
| `model/dto/UserResponse.kt` | `com.ampairs.user.model.dto` |
| `model/dto/UserUpdateRequest.kt` | `com.ampairs.user.model.dto` |
| `repository/UserRepository.kt` | `com.ampairs.user.repository` |
| `service/UserService.kt` | `com.ampairs.user.service` |
| `service/CachedUserDetailsService.kt` | `com.ampairs.user.service` |
| `service/CoreUserServiceImpl.kt` | `com.ampairs.user.service` |
| `service/ProfilePictureService.kt` | `com.ampairs.user.service` |
| `controller/UserController.kt` | `com.ampairs.user.controller` |

### From `auth/src/main/kotlin/com/ampairs/auth/service/JwtService.kt` (extract) → `core/src/main/kotlin/com/ampairs/core/security/UserDetailsExtensions.kt`

| Interface | New location |
|-----------|-------------|
| `UserDetailsWithId` | `com.ampairs.core.security.UserDetailsWithId` |
| `UserDetailsWithRoles` | `com.ampairs.core.security.UserDetailsWithRoles` |

## Migration File Moves

### MySQL

| From | To |
|------|----|
| `auth/src/main/resources/db/migration/mysql/V1.0.13__create_app_user_table.sql` | `user/src/main/resources/db/migration/mysql/V1.0.13__create_app_user_table.sql` |
| `auth/src/main/resources/db/migration/mysql/V1.0.15__add_firebase_uid_to_app_user.sql` | `user/src/main/resources/db/migration/mysql/V1.0.15__add_firebase_uid_to_app_user.sql` |
| `auth/src/main/resources/db/migration/mysql/V1.0.16__add_user_deletion_fields.sql` | `user/src/main/resources/db/migration/mysql/V1.0.16__add_user_deletion_fields.sql` |
| `auth/src/main/resources/db/migration/mysql/V1.0.23__add_profile_picture_fields.sql` | `user/src/main/resources/db/migration/mysql/V1.0.23__add_profile_picture_fields.sql` |

### PostgreSQL

| From | To |
|------|----|
| `auth/src/main/resources/db/migration/postgresql/V1.0.13__create_app_user_table.sql` | `user/src/main/resources/db/migration/postgresql/V1.0.13__create_app_user_table.sql` |
| `auth/src/main/resources/db/migration/postgresql/V1.0.15__add_firebase_uid_to_app_user.sql` | `user/src/main/resources/db/migration/postgresql/V1.0.15__add_firebase_uid_to_app_user.sql` |
| `auth/src/main/resources/db/migration/postgresql/V1.0.16__add_user_deletion_fields.sql` | `user/src/main/resources/db/migration/postgresql/V1.0.16__add_user_deletion_fields.sql` |
| `auth/src/main/resources/db/migration/postgresql/V1.0.23__add_profile_picture_fields.sql` | `user/src/main/resources/db/migration/postgresql/V1.0.23__add_profile_picture_fields.sql` |

## Files That Stay in `auth` (unchanged)

- All `com.ampairs.auth.*` sources (controller, service, model, repository, config, exception, interceptor, utils)
- `V1.0.1__create_auth_module_tables.sql` (both dialects)
- `V1.0.54__fix_device_session_timestamp_types.sql` (postgresql only)
- `V1.0.55__fix_login_session_timestamp_types.sql` (postgresql only)
