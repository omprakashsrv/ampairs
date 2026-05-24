# API URL Migration Guide

## What changed and why

All API endpoints now follow a single consistent pattern:

```
/api/{module}/v{n}/{resource}
```

- `/api` — global prefix, configured once in `application.yml` via `spring.mvc.servlet.path`; never appears in controller `@RequestMapping`
- `/{module}` — domain module name (auth, workspace, product, …)
- `/v{n}` — version at the module level; modules can be versioned independently
- `/{resource}` — plural noun, kebab-case (e.g., `members`, `rate-limits`, `component-types`)

---

## Global config change

```yaml
# ampairs_service/src/main/resources/application.yml
spring:
  mvc:
    servlet:
      path: /api   # ← new — all controllers are now under /api automatically
```

No controller contains `/api` in its `@RequestMapping` anymore.

---

## Full endpoint mapping

### Auth module

| Old URL | New URL |
|---|---|
| `POST /auth/v1/init` | `POST /api/auth/v1/init` |
| `POST /auth/v1/verify` | `POST /api/auth/v1/verify` |
| `POST /auth/v1/verify/firebase` | `POST /api/auth/v1/verify/firebase` |
| `POST /auth/v1/refresh_token` | `POST /api/auth/v1/refresh_token` |
| `POST /auth/v1/logout` | `POST /api/auth/v1/logout` |
| `POST /auth/v1/logout/all` | `POST /api/auth/v1/logout/all` |
| `GET /auth/v1/session/{id}` | `GET /api/auth/v1/session/{id}` |
| `GET /auth/v1/devices` | `GET /api/auth/v1/devices` |
| `GET /auth/v1/devices/{deviceId}` | `GET /api/auth/v1/devices/{deviceId}` |
| `POST /auth/v1/devices/{deviceId}/logout` | `POST /api/auth/v1/devices/{deviceId}/logout` |
| `GET /auth/v1/keys/**` | `GET /api/auth/v1/keys/**` |
| `GET /auth/v1/lockout/**` | `GET /api/auth/v1/lockouts/**` *(plural)* |
| `POST /auth/v1/lockout/unlock` | `POST /api/auth/v1/lockouts/unlock` |
| `GET /core/v1/sessions/**` | `GET /api/auth/v1/sessions/**` *(moved to auth module)* |
| `GET /auth/v1/rate-limit/**` | `GET /api/auth/v1/rate-limits/**` *(plural)* |
| `GET /api/admin/token-cleanup/status` | `GET /api/auth/v1/admin/token-cleanup/status` *(versioned)* |
| `POST /api/admin/token-cleanup/run` | `POST /api/auth/v1/admin/token-cleanup/run` |

### User module

| Old URL | New URL |
|---|---|
| `GET /user/v1/**` | `GET /api/user/v1/**` *(no structural change)* |
| `GET /user/v1/invitation/pending` | `GET /api/user/v1/invitations/pending` *(plural)* |
| `POST /user/v1/invitation/{id}/accept` | `POST /api/user/v1/invitations/{id}/accept` |
| `POST /user/v1/invitation/{id}/reject` | `POST /api/user/v1/invitations/{id}/reject` |

### Workspace module

