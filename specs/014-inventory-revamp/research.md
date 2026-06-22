# Phase 0 Research & Decisions: Inventory Module Revamp

This resolves the design unknowns before contracts/data-model are finalized. Each entry: **Decision /
Rationale / Alternatives considered**.

---

## R1 — Single-warehouse handling on a multi-warehouse-capable schema

**Decision**: Keep the `Warehouse` entity and `warehouseId` foreign keys in the schema, but operate
against a single **default warehouse per workspace** (`is_default = true`). All new items/transactions are
attached to the default warehouse implicitly; no warehouse selection UI is shipped. A backfill ensures
every workspace has exactly one default warehouse.

**Rationale**: The backend already models warehouses; removing them would be destructive and would
preclude the deferred multi-warehouse feature. Hiding warehouse selection behind a default gives the
single-warehouse product now without burning the bridge to multi-warehouse later (FR-029).

**Alternatives considered**: (a) Drop warehouse columns entirely — rejected, destroys the deferred path
and forces a future migration to re-add them. (b) Expose warehouse picker now — rejected, out of scope and
adds UX the pragmatic core doesn't need.

---

## R2 — Idempotency for auto stock deduction (no double-counting)

**Decision**: Add a composite idempotency key to `inventory_transaction`:
`source_type` (ORDER/INVOICE/RETURN/MANUAL/COUNT), `source_id` (document uid), `source_line_uid`
(per-line uid), and the existing `owner_id`. Enforce a **partial unique constraint** on
`(source_type, source_id, source_line_uid, owner_id)` for rows where `source_line_uid IS NOT NULL` (manual
movements leave it null and are exempt). `InventoryStockService` checks/inserts under this key so a retried
or duplicated event is a no-op (upsert-or-skip).

**Rationale**: Order/invoice events and sync retries are at-least-once. A DB-level unique constraint makes
double-deduction structurally impossible (SC-002), which a read-then-write guard cannot guarantee under
concurrency. Per-line granularity supports partial fulfillment/returns (spec edge cases).

**Alternatives considered**: (a) Application-level "already processed?" check — racy. (b) Idempotency on
the whole document — rejected, breaks partial fulfillment/return. (c) An outbox/dedup table — heavier than
needed; the constraint on the transaction row is sufficient since the transaction *is* the record of work.

---

## R3 — Order/invoice → stock trigger (CONFIRMED)

**Decision (CONFIRMED — R3 default adopted)**: Deduct on **order confirmation**
when `autoDeductOnOrder` is enabled; if the workspace operates invoice-first (no order step), deduct on
**invoice finalize**. Restore on **order cancel** or **return/credit-note**, and on **invoice
void/cancel**. The trigger is delivered to inventory via a **public service interface**
`InventoryStockService` called by the order/invoice services (preferred) — falling back to the existing
Spring event listener (`InventoryOrderEventListener`) if a synchronous cross-module call is undesirable.
Because deduction is idempotent (R2), it is safe even if both an event and a direct call fire.

**Rationale**: Confirmation (not draft creation) is the point where goods are committed; this matches the
`reserved` vs `on-hand` distinction and avoids deducting for abandoned carts. Idempotency removes the risk
of picking "the wrong" event — whichever fires first wins and the rest are no-ops.

**Alternatives considered**: (a) Deduct on order *creation* — rejected, deducts for drafts/abandoned
orders. (b) Reserve on confirm + deduct on fulfill — correct long-term but adds a reservation lifecycle
beyond the pragmatic core; modeled (reserved stock exists) but not the primary path here. (c) Pure
event-listener integration — kept as fallback; the explicit service call is preferred for testability and
to honor the module-boundary rule (public service interface, not events crossing as the contract).

**Status**: CONFIRMED (R3 default adopted). The only remaining work is implementation detail — the exact
order/invoice line → `StockLine` mapping and the service call sites (tasks T023/T024 + finding U1); this is
not an open design question. Deduction is idempotent regardless of which event fires.

---

## R4 — Retiring the legacy flat `Inventory` entity (backend)

**Decision**: Treat `InventoryItem` + `InventoryTransaction` as the canonical model. The legacy
`Inventory` entity, `InventoryRepository`, `InventoryRequest`/`InventoryResponse`, and the lone
`GET /inventory/v1/items` (map-shaped) endpoint are **retired**. Provide a one-time Flyway **data
migration** that maps any legacy `inventory` rows into `inventory_item` (best-effort: description→name,
prices, stock→current_stock, attach default warehouse), then drops/renames the legacy table. The
map-shaped `GET /items` is replaced by the canonical `GET /items/sync`.

