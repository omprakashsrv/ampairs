## API Key Authentication Module

Machine-to-machine authentication for CI/CD pipelines and automated systems.

## Architecture

Uses Spring Security's **AuthenticationManager** with multiple authentication providers.

```
┌─────────────────────────────────────────────┐
│  HTTP Request (X-API-Key: amp_xxx...)      │
└────────────────┬────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────┐
│  ApiKeyAuthenticationFilter                 │
│  - Extracts X-API-Key header               │
│  - Creates ApiKeyAuthenticationToken       │
│  - Delegates to AuthenticationManager      │
└────────────────┬────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────┐
│  AuthenticationManager                      │
│  - Tries each provider in order            │
│  - ApiKeyAuthenticationProvider            │
│  - JwtAuthenticationProvider (existing)    │
└────────────────┬────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────┐
│  ApiKeyAuthenticationProvider               │
│  - Checks supports(ApiKeyAuthenticationToken)│
│  - Validates via ApiKeyService             │
│  - Returns authenticated token             │
└────────────────┬────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────┐
│  ApiKeyService                              │
│  - Hash key (SHA-256)                       │
│  - Find in database                         │
│  - Validate (active, not expired, etc.)    │
│  - Update usage tracking                    │
└────────────────┬────────────────────────────┘
                 │
                 v
┌─────────────────────────────────────────────┐
│  ApiKeyRepository                           │
│  - Query by key_hash                        │
│  - Update last_used_at & usage_count       │
└─────────────────────────────────────────────┘
```

## Package Structure

```
com.ampairs.core.auth/
├── controller/
│   └── ApiKeyController.kt          # Admin CRUD endpoints
├── service/
│   └── ApiKeyService.kt             # Key generation, validation
├── repository/
│   └── ApiKeyRepository.kt          # Data access
├── filter/
│   └── ApiKeyAuthenticationFilter.kt # Spring Security filter
└── domain/
    ├── ApiKey.kt                     # Entity
    └── ApiKeyDTOs.kt                # Request/Response DTOs
```

## Components

### 1. ApiKey Entity

- Extends `BaseDomain` (uid, created_at, updated_at)
- Stores SHA-256 hash (never plain key)
- Tracks usage (last_used_at, usage_count)
- Supports expiration and revocation
- Scoped permissions (APP_UPDATES, READ_ONLY, FULL_ADMIN)

### 2. ApiKeyService

**Key Generation:**
- Format: `amp_1a2b3c4d_5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t`
- Prefix: 8 random alphanumeric chars (for identification)
- Secret: 40 random alphanumeric chars
- Hash: SHA-256 of full key

**Validation:**
- Format check (regex)
- Hash lookup in database
- Status check (active, not expired, not revoked)
- Usage tracking update

### 3. ApiKeyAuthenticationFilter

- Intercepts requests with `X-API-Key` header
- Validates key via service
- Creates Spring Security authentication
- Authorities: `API_KEY` + `API_KEY:{scope}`

### 4. ApiKeyController

Admin-only endpoints for key management:
- `POST /api/v1/admin/api-keys` - Create key
- `GET /api/v1/admin/api-keys` - List keys
- `GET /api/v1/admin/api-keys/{uid}` - Get key details
- `PATCH /api/v1/admin/api-keys/{uid}/revoke` - Revoke key
- `DELETE /api/v1/admin/api-keys/{uid}` - Delete key

## Security

