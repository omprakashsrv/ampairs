# auth module

Handles all authentication flows: OTP-based login, Firebase auth, JWT issuance, device session management, and token lifecycle.

## Responsibilities

- Phone-based OTP authentication (MSG91 / AWS SNS)
- Firebase authentication (Google Sign-In, social)
- JWT issuance and verification (RSA-signed, key rotation)
- Device-aware sessions (multi-device concurrent login)
- Token refresh and logout
- Account lockout on repeated failures
- reCAPTCHA v3 validation
- User profile management and profile pictures

## Authentication Flow

```
Client
  │
  ├─1─→ POST /auth/v1/init          (phone + recaptcha)
  │       → OTP sent via SMS
  │
  ├─2─→ POST /auth/v1/verify-otp   (phone + OTP + device_id)
  │       → JWT access token + refresh token
  │
  └─3─→ POST /auth/v1/refresh      (refresh_token)
          → new access token
```

Firebase alternative:
```
POST /auth/v1/firebase   (firebase_id_token + device_id)
  → JWT access token + refresh token
```

## REST Endpoints

### Auth (`/auth/v1`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/v1/init` | Public | Initiate OTP login |
| POST | `/auth/v1/verify-otp` | Public | Verify OTP, get tokens |
| POST | `/auth/v1/firebase` | Public | Firebase token exchange |
| POST | `/auth/v1/refresh` | Public | Refresh access token |
| POST | `/auth/v1/logout` | JWT | Logout current device |
| POST | `/auth/v1/logout-all` | JWT | Logout all devices |
| GET | `/.well-known/jwks.json` | Public | JWKS public keys |

### Sessions (`/auth/v1/sessions`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/auth/v1/sessions` | List active sessions |
| DELETE | `/auth/v1/sessions/{deviceId}` | Revoke specific session |
| DELETE | `/auth/v1/sessions` | Revoke all sessions |

### User Profile (`/user/v1`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/user/v1/profile` | Get user profile |
| PUT | `/user/v1/profile` | Update profile |
| POST | `/user/v1/profile/picture` | Upload profile picture |
| DELETE | `/user/v1/profile/picture` | Remove profile picture |

### Account Management

| Method | Path | Description |
|--------|------|-------------|
| GET | `/auth/v1/lockout/status` | Check lockout status |
| POST | `/auth/v1/lockout/reset` | Admin: reset lockout |
| GET | `/auth/v1/rate-limit/status` | Current rate limit state |

## Key Entities

### User (`com.ampairs.user.model.User`)

```kotlin
class User : BaseDomain() {
    val phone: String
    val countryCode: Int           // default 91
    val firebaseUid: String?
    val name: String?
    val email: String?
    val profilePictureUrl: String?
    val profilePictureThumbnailUrl: String?
    val active: Boolean
    // Deletion fields
    val deleted: Boolean
    val deletedAt: Instant?
    val deletionScheduledFor: Instant?
    val deletionReason: String?
}
```

### Token

```kotlin
class Token : BaseDomain() {
    val userId: String
    val deviceId: String
    val tokenType: TokenType       // ACCESS, REFRESH
    val token: String
    val expiresAt: Instant
    val revoked: Boolean
}
```

### DeviceSession

```kotlin
class DeviceSession : BaseDomain() {
    val userId: String
    val deviceId: String
    val deviceName: String?
    val deviceModel: String?
    val platform: String?          // ANDROID, IOS, WEB, DESKTOP
    val appVersion: String?
    val lastActiveAt: Instant
    val active: Boolean
}
```

### LoginSession

```kotlin
class LoginSession : BaseDomain() {
    val userId: String
    val deviceId: String
    val ipAddress: String?
    val userAgent: String?
    val loginAt: Instant
    val logoutAt: Instant?
}
```

## JWT Token Structure

```json
{
  "sub": "user_uid",
  "device_id": "device_uid",
  "workspace_id": "workspace_uid",
  "iat": 1705000000,
  "exp": 1705086400
}
```

Signed with RSA-256. Public keys served at `/.well-known/jwks.json`. Keys rotate on schedule via `KeyRotationScheduler`.

## Security Features

| Feature | Mechanism |
|---------|-----------|
| Rate limiting | Bucket4J — configurable per endpoint |
| reCAPTCHA | Google v3, min score 0.5 (configurable) |
| Account lockout | Consecutive failure tracking, configurable threshold |
| OTP expiry | Configurable TTL (default 5 min) |
| Token cleanup | Scheduled job purges expired tokens |
| Multi-device | Each device gets its own refresh token |

## Environment Variables

```bash
# OTP
OTP_DEV_MODE=false           # true → fixed OTP (dev only)
OTP_EXPIRY_MINUTES=5

# reCAPTCHA
RECAPTCHA_ENABLED=true
RECAPTCHA_SECRET_KEY=xxx
RECAPTCHA_MIN_SCORE=0.5

# Firebase
FIREBASE_ENABLED=true
FIREBASE_SERVICE_ACCOUNT_KEY_PATH=/path/to/key.json

# Rate Limiting
BUCKET4J_ENABLED=true

# JWT
JWT_SECRET=minimum_256_bit_secret
JWT_EXPIRY_HOURS=24
JWT_REFRESH_EXPIRY_DAYS=30
```

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.1__create_auth_module_tables.sql` | tokens, device_sessions, login_sessions |
| `V1.0.13__create_app_user_table.sql` | app_user table |
| `V1.0.15__add_firebase_uid_to_app_user.sql` | Firebase UID column |
| `V1.0.16__add_user_deletion_fields.sql` | Soft deletion fields |
| `V1.0.23__add_profile_picture_fields.sql` | Profile picture URLs |

## Package Structure

```
com.ampairs.auth
├── config/         — OTP, rate limiting, reCAPTCHA config
├── controller/     — AuthController, SessionManagementController,
│                     JwksController, AccountLockoutController, RateLimitController
├── exception/      — AuthExceptionHandler, RecaptchaValidationException
├── interceptor/    — RateLimitingInterceptor
├── model/          — Token, DeviceSession, LoginSession, DTOs, enums
├── repository/     — TokenRepository, DeviceSessionRepository, LoginSessionRepository
└── service/        — AuthService, JwtService, OtpService, FirebaseAuthService,
                      SessionManagementService, LogoutService, RsaKeyManager,
                      KeyRotationScheduler, TokenCleanupService, AccountLockoutService

com.ampairs.user
├── controller/     — UserController
├── model/          — User, UserResponse, UserUpdateRequest
├── repository/     — UserRepository
└── service/        — UserService, CachedUserDetailsService, ProfilePictureService,
                      CoreUserServiceImpl
```
