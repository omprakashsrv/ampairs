## API Key Authentication for CI/CD

Since Ampairs uses JWT authentication based on phone numbers, we need a separate authentication mechanism for automated systems like GitHub Actions.

## Solution: API Key Authentication

API keys provide machine-to-machine authentication without requiring phone/OTP login.

---

## 🔑 API Key Format

```
amp_1a2b3c4d_5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t
│   │        │
│   │        └─ Secret portion (40 chars)
│   └────────── Prefix for identification (8 chars)
└────────────── Namespace (amp)
```

**Security:**
- Only SHA-256 hash stored in database
- Key shown only once during creation
- Cannot be recovered if lost

---

## 📋 Setup Instructions

### Step 1: Add API Key Support to Spring Security

Update your `SecurityConfig.kt` to allow API key authentication:

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/api/v1/app-updates/check").permitAll()
                    .requestMatchers("/api/v1/app-updates/download/**").permitAll()

                    // API key authenticated endpoints
                    .requestMatchers(HttpMethod.POST, "/api/v1/app-updates").hasAuthority("API_KEY:APP_UPDATES")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/app-updates/**").hasAuthority("API_KEY:APP_UPDATES")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/app-updates/**").hasAuthority("API_KEY:APP_UPDATES")

                    // Admin endpoints (JWT required)
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                    .anyRequest().authenticated()
            }
            .addFilterBefore(apiKeyAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun apiKeyAuthenticationFilter(): ApiKeyAuthenticationFilter {
        return ApiKeyAuthenticationFilter(apiKeyService)
    }
}
```

### Step 2: Create API Key Authentication Filter

```kotlin
@Component
class ApiKeyAuthenticationFilter(
    private val apiKeyService: ApiKeyService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Check for API key in header
        val apiKey = request.getHeader("X-API-Key")

        if (apiKey != null && apiKey.startsWith("amp_")) {
            try {
                // Validate and authenticate
                val keyInfo = apiKeyService.validateAndUse(apiKey)

                // Set authentication in security context
                val authorities = listOf(
                    SimpleGrantedAuthority("API_KEY:${keyInfo.scope}")
                )

                val authentication = PreAuthenticatedAuthenticationToken(
                    "api-key:${keyInfo.uid}",
                    null,
                    authorities
                )

                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: Exception) {
                logger.warn("Invalid API key", e)
                // Continue without authentication
            }
        }

        filterChain.doFilter(request, response)
    }
}
```

### Step 3: Create API Key Management Endpoints

```kotlin
@RestController
@RequestMapping("/api/v1/admin/api-keys")
@PreAuthorize("hasRole('ADMIN')")
class ApiKeyController(
    private val apiKeyService: ApiKeyService
) {

    @PostMapping
    fun createApiKey(@RequestBody request: CreateApiKeyRequest): ApiResponse<ApiKeyCreationResponse> {
        val result = apiKeyService.createApiKey(request)
        return ApiResponse.success(result)
    }

    @GetMapping
    fun listApiKeys(): ApiResponse<List<ApiKeyResponse>> {
        val keys = apiKeyService.listApiKeys()
        return ApiResponse.success(keys)
    }

    @DeleteMapping("/{uid}")
    fun revokeApiKey(
        @PathVariable uid: String,
        @RequestParam reason: String
    ): ApiResponse<Map<String, String>> {
        apiKeyService.revokeApiKey(uid, reason)
        return ApiResponse.success(mapOf("message" to "API key revoked"))
    }
}

data class CreateApiKeyRequest(
    val name: String,
    val description: String? = null,
    val scope: ApiKeyScope = ApiKeyScope.APP_UPDATES,
    val expiresInDays: Int? = null  // null = never expires
)

data class ApiKeyCreationResponse(
    val uid: String,
    val name: String,
    val apiKey: String,  // ONLY RETURNED ONCE!
    val keyPrefix: String,
    val scope: String,
    val expiresAt: Instant?,
    val warning: String = "Store this key securely. It will not be shown again."
)

data class ApiKeyResponse(
    val uid: String,
    val name: String,
    val keyPrefix: String,  // e.g., "amp_1a2b3c4d"
    val scope: String,
    val isActive: Boolean,
    val expiresAt: Instant?,
    val lastUsedAt: Instant?,
    val usageCount: Long,
    val createdAt: Instant
)
```

---

## 🚀 Creating an API Key for GitHub Actions

### Using Admin Web UI (Recommended)

1. Login as admin at https://ampairs.in/admin
2. Navigate to **Settings** → **API Keys**
3. Click **Create API Key**
4. Fill in details:
   ```
   Name: GitHub Actions - App Updates
   Description: Automated releases from GitHub CI/CD
   Scope: APP_UPDATES
   Expires: 365 days (or never)
   ```
5. Click **Create**
6. **COPY THE KEY IMMEDIATELY** (shown only once):
   ```
   amp_1a2b3c4d_5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t
   ```

### Using API (CLI)

```bash
# Login as admin first to get JWT token
TOKEN=$(curl -X POST https://api.ampairs.in/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone": "your-admin-phone", "otp": "123456"}' \
  | jq -r '.data.token')

# Create API key
API_KEY_RESPONSE=$(curl -X POST https://api.ampairs.in/api/v1/admin/api-keys \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "GitHub Actions - App Updates",
    "description": "Automated releases from GitHub CI/CD",
    "scope": "APP_UPDATES",
    "expires_in_days": 365
  }')

# Extract API key
API_KEY=$(echo $API_KEY_RESPONSE | jq -r '.data.api_key')
echo "API Key: $API_KEY"
echo "⚠️  Store this securely! It will not be shown again."
```

---

## 🔧 Configure GitHub Actions

### 1. Add API Key to GitHub Secrets

1. Go to your GitHub repository
2. Settings → Secrets and variables → Actions
3. Click **New repository secret**
4. Name: `AMPAIRS_API_KEY`
5. Value: `amp_1a2b3c4d_5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t`
6. Click **Add secret**

### 2. Update GitHub Actions Workflow

```yaml
# .github/workflows/release-desktop-app.yml

- name: Register version in database
  run: |
    curl -X POST "${{ env.API_BASE_URL }}/api/v1/app-updates" \
      -H "X-API-Key: ${{ secrets.AMPAIRS_API_KEY }}" \
      -H "Content-Type: application/json" \
      -d @- <<EOF
    {
      "version": "${{ steps.version.outputs.version }}",
      "version_code": ${{ steps.version.outputs.version_code }},
      "platform": "${{ matrix.platform }}",
      "s3_key": "${{ steps.artifact.outputs.s3_key }}",
      "filename": "${{ steps.artifact.outputs.filename }}",
      "file_size_mb": ${{ steps.filesize.outputs.size_mb }},
      "checksum": "${{ steps.checksum.outputs.checksum }}",
      "release_date": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
    }
    EOF
```

**Key Change:** Use `X-API-Key` header instead of `Authorization: Bearer` token.

---

## 🔒 Security Best Practices

### API Key Storage

✅ **DO:**
- Store in GitHub Secrets (encrypted at rest)
- Use environment variables in CI/CD
- Rotate keys regularly (annually)
- Use least-privilege scope

❌ **DON'T:**
- Commit keys to Git
- Share keys via email/Slack
- Use same key for multiple systems
- Store in plain text files

### Monitoring

**Track usage:**
```bash
# List all API keys
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://api.ampairs.in/api/v1/admin/api-keys

# Response shows usage:
{
  "data": [
    {
      "name": "GitHub Actions",
      "key_prefix": "amp_1a2b3c4d",
      "last_used_at": "2025-11-10T08:30:00Z",
      "usage_count": 142
    }
  ]
}
```

**Set up alerts:**
- Unusual usage patterns
- Keys not used in 30+ days
- Keys approaching expiry
- Failed authentication attempts

### Rotation

Rotate API keys annually or when:
- Team member leaves
- Key might be compromised
- Compliance requirements

**Rotation process:**
1. Create new API key
2. Update GitHub Secrets
3. Verify new key works
4. Revoke old key
5. Monitor for errors

---

## 🧪 Testing API Key Authentication

### 1. Test API Key Locally

```bash
# Set environment variable
export AMPAIRS_API_KEY="amp_1a2b3c4d_..."

# Test authentication
curl -X GET https://api.ampairs.in/api/v1/app-updates \
  -H "X-API-Key: $AMPAIRS_API_KEY"

# Expected: 200 OK with version list
```

### 2. Test in GitHub Actions

Create a test workflow:

```yaml
name: Test API Key
on: workflow_dispatch

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Test API Key
        run: |
          curl -X GET https://api.ampairs.in/api/v1/app-updates \
            -H "X-API-Key: ${{ secrets.AMPAIRS_API_KEY }}" \
            -v
```

---

## 🔄 Alternative: OAuth2 Client Credentials (Future)

For more advanced use cases, consider implementing OAuth2 client credentials flow:

```yaml
# Future implementation
- name: Get access token
  run: |
    TOKEN=$(curl -X POST https://api.ampairs.in/oauth/token \
      -d "grant_type=client_credentials" \
      -d "client_id=${{ secrets.CLIENT_ID }}" \
      -d "client_secret=${{ secrets.CLIENT_SECRET }}" \
      -d "scope=app_updates" \
      | jq -r '.access_token')

    echo "::add-mask::$TOKEN"
    echo "ACCESS_TOKEN=$TOKEN" >> $GITHUB_ENV
```

---

## 📝 Summary

| Method | Use Case | Pros | Cons |
|--------|----------|------|------|
| **API Keys** | CI/CD, integrations | Simple, fast, no expiry hassles | Less granular permissions |
| **JWT (Phone)** | User authentication | Strong, mobile-friendly | Not suitable for automation |
| **OAuth2** | Third-party apps | Industry standard, granular | Complex to implement |

**Recommendation:** Use API keys for CI/CD pipelines.

---

## 🆘 Troubleshooting

### Error: 401 Unauthorized

```bash
# Check API key format
echo $AMPAIRS_API_KEY | grep -E '^amp_[a-z0-9]{8}_[a-z0-9]{40}$'

# Check key is active
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://api.ampairs.in/api/v1/admin/api-keys \
  | jq '.data[] | select(.key_prefix == "amp_1a2b3c4d")'
```

### Error: 403 Forbidden

```bash
# Check scope
# APP_UPDATES scope required for POST /api/v1/app-updates
```

### Key Not Working After Creation

- Wait 30 seconds for propagation
- Check backend logs for authentication errors
- Verify key wasn't revoked immediately

---

**Implementation Status:** ⏳ Pending

This requires:
1. ✅ Database schema (V1.0.19 migration created)
2. ⏳ API key entity and repository
3. ⏳ Authentication filter
4. ⏳ Admin endpoints for key management
5. ⏳ Updated GitHub Actions workflow

**Estimated time:** 2-3 hours development + testing
