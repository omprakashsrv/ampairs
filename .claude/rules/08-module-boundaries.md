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
| `workspace` | Workspaces, roles, RBAC |
| `product` | Catalog, inventory, SKU |
| `customer` | CRM, contacts |
| `tax` | GST, tax rules |
| `order` | Order processing, fulfillment |
| `invoice` | Invoices, payments |
| `subscription` | Plans, billing |
| `setting` | Central workspace settings registry (cross-module toggles, offline-sync) |
| `printing` | Print-template storage + offline-sync (`/printing/v1`, opaque layout JSON) |
| `trade` | Brand↔distributor network & consent edge — links, consent scope, brand/product attribution (NetworkBrand/NetworkProduct + NPI), primary-order handshake, `CrossTenantReadGuard`. Depends on `workspace`, `core` only |
| `sfa` | Distributor field-sales automation (offline `/sync`) — beats/PJP, store visits, attendance, surveys, leave + reporting (adherence/summary/productivity). Standalone (the MVP); reads `customer`/`order`/`form` via public services |
| `dms` | Brand distribution-management visibility — secondary-sales/distributor-stock snapshots + targets. Consent-gated via `trade`; reads `order`/`invoice`/`inventory` via public services + events |
| `claim` | Trade-scheme **claims & settlement** — SchemeClaim/ClaimSettlement reimbursement lifecycle (accrue→submit→approve→settle). Brand-funded **scheme definition** itself lives in `pricing` (spec 015); `claim` reuses it. Depends on `pricing`/`dms`/`trade` + `payment` (ledger) |
