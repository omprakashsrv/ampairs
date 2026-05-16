# core module

Shared infrastructure consumed by every other module. Contains no business logic — only primitives, config, and cross-cutting concerns.

## Responsibilities

- Base entity classes (`BaseDomain`, `OwnableBaseDomain`)
- Shared DTOs (`ApiResponse<T>`, `PageResponse<T>`, `Address`)
- Multi-tenancy infrastructure (`TenantContextHolder`, `CurrentTenantIdentifierResolver`)
- Global exception handling (`GlobalExceptionHandler`, `BaseExceptionHandler`)
- Security filters (XSS protection, SQL injection prevention)
- API key authentication (filter, provider, token, repository)
- App version / update management (entity, service, S3 streaming)
- Rate limiting service (Bucket4J integration)
- Utility functions (UUID generation, time utils, validation helpers)

## Key Classes

### Base Entities

```kotlin
abstract class BaseDomain {
    @Id val uid: String          // prefixed nanoid e.g. "PRD_xxxx"
    val createdAt: Instant       // UTC
    val updatedAt: Instant       // UTC
    val deleted: Boolean         // soft delete flag
}

abstract class OwnableBaseDomain : BaseDomain() {
    @TenantId val ownerId: String   // binds row to current tenant
    val workspaceId: String         // workspace reference
}
```

### Shared DTOs

```kotlin
// All endpoints return this
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorDetails?,
    val timestamp: Instant,
    val path: String?,
    val traceId: String?
)

// Paginated lists
data class PageResponse<T>(
    val content: List<T>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

// Shared address structure
data class Address(
    val street: String?,
    val street2: String?,
    val city: String?,
    val state: String?,
    val pincode: String?,
    val country: String?
)
```

### Multi-Tenancy

```kotlin
// Set before any repository access (in controller)
TenantContextHolder.setCurrentTenant(workspaceId)

// Clear in finally block
TenantContextHolder.clear()

// Device context (for multi-device session tracking)
DeviceContextHolder.setCurrentDevice(deviceId)
```

### Exceptions

| Exception | HTTP Status | Use case |
|-----------|------------|---------|
| `BusinessException` | 400 | Invalid business rule |
| `NotFoundException` | 404 | Entity not found |
| `RateLimitExceededException` | 429 | Rate limit hit |
| `RecaptchaValidationException` | 400 | Bad reCAPTCHA token |

All exceptions bubble to `GlobalExceptionHandler` which wraps them in `ApiResponse`.

### API Key Authentication

Workspace-scoped API keys for machine-to-machine access. Endpoints:

| Method | Path | Description |
|--------|------|-------------|
| POST | `/core/v1/api-keys` | Create API key |
| GET | `/core/v1/api-keys` | List API keys |
| DELETE | `/core/v1/api-keys/{keyId}` | Revoke API key |

### App Update Management

Tracks desktop app versions per platform for forced/optional update prompts.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/core/v1/app-update/check` | Check for update (client) |
| POST | `/core/v1/app-update/versions` | Create version (admin) |
| GET | `/core/v1/app-update/versions` | List versions (admin) |

Platforms: `MACOS`, `WINDOWS`, `LINUX`

### Security Filters (applied to all requests)

- `XssProtectionFilter` — strips XSS payloads from request parameters
- `SqlInjectionProtectionFilter` — rejects SQL injection patterns
- `TraceIdFilter` — injects `X-Trace-Id` header for log correlation
- `SecurityValidationInterceptor` — validates content-type and headers

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.2__create_core_tables.sql` | Core infrastructure tables |
| `V1.0.17__create_app_versions_table.sql` | App version tracking |
| `V1.0.18__migrate_app_versions_to_s3_streaming.sql` | S3 streaming for large files |
| `V1.0.19__create_api_keys_table.sql` | API key authentication |

## Package Structure

```
com.ampairs.core
├── appupdate/      — app version entity, service, S3 streaming, controller
├── auth/           — API key domain, filter, provider, repository, service
├── config/         — Jackson, security, validation config
├── domain/
│   ├── dto/        — ApiResponse, PageResponse, Address, Error
│   ├── enums/      — VerificationStatus
│   └── model/      — BaseDomain, OwnableBaseDomain, AbstractIdVerification
├── exception/      — GlobalExceptionHandler, typed exceptions
├── filter/         — XSS, SQL injection filters
├── interceptor/    — Security validation
├── logging/        — TraceIdFilter
├── multitenancy/   — TenantContextHolder, DeviceContextHolder, config
├── security/       — AdminService, AuthenticationHelper
├── service/        — RateLimitingService, SecurityAuditService, ValidationService
├── utils/          — UniqueIdGenerator, TimeUtils, Helper
└── validation/     — Custom validation annotations
```
