# Auth Module

## Overview
The Auth module secures the Ampairs platform with OTP-based login, JWT issuance, device session management, and user profile APIs. It supports both web and mobile clients, integrates with Google reCAPTCHA, enforces rate limits and account lockout policies, and rotates RSA signing keys to keep tokens resilient.

The module is split into two top-level packages:
- `com.ampairs.auth` – authentication flows, security utilities, session/token persistence.
- `com.ampairs.user` – user profile endpoints and backing services.

## Architecture
### Package Structure
```
com.ampairs.auth/
├── config/              # OTP, rate limiting, reCAPTCHA configuration beans
├── controller/          # Authentication REST APIs (/auth/v1)
├── exception/           # Domain-specific exceptions and handlers
├── interceptor/         # Rate limiting interceptor (Bucket4j-backed)
├── model/               # Entities (LoginSession, Token, DeviceSession) + DTOs & enums
├── repository/          # Spring Data repositories for auth entities
├── service/             # AuthService, JwtService, OTP, RSA key management, cleanup, lockout
└── utils/               # Helper utilities (phone validation, token helpers)

com.ampairs.user/
├── controller/          # User profile APIs (/user/v1)
├── model/               # User entity and DTO mappers
├── repository/          # User persistence
└── service/             # UserService (profile read/write, session context)
```

## Key Components
- **`AuthController`** – Handles `/auth/v1` endpoints: OTP init/verify, session status, refresh, logout, device session listing, and JWK exposure.
- **`AuthService`** – Orchestrates OTP delivery, session creation, refresh-token flow, and device concurrency checks.
- **`OtpService`** – Generates, caches, and verifies OTP codes with configurable TTL and channel-specific templates.
- **`JwtService`** – Issues RS256 access/refresh tokens, validates claims, and exposes JWK sets via `RsaKeyManager`.
- **`RsaKeyManager` + `KeyRotationScheduler`** – Manage RSA key generation, storage, rotation (scheduled/manual), and audit logging.
- **`SessionManagementService`** – Enforces per-user/device session limits, idle timeout rules, and device activity tracking.
- **`AccountLockoutService`** – Implements soft lockout with progressive penalties, security audit logging, and admin unlock helpers.
- **`TokenCleanupService`** – Scheduled purge of expired/blacklisted tokens with paginated cleanup to avoid DB bloat.
- **`RecaptchaValidationService`** – Integrates with Google reCAPTCHA v3, supporting dev/whitelist bypass modes.
- **`RateLimitingInterceptor`** – Bucket4j interceptor applied to `/auth/v1`, `/user/v1`, and `/api/**` routes (configurable exclusions).
- **`UserController` / `UserService`** – Update and fetch the authenticated user profile using the session security context.

## Features
- **OTP-first login flow**
  - `POST /auth/v1/init` validates reCAPTCHA, applies rate limiting, and dispatches OTP via configured channels.
  - `POST /auth/v1/verify` completes login, issues JWTs, records device session, and enforces account lockout policy.
  - `GET /auth/v1/session/{id}` lets clients poll session status during OTP entry.
- **JWT ecosystem**
  - RS256 signing with rotating key pairs (auto + manual rotation).
  - Access/refresh pair issuance with configurable lifetimes and blacklist enforcement.
  - `/auth/v1/jwks` exposes current public keys for downstream verification.
- **Session governance**
  - Device-aware session records (`DeviceSession`) track last activity, IP, and device metadata.
  - Concurrent session limits per user and per device type with automatic eviction of oldest sessions.
  - Scheduled validation ensures sessions align with refresh-token lifetime and idle timeout policies.
- **Security hardening**
  - Progressive account lockout after configurable failed attempts with scheduled auto-release.
  - Google reCAPTCHA validation (with dev/whitelist overrides) for login and OTP verification actions.
  - Request-level rate limiting via Bucket4j interceptor and dedicated configuration properties.
  - Security audit hooks (via `SecurityAuditService`) for suspicious events, key rotation, and lockout actions.
- **User profile management**
  - `/user/v1` endpoints expose session-bound user info and allow profile updates.
  - DTO mappers keep API responses immutable and sanitized.

## API Highlights
| Endpoint | Description |
|----------|-------------|
| `POST /auth/v1/init` | Start OTP flow (reCAPTCHA + rate limiting). |
| `POST /auth/v1/verify` | Verify OTP and obtain access/refresh tokens. |
| `POST /auth/v1/refresh` | Exchange refresh token for a new token pair. |
| `POST /auth/v1/logout` | Revoke current session and tokens. |
| `GET /auth/v1/session/{sessionId}` | Poll authentication session status. |
| `GET /auth/v1/devices` | List active device sessions for the user. |
| `DELETE /auth/v1/devices/{deviceId}` | Revoke a specific device session. |
| `GET /auth/v1/jwks` | Retrieve JWK set for token verification. |
| `GET /user/v1` | Fetch the authenticated user profile. |
| `POST /user/v1/update` | Update user profile details. |

All endpoints return the shared `ApiResponse<T>` contract from the `core` module and leverage standard error envelopes.

## Integration Points
- **Core** – Provides `ApplicationProperties`, multi-tenancy helpers, `SecurityAuditService`, response wrappers, and storage of RSA key files.
- **Workspace** – Supplies tenant context used in token claims and session scoping.
- **Notification/Notification Providers** – OTP delivery (SMS/email/push) piggybacks on providers configured in other modules.
- **Event** – Device/session changes and security incidents can be observed via audit logs consumed by event pipelines.
- **External Services** – Google reCAPTCHA v3 API; optional SMS/email gateways for OTP dispatch (configured in properties).

## Configuration & Operations
- Toggle features via `application.security.*` and `ampairs.security.*` properties (account lockout, token cleanup, key rotation, session limits).
- RSA keys default to `auth/keys/` but can be overridden through `ApplicationProperties`.
- ReCAPTCHA dev and whitelist modes simplify local testing without Google calls.
- Scheduled tasks: key rotation, token cleanup, lockout cache purge, session validation.

## Build & Test
```bash
# From ampairs-backend/
./gradlew :auth:build
./gradlew :auth:test
```

Run `./gradlew :ampairs_service:bootRun` to validate auth flows alongside the aggregated backend service.