**Rationale**: Two parallel models are the root cause of the incoherence. Collapsing to one removes
ambiguity and lets the mobile client target a single contract.

**Alternatives considered**: (a) Keep legacy for back-compat — rejected, perpetuates the split-brain. (b)
Drop without migrating data — rejected, risks losing existing stock numbers; a best-effort migration is
cheap insurance (document gaps in `NO_MIGRATION_NEEDED.md` if a workspace has no legacy rows).

---

## R5 — Which resources go on the `/sync` contract

**Decision**: **Two** inventory sync resources:
- `GET/POST /inventory/v1/items/sync` → InventoryItem (stock levels + pricing)
- `GET/POST /inventory/v1/transactions/sync` → InventoryTransaction (movements; **append-only** — see R6)

Inventory **policy/config is NOT an inventory sync resource** — it lives in the central `setting` module
and rides `SyncEntity.STORE` (see R11). `Warehouse`, `InventoryBatch`, `InventorySerial`,
`InventoryLedger` are **not** synced in this feature.

**Rationale**: Items and movements are the inventory-owned data the mobile UX (dashboard, list, detail,
adjust, count) needs. Config is cross-cutting workspace policy that the platform already syncs generically
via the setting module — adding a bespoke `/config/sync` would duplicate that infrastructure.

**Alternatives considered**: (a) A dedicated `/inventory/v1/config/sync` — rejected, duplicates the
central setting module's sync/UI for no benefit (this is exactly the change requested). (b) Sync
warehouses — unnecessary while single-warehouse; the default warehouse is resolved server-side.

---

## R6 — Movements are append-only across the sync boundary

**Decision**: `InventoryTransaction` is immutable and **append-only**. The transactions `/sync` push only
ever *creates* new movements (client-generated uid); it never updates or soft-deletes an existing one.
Pull returns movements `>= last_sync`. There is no soft-delete column on transactions (the in-band-delete
clause of the contract is a no-op for this resource, which is acceptable — like form's aggregate nuance,
this resource has a documented contract nuance). Corrections happen by appending a compensating movement
(FR-008).

**Rationale**: An auditable ledger must be immutable. Append-only also makes sync trivially conflict-free
for movements (two devices can only ever add disjoint uids).

**Alternatives considered**: Allow movement edits/deletes — rejected, destroys auditability and
introduces sync conflicts. Note this nuance in the `/sync` contract doc so reviewers don't flag the
missing soft-delete.

---

## R7 — Running balance computation

**Decision**: Each `InventoryTransaction` stores `balance_after` computed **server-side at write time**
under the per-item transactional lock used by `InventoryStockService`/`InventoryTransactionService`. The
mobile client displays the stored `balance_after`; it does **not** recompute. For movements created
offline on mobile, the client stores a provisional local balance and the server's authoritative
`balance_after` overwrites it on pull.

**Rationale**: Authoritative balances must be serialized with the stock mutation to stay correct under
concurrency (spec edge case: concurrent sale + adjustment). Letting the client compute risks drift.

**Alternatives considered**: Compute balance only on read — rejected, expensive for long histories and
ambiguous under concurrency. Client-authoritative balance — rejected, can't be trusted across devices.

---

## R8 — Mobile SyncEntity additions

