# auth module

Phone OTP + Firebase auth, JWT (RSA-signed), device-aware sessions, user profiles.

## Flow
`POST /auth/v1/init` (phone + reCAPTCHA) → OTP → `POST /auth/v1/verify-otp` (OTP + device_id) → JWT + refresh token

## Key entities
- `User` — phone, firebaseUid, profilePictureUrl, deleted/deletedAt (soft delete)
- `Token` — access/refresh, device-scoped, revocable
- `DeviceSession` — per-device tracking, lastActivity
- `LoginSession` — audit log of login events

## JWT claims
`sub` (user uid), `device_id`, `workspace_id`, `iat`, `exp` — RSA-256 signed

## Controllers
`AuthController`, `SessionManagementController`, `JwksController`, `AccountLockoutController`, `UserController`

## Key env vars
`OTP_DEV_MODE`, `RECAPTCHA_ENABLED`, `FIREBASE_ENABLED`, `BUCKET4J_ENABLED`, `JWT_SECRET`

## Migrations
`V1.0.1`, `V1.0.13`, `V1.0.15`, `V1.0.16` (deletion), `V1.0.23` (profile picture)

## Full docs
`docs/modules/auth.md`
