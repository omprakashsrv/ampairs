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
