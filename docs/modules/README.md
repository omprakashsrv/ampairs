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
| [setting](setting.md) | `/setting/v1/settings/` | Central workspace settings registry (cross-module toggles, offline-sync) |
| [form](form.md) | `/form/v1/` | Dynamic field config and custom attributes |
| [file](file.md) | `/files/` | Object storage — S3, MinIO, local |
| [user](user.md) | `/user/v1/` | User identity, profile, profile picture, account deletion |
| [supplier](supplier.md) | `/supplier/v1/` | Supplier management |
| [purchase](purchase.md) | `/purchase/v1/purchases/` | Purchase recording (bills + purchase items) |
| [payment](payment.md) | `/payment/v1/` | Payment recording and allocation |
| [pricing](pricing.md) | `/pricing/v1/` | Price lists, tiers, offers/coupons, geo zones |
| [sequence](sequence.md) | `/sequence/v1/` | Document number sequences — definitions + allocations |
| [ecom](ecom.md) | `/api/v1/store/{slug}`, `/api/v1/ecom/`, `/api/v1/storefronts` | Storefronts — public catalog, cart, checkout, buyer accounts |
| [printing](printing.md) | `/printing/v1/templates/` | Print-template storage + offline-sync (opaque layout JSON) |
| [agent](agent.md) | `/agent/v1/` | AI model manifest/download proxy + chat telemetry for the on-device assistant |

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
