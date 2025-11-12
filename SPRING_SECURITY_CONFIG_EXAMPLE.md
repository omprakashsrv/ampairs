# Spring Security Configuration with Multiple Authentication Providers

Example configuration for integrating API key authentication with your existing JWT authentication.

## Architecture

```
HTTP Request
     │
     ├─ Has X-API-Key header?
     │   │
     │   ├─ Yes → ApiKeyAuthenticationFilter
     │   │         │
     │   │         ├─ Create ApiKeyAuthenticationToken (unauthenticated)
     │   │         │
     │   │         └─ AuthenticationManager.authenticate()
     │                   │
     │                   ├─ ApiKeyAuthenticationProvider.supports()? Yes
     │                   │   └─ ApiKeyAuthenticationProvider.authenticate()
     │                   │       └─ ApiKeyService.validateAndUse()
     │                   │           └─ Returns authenticated token
     │                   │
     │                   └─ Set SecurityContext
     │
     └─ Has Authorization: Bearer header?
         │
         └─ Yes → JwtAuthenticationFilter
                   │
                   └─ (Your existing JWT flow)
```

## Configuration Class

Create this in your `workspace` or `ampairs_service` module:

```kotlin
package com.ampairs.config

import com.ampairs.core.auth.filter.ApiKeyAuthenticationFilter
import com.ampairs.core.auth.provider.ApiKeyAuthenticationProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Spring Security configuration with multiple authentication providers.
 *
 * Supports:
 * 1. JWT authentication (existing)
 * 2. API key authentication (new)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val apiKeyAuthenticationProvider: ApiKeyAuthenticationProvider,
    // Your existing JWT provider (if you have one)
    // private val jwtAuthenticationProvider: JwtAuthenticationProvider
) {

    /**
     * Configure AuthenticationManager with multiple providers.
     *
     * Spring Security will try each provider in order until one succeeds.
     */
    @Bean
    fun authenticationManager(): AuthenticationManager {
        return ProviderManager(
            listOf(
                apiKeyAuthenticationProvider,
                // Add your JWT provider here if you have one
                // jwtAuthenticationProvider
            )
        )
    }

    /**
     * Configure API key authentication filter.
     */
    @Bean
    fun apiKeyAuthenticationFilter(authenticationManager: AuthenticationManager): ApiKeyAuthenticationFilter {
        return ApiKeyAuthenticationFilter(authenticationManager)
    }

    /**
     * Configure security filter chain.
     */
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        apiKeyAuthenticationFilter: ApiKeyAuthenticationFilter,
        // Your existing JWT filter
        // jwtAuthenticationFilter: JwtAuthenticationFilter
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

            // Add authentication filters
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            // Add your JWT filter here
            // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

            // Authorization rules
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/api/v1/app-updates/check").permitAll()
                    .requestMatchers("/api/v1/app-updates/download/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()

                    // API key or admin can manage app versions
                    .requestMatchers(HttpMethod.POST, "/api/v1/app-updates")
                        .hasAnyAuthority("ROLE_ADMIN", "API_KEY:APP_UPDATES")

                    .requestMatchers(HttpMethod.PUT, "/api/v1/app-updates/**")
                        .hasAnyAuthority("ROLE_ADMIN", "API_KEY:APP_UPDATES")

                    .requestMatchers(HttpMethod.DELETE, "/api/v1/app-updates/**")
                        .hasAnyAuthority("ROLE_ADMIN", "API_KEY:APP_UPDATES")

                    // Admin-only endpoints
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                    // All other endpoints require authentication
                    .anyRequest().authenticated()
            }

        return http.build()
    }
}
```

## How It Works

### 1. Request with API Key

```bash
curl -X POST https://api.ampairs.in/api/v1/app-updates \
  -H "X-API-Key: amp_xxx..." \
  -H "Content-Type: application/json" \
  -d '{...}'
```

**Flow:**
1. `ApiKeyAuthenticationFilter` detects X-API-Key header
2. Creates `ApiKeyAuthenticationToken` (unauthenticated)
3. Calls `authenticationManager.authenticate(token)`
4. `AuthenticationManager` tries each provider:
   - `ApiKeyAuthenticationProvider.supports()`? Yes!
   - `ApiKeyAuthenticationProvider.authenticate()`
   - Validates via `ApiKeyService`
   - Returns authenticated token with authorities
5. Filter sets token in SecurityContext
6. Request proceeds to controller
7. `@PreAuthorize("hasAuthority('API_KEY:APP_UPDATES')")` succeeds ✅

### 2. Request with JWT

```bash
curl -X POST https://api.ampairs.in/api/v1/app-updates \
  -H "Authorization: Bearer eyJhbG..." \
  -H "Content-Type: application/json" \
  -d '{...}'
```

**Flow:**
1. `JwtAuthenticationFilter` detects Authorization header
2. Validates JWT and sets authentication
3. Request proceeds to controller
4. `@PreAuthorize("hasRole('ADMIN')")` succeeds ✅

### 3. Request with Neither

```bash
curl -X POST https://api.ampairs.in/api/v1/app-updates \
  -H "Content-Type: application/json" \
  -d '{...}'
```

**Flow:**
1. No authentication filters trigger
2. SecurityContext remains empty
3. Controller requires authentication
4. Spring Security returns 401 Unauthorized ❌

## Key Benefits

### 1. **Proper Spring Security Integration**
- Uses `AuthenticationProvider` interface
- Integrates with `AuthenticationManager`
- Follows Spring Security best practices

### 2. **Multiple Authentication Methods**
- API keys for CI/CD
- JWT for user sessions
- Easy to add more providers