| Old URL | New URL |
|---|---|
| `GET /workspace/v1` | `GET /api/workspace/v1/workspaces` |
| `POST /workspace/v1` | `POST /api/workspace/v1/workspaces` |
| `GET /workspace/v1/{id}` | `GET /api/workspace/v1/workspaces/{id}` |
| `PUT /workspace/v1/{id}` | `PUT /api/workspace/v1/workspaces/{id}` |
| `DELETE /workspace/v1/{id}` | `DELETE /api/workspace/v1/workspaces/{id}` |
| `GET /workspace/v1/by-slug/{slug}` | `GET /api/workspace/v1/workspaces/by-slug/{slug}` |
| `GET /workspace/v1/check-slug/{slug}` | `GET /api/workspace/v1/workspaces/check-slug/{slug}` |
| `GET /workspace/v1/search` | `GET /api/workspace/v1/workspaces/search` |
| `GET /workspace/v1/member` | `GET /api/workspace/v1/members` *(plural)* |
| `GET /workspace/v1/member/{id}` | `GET /api/workspace/v1/members/{id}` |
| `PUT /workspace/v1/member/{id}` | `PUT /api/workspace/v1/members/{id}` |
| `DELETE /workspace/v1/member/{id}` | `DELETE /api/workspace/v1/members/{id}` |
| `GET /workspace/v1/invitation` | `GET /api/workspace/v1/invitations` *(plural)* |
| `POST /workspace/v1/invitation` | `POST /api/workspace/v1/invitations` |
| `DELETE /workspace/v1/invitation/{id}` | `DELETE /api/workspace/v1/invitations/{id}` |
| `GET /workspace/v1/modules` | `GET /api/workspace/v1/modules` |
| `GET /workspace/v1/workspaces/{wId}/teams` | `GET /api/workspace/v1/teams` *(removed redundant segment, workspaceId from header)* |
| `POST /workspace/v1/workspaces/{wId}/teams` | `POST /api/workspace/v1/teams` |
| `GET /workspace/v1/workspaces/{wId}/teams/{tId}` | `GET /api/workspace/v1/teams/{tId}` |
| `PUT /workspace/v1/workspaces/{wId}/teams/{tId}` | `PUT /api/workspace/v1/teams/{tId}` |
| `DELETE /workspace/v1/workspaces/{wId}/teams/{tId}` | `DELETE /api/workspace/v1/teams/{tId}` |

> **Teams endpoint**: `workspaceId` is no longer a path variable. It is read from the `X-Workspace-ID` header (same as all other workspace-scoped endpoints).

### Business module

| Old URL | New URL |
|---|---|
| `GET /api/v1/business` | `GET /api/business/v1/businesses` |
| `POST /api/v1/business` | `POST /api/business/v1/businesses` |
| `PUT /api/v1/business` | `PUT /api/business/v1/businesses` |
| `GET /api/v1/business/overview` | `GET /api/business/v1/businesses/overview` |
| `GET /api/v1/business/logo` | `GET /api/business/v1/businesses/logo` |
| `POST /api/v1/business/logo` | `POST /api/business/v1/businesses/logo` |
| `GET /api/v1/business/images` | `GET /api/business/v1/businesses/images` |

### Product module

| Old URL | New URL |
|---|---|
| `GET /product/v1` | `GET /api/product/v1/products` |
| `POST /product/v1` | `POST /api/product/v1/products` |
| `GET /product/v1/list` | `GET /api/product/v1/products/list` |
| `GET /product/v1/{id}` | `GET /api/product/v1/products/{id}` |
| `PUT /product/v1/{id}` | `PUT /api/product/v1/products/{id}` |
| `GET /product/v1/sku/{sku}` | `GET /api/product/v1/products/sku/{sku}` |
| `POST /product/v1/products` | `POST /api/product/v1/products/products` *(sync endpoint — consider renaming to /sync)* |
| `GET /product/v1/groups` | `GET /api/product/v1/products/groups` |
| `POST /product/v1/groups` | `POST /api/product/v1/products/groups` |
| `GET /product/v1/brands` | `GET /api/product/v1/products/brands` |
| `GET /product/v1/sub-categories` | `GET /api/product/v1/products/sub-categories` *(snake_case → kebab-case)* |
| `POST /product/v1/upload_image` | `POST /api/product/v1/products/upload-image` *(snake_case → kebab-case)* |
| `GET /product/v1/{pid}/variants` | `GET /api/product/v1/products/{pid}/variants` |
| `POST /product/v1/{pid}/variants` | `POST /api/product/v1/products/{pid}/variants` |
| `GET /product/v1/variants/{vid}` | `GET /api/product/v1/products/variants/{vid}` |
| `PUT /product/v1/variants/{vid}` | `PUT /api/product/v1/products/variants/{vid}` |
| `DELETE /product/v1/variants/{vid}` | `DELETE /api/product/v1/products/variants/{vid}` |

### Inventory module

