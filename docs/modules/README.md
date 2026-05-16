# Module Documentation

Each module is an independent Spring Boot domain bounded context. All share `core` infrastructure but never import directly from each other.

## Module Overview

| Module | Base Path | Purpose |
|--------|-----------|---------|
| [core](core.md) | `/api/v1/` | Shared infrastructure — base entities, API keys, app updates, exception handling |
| [auth](auth.md) | `/auth/v1/`, `/user/v1/` | Authentication, JWT, OTP, sessions, user profiles |
| [workspace](workspace.md) | `/workspace/v1/` | Tenants, members, teams, invitations, modules, RBAC |
| [product](product.md) | `/product/v1/`, `/inventory/v1/` | Product catalog, variants, multi-warehouse inventory |
| [subscription](subscription.md) | `/api/v1/subscriptions/`, `/api/v1/subscription/` | Plans, billing, payments, invoices |
| [customer](customer.md) | `/customer/v1/` | CRM, customer groups, types, images |
| [tax](tax.md) | `/tax/v1/` | GST config, HSN/SAC codes, tax rules |
| [business](business.md) | `/api/v1/business` | Business profile, logo, gallery |
| [order](order.md) | `/order/v1/` | Order lifecycle, line items, tax and discount breakdowns |
| [invoice](invoice.md) | `/invoice/v1/` | GST-compliant invoices, payment status |
| [event](event.md) | `/api/v1/events/`, WebSocket | Domain event streaming, device presence |
| [notification](notification.md) | `/notification/v1/` | SMS, push notifications, retry queue |
| [unit](unit.md) | `/api/v1/unit/` | Units of measure and conversions |
| [form](form.md) | `/form/v1/` | Dynamic field config and custom attributes |
| [file](file.md) | `/files/` | Object storage — S3, MinIO, local |

## Dependency Rules

```
ampairs_service → all modules    (aggregator)
any module      → core           (shared infra)
any module      ✗ other modules  (no cross-module imports)
```

Cross-module communication uses Spring `ApplicationEvent` (for domain events) or goes through `ampairs_service` service orchestration.

## Common Patterns Across All Modules

- Entities extend `BaseDomain` or `OwnableBaseDomain`
- All timestamps: `java.time.Instant` (UTC)
- All endpoints return `ApiResponse<T>` wrapper
- Tenant context set at controller level via `X-Workspace-ID` header
- DTOs in `domain/dto/` — entities never exposed in API responses
- Exceptions bubble to `GlobalExceptionHandler` in `core`