**Decision**: Reuse the existing `SyncEntity.INVENTORY` for items. **Add only** `INVENTORY_TRANSACTION` to
the `SyncEntity` enum (`data/sync`). Each gets its own `SyncDelegate` (`@SyncEntityKey`,
`@ContributesIntoMap(WorkspaceScope)`). Declare `pushDependencies` so items push before transactions (a
movement references an item). **No `INVENTORY_CONFIG`** — inventory policy syncs via the existing
`SyncEntity.STORE` (the setting module's delegate already handles it).

**Rationale**: The generic engine keys everything by `SyncEntity`; the two inventory-owned resources need
two keys. Config is not inventory-owned data on the wire — it is a setting row, already covered by
`SyncEntity.STORE`.

**Alternatives considered**: Add `INVENTORY_CONFIG` — rejected, it would create a second sync path for data
the setting module already syncs.

---

## R9 — Mobile data layer rebuild vs. patch

**Decision**: **Rebuild** the `feature/inventory` data/sync/UI layers to the customer template rather than
patching the current flat entity. Provide a Room migration that maps the old `inventoryEntity` rows into
the new `InventoryItemEntity` (preserving uid, description→name, prices, stock→current_stock) and creates
no movements for them (their history starts at the migration as an opening balance, optional).

**Rationale**: The current repository (API-injected), state (non-MVI), and model (flat) all violate the
architecture; incremental patching would be more work than a clean rebuild against the gold-standard
pattern. A Room migration preserves users' local data.

**Alternatives considered**: Patch in place — rejected, the anti-patterns are foundational (no SyncDelegate,
no markPendingPush), so a rewrite is cleaner and safer.

---

## R10 — Low-stock alert delivery & dedup

**Decision**: Detection stays in the existing schedulers/queries. Delivery routes through the
**notification module** (per constitution topology, `notification` is a first-class module). Dedup by
tracking the last-alerted condition per `(item, condition)` so an unchanged low/out state does not
re-alert (FR-017, SC-004). On mobile, the dashboard derives low/out lists reactively from the local DB
(no server round-trip needed for display); push notifications come via the existing FCM path.

**Rationale**: Reuses platform notification infrastructure instead of inventing a channel; dashboard
display is local-first so it works offline.

**Alternatives considered**: Compute alerts client-side only — rejected, misses cross-device/server-driven
delivery and background alerts when the app is closed.

---

## R11 — Inventory policy via the central `setting` module (replaces `InventoryConfig`)

**Decision**: Retire the dedicated `InventoryConfig` entity/table/service/`/config/sync`. Inventory policy
becomes five settings under module namespace `inventory`, managed by the central `setting` module:

| Setting key (`inventory/…`) | Type | Default |
|---|---|---|
| `auto_deduct_on_order` | BOOLEAN | `true` |
| `block_orders_when_out_of_stock` | BOOLEAN | `false` |
| `allow_negative_stock` | BOOLEAN | `false` |
| `allow_manual_override` | BOOLEAN | `true` |
| `enable_low_stock_alerts` | BOOLEAN | `true` |

- **Backend declares** them via a `@Component InventorySettingDefinitions : SettingDefinitionProvider`
  (in `inventory/config/`), each with `requiresModule = "inventory-management"` so they only surface when
  the module is installed. Definitions are code-based — **no Flyway migration** to define them; the
  `setting` module's `GET /setting/v1/definitions` exposes them and `GET/POST /setting/v1/settings/sync`
  syncs overrides.
- **Backend reads** effective values via the public `SettingService.getBoolean("inventory", key)` (the
  precedent used by `payment`/`invoice`/`common`) inside `InventoryStockService` and the alert scheduler.
- **Mobile reads** via the existing `StoreSettingsProvider.getBoolean/observeBoolean("inventory", key,
  default)` injected into inventory ViewModels (precedent: `InvoiceViewModel` reading
  `common/prices_include_tax`). Editing happens in the existing generic `feature/store` settings screen,
  which renders any definition for installed modules — **no inventory settings screen, entity, or delegate
  on either side**.
- **Migration**: optional one-time data backfill copying legacy `inventory_config` rows into `store_setting`
  rows (`module_code='inventory'`, one row per key) before dropping `inventory_config`; if a workspace has
  no legacy config, defaults apply and nothing is backfilled.

**Rationale**: This is the requested change. The `setting` module already provides definitions, typed
values, per-module gating, offline sync (`SyncEntity.STORE`), and a generic settings UI — duplicating any
of it for inventory is waste and drift risk.

**Alternatives considered**: Keep `InventoryConfig` as the source of truth and mirror into settings —
rejected, two sources of truth. Keep `InventoryConfig` for "advanced" inventory config not in the
pragmatic core — deferred config (expiry/overstock/strategy) can become additional `inventory/*` settings
later; no need to retain the table.

---

## Summary of new/changed schema (feeds data-model.md)

- `inventory_transaction`: **add** `source_type`, `source_id`, `source_line_uid`, `balance_after` (if not
  present), partial unique index on `(source_type, source_id, source_line_uid, owner_id)`.
- `inventory_item`: ensure `updated_at: Instant` + soft-delete (`is_active`) present for `/sync`; add
  `reserved_stock`/`available_stock` if missing (present per current model).
- `inventory_config`: **retire** — declare `inventory/*` settings in the `setting` module instead; optional
  one-time backfill `inventory_config` → `store_setting`, then drop `inventory_config` (R11).
- Retire legacy `inventory` table (data migration → `inventory_item`, then drop).
- Mobile: new Room entities (items + transactions only) + migration off `inventoryEntity`; **no** mobile
  config table (policy read via `StoreSettingsProvider`).
