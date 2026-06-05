# Auth API Reference

**Base path**: `/auth/v1`  
**Controller**: `com.ampairs.auth.controller.AuthController`

All responses are wrapped in `ApiResponse<T>`:
```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2025-01-04T10:04:56Z"
}
```

Errors follow the same envelope with `"success": false` and an `"error"` object instead of `"data"`.

---

## Phone OTP Flow

```
POST /auth/v1/init       →  session_id (OTP sent via SMS)
POST /auth/v1/verify     →  access_token + refresh_token
```

## Firebase Flow

```
POST /auth/v1/verify/firebase  →  access_token + refresh_token
```

---

## Endpoints

### `POST /auth/v1/init`

Start authentication. Sends a 6-digit OTP to the given phone number. Returns a `session_id` used in the verify step.

**Request body**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `country_code` | integer | Yes | valid dial code | e.g. `91` for India |
| `phone` | string | Yes | valid phone format | without country code |
| `recaptcha_token` | string | No | max 1000 chars | required when reCAPTCHA is enabled |
| `device_id` | string | No | max 100 chars | stable device fingerprint; auto-generated server-side for web |
| `device_name` | string | No | max 100 chars | e.g. `"John's iPhone 15"` |
| `device_type` | string | No | max 50 chars | `"Mobile"`, `"Tablet"`, `"Desktop"` |
| `platform` | string | No | max 50 chars | `"iOS"`, `"Android"`, `"Windows"`, `"macOS"` |
| `browser` | string | No | max 100 chars | `"Google Chrome"`, `"Safari"` |
| `os` | string | No | max 100 chars | `"iOS 17.1"`, `"Windows 11"` |

**Example request**
```json
{
  "country_code": 91,
  "phone": "9591781662",
  "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
  "device_name": "John's iPhone 15",
  "recaptcha_token": "03AGdBq..."
}
```

**Response `200`**
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

**Error codes**

| Status | Code | Cause |
|--------|------|-------|
| 400 | `VALIDATION_ERROR` | Missing/invalid phone or country code |
| 429 | `RATE_LIMIT_EXCEEDED` | 1 request per 20 seconds per phone |
| 423 | `ACCOUNT_LOCKED` | Too many failed attempts; includes `remaining_minutes` |

---

### `GET /auth/v1/session/{sessionId}`

Check whether an OTP session is still valid.

**Path parameter**: `sessionId` — the value returned by `/init`.

**Response `200`**
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

`valid: false` is returned (not 404) when the session is expired or not found.

---

### `POST /auth/v1/verify`

Submit the OTP. On success, returns JWT tokens and creates or updates the device session.

**Request body**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `session_id` | string | Yes | 10–50 chars | from `/init` response |
| `otp` | string | Yes | 4–8 digits | SMS code |
| `auth_mode` | string | Yes | `OTP` | currently only `OTP` supported |
| `device_id` | string | No | max 100 chars | should match the value sent in `/init` |
| `device_name` | string | No | max 100 chars | |
| `recaptcha_token` | string | No | max 1000 chars | |

**Example request**
```json
{
  "session_id": "LSQ20250804100456522TBFOQ8U44LIBLX",
  "otp": "123456",
  "auth_mode": "OTP",
  "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
  "device_name": "John's iPhone 15"
}
```

**Response `200`**
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

**Error codes**

| Status | Cause |
|--------|-------|
| 400 | Invalid OTP or malformed request |
| 422 | Session expired, already used, or not found |
| 423 | Account locked |

---

### `POST /auth/v1/verify/firebase`

Authenticate using a Firebase ID token (phone auth). Verifies the Firebase token, matches it against the provided phone number, then creates or updates the user and device session.

**Request body**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `firebase_id_token` | string | Yes | max 5000 chars | Firebase ID token from client SDK |
| `phone` | string | Yes | 10–15 chars | without country code |
| `country_code` | integer | Yes | | e.g. `91` |
| `device_id` | string | No | max 100 chars | |
| `device_name` | string | No | max 100 chars | |
| `recaptcha_token` | string | No | max 1000 chars | currently not validated on this endpoint |

**Example request**
```json
{
  "firebase_id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6...",
  "phone": "9591781662",
  "country_code": 91,
  "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
  "device_name": "John's iPhone 15"
}
```

**Response `200`** — same shape as `/verify`.