| Old URL | New URL |
|---|---|
| `GET /inventory/v1/items` | `GET /api/inventory/v1/items` |
| `POST /inventory/v1/items` | `POST /api/inventory/v1/items` |
| `GET /inventory/v1/warehouses` | `GET /api/inventory/v1/warehouses` |
| `POST /inventory/v1/warehouses` | `POST /api/inventory/v1/warehouses` |
| `GET /inventory/v1/transactions` | `GET /api/inventory/v1/transactions` |
| `POST /inventory/v1/transactions/stock-in` | `POST /api/inventory/v1/transactions/stock-in` |
| `GET /inventory/v1/batches` | `GET /api/inventory/v1/batches` |
| `GET /inventory/v1/serials` | `GET /api/inventory/v1/serials` |
| `GET /inventory/v1/ledger` | `GET /api/inventory/v1/ledger` |
| `GET /inventory/v1/dashboard` | `GET /api/inventory/v1/dashboard` |
| `GET /inventory/v1/config` | `GET /api/inventory/v1/config` |

*(All inventory paths: only `/api` prefix added, no structural change.)*

### Customer module

| Old URL | New URL |
|---|---|
| `GET /customer/v1` | `GET /api/customer/v1/customers` |
| `POST /customer/v1` | `POST /api/customer/v1/customers` |
| `GET /customer/v1/{id}` | `GET /api/customer/v1/customers/{id}` |
| `PUT /customer/v1/{id}` | `PUT /api/customer/v1/customers/{id}` |
| `DELETE /customer/v1/{id}` | `DELETE /api/customer/v1/customers/{id}` |
| `GET /customer/v1/groups` | `GET /api/customer/v1/groups` |
| `GET /customer/v1/types` | `GET /api/customer/v1/types` |
| `GET /customer/v1/images` | `GET /api/customer/v1/images` |
| `GET /customer/v1/master-states` | `GET /api/customer/v1/master-states` |

### Tax module

| Old URL | New URL |
|---|---|
| `GET /tax/v1/rule` | `GET /api/tax/v1/rules` *(plural)* |
| `PUT /tax/v1/rule/{id}` | `PUT /api/tax/v1/rules/{id}` |
| `DELETE /tax/v1/rule/{id}` | `DELETE /api/tax/v1/rules/{id}` |
| `GET /tax/v1/code` | `GET /api/tax/v1/codes` *(plural)* |
| `GET /tax/v1/component` | `GET /api/tax/v1/components` *(plural)* |
| `GET /tax/v1/component-type` | `GET /api/tax/v1/component-types` *(plural)* |
| `GET /tax/v1/configuration` | `GET /api/tax/v1/configurations` *(plural)* |
| `GET /tax/v1/master-code` | `GET /api/tax/v1/master-codes` *(plural)* |
| `GET /tax/v1/master-component` | `GET /api/tax/v1/master-components` *(plural)* |
| `GET /tax/v1/master-rule` | `GET /api/tax/v1/master-rules` *(plural)* |

### Order module

| Old URL | New URL |
|---|---|
| `GET /order/v1` | `GET /api/order/v1/orders` |
| `POST /order/v1` | `POST /api/order/v1/orders` |
| `POST /order/v1/create_invoice` | `POST /api/order/v1/orders/create-invoice` *(kebab-case)* |

### Invoice module

| Old URL | New URL |
|---|---|
| `GET /invoice/v1` | `GET /api/invoice/v1/invoices` |
| `POST /invoice/v1` | `POST /api/invoice/v1/invoices` |

### Notification module

| Old URL | New URL |
|---|---|
| `GET /notification/v1/**` | `GET /api/notification/v1/notifications/**` |
| `POST /notification/v1/**` | `POST /api/notification/v1/notifications/**` |

### Form module

| Old URL | New URL |
|---|---|
| `GET /form/v1/schema` | `GET /api/form/v1/config/schema` |
| `POST /form/v1/config` | `POST /api/form/v1/config/config` *(consider renaming sub-path)* |
| `POST /form/v1/field-config` | `POST /api/form/v1/config/field-config` |
| `POST /form/v1/attribute-definition` | `POST /api/form/v1/config/attribute-definition` |

### Unit module

