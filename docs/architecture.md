# Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        Clients                              │
│  ampairs-web (Angular 20)  │  ampairs-app (Compose KMP)    │
└────────────────────┬────────────────────┬───────────────────┘
                     │  REST + JWT         │  WebSocket (STOMP)
                     ▼                     ▼
┌─────────────────────────────────────────────────────────────┐
│              ampairs_service  (Spring Boot 3.5)             │
│                                                             │
│  ┌──────┐ ┌──────┐ ┌───────────┐ ┌────────┐ ┌──────────┐  │
│  │ auth │ │ core │ │ workspace │ │product │ │  order   │  │
│  ├──────┤ ├──────┤ ├───────────┤ ├────────┤ ├──────────┤  │
│  │  tax │ │ unit │ │ customer  │ │invoice │ │  event   │  │
│  ├──────┤ ├──────┤ ├───────────┤ ├────────┤ ├──────────┤  │
│  │ form │ │ file │ │ business  │ │notifcn │ │ subscptn │  │
│  └──────┘ └──────┘ └───────────┘ └────────┘ └──────────┘  │
└──────────────────────────┬──────────────────────────────────┘
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
           MySQL 8                  RabbitMQ
        (Flyway migrations)     (STOMP WebSocket relay)
```

## Module Dependency Rules

```
ampairs_service  →  all domain modules (aggregator only)
domain modules   →  core  (shared infrastructure)
domain modules   ✗  other domain modules (no cross-module imports)
cross-module     →  through public service interfaces only
```

## Request Lifecycle

```
HTTP Request
  → SecurityValidationInterceptor (XSS, SQL injection scan)
  → ApiKeyAuthenticationFilter or JWT filter
  → SessionUserFilter (sets TenantContext from X-Workspace-ID header)
  → RateLimitingInterceptor (Bucket4J)
  → Controller
    → Service
      → Repository (auto-filtered by @TenantId)
      → domain events published
  → GlobalExceptionHandler (on error)
  ← ApiResponse<T> wrapper
```

## Multi-Tenancy Model

Every workspace-scoped request sets a tenant context from the `X-Workspace-ID` header. All entities extending `OwnableBaseDomain` are automatically filtered by this context:

```kotlin
abstract class OwnableBaseDomain : BaseDomain() {
    @TenantId
    var ownerId: String = ""       // bound to TenantContextHolder
    var workspaceId: String = ""   // workspace isolation
}
```

The tenant context is set in `SessionUserFilter` after JWT verification, and must be manually overridden (with try/finally) only in controllers that need cross-tenant access.

## Base Entities

```kotlin
// All entities
abstract class BaseDomain {
    @Id val uid: String              // prefixed nanoid (e.g. PRD_xxxx)
    val createdAt: Instant           // UTC
    val updatedAt: Instant           // UTC
    val deleted: Boolean             // soft delete flag
}

// Workspace-scoped entities
abstract class OwnableBaseDomain : BaseDomain() {
    @TenantId val ownerId: String    // tenant identifier
    val workspaceId: String          // workspace reference
}
```

## API Response Envelope

All endpoints return:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2025-01-15T10:00:00Z",
  "path": "/product/v1/products",
  "trace_id": "abc123"
}
```

Paginated responses wrap data in:

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page_number": 0,
    "page_size": 20,
    "total_elements": 150,
    "total_pages": 8,
    "first": true,
    "last": false,
    "has_next": true,
    "has_previous": false
  }
}
```

## Security Layers

| Layer | Mechanism | Scope |
|-------|-----------|-------|
| Transport | HTTPS | All traffic |
| Authentication | JWT (RSA-signed) + API Key | All protected endpoints |
| Rate Limiting | Bucket4J (token bucket) | Auth endpoints |
| reCAPTCHA v3 | Google reCAPTCHA | Login, OTP |
| Input sanitization | XSS + SQL injection filters | All requests |
| Tenant isolation | @TenantId auto-filtering | All data queries |
| Account lockout | Failed attempt tracking | Auth endpoints |

## WebSocket / Real-time Events

The `event` module captures domain events from all modules and delivers them to clients over WebSocket:

```
Domain Service → ApplicationEvent → EventCaptureService
                                         ↓
                                   event_log table
                                         ↓
                              WebSocket STOMP relay (RabbitMQ or in-memory)
                                         ↓
                                     Client
```

Broker configuration (via `WEBSOCKET_BROKER_TYPE`):
- `SIMPLE` — in-memory, single instance
- `RABBITMQ` — external STOMP relay, production
- `AUTO` — detects RabbitMQ, falls back to SIMPLE

## Payment Integrations

The `subscription` module integrates with:

| Provider | Market | Use case |
|----------|--------|---------|
| Google Play Billing | Android | In-app purchases |
| Apple App Store | iOS | In-app purchases |
| Razorpay | India | Web payments |
| Stripe | International | Web payments |

## Storage

Files are stored in AWS S3 via the `file` module. Path convention:
```
{workspace_slug}/{entity_type}/{entity_uid}/{file_uid}.{ext}
```

## Notification Channels

The `notification` module supports:

| Channel | Provider | Status |
|---------|----------|--------|
| SMS | MSG91 (primary), AWS SNS (fallback) | Active |
| Push | Firebase FCM | Active |
| Email | SMTP | Planned |
| WhatsApp | Twilio | Planned |
