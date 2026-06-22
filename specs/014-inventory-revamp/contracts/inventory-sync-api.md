# Contract: Inventory `/sync` APIs

Conforms to the canonical offline-sync contract (`docs/guides/offline-sync-contract.md`). **Two**
inventory-owned resources: items and transactions. (Inventory *policy* is NOT here — it lives in the
central `setting` module; see [inventory-settings.md](./inventory-settings.md).)
All endpoints return `ApiResponse<T>`; tenant scope via `X-Workspace-ID`; params snake_case; pull feed
includes soft-deleted rows; push is UID-keyed bulk upsert. DTOs live in `inventory/domain/dto/`.

Base module path: `/inventory/v1`.

---

## 1. Inventory Items — `/inventory/v1/items/sync`

### PULL `GET /inventory/v1/items/sync`
Query: `last_sync?` (ISO-8601), `page=0`, `size=100`, `sort_by=updatedAt`, `sort_dir=ASC`.
Returns `ApiResponse<PageResponse<InventoryItemSyncResponse>>`. **Includes `is_active = false` rows.**

### PUSH `POST /inventory/v1/items/sync`
Body: `List<InventoryItemSyncRequest>` (active upserts AND soft-deleted, UID-keyed). Returns
`ApiResponse<List<InventoryItemSyncResponse>>`.

`InventoryItemSyncResponse` (snake_case): `uid, name, sku, product_id, product_variant_id, unit_id,
current_stock, reserved_stock, available_stock, reorder_level, cost_price, selling_price, mrp, is_active,
created_at, updated_at`.

`InventoryItemSyncRequest`: `uid (required), name, sku?, product_id?, product_variant_id?, unit_id?,
current_stock?, reserved_stock?, reorder_level?, cost_price?, selling_price?, mrp?, is_active`.
**Note:** `current_stock` from the client is only honored for create (opening balance); thereafter on-hand
is authoritative via movements — item push updates pricing/metadata/reorder_level/active, not arbitrary
stock jumps (stock changes ride the transactions feed). `available_stock` is server-derived (never trusted
from client).

Validation: `@field:NotBlank uid, name`. Soft-delete via `is_active = false`.

---

## 2. Inventory Transactions (movements) — `/inventory/v1/transactions/sync`

**Append-only** (R6): push only creates; never updates/deletes existing movements. No soft-delete column.

### PULL `GET /inventory/v1/transactions/sync`
Query as above. Returns `ApiResponse<PageResponse<InventoryTransactionSyncResponse>>` ordered by
`updated_at ASC`. Client filters/sorts per item for display.

### PUSH `POST /inventory/v1/transactions/sync`
Body: `List<InventoryTransactionSyncRequest>` (client-created movements: manual adjustments, counts).
Returns `ApiResponse<List<InventoryTransactionSyncResponse>>` with **server-computed `balance_after`** and
`transaction_number` filled in (client reconciles by uid, overwrites provisional balance).

`InventoryTransactionSyncResponse`: `uid, transaction_number, transaction_type, transaction_reason,
inventory_item_id, quantity, balance_after, unit_cost, total_cost, source_type, source_id,
source_line_uid, reference_number, transaction_date, performed_by, notes, created_at, updated_at`.

`InventoryTransactionSyncRequest`: `uid (required), transaction_type (required), transaction_reason
(required), inventory_item_id (required), quantity (required, > 0), unit_cost?, source_type
(default MANUAL), source_line_uid?, transaction_date?, notes?`.

Server behavior on push: applies the movement to the item's stock under a per-item lock, computes
`balance_after`, assigns `transaction_number`. Idempotent for non-MANUAL via the `(source_type, source_id,
source_line_uid, owner_id)` constraint (manual movements have null `source_line_uid` and are always
inserted). Rejects `quantity <= 0`.

---

## 3. Inventory Config — NOT an inventory endpoint

Inventory policy is **not** synced through an inventory `/config/sync` endpoint. It is declared as
`inventory/*` settings in the central `setting` module and synced via `GET/POST /setting/v1/settings/sync`
(client `SyncEntity.STORE`). See [inventory-settings.md](./inventory-settings.md). No
`InventoryConfig*` DTOs are introduced.

---

## Standard (non-sync) endpoints still needed

- `GET /inventory/v1/items/{uid}` — single item (item detail header), `ApiResponse<InventoryItemResponse>`.
- `GET /inventory/v1/items/{uid}/transactions` — paged movement history (server-side convenience; mobile
  primarily reads local DB), `ApiResponse<PageResponse<InventoryTransactionSyncResponse>>`.
- (Internal) physical-count submit may reuse the transactions push (one COUNT movement per differing item).

Legacy `GET /inventory/v1/items` (map-shaped) is **removed** (R4).

---

## Mobile delegate mapping

| Resource | SyncEntity | Delegate | Push deps |
|---|---|---|---|
| items | `INVENTORY` (exists) | `InventoryItemSyncDelegate` | — |
| transactions | `INVENTORY_TRANSACTION` (🆕) | `InventoryTransactionSyncDelegate` | after `INVENTORY` |
| policy/config | `STORE` (exists) | `StoreSyncDelegate` (existing — no new delegate) | — |

Contract conformance checklist (per inventory resource): `/sync` GET+POST, snake_case params, pull includes
soft-deleted (items; N/A append-only transactions), in-band delete (items), UID-keyed bulk upsert,
`ApiResponse<PageResponse<T>>`/`ApiResponse<List<T>>`, DTO isolation, `@TenantId` scoping.