### 3. **Flexible Authorization**
```kotlin
// Allow either authentication method
@PreAuthorize("hasRole('ADMIN') or hasAuthority('API_KEY:APP_UPDATES')")
fun createVersion(...) { }

// Admin only (JWT)
@PreAuthorize("hasRole('ADMIN')")
fun deleteUser(...) { }

// API key only
@PreAuthorize("hasAuthority('API_KEY:APP_UPDATES')")
fun cicdOperation(...) { }
```

### 4. **Testable**
```kotlin
@Test
fun `should authenticate with API key`() {
    val token = ApiKeyAuthenticationToken("amp_test_...")
    val result = apiKeyAuthenticationProvider.authenticate(token)

    assertTrue(result.isAuthenticated)
    assertTrue(result.authorities.any { it.authority == "API_KEY:APP_UPDATES" })
}
```

## Migration from Old Approach

### Old (Manual SecurityContext)
```kotlin
// ❌ Don't do this
class ApiKeyAuthenticationFilter(
    private val apiKeyService: ApiKeyService
) : OncePerRequestFilter() {
    override fun doFilterInternal(...) {
        val keyInfo = apiKeyService.validateAndUse(apiKey)
        val auth = UsernamePasswordAuthenticationToken(...)
        SecurityContextHolder.getContext().authentication = auth
    }
}
```

**Problems:**
- Bypasses AuthenticationManager
- No provider chain
- Hard to test
- Doesn't follow Spring Security patterns

### New (AuthenticationManager + Provider)
```kotlin
// ✅ Do this
class ApiKeyAuthenticationFilter(
    private val authenticationManager: AuthenticationManager
) : OncePerRequestFilter() {
    override fun doFilterInternal(...) {
        val authRequest = ApiKeyAuthenticationToken(apiKey)
        val authResult = authenticationManager.authenticate(authRequest)
        SecurityContextHolder.getContext().authentication = authResult
    }
}

@Component
class ApiKeyAuthenticationProvider(
    private val apiKeyService: ApiKeyService
) : AuthenticationProvider {
    override fun authenticate(auth: Authentication): Authentication {
        // Validation logic here
    }
}
```

**Benefits:**
- Proper Spring Security integration
- Testable providers
- Provider chain support
- Consistent with JWT authentication

## Testing

### Unit Test Provider

```kotlin
@ExtendWith(MockitoExtension::class)
class ApiKeyAuthenticationProviderTest {

    @Mock
    lateinit var apiKeyService: ApiKeyService

    @InjectMocks
    lateinit var provider: ApiKeyAuthenticationProvider

    @Test
    fun `should authenticate valid API key`() {
        val apiKey = "amp_test_1234567890..."
        val mockKeyInfo = mockk<ApiKey> {
            every { uid } returns "KEY-123"
            every { scope } returns ApiKeyScope.APP_UPDATES
        }

        `when`(apiKeyService.validateAndUse(apiKey)).thenReturn(mockKeyInfo)

        val token = ApiKeyAuthenticationToken(apiKey)
        val result = provider.authenticate(token) as ApiKeyAuthenticationToken

        assertTrue(result.isAuthenticated)
        assertEquals(2, result.authorities.size)
        assertTrue(result.authorities.any { it.authority == "API_KEY:APP_UPDATES" })
    }

    @Test
    fun `should throw BadCredentialsException for invalid key`() {
        val apiKey = "invalid_key"

        `when`(apiKeyService.validateAndUse(apiKey))
            .thenThrow(IllegalArgumentException("Invalid key"))

        val token = ApiKeyAuthenticationToken(apiKey)

        assertThrows<BadCredentialsException> {
            provider.authenticate(token)
        }
    }
}
```

### Integration Test

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class ApiKeyAuthenticationIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var apiKeyService: ApiKeyService

    @Test
    fun `should access endpoint with valid API key`() {
        // Create test key
        val response = apiKeyService.createApiKey(
            CreateApiKeyRequest("Test Key", scope = ApiKeyScope.APP_UPDATES),
            "test-user"
        )

        // Use key to access protected endpoint
        mockMvc.perform(
            post("/api/v1/app-updates")
                .header("X-API-Key", response.apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"version": "1.0.0.10", ...}""")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `should return 401 for invalid API key`() {
        mockMvc.perform(
            post("/api/v1/app-updates")
                .header("X-API-Key", "amp_invalid_key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"version": "1.0.0.10", ...}""")
        )
            .andExpect(status().isUnauthorized)
    }
}
```

## Troubleshooting

### Provider Not Found

**Error:** `No AuthenticationProvider found for ApiKeyAuthenticationToken`

**Fix:** Ensure provider is registered in AuthenticationManager:
```kotlin
@Bean
fun authenticationManager(): AuthenticationManager {
    return ProviderManager(listOf(apiKeyAuthenticationProvider))
}
```

### Filter Not Working

**Error:** Filter doesn't intercept requests

**Fix:** Ensure filter is added before `UsernamePasswordAuthenticationFilter`:
```kotlin
http.addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
```

### Circular Dependency

**Error:** Circular dependency between SecurityConfig and AuthenticationManager

**Fix:** Use constructor injection and let Spring resolve:
```kotlin
@Configuration
class SecurityConfig(
    private val apiKeyAuthenticationProvider: ApiKeyAuthenticationProvider
) {
    @Bean
    fun authenticationManager() = ProviderManager(listOf(apiKeyAuthenticationProvider))
}
```

## Summary

| Component | Responsibility |
|-----------|----------------|
| `ApiKeyAuthenticationToken` | Represents authentication request/result |
| `ApiKeyAuthenticationProvider` | Validates API key and creates authorities |
| `ApiKeyAuthenticationFilter` | Extracts key and delegates to manager |
| `AuthenticationManager` | Coordinates multiple providers |
| `SecurityConfig` | Wires everything together |

This approach follows Spring Security best practices and integrates seamlessly with your existing JWT authentication!
