# supplier module

Supplier (vendor) management for the purchase side of the business. Workspace-scoped (`OwnableBaseDomain` + `X-Workspace-ID`), on the canonical offline-sync contract.

## REST Endpoints (`/supplier/v1`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/supplier/v1/suppliers/sync` | Incremental pull feed (snake_case params: `last_sync`, `page`, `size`, `sort_by`, `sort_dir`; includes soft-deleted rows) |
| POST | `/supplier/v1/suppliers/sync` | UID-keyed bulk upsert; soft-deleted rows ride along in-band |
| GET | `/supplier/v1/{supplierId}` | Get a single supplier |

Follows the canonical `/sync` contract — see `docs/guides/offline-sync-contract.md`.

## Key Entities

### Supplier

Tenant-scoped supplier record: name, contact details, addressing, GST details, `active` flag for in-band soft delete.

## Services

- `SupplierService` — CRUD + sync reconciliation

## Dependencies

- `:core` only (standard module rules apply — no cross-module imports).
