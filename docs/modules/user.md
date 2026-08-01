# user module

User identity, profile management, and soft-delete account lifecycle. Authentication mechanics (JWT, OTP, device sessions) live in the `auth` module — `user` owns the registered platform user itself.

## REST Endpoints (`/user/v1`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/user/v1` | Current user profile |
| PUT | `/user/v1` | Update profile fields |
| POST | `/user/v1/profile-picture` | Upload profile picture (resized + thumbnail, stored in S3) |
| DELETE | `/user/v1` | Schedule account deletion (soft delete; see `docs/features/account-deletion.md`) |

(See `UserController` for the authoritative list.)

## Key Entities

### User

- `phone`, `countryCode`, `email`, `firstName`, `lastName`
- `firebaseUid`, `profilePictureUrl`
- `deleted`, `deletedAt`, `deletionScheduledFor` — soft-delete lifecycle

## Services

- `UserService` — profile updates, user lookup
- `ProfilePictureService` — upload, resize, thumbnail, S3 storage
- `CachedUserDetailsService` — Spring Security `UserDetailsService` backed by cache
- `CoreUserServiceImpl` — implements `com.ampairs.core.domain.CoreUserService` for other modules

## Dependencies

- `:core` — `BaseDomain`, `ApiResponse`, `CoreUser`, `UserDetailsWithId`, `UserDetailsWithRoles`
- `:file` — `ObjectStorageService`, `ImageResizingService`, `StorageProperties`

See `user/CLAUDE.md` for module-specific coding guidance.
