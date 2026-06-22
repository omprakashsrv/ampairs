# Quickstart / Validation Guide: Inventory Module Revamp

End-to-end checks that prove the feature works. Run after each workstream lands. References:
[contracts/](./contracts/), [data-model.md](./data-model.md).

## Prerequisites

- Backend: Docker running (Testcontainers); `cd /home/user/ampairs`.
- Mobile: Android SDK / iOS toolchain / JVM; `cd /home/user/ampairs-app`.
- A test workspace with `X-Workspace-ID` and at least one product/variant.

## Build & test gates

```bash
# Backend
cd /home/user/ampairs
./gradlew :ampairs_service:flywayInfo          # confirm next migration version (after V1.0.51)
./gradlew testAll                               # unit + integration (idempotency/concurrency/sync)

# Mobile — 3-target compile gate (mandatory after any commonMain change)
cd /home/user/ampairs-app
./gradlew androidApp:compileDebugKotlinAndroid
./gradlew shared:compileKotlinIosSimulatorArm64
./gradlew desktopApp:compileKotlin
./gradlew check
```

## Scenario A — Backend `/sync` round-trip (Story 4, contract)

1. `POST /inventory/v1/items/sync` with one `InventoryItemSyncRequest` (uid `INV-test-1`, current_stock 10).
   Expect `ApiResponse.success` echoing the row.
2. `GET /inventory/v1/items/sync` (no `last_sync`) → item present, `available_stock = 10`.
3. `POST .../items/sync` again with `is_active = false` → soft-delete.
4. `GET .../items/sync` → the row is **still returned** with `is_active = false` (deletes propagate).
   ✅ Pull includes soft-deleted rows; in-band delete works.

## Scenario B — Auto-deduction on sale (Story 1, FR-010/012/013/014)

1. Item `INV-test-1` at 10 on-hand; setting `inventory/auto_deduct_on_order = true` (default; settable via
   `POST /setting/v1/settings/sync` with `{module:"inventory", key:"auto_deduct_on_order", value:"true"}`
   or the workspace settings UI).
2. Confirm an order/invoice for 3 units of that product → on-hand becomes **7**; one STOCK_OUT movement
   (reason SALE) with `balance_after = 7`, `source_id` = doc uid.
3. Re-fire the same confirmation event (simulate retry) → on-hand **still 7**, **no second movement**.
   ✅ Idempotency (SC-002).
4. Cancel/return the sale → on-hand back to **10**; one STOCK_IN (reason RETURN).
5. Set settings `inventory/block_orders_when_out_of_stock = true`, `inventory/allow_negative_stock =
   false`; attempt a sale > on-hand → rejected (`InsufficientStockException`), stock unchanged. Flip
   `inventory/allow_negative_stock = true` → succeeds, on-hand goes negative. ✅ Policy matrix.
6. Set `inventory/auto_deduct_on_order = false`; confirm a sale → stock unchanged. ✅ Scenario 5.
   (All policy values are central settings read via `SettingService`/`StoreSettingsProvider`, not a
   dedicated inventory config — see contracts/inventory-settings.md.)

## Scenario C — Manual adjustment & history (Story 2, FR-005/008/009)

1. Stock-in +5 (reason PURCHASE) → on-hand +5; movement with running balance.
2. Stock-out -2 (reason DAMAGE) → on-hand -2; movement with running balance.
3. `GET /items/{uid}/transactions` (and mobile detail) → newest-first list with reason, signed qty,
   `balance_after`, source, timestamp.
4. Attempt adjustment with quantity 0 → rejected. ✅ Validation.

## Scenario D — Physical count (Story 5, FR-020)

1. Item at system on-hand 10. Submit counted = 8 → on-hand 8; one COUNT movement of -2.
2. Submit counted = 8 again (no change) → **no** movement created. ✅ SC-003.

## Scenario E — Low-stock & dashboard (Story 3, FR-015/016/017)

1. Set reorder_level = 5; reduce on-hand to 4 → item appears in **low-stock** list; one low-stock alert.
2. Persist the condition across a scheduler run → **no duplicate** alert. ✅ SC-004.
3. Reduce to 0 → moves to **out-of-stock**; out-of-stock alert.
4. Dashboard shows total stock value = Σ(on-hand × cost) and reconciles exactly. ✅ SC-009.

## Scenario F — Mobile offline-first (Story 4, FR-021..026, SC-005/006)

1. Disable connectivity. Create an item, record an adjustment, run a count → all succeed locally and are
   visible immediately. ✅ SC-005.
2. Kill & relaunch the app (process-death) → queued changes persist (PENDING_PUSH observed on start).
3. Re-enable connectivity → changes push automatically; server reflects them; server-side changes pull in.
   ✅ SC-006.
4. Edit an item offline that was also changed on the server → local unsynced edit wins until pushed.
   ✅ Conflict resolution.

## Scenario G — Architecture conformance (SC-008)

- Mobile `InventoryRepository` injects **no** `Api` (only DAO + `SyncStateDao`); writes set `synced=false`
  and call `markPendingPush`.
- All inventory server traffic lives in the two `SyncDelegate`s (items, transactions) (`@SyncEntityKey`,
  `@ContributesIntoMap(WorkspaceScope)`).
- ViewModels expose `StateFlow` UiState + `SharedFlow` events; screens use `collectAsStateWithLifecycle`;
  list VM drives `isRefreshing` from `syncService.observeEntity(...)`.
- All UI strings come from `composeResources`; UIDs from `UidGenerator.generateUid`.
- Inventory policy is read via `SettingService` (backend) / `StoreSettingsProvider` (mobile) under the
  `inventory` namespace — there is **no** `InventoryConfig` entity, `inventory_config` table,
  `/config/sync` endpoint, `INVENTORY_CONFIG` SyncEntity, or inventory settings screen.
- Spot-check via review + grep; the 3-target compile gate must be green.

## Scenario H — Legacy migration (R4/R9/R11)

- Backend: a workspace with legacy `inventory` rows → after migration, equivalent `inventory_item` rows
  exist with preserved stock/prices; legacy table dropped; no `GET /inventory/v1/items` (map) remains.
- Backend config: a workspace with a legacy `inventory_config` row → after the optional backfill, five
  `store_setting` rows (`module_code='inventory'`) carry the same values; `inventory_config` table dropped;
  `GET /setting/v1/definitions` now lists the `inventory/*` keys for installed workspaces.
- Mobile: existing local `inventoryEntity` rows → after Room migration, present as `InventoryItemEntity`
  with preserved data; app opens without crash; inventory toggles appear in the existing settings screen.