| Old URL | New URL |
|---|---|
| `GET /api/v1/unit` | `GET /api/unit/v1/units` |
| `POST /api/v1/unit` | `POST /api/unit/v1/units` |
| `GET /api/v1/unit/{id}` | `GET /api/unit/v1/units/{id}` |
| `PUT /api/v1/unit/{id}` | `PUT /api/unit/v1/units/{id}` |
| `DELETE /api/v1/unit/{id}` | `DELETE /api/unit/v1/units/{id}` |
| `GET /api/v1/unit/conversion` | `GET /api/unit/v1/conversions` |
| `POST /api/v1/unit/conversion` | `POST /api/unit/v1/conversions` |
| `POST /api/v1/unit/conversion/convert` | `POST /api/unit/v1/conversions/convert` |

### Subscription module

| Old URL | New URL |
|---|---|
| `GET /api/v1/subscriptions/**` | `GET /api/subscription/v1/subscriptions/**` |
| `GET /api/v1/billing/**` | `GET /api/subscription/v1/billing/**` |
| `GET /api/v1/subscription/invoices/**` | `GET /api/subscription/v1/invoices/**` |
| `GET /api/v1/subscription/billing-preferences/**` | `GET /api/subscription/v1/billing-preferences/**` |
| `POST /api/v1/subscription/payment/**` | `POST /api/subscription/v1/payments/**` |
| `GET /api/v1/devices/**` (subscription) | `GET /api/subscription/v1/devices/**` |
| `POST /webhooks/{provider}` | `POST /api/subscription/v1/webhooks/{provider}` |

> **Webhook URLs**: Update configured webhook endpoints in Razorpay, Stripe, Google Play, and App Store dashboards to use the new path `/api/subscription/v1/webhooks/{provider}`.

### Event module

| Old URL | New URL |
|---|---|
| `GET /api/v1/events/**` | `GET /api/event/v1/events/**` |
| `GET /api/v1/devices/active` *(was collision)* | `GET /api/event/v1/device-sessions/active` |
| `GET /api/v1/devices/active/user/{id}` | `GET /api/event/v1/device-sessions/active/user/{id}` |
| `GET /api/v1/devices/active/count` | `GET /api/event/v1/device-sessions/active/count` |

### Core module

| Old URL | New URL |
|---|---|
| `GET /api/v1/app-updates/check` | `GET /api/core/v1/app-updates/check` |
| `GET /api/v1/app-updates/download/{uid}` | `GET /api/core/v1/app-updates/download/{uid}` |
| `GET /api/v1/app-updates` | `GET /api/core/v1/app-updates` |
| `POST /api/v1/app-updates` | `POST /api/core/v1/app-updates` |
| `GET /api/v1/admin/api-keys` | `GET /api/core/v1/admin/api-keys` |
| `POST /api/v1/admin/api-keys` | `POST /api/core/v1/admin/api-keys` |
| `GET /api/test/**` | `GET /api/core/v1/test/**` |
| `GET /api/admin/token-cleanup/status` | `GET /api/auth/v1/admin/token-cleanup/status` |
| `POST /api/admin/token-cleanup/run` | `POST /api/auth/v1/admin/token-cleanup/run` |

### Account module

| Old URL | New URL |
|---|---|
| `POST /api/v1/account/delete-request` | `POST /api/account/v1/accounts/delete-request` |
| `POST /api/v1/account/delete-cancel` | `POST /api/account/v1/accounts/delete-cancel` |
| `GET /api/v1/account/delete-status` | `GET /api/account/v1/accounts/delete-status` |

### File module (unchanged — not a REST API)

| URL | Notes |
|---|---|
| `GET /files/{bucket}/**` | Dev-only local file server. Not versioned. Not affected by `spring.mvc.servlet.path`. |

---

## Method-level path changes (within controllers)

These are sub-path changes inside controllers that also affect client calls:

| Old endpoint | New endpoint | Reason |
|---|---|---|
| `POST /api/auth/v1/refresh_token` | `POST /api/auth/v1/refresh-token` | kebab-case |
| `POST /api/order/v1/orders/create_invoice` | `POST /api/order/v1/orders/create-invoice` | kebab-case |
| `GET /api/product/v1/products/product_category` | `GET /api/product/v1/products/product-category` | kebab-case |
| `GET /api/product/v1/products/all_groups_category` | `GET /api/product/v1/products/all-groups-category` | kebab-case |
| `GET /api/product/v1/products/sub_categories` | `GET /api/product/v1/products/sub-categories` | kebab-case |
| `POST /api/product/v1/products/sub_categories` | `POST /api/product/v1/products/sub-categories` | kebab-case |
| `POST /api/product/v1/products/upload_image` | `POST /api/product/v1/products/upload-image` | kebab-case |
| `GET /api/product/v1/products/{product_id}/variants` | `GET /api/product/v1/products/{productId}/variants` | camelCase path variable |
| `POST /api/product/v1/products/{product_id}/variants` | `POST /api/product/v1/products/{productId}/variants` | camelCase path variable |
| `GET /api/product/v1/products/variants/{variant_id}` | `GET /api/product/v1/products/variants/{variantId}` | camelCase path variable |
| `PUT /api/product/v1/products/variants/{variant_id}` | `PUT /api/product/v1/products/variants/{variantId}` | camelCase path variable |
| `DELETE /api/product/v1/products/variants/{variant_id}` | `DELETE /api/product/v1/products/variants/{variantId}` | camelCase path variable |

## JSON response key changes

Raw `Map` keys were camelCase and were NOT being converted by Jackson. These are now snake_case to match the global contract:

| Controller | Old key | New key |
|---|---|---|
| `TokenCleanupController` | `expiredTokenCount` | `expired_token_count` |
| `TokenCleanupController` | `deletedCount` | `deleted_count` |
| `NotificationController` | `notificationId` | `notification_id` |
| `NotificationController` | `smsId` | `sms_id` |
| `DeviceController` | `accessMode` | `access_mode` |

---

## Client implementation checklist

### Angular web app (`ampairs-web`)
- [ ] Update `environment.ts` / `environment.prod.ts` API base URL if hardcoded
- [ ] Replace all hardcoded `/auth/v1/`, `/workspace/v1/`, `/api/v1/` prefixes with new paths
- [ ] Update HTTP interceptors that prepend module paths
- [ ] Update Swagger/OpenAPI generated client if used
- [ ] Regression-test login flow, workspace switch, and workspace team management (team endpoints changed structure)

### Compose Multiplatform app (`ampairs-app`)
- [ ] Update all `HttpClient` base URLs / path constants
- [ ] Update device registration call: `/api/v1/devices/register` → `/api/subscription/v1/devices/register`
- [ ] Update event socket paths if any REST polling references `/api/v1/events`
- [ ] Rebuild and test on Android + Desktop

### External webhooks
- [ ] **Razorpay**: Update webhook URL to `https://{host}/api/subscription/v1/webhooks/razorpay`
- [ ] **Stripe**: Update webhook URL to `https://{host}/api/subscription/v1/webhooks/stripe`
- [ ] **Google Play**: Update RTDN (Real-Time Developer Notifications) URL to `https://{host}/api/subscription/v1/webhooks/google-play`
- [ ] **App Store**: Update App Store Server Notifications URL to `https://{host}/api/subscription/v1/webhooks/app-store`

---

## Backend-only changes (no client update needed)

These are internal reshuffles visible only to the backend. No client calls these directly:

- `SessionManagementController` moved from `/core/v1/sessions` → `/auth/v1/sessions`
- `TokenCleanupController` moved from `/api/admin/token-cleanup` → `/auth/v1/admin/token-cleanup`
- `DeviceStatusController` route collision resolved (now `/event/v1/device-sessions`)

---

## Breaking changes summary

| Change | Impact |
|---|---|
| All paths now prefixed with `/api` | All clients |
| Plural resource names (`/member` → `/members`, `/rule` → `/rules`, etc.) | Clients using those paths |
| Teams endpoint: `workspaceId` no longer in path | Web + mobile teams UI |
| Webhook paths changed | Payment provider dashboards |
| Device sessions split to separate path | Event monitoring clients |