### Storage
- ✅ Plain key never stored
- ✅ SHA-256 hash stored in database
- ✅ Prefix stored for identification (doesn't reveal key)
- ✅ Key shown only once during creation

### Validation
- ✅ Active status check
- ✅ Expiration check
- ✅ Revocation check
- ✅ Scope-based authorization

### Monitoring
- ✅ Usage tracking (count, last used)
- ✅ Audit trail (created_by, revoked_by)
- ✅ Revocation reason logging

## Usage

### Creating an API Key (Admin)

**Via API:**
```bash
curl -X POST https://api.ampairs.in/api/v1/admin/api-keys \
  -H "Authorization: Bearer $ADMIN_JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "GitHub Actions - App Updates",
    "description": "CI/CD pipeline for desktop releases",
    "scope": "APP_UPDATES",
    "expires_in_days": 365
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "uid": "KEY-xxx",
    "name": "GitHub Actions - App Updates",
    "api_key": "amp_1a2b3c4d_5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t",
    "key_prefix": "amp_1a2b3c4d",
    "scope": "APP_UPDATES",
    "expires_at": "2026-11-10T10:00:00Z",
    "warning": "⚠️ Store this API key securely. It will not be shown again!"
  }
}
```

### Using API Key

**In HTTP Requests:**
```bash
curl -X POST https://api.ampairs.in/api/v1/app-updates \
  -H "X-API-Key: amp_1a2b3c4d_5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

**In GitHub Actions:**
```yaml
- name: Publish app update
  env:
    AMPAIRS_API_KEY: ${{ secrets.AMPAIRS_API_KEY }}
  run: |
    curl -X POST https://api.ampairs.in/api/v1/app-updates \
      -H "X-API-Key: $AMPAIRS_API_KEY" \
      -H "Content-Type: application/json" \
      -d '{...}'
```

### Listing API Keys

```bash
curl -X GET https://api.ampairs.in/api/v1/admin/api-keys \
  -H "Authorization: Bearer $ADMIN_JWT"
```

### Revoking API Key

```bash
curl -X PATCH https://api.ampairs.in/api/v1/admin/api-keys/{uid}/revoke \
  -H "Authorization: Bearer $ADMIN_JWT" \
  -d "reason=Key compromised"
```

## Authorization

### Spring Security Integration

API keys work alongside JWT authentication. Use `@PreAuthorize` with authorities:

```kotlin
@PostMapping("/api/v1/app-updates")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('API_KEY:APP_UPDATES')")
fun createVersion(...) {
    // Both JWT admin and API keys with APP_UPDATES scope can access
}
```

### Authority Mapping

| Scope | Authority | Description |
|-------|-----------|-------------|
| APP_UPDATES | `API_KEY:APP_UPDATES` | Can manage app versions |
| READ_ONLY | `API_KEY:READ_ONLY` | Read-only access |
| FULL_ADMIN | `API_KEY:FULL_ADMIN` | Full admin access |

All API keys also get `API_KEY` authority.

## Best Practices

### Key Management

1. **Use Descriptive Names**: "GitHub Actions - App Updates", not "key1"
2. **Set Expiration**: Annual rotation recommended
3. **Minimal Scope**: Use `APP_UPDATES` not `FULL_ADMIN`
4. **Revoke Unused Keys**: Clean up regularly

### Security

1. **Store Securely**: Use secrets management (GitHub Secrets, Vault)
2. **Never Commit**: Add to .gitignore
3. **Rotate Regularly**: Annual or when staff changes
4. **Monitor Usage**: Track usage_count and last_used_at
5. **Revoke on Compromise**: Immediate revocation capability

### Monitoring

```bash
# Check key usage
curl https://api.ampairs.in/api/v1/admin/api-keys/{uid} \
  -H "Authorization: Bearer $ADMIN_JWT"

# Look for:
# - Unusual usage patterns
# - Keys not used in 30+ days
# - High usage_count (potential abuse)
```

## Troubleshooting

### Key Not Working

1. **Check format**: Must match `amp_[a-z0-9]{8}_[a-z0-9]{40}`
2. **Check status**: May be revoked or expired
3. **Check scope**: Must have required authority
4. **Check backend logs**: Look for authentication errors

### 401 Unauthorized

- Key is invalid or expired
- Key format is incorrect
- Key not in database

### 403 Forbidden

- Key is valid but lacks required scope
- Endpoint requires `APP_UPDATES` but key has `READ_ONLY`

## Database Schema

```sql
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,

    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    key_hash VARCHAR(64) NOT NULL UNIQUE,    -- SHA-256 hash
    key_prefix VARCHAR(20) NOT NULL,         -- For identification

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP,

    last_used_at TIMESTAMP,
    usage_count BIGINT NOT NULL DEFAULT 0,

    scope VARCHAR(50) NOT NULL,              -- APP_UPDATES, READ_ONLY, FULL_ADMIN

    created_by_user_id VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    revoked_at TIMESTAMP,
    revoked_by VARCHAR(100),
    revoked_reason VARCHAR(500)
);
```

## Migration

Migration: `V1.0.19__create_api_keys_table.sql`

Apply via Flyway on application startup.

## Testing

### Unit Tests

Test key generation:
```kotlin
@Test
fun `should generate valid API key format`() {
    val (plainKey, prefix, hash) = apiKeyService.generateApiKey()

    assertTrue(plainKey.startsWith("amp_"))
    assertEquals(8, prefix.length - 4)  // minus "amp_"
    assertEquals(64, hash.length)       // SHA-256 = 64 hex chars
}
```

### Integration Tests

Test authentication:
```kotlin
@Test
fun `should authenticate with valid API key`() {
    val key = createTestApiKey()

    mockMvc.perform(post("/api/v1/app-updates")
        .header("X-API-Key", key)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""..."""))
        .andExpect(status().isOk())
}
```

## Performance

- **Hash lookup**: Indexed on `key_hash` (fast)
- **Usage update**: Single UPDATE per request
- **Memory**: Filter instantiated once, minimal overhead

## Related Documentation

- [API_KEY_AUTHENTICATION.md](../../../../../API_KEY_AUTHENTICATION.md) - Complete setup guide
- [CICD_APP_UPDATES.md](../../../../../CICD_APP_UPDATES.md) - CI/CD pipeline docs
- [App Update API](../appupdate/README.md) - Protected by API keys

---

**Status:** ✅ Implementation Complete

All components implemented and ready for testing.
