# purchase module

Purchase recording — supplier bills and their line items. Workspace-scoped, on the canonical offline-sync contract (the mobile app records purchases offline and syncs them up).

## REST Endpoints (`/purchase/v1/purchases`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/purchase/v1/purchases/sync` | Incremental pull feed (snake_case params; includes soft-deleted rows) |
| POST | `/purchase/v1/purchases/sync` | UID-keyed bulk upsert; in-band soft delete |
| GET | `/purchase/v1/purchases/{purchaseId}` | Get a single purchase with items |

Follows the canonical `/sync` contract — see `docs/guides/offline-sync-contract.md`.

## Key Entities

### Purchase

Header record for a supplier bill: supplier reference, document number/date, totals, status, `active` flag.

### PurchaseItem

Line item belonging to a `Purchase`: product/variant reference, quantity, unit, rate, tax and discount breakdown.

## Dependencies

- `:core` only (standard module rules apply — no cross-module imports).
