# Phase 1 Data Model: Inventory Module Revamp (Pragmatic Core)

Covers both the **backend JPA model** (canonical source of truth) and the **mobile Room model** (offline
mirror). Deferred entities (Batch, Serial, Ledger, multi-warehouse) are noted but unchanged/untouched.

Legend: 🆕 new field/column · ✏️ changed · ♻️ retired · (kept) existing and unchanged.

---

## 1. Backend (JPA — `com.ampairs.inventory.domain.model`)

All tenant-scoped entities extend `OwnableBaseDomain` (`@TenantId ownerId`, `createdAt`/`updatedAt:
Instant`). Timestamps are `Instant`; columns `TIMESTAMPTZ` (PG) / `TIMESTAMP` (MySQL).

### 1.1 InventoryItem (`inventory_item`) — kept, minor additions

| Field | Type | Notes |
|---|---|---|
| uid | String | UID prefix `INV`; unique per tenant. **Sync key.** |
| name | String | Item name. |
| sku | String? | Unique per tenant when present. |
| productId / productVariantId | String? | Optional link to product/variant. |
| unitId | String? | Optional unit of measure. |
| warehouseId | String | Resolved to the **default warehouse** (R1). |
| currentStock | BigDecimal(15,3) | On-hand. |
| reservedStock | BigDecimal(15,3) | Allocated; minimal use this feature. |
| availableStock | BigDecimal(15,3) | Derived = current − reserved (kept in sync on write). |
| reorderLevel | BigDecimal(15,3) | Low-stock threshold (0/unset ⇒ never "low"). |
| costPrice / sellingPrice / mrp | BigDecimal(15,2) | Pricing. |
| isActive | Boolean | Soft-delete flag for `/sync` (false = deleted). |
| updatedAt | Instant | **Sync cursor** (`last_sync`). |

State/derivation: `recalculateAvailableStock()` on every stock mutation; `isLowStock()` /
`isOverStock()` retained.

### 1.2 InventoryTransaction (`inventory_transaction`) — append-only ledger, additions