**Error codes**

| Status | Cause |
|--------|-------|
| 400 | Phone number doesn't match Firebase token |
| 401 | Firebase token invalid or expired |

---

### `POST /auth/v1/refresh-token`

Obtain a new access token using a valid refresh token. The refresh token itself is reused (rotation only happens on explicit re-login). Device session activity is updated to prevent expiry.

**Request body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `refresh_token` | string | Yes | |
| `device_id` | string | No | required only when `device_id` claim is absent from the refresh token |

**Example request**
```json
{
  "refresh_token": "eyJhbGciOiJSUzI1NiJ9...",
  "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT"
}
```

**Response `200`** — same shape as `/verify`. `refresh_token` in the response is the same token passed in.

**Error codes**

| Status | Cause |
|--------|-------|
| 401 | Token expired, revoked, or device session inactive |

---

### `POST /auth/v1/logout`

**Requires**: `Authorization: Bearer <access_token>`

Deactivates the device session associated with the current token. Other device sessions are unaffected.

**Response `200`**
```json
{
  "success": true,
  "data": { "message": "Device logged out successfully" },
  "timestamp": "2025-01-04T10:04:56Z"
}
```

---

### `POST /auth/v1/logout/all`

**Requires**: `Authorization: Bearer <access_token>`

Deactivates all active device sessions for the authenticated user and revokes stored tokens.

**Response `200`**
```json
{
  "success": true,
  "data": { "message": "Logged out from all devices successfully" },
  "timestamp": "2025-01-04T10:04:56Z"
}
```

---

### `GET /auth/v1/devices`

**Requires**: `Authorization: Bearer <access_token>`

Returns all active device sessions for the authenticated user.

**Response `200`**
```json
{
  "success": true,
  "data": [
    {
      "device_id": "MOBILE_ABC123_DEVICE_FINGERPRINT",
      "device_name": "John's iPhone 15",
      "device_type": "Mobile",
      "platform": "iOS",
      "browser": "Mobile App",
      "os": "iOS 17.1",
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

**Device session fields**

| Field | Type | Description |
|-------|------|-------------|
| `device_id` | string | Stable device identifier |
| `device_name` | string | Human-readable name |
| `device_type` | string | `Mobile`, `Tablet`, `Desktop` |
| `platform` | string | OS family |
| `browser` | string | Browser or `"Mobile App"` |
| `os` | string | OS + version |
| `ip_address` | string | Last seen IP |
| `location` | string\|null | Geo-location if available |
| `last_activity` | ISO-8601 | Last request timestamp |
| `login_time` | ISO-8601 | Session creation timestamp |
| `is_current_device` | boolean | Whether this is the token's device |

---

### `POST /auth/v1/devices/{deviceId}/logout`

**Requires**: `Authorization: Bearer <access_token>`

Deactivates a specific device session. Useful for revoking access from a lost or unrecognised device.

**Path parameter**: `deviceId` — from the `device_id` field in `/devices`.

**Response `200`**
```json
{
  "success": true,
  "data": { "message": "Device logged out successfully" },
  "timestamp": "2025-01-04T10:04:56Z"
}
```

**Error codes**

| Status | Cause |
|--------|-------|
| 404 | Device not found or already inactive |

---

## Token Details

| Token | Algorithm | Lifetime | Claims |
|-------|-----------|----------|--------|
| Access | RS256 (or HS256 legacy) | 1 hour (configurable) | `sub`, `userId`, `deviceId`, `tenant`, `roles`, `type=access`, `kid`, `iss`, `aud` |
| Refresh | RS256 (or HS256 legacy) | 7 days (configurable) | `sub`, `deviceId`, `type=refresh` |

- `sub` — user `uid`
- `deviceId` — device session identifier
- `tenant` — active workspace ID (set after workspace selection)
- Tokens are RSA-256 signed; the public key is available at `GET /auth/v1/jwks`

## Rate Limiting

- `/init`: 1 request per 20 seconds per IP
- `/verify`: subject to account lockout after repeated failures
- Account lockout duration and threshold are configurable via `OtpProperties`

## reCAPTCHA

When `RECAPTCHA_ENABLED=true`, the `recaptcha_token` field is required on `/init` and `/verify`. Firebase verify (`/verify/firebase`) skips reCAPTCHA validation.
