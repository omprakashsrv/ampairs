# Module Boundaries

- Package layout: `com.ampairs.{module}.{domain|repository|service|controller}`
- Each module is an independent bounded context — no direct cross-module repository access.
- Entities extend `BaseDomain` (system-wide) or `OwnableBaseDomain` (tenant-scoped).
- Cross-module interactions go through public service interfaces only.
- New bounded contexts get their own module — do not add unrelated logic to existing modules.

## Module Ownership Quick Reference
| Module | Owns |
|--------|------|
| `core` | Shared utils, base domains, ApiResponse, TenantContextHolder |
| `auth` | JWT, device sessions, refresh tokens |
| `user` | User identity, profile, account deletion lifecycle |
| `workspace` | Workspaces, roles, RBAC |
| `product` | Catalog, inventory, SKU |
| `customer` | CRM, contacts |
| `supplier` | Suppliers (vendors) |
| `tax` | GST, tax rules |
| `order` | Order processing, fulfillment |
| `invoice` | Invoices, payment status |
| `purchase` | Purchase recording (supplier bills + items) |
| `payment` | Payment vouchers, allocations, ledger, party balances |
| `pricing` | Price lists, tiers, offers/coupons, geo zones |
| `sequence` | Document number sequences (definitions + allocations) |
| `ecom` | Storefronts — public catalog, cart, checkout, buyer accounts |
| `subscription` | Plans, billing |
| `setting` | Central workspace settings registry (cross-module toggles, offline-sync) |
| `printing` | Print-template storage + offline-sync (`/printing/v1`, opaque layout JSON) |
| `agent` | AI model manifest/download proxy + chat telemetry |