| Field | Type | Notes |
|---|---|---|
| uid | String | UID prefix `TXN`; client-generatable. **Sync key.** |
| transactionNumber | String | Server-assigned human number (kept). |
| transactionType | enum | STOCK_IN / STOCK_OUT / ADJUSTMENT / COUNT (TRANSFER deferred). |
| transactionReason | enum | PURCHASE/SALE/RETURN/DAMAGE/LOSS/OPENING/CORRECTION/COUNT_ADJUSTMENT/… |
| inventoryItemId | String | FK → item (the movement's subject). |
| warehouseId | String | Default warehouse. |
| quantity | BigDecimal(15,3) | Always positive; sign implied by type. |
| balanceAfter | BigDecimal(15,3) | ✏️/(kept) **server-computed** on-hand after this movement (R7). |
| unitCost / totalCost | BigDecimal(15,2) | Optional valuation of the movement. |
| 🆕 sourceType | enum/String | ORDER / INVOICE / RETURN / MANUAL / COUNT. |
| 🆕 sourceId | String? | Source document uid (e.g., order/invoice uid). |
| 🆕 sourceLineUid | String? | Per-line uid for idempotency & partials. |
| referenceNumber | String? | Human-readable doc number (kept). |
| transactionDate | Instant | When it happened. |
| performedBy | String? | Actor uid. |
| notes | String? | Free text (reason detail). |
| updatedAt | Instant | **Sync cursor** (append-only ⇒ effectively createdAt). |

**Constraint** 🆕: partial unique index `ux_txn_idem (source_type, source_id, source_line_uid, owner_id)`
WHERE `source_line_uid IS NOT NULL` (R2). No soft-delete column — transactions are immutable/append-only
(R6).

### 1.3 InventoryConfig (`inventory_config`) — one per workspace, sync-enabled

| Field | Type | Notes |
|---|---|---|
| uid | String | ✏️ stable per-workspace key (e.g., `CFG-{workspace}`) so it has a deterministic **sync key**. |
| autoDeductOnOrder | Boolean | default **true**. |
| blockOrdersWhenOutOfStock | Boolean | default **false**. |
| allowNegativeStock | Boolean | default **false**. |
| allowManualOverride | Boolean | default **true**. |
| enableLowStockAlerts | Boolean | default **true**. |
| stockConsumptionStrategy | enum | kept = FIFO (irrelevant until batches; not exposed). |
| defaultWarehouseId | String? | Resolved default (R1). |
| updatedAt | Instant | **Sync cursor.** |

(Other existing config fields — expiry/overstock/ledger flags — remain but are **not exposed** in the
pragmatic-core UI.)

### 1.4 Warehouse (`warehouse`) — kept, single default per tenant

Unchanged. Invariant enforced: exactly one `isDefault = true` per workspace (backfilled).

### 1.5 Retired ♻️

- `Inventory` (legacy flat entity), `InventoryRepository`, `InventoryRequest`/`InventoryResponse`, and the
  map-shaped `GET /inventory/v1/items`. Data migrated into `inventory_item` (R4) then table dropped.

### 1.6 Deferred (untouched)

`InventoryBatch`, `InventorySerial`, `InventoryLedger`, multi-warehouse transfer fields — remain in schema,
no new work, not synced.

---

## 2. Mobile (Room — `com.ampairs.inventory.data.db`)

Workspace-scoped DB (`moduleName = "inventory"`). Every entity carries `synced: Boolean` (false = pending
push) and the server `updatedAt`/`lastUpdated` cursor. Mappers: `toEntity()/toDomain()/toRequest()` per
the customer template.

### 2.1 InventoryItemEntity (replaces the old flat `inventoryEntity`)

`id (uid, PK-unique)`, `name`, `sku?`, `productId?`, `productVariantId?`, `unitId?`,
`currentStock: Double`, `reservedStock: Double`, `availableStock: Double`, `reorderLevel: Double`,
`costPrice: Double`, `sellingPrice: Double`, `mrp: Double`, `active: Int`, `synced: Int`,
`updatedAt: String?` (ISO), `lastUpdated: Long?`.
DAO: reactive `Flow` lists, search, low-stock/out-of-stock queries, total-value aggregate, Paging source,
`getUnsynced()`, upsert, hard-delete.

### 2.2 InventoryTransactionEntity (new — movement history)

`id (uid, PK-unique)`, `inventoryItemId`, `type`, `reason`, `quantity: Double` (signed for display or
signed-by-type), `balanceAfter: Double?`, `unitCost: Double?`, `sourceType`, `sourceId?`,
`sourceLineUid?`, `referenceNumber?`, `transactionDate: String`, `performedBy?`, `notes?`, `synced: Int`,
`updatedAt: String?`.
DAO: `Flow`/Paging history by `inventoryItemId` (newest-first), `getUnsynced()`, insert (append-only),
`upsertFromServer()`. **No update/delete of existing rows** (append-only, R6).

### 2.3 InventoryConfigEntity (new — single cached row)

Mirror of backend config fields above + `synced: Int`, `updatedAt: String?`. Single row keyed by the
deterministic config uid.

### 2.4 Room migration

`MigrationOldFlatToV2`: create new tables; copy `inventoryEntity` → `InventoryItemEntity`
(`id→id, description→name, mrp/dp/selling/buying→prices, stock→currentStock, active, synced`); drop the old
table. No movement rows created (history begins post-migration; an optional OPENING movement per item can
be appended on first sync if desired — left as a task option).

---

## 3. Relationships

```
Workspace ──1:N── InventoryItem ──1:N── InventoryTransaction
   │                  │  (FK inventoryItemId)
   │                  └── optional → Product / ProductVariant / Unit
   ├──1:1── InventoryConfig
   └──1:1── default Warehouse (single-warehouse mode)

InventoryTransaction.sourceId ─ references ─▶ Order / Invoice document (loose, by uid; no FK across modules)
```

---

## 4. Validation rules (from spec FRs)

- Manual adjustment quantity MUST be > 0 with a reason (FR-009).
- Physical count equal to system on-hand ⇒ **no** movement (FR-020 / SC-003).
- `blockOrdersWhenOutOfStock` ⇒ reject sale that drives tracked item < 0 unless `allowNegativeStock`
  (FR-013/FR-014).
- Auto-deduction unique per `(source_type, source_id, source_line_uid)` ⇒ no double-count (FR-012/SC-002).
- Low = `0 < currentStock <= reorderLevel && reorderLevel > 0`; Out = `currentStock <= 0` (FR-015).
- Items without an inventory record are unaffected by stock logic (FR-004).
- Movements immutable; corrections via compensating movement (FR-008).
