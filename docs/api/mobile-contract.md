# Ampairs Mobile API Contract

**Audience**: Android & iOS engineers  
**Version**: 1.0  
**Last updated**: 2026-05-25

---

## Table of Contents

1. [Environments](#1-environments)
2. [Global Conventions](#2-global-conventions)
3. [Response Envelope](#3-response-envelope)
4. [Error Handling](#4-error-handling)
5. [Authentication Flow](#5-authentication-flow)
6. [Auth Endpoints](#6-auth-endpoints)
7. [User Endpoints](#7-user-endpoints)
8. [Token Management](#8-token-management)
9. [Device Fingerprinting](#9-device-fingerprinting)
10. [Rate Limits & Retry Strategy](#10-rate-limits--retry-strategy)

---

## 1. Environments

| Environment | Base URL |
|-------------|----------|
| Development | `http://10.0.2.2:8080` (Android emulator) / `http://localhost:8080` (iOS simulator) |
| Staging | TBD |
| Production | TBD |

All endpoints are relative to the base URL. No global `/api` prefix.

---

## 2. Global Conventions

### Request headers

| Header | Required | Value |
|--------|----------|-------|
| `Content-Type` | Yes (POST/PUT/PATCH) | `application/json` |
| `Authorization` | On authenticated endpoints | `Bearer <access_token>` |
| `X-Workspace-ID` | On workspace-scoped endpoints | workspace `uid` string |

### JSON field naming
All fields use **snake_case** in both requests and responses. There are no camelCase variants.

### Timestamps
All timestamps are **ISO-8601 UTC** strings (e.g. `"2025-01-04T10:04:56Z"`). Store and compare them as UTC; convert to local time only for display.

---

## 3. Response Envelope

Every endpoint returns the same top-level wrapper.

### Success

```json
{
  "success": true,
  "data": { },
  "timestamp": "2025-01-04T10:04:56Z",
  "path": "/auth/v1/init",
  "trace_id": "abc-123"
}
```

### Error

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": "Request validation failed",
    "validation_errors": {
      "phone": "Phone number is required"
    },
    "module": "auth"
  },
  "timestamp": "2025-01-04T10:04:56Z",
  "path": "/auth/v1/init"
}
```

**Envelope fields**

| Field | Type | Always present | Description |
|-------|------|----------------|-------------|
| `success` | boolean | Yes | `true` on success, `false` on error |
| `data` | object \| array \| null | On success | Response payload |
| `error` | object \| null | On error | Error details (see §4) |
| `timestamp` | string (ISO-8601) | Yes | Server time of response |
| `path` | string \| null | Sometimes | Request path |
| `trace_id` | string \| null | Sometimes | Correlation ID for support |

> **Tip**: Always check `success` first, not the HTTP status code alone.

---

## 4. Error Handling

### Error object fields

| Field | Type | Description |
|-------|------|-------------|
| `code` | string | Machine-readable error code (see table below) |
| `message` | string | Human-readable description |
| `details` | string \| null | Additional context |
| `validation_errors` | object \| null | Field-level errors keyed by field name |
| `module` | string \| null | Source module |

### Error codes

| Code | HTTP | Meaning | Action |
|------|------|---------|--------|
| `VALIDATION_ERROR` | 400 | Request fields failed validation | Show `validation_errors` to user |
| `BAD_REQUEST` | 400 | Malformed request body | Fix request structure |
| `AUTH_001` | 401 | Authentication failed | Re-authenticate |
| `AUTH_002` | 401 | Invalid credentials (OTP wrong) | Show "Invalid OTP" |
| `AUTH_003` | 401 | Access token expired | Refresh token (see §8) |
| `AUTH_004` | 401 | Token invalid or malformed | Re-authenticate |
| `AUTH_006` | 403 | Access denied (missing permission) | Show access denied screen |
| `AUTH_008` | 423 | Account locked after repeated failures | Show lockout duration |
| `AUTH_009` | 429 | Rate limit exceeded | Respect `Retry-After` header |
| `NOT_FOUND` | 404 | Resource not found | Show 404 state |
| `INTERNAL_SERVER_ERROR` | 500 | Server error | Retry with backoff; report if persistent |

### Validation errors

When `code` is `VALIDATION_ERROR`, the `validation_errors` map contains one entry per invalid field:

```json
"validation_errors": {
  "phone": "Phone number is required",
  "country_code": "Invalid country code"
}
```

The key matches the request field name exactly (snake_case).

---

## 5. Authentication Flow

### Phone OTP (primary)

```
┌─────────┐                          ┌────────────┐
│  App    │                          │  Server    │
└────┬────┘                          └─────┬──────┘
     │  POST /auth/v1/init                 │
     │  { phone, country_code, device_id } │
     │ ─────────────────────────────────► │
     │                                     │  Send OTP via SMS
     │  { session_id }                     │◄──────────────────
     │ ◄───────────────────────────────── │
     │                                     │
     │  [User enters OTP]                  │
     │                                     │
     │  POST /auth/v1/verify               │
     │  { session_id, otp, device_id }     │
     │ ─────────────────────────────────► │
     │                                     │
     │  { access_token, refresh_token }    │
     │ ◄───────────────────────────────── │
     │                                     │
     │  Store tokens securely              │
     │  All subsequent requests:           │
     │  Authorization: Bearer <access_token>
```

### Firebase phone auth (alternative)

```
┌─────────┐     ┌──────────┐          ┌────────────┐
│  App    │     │ Firebase │          │  Server    │
└────┬────┘     └────┬─────┘          └─────┬──────┘
     │               │                       │
     │  signInWithPhoneNumber()              │
     │ ────────────► │                       │
     │  firebase_id_token                    │
     │ ◄──────────── │                       │
     │                                       │
     │  POST /auth/v1/verify/firebase        │
     │  { firebase_id_token, phone,          │
     │    country_code, device_id }          │
     │ ─────────────────────────────────── ► │
     │                                       │
     │  { access_token, refresh_token }      │
     │ ◄───────────────────────────────────  │
```

### Token refresh

```
┌─────────┐                          ┌────────────┐
│  App    │                          │  Server    │
└────┬────┘                          └─────┬──────┘
     │  POST /auth/v1/refresh-token        │
     │  { refresh_token, device_id }       │
     │ ─────────────────────────────────► │
     │                                     │
     │  { access_token, refresh_token }    │
     │  (same refresh_token returned)      │
     │ ◄───────────────────────────────── │
```

---

## 6. Auth Endpoints

### `POST /auth/v1/init`

Send OTP to a phone number. Call this first in the phone auth flow.

**Request**

```json
{
  "country_code": 91,
  "phone": "9591781662",
  "device_id": "550e8400-e29b-41d4-a716-446655440000",
  "device_name": "Pixel 8 Pro",
  "device_type": "Mobile",
  "platform": "Android",
  "os": "Android 14",
  "recaptcha_token": "03AGdBq..."
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `country_code` | integer | **Yes** | valid dial code, e.g. `91` |
| `phone` | string | **Yes** | local number without country code |
| `device_id` | string | No | max 100 chars; stable identifier (see §9) |
| `device_name` | string | No | max 100 chars; e.g. `"Pixel 8 Pro"` |
| `device_type` | string | No | `"Mobile"` \| `"Tablet"` \| `"Desktop"` |
| `platform` | string | No | `"Android"` \| `"iOS"` |
| `os` | string | No | max 100 chars; e.g. `"Android 14"`, `"iOS 17.1"` |
| `browser` | string | No | `"Mobile App"` for native apps |
| `recaptcha_token` | string | Conditional | Required when reCAPTCHA is enabled server-side |

**Success response `200`**

```json
{
  "success": true,
  "data": {
    "message": "OTP sent successfully",
    "session_id": "LSQ20250804100456522TBFOQ8U44LIBLX"
  },
  "timestamp": "2025-01-04T10:04:56Z"
}
```

**Save `session_id`** — needed for the verify call.

**Error responses**

| `success` | HTTP | `error.code` | When |
|-----------|------|--------------|------|
| false | 400 | `VALIDATION_ERROR` | Missing/invalid phone or country code |
| false | 423 | `AUTH_008` | Account locked; `error.details` contains remaining minutes |
| false | 429 | `AUTH_009` | Rate limited (1 req / 20 s per phone) |

---

### `GET /auth/v1/session/{sessionId}`

Check if a session is still valid before asking the user to enter OTP.

**Path param**: `sessionId` — value from `/init` response.

**Success response `200`**

```json
{
  "success": true,
  "data": {
    "id": "LSQ20250804100456522TBFOQ8U44LIBLX",
    "country_code": 91,
    "phone": "9591781662",
    "valid": true
  },
  "timestamp": "2025-01-04T10:04:56Z"
}
```

`valid: false` means the session has expired. Restart from `/init`. This endpoint never returns 404.

---

### `POST /auth/v1/verify`

Submit the OTP. Returns tokens on success.

**Request**

```json
{
  "session_id": "LSQ20250804100456522TBFOQ8U44LIBLX",
  "otp": "123456",
  "auth_mode": "OTP",
  "device_id": "550e8400-e29b-41d4-a716-446655440000",
  "device_name": "Pixel 8 Pro"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `session_id` | string | **Yes** | 10–50 chars; from `/init` |
| `otp` | string | **Yes** | 4–8 digits |
| `auth_mode` | string | **Yes** | Always `"OTP"` |
| `device_id` | string | No | Should match `device_id` sent in `/init` |
| `device_name` | string | No | max 100 chars |
| `recaptcha_token` | string | Conditional | Required when reCAPTCHA enabled |

**Success response `200`**

```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJSUzI1NiJ9...",
    "refresh_token": "eyJhbGciOiJSUzI1NiJ9...",
    "access_token_expires_at": "2025-01-04T11:04:56Z",
    "refresh_token_expires_at": "2025-01-11T10:04:56Z"
  },
  "timestamp": "2025-01-04T10:04:56Z"
}
```

**Error responses**

| HTTP | `error.code` | When |
|------|--------------|------|
| 400 | `VALIDATION_ERROR` | Missing fields or OTP not 4–8 digits |
| 401 | `AUTH_002` | Wrong OTP |
| 422 | `BAD_REQUEST` | Session expired, already used, or not found |
| 423 | `AUTH_008` | Account locked after too many wrong OTPs |

---

### `POST /auth/v1/verify/firebase`

Exchange a Firebase ID token for Ampairs tokens.

**Request**

```json
{
  "firebase_id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6...",
  "phone": "9591781662",
  "country_code": 91,
  "device_id": "550e8400-e29b-41d4-a716-446655440000",
  "device_name": "Pixel 8 Pro"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `firebase_id_token` | string | **Yes** | max 5000 chars; from Firebase SDK |
| `phone` | string | **Yes** | 10–15 chars |
| `country_code` | integer | **Yes** | e.g. `91` |
| `device_id` | string | No | max 100 chars |
| `device_name` | string | No | max 100 chars |

**Success response `200`** — identical shape to `/verify`.

**Error responses**

| HTTP | `error.code` | When |
|------|--------------|------|
| 400 | `BAD_REQUEST` | Phone number doesn't match Firebase token |
| 401 | `AUTH_001` | Firebase token invalid or expired |

---

### `POST /auth/v1/refresh-token`

Get a new access token. No `Authorization` header needed — pass the refresh token in the body.

**Request**

```json
{
  "refresh_token": "eyJhbGciOiJSUzI1NiJ9...",
  "device_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

| Field | Type | Required |
|-------|------|----------|
| `refresh_token` | string | **Yes** |
| `device_id` | string | No; required only when the refresh token has no `deviceId` claim |

**Success response `200`**

```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJSUzI1NiJ9...<new>",
    "refresh_token": "eyJhbGciOiJSUzI1NiJ9...<same>",
    "access_token_expires_at": "2025-01-04T12:00:00Z",
    "refresh_token_expires_at": "2025-01-11T10:04:56Z"
  },
  "timestamp": "2025-01-04T11:00:00Z"
}
```

The `refresh_token` in the response is the **same** token you sent — it is not rotated. Store the new `access_token` and its expiry.

**Error responses**

| HTTP | `error.code` | When |
|------|--------------|------|
| 401 | `AUTH_003` | Refresh token expired |
| 401 | `AUTH_004` | Refresh token invalid or device session inactive |

---

### `POST /auth/v1/logout`

**Auth required**: `Authorization: Bearer <access_token>`

Deactivates the current device session only.

**No request body.**

**Success response `200`**

```json
{
  "success": true,
  "data": { "message": "Device logged out successfully" },
  "timestamp": "2025-01-04T10:04:56Z"
}
```

After receiving this response, delete both tokens from local storage.

---

### `POST /auth/v1/logout/all`

**Auth required**: `Authorization: Bearer <access_token>`

Deactivates all device sessions for this user.

**No request body.**

**Success response `200`**

```json
{
  "success": true,
  "data": { "message": "Logged out from all devices successfully" },
  "timestamp": "2025-01-04T10:04:56Z"
}
```

---

### `GET /auth/v1/devices`

**Auth required**: `Authorization: Bearer <access_token>`

List all active sessions. Use this to build a "Logged-in devices" screen.

**No request body.**

**Success response `200`**

```json
{
  "success": true,
  "data": [
    {
      "device_id": "550e8400-e29b-41d4-a716-446655440000",
      "device_name": "Pixel 8 Pro",
      "device_type": "Mobile",
      "platform": "Android",
      "browser": "Mobile App",
      "os": "Android 14",
      "ip_address": "192.168.1.100",
      "location": null,
      "last_activity": "2025-01-04T10:30:00Z",
      "login_time": "2025-01-04T09:00:00Z",
      "is_current_device": true
    }
  ],
  "timestamp": "2025-01-04T10:04:56Z"
}
```

**DeviceSession object**

| Field | Type | Description |
|-------|------|-------------|
| `device_id` | string | Stable device identifier |
| `device_name` | string | Human-readable label |
| `device_type` | string | `"Mobile"` \| `"Tablet"` \| `"Desktop"` |
| `platform` | string | `"Android"` \| `"iOS"` \| `"Windows"` |
| `browser` | string | `"Mobile App"` for native |
| `os` | string | OS + version string |
| `ip_address` | string | Last seen IP |
| `location` | string \| null | Geo-location if available |
| `last_activity` | string (ISO-8601) | Last request time |
| `login_time` | string (ISO-8601) | Session creation time |
| `is_current_device` | boolean | `true` for the token making this request |

---

### `POST /auth/v1/devices/{deviceId}/logout`

**Auth required**: `Authorization: Bearer <access_token>`

Revoke a specific device session — useful for "remove this device" from the devices screen.

**Path param**: `deviceId` — the `device_id` value from `/devices`.

**No request body.**

**Success response `200`**

```json
{
  "success": true,
  "data": { "message": "Device logged out successfully" },
  "timestamp": "2025-01-04T10:04:56Z"
}
```

| HTTP | `error.code` | When |
|------|--------------|------|
| 404 | `NOT_FOUND` | Device not found or already inactive |

---

## 7. User Endpoints

All endpoints require `Authorization: Bearer <access_token>`.

### `GET /user/v1`

Get the authenticated user's profile.

**Success response `200`**

```json
{
  "success": true,
  "data": {
    "id": "USR20250101000000000XXXXXXXXXXX",
    "first_name": "John",
    "last_name": "Doe",
    "user_name": "919591781662",
    "country_code": 91,
    "phone": "9591781662",
    "email": null,
    "full_name": "John Doe",
    "active": true,
    "profile_picture_url": null,
    "profile_picture_thumbnail_url": null
  },
  "timestamp": "2025-01-04T10:04:56Z"
}
```

**User object**

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique user identifier |
| `first_name` | string | |
| `last_name` | string \| null | |
| `user_name` | string | Derived from `{country_code}{phone}` |
| `country_code` | integer | |
| `phone` | string | |
| `email` | string \| null | |
| `full_name` | string | Computed: `first_name + last_name` |
| `active` | boolean | Account status |
| `profile_picture_url` | string \| null | Full-size S3 URL (or server proxy path) |
| `profile_picture_thumbnail_url` | string \| null | Thumbnail S3 URL |

---

### `POST /user/v1/update`

Update the authenticated user's profile.

**Request**

```json
{
  "first_name": "John",
  "last_name": "Doe"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `first_name` | string | **Yes** | max 100 chars, non-blank |
| `last_name` | string \| null | No | max 100 chars |

**Success response `200`** — returns the updated `User` object (same shape as `GET /user/v1`).

---

### `POST /user/v1/upload-picture`

Upload a profile picture.

**Content-Type**: `multipart/form-data`

| Part | Type | Constraints |
|------|------|-------------|
| `file` | binary | JPEG, PNG, or WebP; max 5 MB |

The server resizes to 512×512 px and generates a 256×256 px thumbnail.

**Success response `200`** — returns the updated `User` object with `profile_picture_url` and `profile_picture_thumbnail_url` populated.

---

### `DELETE /user/v1/picture`

Remove the profile picture.

**No request body.**

**Success response `200`** — returns the updated `User` object with both picture fields set to `null`.

---

### `GET /user/v1/picture`

Download the authenticated user's full-size profile picture.

**Response**: binary image (`image/jpeg`, `image/png`, or `image/webp`).  
**Cache-Control**: `public, max-age=31536000` (1 year). Cache aggressively on the client.

Returns `404` with `FILE_001` if no picture is set.

---

### `GET /user/v1/picture/thumbnail`

Download the authenticated user's 256×256 thumbnail.

Same caching rules as above.

---

### `GET /user/v1/{userId}/picture`

Download any user's full-size profile picture by `userId`.

---

### `GET /user/v1/{userId}/picture/thumbnail`

Download any user's thumbnail by `userId`.

---

## 8. Token Management

### Storage

| Platform | Recommendation |
|----------|----------------|
| Android | `EncryptedSharedPreferences` (Jetpack Security) |
| iOS | Keychain (`kSecClassGenericPassword`) |

Never store tokens in plain `SharedPreferences`, `UserDefaults`, or local SQLite without encryption.

### Token properties

| | Access token | Refresh token |
|-|-------------|---------------|
| Algorithm | RS256 | RS256 |
| Lifetime | ~1 hour (see `access_token_expires_at`) | ~7 days (see `refresh_token_expires_at`) |
| Use | `Authorization: Bearer` header | Refresh endpoint only |
| Claims | `sub`, `userId`, `deviceId`, `tenant`, `type=access` | `sub`, `deviceId`, `type=refresh` |

### Refresh strategy

1. On every API call, check if the access token expires within the next **5 minutes**.
2. If so, call `POST /auth/v1/refresh-token` before making the real request.
3. If the refresh fails with `AUTH_003` or `AUTH_004`, the refresh token is invalid — redirect to login.
4. Implement a mutex/lock so that concurrent requests trigger only **one** refresh, then retry all waiting requests with the new token.

```
Request interceptor pseudocode:

if (accessToken.expiresAt - now < 5 minutes):
    newTokens = refreshAccessToken(refreshToken, deviceId)
    if newTokens.success:
        store(newTokens.access_token, newTokens.access_token_expires_at)
    else:
        logout() and redirect to login
```

### When to re-authenticate

Re-prompt the user for their phone + OTP when:
- `POST /auth/v1/refresh-token` returns `401`
- `error.code` is `AUTH_003`, `AUTH_004`
- The user explicitly logs out

---

## 9. Device Fingerprinting

The `device_id` field links a user's tokens to a specific device session. You must generate and persist it.

### Android

```kotlin
// Generate once and persist in EncryptedSharedPreferences
fun getOrCreateDeviceId(prefs: SharedPreferences): String {
    return prefs.getString("device_id", null) ?: run {
        val id = UUID.randomUUID().toString()
        prefs.edit().putString("device_id", id).apply()
        id
    }
}
```

Do **not** use `Settings.Secure.ANDROID_ID` — it changes on factory reset and is unreliable across Android versions.

### iOS

```swift
// Generate once and persist in Keychain
func getOrCreateDeviceId() -> String {
    if let existing = KeychainHelper.read(key: "device_id") {
        return existing
    }
    let id = UUID().uuidString
    KeychainHelper.save(key: "device_id", value: id)
    return id
}
```

Do **not** use `UIDevice.current.identifierForVendor` — it changes when all apps from a vendor are uninstalled.

### What to send

Send these fields on every `/init` and `/verify` call:

```json
{
  "device_id": "550e8400-e29b-41d4-a716-446655440000",
  "device_name": "Pixel 8 Pro",
  "device_type": "Mobile",
  "platform": "Android",
  "os": "Android 14"
}
```

---

## 10. Rate Limits & Retry Strategy

### Rate limits

| Endpoint | Limit | Window |
|----------|-------|--------|
| `POST /auth/v1/init` | 1 request | per 20 seconds per IP |
| `POST /auth/v1/verify` | subject to account lockout | — |

When rate limited, the server returns HTTP `429` with `error.code = "AUTH_009"`. Respect the `Retry-After` response header if present.

### Account lockout

After repeated failed OTP attempts, the account is temporarily locked. The error response will be:

```json
{
  "success": false,
  "error": {
    "code": "AUTH_008",
    "message": "Account is locked",
    "details": "Account locked for 15 minutes"
  }
}
```

Display the remaining lock duration to the user. Do not retry automatically — wait for the user to try again after the lockout period.

### Retry strategy for server errors

For `5xx` errors and network failures, use **exponential backoff**:

```
attempt 1 → wait 1s
attempt 2 → wait 2s
attempt 3 → wait 4s
max 3 retries, then show error to user
```

Do **not** retry `4xx` errors automatically (except token refresh on `401`).
