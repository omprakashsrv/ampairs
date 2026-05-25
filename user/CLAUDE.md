# user module

User identity, profile management, and soft-delete lifecycle.

## Bounded context

Owns the `User` entity and everything directly related to a registered platform user: profile fields, profile picture, account deletion. Authentication mechanics (JWT, OTP, sessions) live in `auth`.

## Key entities

- `User` — phone, countryCode, email, firstName, lastName, firebaseUid, profilePictureUrl, deleted/deletedAt/deletionScheduledFor (soft delete)

## Controllers

`UserController` — `/user/v1/`

## Services

- `UserService` — profile updates, user lookup
- `ProfilePictureService` — upload, resize, thumbnail, S3 storage
- `CachedUserDetailsService` — Spring Security `UserDetailsService` backed by cache
- `CoreUserServiceImpl` — implements `com.ampairs.core.domain.CoreUserService`

## Dependencies

- `:core` — `BaseDomain`, `ApiResponse`, `CoreUser`, `UserDetailsWithId`, `UserDetailsWithRoles`
- `:file` — `ObjectStorageService`, `ImageResizingService`, `StorageProperties`

## Migrations

`V1.0.13` (create app_user), `V1.0.15` (firebase_uid), `V1.0.16` (deletion fields), `V1.0.23` (profile picture)
