# API Key Authentication Testing Guide

## Current Issue Diagnosis

If you're seeing this error:
```
org.springframework.security.authentication.InsufficientAuthenticationException: Full authentication is required to access this resource
```

**This means**: You're not providing authentication credentials in the request.

## How to Test API Key Authentication

### Step 1: Check the Logs

With the new debug logging, you should see one of these:

**If no X-API-Key header is provided:**
```
INFO [ApiKeyAuthenticationFilter]: ApiKeyAuthenticationFilter SKIPPED for /api/v1/app-updates - no X-API-Key header
```

**If X-API-Key header is provided:**
```
INFO [ApiKeyAuthenticationFilter]: ApiKeyAuthenticationFilter processing: /api/v1/app-updates
INFO [ApiKeyAuthenticationFilter]: API key header present: true, starts with prefix: true
INFO [ApiKeyAuthenticationFilter]: Attempting API key authentication for: /api/v1/app-updates
INFO [ApiKeyAuthenticationFilter]: API key authentication successful for: /api/v1/app-updates
```

### Step 2: Create an API Key (First Time Only)

You need an admin JWT token first:

```bash
# 1. Login as admin user
curl -X POST http://localhost:8080/auth/v1/init \
  -H "Content-Type: application/json" \
  -d '{"phone": "1234567890", "country_code": 91}'

# 2. Verify OTP
curl -X POST http://localhost:8080/auth/v1/verify \
  -H "Content-Type: application/json" \
  -d '{"session_id": "<session_id>", "otp": "123456"}'

# 3. Use the access_token to create API key
curl -X POST http://localhost:8080/api/v1/admin/api-keys \
  -H "Authorization: Bearer <admin-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CI/CD Pipeline Key",
    "scope": "APP_UPDATES",
    "description": "For automated app updates",
    "expires_in_days": 365
  }'

# Response will include the API key (SAVE THIS - it's only shown once):
{
  "success": true,
  "data": {
    "uid": "...",
    "name": "CI/CD Pipeline Key",
    "scope": "APP_UPDATES",
    "plain_key": "amp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",  ← COPY THIS
    "prefix": "amp_xxxx",
    "created_at": "2025-01-01T00:00:00Z"
  }
}
```

### Step 3: Test API Key Authentication

```bash
# Use the API key to create an app version
curl -X POST http://localhost:8080/api/v1/app-updates \
  -H "X-API-Key: amp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" \
  -H "Content-Type: application/json" \
  -d '{
    "platform": "MACOS",
    "version_name": "1.0.0.10",
    "version_code": 10,
    "s3_key": "updates/macos/MyApp-1.0.0.10.dmg",
    "filename": "MyApp-1.0.0.10.dmg",
    "file_size": 123456789,
    "checksum": "abc123...",
    "release_notes": "Bug fixes and improvements",
    "is_mandatory": false
  }'
```

### Step 4: Test Public Endpoints (No Auth Required)

```bash
# Check for updates (no authentication needed)
curl "http://localhost:8080/api/v1/app-updates/check?platform=MACOS&currentVersion=1.0.0&versionCode=1"

# Download update file (no authentication needed)
curl -O "http://localhost:8080/api/v1/app-updates/download/<uid>"
```

## Common Issues

### Issue 1: "Full authentication is required"
**Cause**: No X-API-Key header or invalid key
**Solution**: Add `X-API-Key: amp_xxxxx` header to your request

### Issue 2: "Invalid API key"
**Cause**:
- Key is revoked
- Key is expired
- Key doesn't start with `amp_` prefix
- Key not found in database

**Solution**: Create a new API key using admin endpoint

### Issue 3: "Access Denied" (403)
**Cause**: API key doesn't have required scope
**Solution**: For app-updates endpoints, scope must be `APP_UPDATES`

## Authentication Methods Summary

| Endpoint | Admin JWT | API Key (APP_UPDATES) | No Auth |
|----------|-----------|----------------------|---------|
| POST /api/v1/app-updates | ✅ | ✅ | ❌ |
| GET /api/v1/app-updates | ✅ | ❌ | ❌ |
| PUT /api/v1/app-updates/{uid} | ✅ | ❌ | ❌ |
| DELETE /api/v1/app-updates/{uid} | ✅ | ❌ | ❌ |
| GET /api/v1/app-updates/check | ✅ | ✅ | ✅ |
| GET /api/v1/app-updates/download/{uid} | ✅ | ✅ | ✅ |

## Debugging Checklist

1. ✅ Check application logs for `ApiKeyAuthenticationFilter` messages
2. ✅ Verify X-API-Key header is present in request
3. ✅ Verify key starts with `amp_` prefix
4. ✅ Verify key exists in database and is not revoked
5. ✅ Verify key has correct scope for the endpoint
6. ✅ Check that ApiKeyAuthenticationFilter bean is registered in Spring context
