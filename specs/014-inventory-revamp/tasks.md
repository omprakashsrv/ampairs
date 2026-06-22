---
description: "Task list for Inventory Module Revamp (Pragmatic Core)"
---

# Tasks: Inventory Module Revamp (Pragmatic Core)

**Feature**: `specs/014-inventory-revamp/` · **Branch**: `claude/zealous-meitner-q8hovy` (both repos)
**Input**: spec.md, plan.md, research.md, data-model.md, contracts/{inventory-sync-api, inventory-settings, stock-integration}.md, quickstart.md

**Repos**: `BE` = backend `/home/user/ampairs` (inventory lives in the `product` module → `com.ampairs.inventory`; settings in the `setting` module) · `APP` = mobile `/home/user/ampairs-app` (`feature/inventory`, plus `shared/` and `data/sync`).

**Format**: `- [ ] [ID] [P?] [Story?] [Repo] Description with file path`
- **[P]** = parallelizable (different files, no incomplete-task dependency)
- **[USx]** = maps to a spec user story (US1 auto-deduct, US2 adjustments+history, US3 low-stock+alerts, US4 offline-first, US5 physical count, US6 settings)

**Locked decisions** (do not re-open): central `setting` module for policy (no `inventory_config`); two inventory `/sync` resources only (items = `INVENTORY`, transactions = `INVENTORY_TRANSACTION`, append-only); idempotency via `(source_type, source_id, source_line_uid, owner_id)`; server-side `balance_after`; order/invoice→stock via public `InventoryStockService` with the **R3 default trigger** (deduct on order-confirm / invoice-finalize, restore on cancel/return/void); retire legacy flat `Inventory` + map `GET /items`; mobile rebuilt to the customer-module gold standard. Deferred (OUT): multi-warehouse, batch/serial, ledger.

**Cross-repo sequencing**: Backend Foundational + `/sync` contract (Phase 2) MUST land and be stable before mobile sync (Phase 4 / US4) can be built or tested.

---

## Phase 1: Setup (Shared Infrastructure)

- [x] T001 [BE] Confirm next Flyway version: run `./gradlew :ampairs_service:flywayInfo` and record the next free `V{semver}` after the latest applied (project-wide ≈ V1.0.98; inventory tables in `product` ended at V1.0.51). Use sequential versions for the migrations below; note them in the migration filenames.
- [x] T002 [P] [BE] Verify `product` and `setting` are listed in `migrationModules` in `ampairs_service/build.gradle.kts`; add any missing module so Flyway picks up the new migrations.
- [x] T003 [P] [APP] Add `INVENTORY_TRANSACTION("inventory_transaction")` to the `SyncEntity` enum in `data/sync/src/commonMain/.../SyncEntity.kt` (keep existing `INVENTORY`; do NOT add `INVENTORY_CONFIG`).
- [ ] T004 [P] [APP] Audit `feature/inventory/build.gradle.kts` for the deps the gold-standard `feature/customer` uses (Room KMP, Paging3, Metro, `data/sync`, `feature/store` for `StoreSettingsProvider`); add missing ones via the version catalog only.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: Backend data model, migrations, the `/sync` contract, the settings provider, and the `InventoryStockService` interface must exist before any user story is implementable. The `/sync` endpoints here unblock ALL mobile work.

### Backend data model & migrations (workstream A)

- [x] T005 [BE] Update `InventoryTransaction` entity (`product/.../inventory/domain/model/InventoryTransaction.kt`): add `sourceType` (enum/String: ORDER/INVOICE/RETURN/MANUAL/COUNT), `sourceId: String?`, `sourceLineUid: String?`; ensure `balanceAfter: BigDecimal`, `updatedAt: Instant` present. Add `SourceType` enum in `domain/enums/`.
- [x] T006 [BE] Ensure `InventoryItem` (`.../domain/model/InventoryItem.kt`) has `updatedAt: Instant` + soft-delete `isActive` + `reservedStock`/`availableStock` for `/sync` (add only if missing).
- [x] T007 [BE] Flyway migration `V{n}__inventory_txn_idempotency.sql` in BOTH `product/src/main/resources/db/migration/mysql/` and `.../postgresql/`: add `source_type`, `source_id`, `source_line_uid`, `balance_after` columns to `inventory_transaction`; add **partial unique index** `ux_inv_txn_idem (source_type, source_id, source_line_uid, owner_id)` WHERE `source_line_uid IS NOT NULL`.
  - **(finding U3)** MySQL has **no** partial/filtered unique index. Runtime DB is Postgres (where the partial index is the backstop). On MySQL, do NOT emulate with a plain unique index (nulls + the no-line case would over-constrain); instead rely on the **app-level skip-if-exists guard in T021 as the authoritative idempotency mechanism** and add a clear comment in the MySQL migration documenting this vendor difference.
- [x] T008 [BE] Flyway migration `V{n+1}__retire_legacy_inventory.sql` (both vendors): data-migrate legacy `inventory` rows → `inventory_item` (description→name, prices, stock→current_stock, attach default warehouse), then drop the `inventory` table. Document workspaces with no legacy rows as no-op in `NO_MIGRATION_NEEDED.md` if applicable.
- [ ] T009 [BE] Ensure exactly one default `warehouse` per workspace: migration `V{n+2}__inventory_default_warehouse_backfill.sql` (both vendors) creating a default warehouse where none exists and setting `is_default = true` (single-warehouse invariant, R1).

### Backend settings (replaces InventoryConfig — workstream config)

- [x] T010 [P] [BE] Create `InventorySettingDefinitions : SettingDefinitionProvider` in `product/.../inventory/config/InventorySettingDefinitions.kt` declaring 5 BOOLEAN keys under module `inventory` (`auto_deduct_on_order`=true, `block_orders_when_out_of_stock`=false, `allow_negative_stock`=false, `allow_manual_override`=true, `enable_low_stock_alerts`=true), each `requiresModule = "inventory-management"`. Mirror the `PaymentSettingDefinitions`/`InvoiceSettingDefinitions` pattern.
- [x] T011 [BE] Retire `InventoryConfig`: delete `InventoryConfig.kt`, `InventoryConfigService`, `InventoryConfigRepository`, config DTOs; replace all reads with `settingService.getBoolean("inventory", key)`.
  - **(finding U2 — migration ownership & ordering)** The retire touches two modules' tables: backfill writes `store_setting` (owned by `setting`), drop targets `inventory_config` (owned by `product`). Decision: **(a)** backfill migration `V{n+3}__backfill_inventory_settings.sql` in the **`setting`** module inserts 5 `store_setting` rows per `inventory_config` row (`module_code='inventory'`, value `"true"/"false"`); **(b)** drop migration `V{n+4}__drop_inventory_config.sql` in the **`product`** module drops `inventory_config`. The drop's version MUST be strictly greater than the backfill's so global semver ordering runs backfill first. **Verify** Flyway applies migrations by global version across module locations (single shared history); if it does NOT, fold backfill+drop into one migration that reads `inventory_config` and writes `store_setting` in the same script.

### Backend `/sync` contract (workstream B) — unblocks mobile

- [x] T012 [P] [BE] Item sync DTOs in `product/.../inventory/domain/dto/`: `InventoryItemSyncRequest`/`InventoryItemSyncResponse` (snake_case, `@field:NotBlank uid,name`) + `asResponse()`/`applyRequest()`/`toEntity()` extensions per contracts/inventory-sync-api.md §1.
- [x] T013 [P] [BE] Transaction sync DTOs: `InventoryTransactionSyncRequest`/`InventoryTransactionSyncResponse` (+ mappers) per §2 (append-only; server fills `balance_after`, `transaction_number`).
- [x] T014 [BE] Repository sync feeds in `InventoryItemRepository`/`InventoryTransactionRepository`: `findUpdatedAfter(lastSync, pageable)` (items: **includes** soft-deleted) and `findAllForSync(pageable)`; transactions ordered by `updatedAt ASC`.
- [x] T015 [BE] `InventoryItemService`: `getAfterSync(lastSync, pageable)` + `bulkUpsert(requests)` (UID-keyed; honor `is_active=false` soft-delete; item push updates pricing/metadata/reorder/active only — NOT arbitrary stock).
- [x] T016 [BE] `InventoryTransactionService`: `getAfterSync(...)` + `bulkUpsertAppendOnly(requests)` that, for each MANUAL/COUNT movement, applies it to item stock under a per-item lock, computes `balance_after`, assigns `transaction_number` (append-only; never updates existing rows).
- [x] T017 [BE] `InventoryItemController`: add `GET/POST /inventory/v1/items/sync` (`ApiResponse<PageResponse<...>>` / `ApiResponse<List<...>>`), `GET /inventory/v1/items/{uid}`; **remove** the legacy map-shaped `GET /inventory/v1/items`. Set tenant context at controller; no try/catch.
- [x] T018 [BE] `InventoryTransactionController`: add `GET/POST /inventory/v1/transactions/sync` and `GET /inventory/v1/items/{uid}/transactions` (paged history).
- [ ] T018a [P] [BE] **(finding G1 — FR-027 tenant isolation test)** Test in `product/src/test/...`: with two workspaces seeded, `GET /inventory/v1/items/sync` and `/transactions/sync` return ONLY the caller's workspace rows; a bulk push under workspace A never creates/updates rows visible to workspace B; movement reads never cross tenants. Asserts `@TenantId` auto-filtering on the sync feeds.

### Cross-module stock interface skeleton (workstream C foundation)

- [x] T019 [BE] Define public `InventoryStockService` interface + `StockMutationCommand`/`StockLine`/`SourceType` in `product/.../inventory/service/` per contracts/stock-integration.md (no impl yet).
- [ ] T019a [BE] **(finding U1 — order/invoice line discovery, prerequisite for T023/T024)** Locate the `order` and `invoice` service classes and their line DTO/entity (exact file paths) and document the line→`StockLine` mapping: which fields supply `productId`/`productVariantId`, `quantity`, `unitCost`, and a **stable per-line uid** for `sourceLineUid` (the idempotency key). If order/invoice lines lack a stable line uid, define how to derive one (e.g., existing line uid, else `{docUid}:{lineIndex}`). Record findings inline in contracts/stock-integration.md before implementing T021–T024.

### Mobile data/sync foundation scaffolding (workstream E — interface only; full build in US4)

- [ ] T020 [P] [APP] Remove the legacy flat path: delete/replace the old `InventoryEntity`, `InventoryRepository` (API-injected), `InventoryApi`/`Impl`, and non-MVI ViewModels under `feature/inventory/src/commonMain/...` (keep the module + DI shell + agent handler; agent will be re-pointed in T049).

**Checkpoint**: Backend schema/migrations applied, `/sync` for items+transactions live, settings provider registered, `InventoryStockService` interface exists. Mobile can now build against the contract.

---

## Phase 3: User Story 1 — Stock moves automatically on sale (Priority: P1) 🎯 MVP

**Goal**: A confirmed sale deducts stock idempotently per policy; cancel/return restores it (FR-010–014, FR-012/SC-002).
**Independent Test**: quickstart.md Scenario B (deduct, retry-idempotent, restore, policy matrix, auto-deduct off) — all via API.

- [x] T021 [BE] [US1] Implement `InventoryStockServiceImpl.applySale(cmd)` in `product/.../inventory/service/`: resolve each line's product/variant → `InventoryItem` at default warehouse; **skip untracked lines** (FR-004); policy gate via `settingService.getBoolean("inventory", …)` (auto-deduct off → no-op; block-on-OOS + !negative → throw `InsufficientStockException`); create one STOCK_OUT (reason SALE) per line under per-item lock with `balance_after`; idempotent via the unique key (skip-if-exists). One DB transaction per call.
- [x] T022 [BE] [US1] Implement `reverseSale(cmd)`: create STOCK_IN (reason RETURN, `source_type=RETURN`) per affected line, restoring only given quantities; idempotent.
- [x] T023 [BE] [US1] Wire **order** service to call `InventoryStockService.applySale(ORDER, orderUid, lines)` on order-confirm and `reverseSale(ORDER, …)` on order-cancel (R3 default). Cross-module via the public interface (constructor-inject `InventoryStockService` in order service). Build `lines` using the order line→`StockLine` mapping pinned in T019a (incl. the stable `sourceLineUid`).
- [x] T024 [BE] [US1] Wire **invoice** service to call `applySale(INVOICE, invoiceUid, lines)` on invoice-finalize and `reverseSale(INVOICE/RETURN, …)` on void/credit-note (R3 default). Build `lines` using the invoice line→`StockLine` mapping pinned in T019a.
- [ ] T025 [BE] [US1] Update `InventoryOrderEventListener` as the **fallback** path: have it call `InventoryStockService` (not just log). Idempotency makes belt-and-suspenders safe; document that explicit calls (T023/T024) are primary.
- [ ] T026 [P] [US1] [BE] Tests in `product/src/test/...`: idempotency (double `applySale` → one movement/one deduction), policy matrix (auto-deduct on/off × block on/off × negative on/off), partial return, untracked-line skip, `balance_after` correctness.
- [ ] T027 [US1] [BE] Concurrency test: concurrent `applySale` + manual adjustment on the same item leave consistent on-hand and monotonic `balance_after` (per-item lock).

**Checkpoint**: A sale reliably and idempotently moves stock per policy — demoable via API/quickstart B without any mobile work.

---

## Phase 4: User Story 4 — Offline-first inventory on mobile (Priority: P1) — Mobile foundation

**Goal**: Mobile inventory is fully offline-capable and reconciles via `/sync` (FR-021–026, SC-005/006). This rebuild is the foundation all other mobile stories build on.
**Independent Test**: quickstart.md Scenario F (offline create/adjust, process-death persistence, auto-push on reconnect, local-edit-wins) + Scenario G (architecture conformance).

- [x] T028 [P] [APP] [US4] Room entities in `feature/inventory/.../data/db/entity/`: `InventoryItemEntity` (uid PK, name, sku, productId, variantId, unitId, current/reserved/available/reorder, cost/selling/mrp, active, synced, updatedAt, lastUpdated) and `InventoryTransactionEntity` (uid PK, inventoryItemId, type, reason, quantity, balanceAfter, unitCost, sourceType, sourceId, sourceLineUid, referenceNumber, transactionDate, performedBy, notes, synced, updatedAt). No config entity.
- [x] T029 [P] [APP] [US4] DAOs in `.../data/db/dao/`: `InventoryItemDao` (reactive `Flow` list/search, low-stock/out-of-stock queries, total-value aggregate, Paging source, `getUnsynced()`, upsert, hard-delete) and `InventoryTransactionDao` (Paging history by item newest-first, `getUnsynced()`, insert append-only, `upsertFromServer()`).
- [x] T030 [APP] [US4] `InventoryDatabase` (`@Database` both entities) + `@ConstructedBy` constructor; Room migration `MigrationOldFlatToV2` copying old `inventoryEntity` → `InventoryItemEntity` (id, description→name, prices, stock→currentStock, active, synced) and dropping the old table.
- [x] T031 [P] [APP] [US4] Platform DB providers in `androidMain/iosMain/desktopMain` `.../di/`: `@ContributesTo(WorkspaceScope)` + `@Provides @SingleIn(WorkspaceScope)` `createAndroidDatabase<InventoryDatabase>(...)`/`createDatabase<...>(...)` with `moduleName="inventory"` + `.also { closableRegistry.register { it.close() } }`; common DAO providers module.
- [x] T032 [P] [APP] [US4] Domain models + mappers (`toEntity()/toDomain()/toRequest()`, snake_case `@SerialName`) in `.../domain/`.
- [x] T033 [APP] [US4] Local-only repositories in `.../data/repository/`: `InventoryItemRepository` and `InventoryTransactionRepository` inject `Dao` + `SyncStateDao` (NOT the Api); writes set `synced=false` then `markPendingPush(SyncEntity.INVENTORY | INVENTORY_TRANSACTION, now)`. Movement create is append-only.
- [x] T034 [P] [APP] [US4] API interfaces+impls in `.../data/api/`: `InventoryItemApi`/`InventoryTransactionApi` → `ApiUrlBuilder.inventoryUrl("v1/items/sync")` / `"v1/transactions/sync"`, snake_case params, `Response<PageResponse<T>>`/`postList`.
- [x] T035 [APP] [US4] `InventoryItemSyncDelegate` (`@ContributesIntoMap(WorkspaceScope)`, `@SyncEntityKey(SyncEntity.INVENTORY)`): batched pull (incl. soft-deleted → hard-delete; local-unsynced wins), bulk push (UID-keyed, mark synced). Mirror `CustomerSyncDelegate`.
- [x] T036 [APP] [US4] `InventoryTransactionSyncDelegate` (`@SyncEntityKey(INVENTORY_TRANSACTION)`, `pushDependencies` = after `INVENTORY`): append-only push (create only), pull upserts incl. server `balance_after`/`transaction_number`.
- [x] T036a [P] [APP] [US4] Create the **`feature/inventory-api`** contract module (mirror `feature/customer-api`): `build.gradle.kts`, add `:feature:inventory-api` to `settings.gradle.kts`. Define `InventoryDataService` (`getStock`/`observeStock`) + `InventoryStockInfo` model (read-side availability for cross-feature use). See contracts/stock-integration.md "App-side cross-communication".
- [x] T036b [APP] [US4] In `feature/inventory`, implement `InventoryDataServiceImpl` over `InventoryItemDao` and bind it with `@ContributesBinding(WorkspaceScope::class)`; `feature/inventory` depends on `feature/inventory-api`.
- [ ] T036c [APP] [US4] Make `feature/order` and `feature/invoice` depend on **`feature/inventory-api`** (not `feature/inventory`); inject `InventoryDataService` to show available/low-stock in the line-item editor (cf. `customer-api` usage). Keeps the module graph acyclic.
- [ ] T037 [APP] [US4] Run the 3-target compile gate (`androidApp:compileDebugKotlinAndroid`, `shared:compileKotlinIosSimulatorArm64`, `desktopApp:compileKotlin`) and fix any commonMain platform leaks.
- [ ] T038 [APP] [US4] Sync round-trip test/manual validation (quickstart F): offline create+adjust persists, PENDING_PUSH survives restart, auto-push on reconnect, server changes pull in, local-edit-wins.

**Checkpoint**: Mobile inventory data layer is offline-first and architecture-conformant (Scenario G). Other mobile stories can now add UI.

---

## Phase 5: User Story 2 — Adjust stock with a reason & see history (Priority: P1)

**Goal**: Manual stock-in/out/adjustment with reason; immutable movement history with running balance (FR-005–009).
**Independent Test**: quickstart.md Scenario C (stock-in +5, stock-out −2, history newest-first, qty=0 rejected).

- [ ] T039 [P] [US2] [APP] Item list screen + `InventoryListViewModel` (MVI: `StateFlow` UiState + `SharedFlow` events; Paging3 + debounced search; `observeEntity(INVENTORY)` → isRefreshing; `TriggerPull` on open) in `.../ui/list/` + `.../viewmodel/`.
- [ ] T040 [P] [US2] [APP] Item form (create/edit) screen + assisted ViewModel: `UidGenerator.generateUid("INV")` for new; edits pricing/metadata/reorder; save via local-only repo. `.../ui/detail/` form section.
- [ ] T041 [US2] [APP] Item detail screen with **movement history** (Paging3 from `InventoryTransactionDao`, newest-first; reason, signed qty, `balance_after`, source, locale-formatted date) in `.../ui/detail/`.
- [ ] T042 [US2] [APP] Stock-adjustment flow (`.../ui/adjust/`): pick type (stock-in/out/adjustment) + reason + positive quantity (reject ≤0); create append-only movement via repo with `UidGenerator` uid; provisional local `balanceAfter` overwritten on pull.
- [ ] T043 [P] [US2] [APP] All US2 strings in `feature/inventory/src/commonMain/composeResources/values/strings.xml`; no hardcoded UI text; money/date via `formatMoney`/`formatDate(LocalAppLocale.current)`.
- [ ] T044 [US2] [APP] 3-target compile gate after these commonMain changes.

**Checkpoint**: Users can adjust stock and read a trustworthy history offline; movements sync as append-only.

---

## Phase 6: User Story 3 — Low-stock visibility & alerts (Priority: P1)

**Goal**: Dashboard (low-stock / out-of-stock / total value) + dedup'd alerts gated on the setting (FR-015–017, SC-004/009).
**Independent Test**: quickstart.md Scenario E (low at ≤reorder, out at ≤0, total value reconciles, no duplicate alerts).

- [ ] T045 [BE] [US3] Wire low-stock/out-of-stock detection in `InventoryScheduler` + `InventoryItemService` to the **notification** module; gate on `settingService.getBoolean("inventory","enable_low_stock_alerts")`; dedup per `(item, condition)` (track last-alerted condition) so unchanged states don't re-alert.
- [ ] T046 [P] [US3] [BE] Test: crossing reorder generates one alert; persistent condition generates no duplicate; out-of-stock transition alerts.
- [ ] T047 [P] [US3] [APP] Inventory dashboard (`.../ui/dashboard/` + ViewModel): low-stock list, out-of-stock list, total stock value (Σ on-hand×cost) derived reactively from local DAO; pull-to-refresh via `TriggerFullSync(INVENTORY)`.
- [ ] T048 [US3] [APP] Reorder-level edit on the item form drives low/out classification; strings + locale formatting; 3-target compile gate.

**Checkpoint**: Owners see what's low/out and get non-spammy alerts.

---

## Phase 7: User Story 5 — Physical stock take (Priority: P2)

**Goal**: Count reconciles on-hand to counted value; difference recorded as a COUNT movement; equal counts produce nothing (FR-020, SC-003).
**Independent Test**: quickstart.md Scenario D.

- [ ] T049 [BE] [US5] `InventoryTransactionService.physicalCount(item, counted)` (or via the transactions bulk-upsert COUNT path): create one COUNT-adjustment movement = counted − system on-hand; **no movement when equal**.
- [ ] T050 [APP] [US5] Physical-count flow (`.../ui/count/` + ViewModel): per-item counted entry; submit creates COUNT movements via repo (only for differing items); strings; 3-target compile gate.

**Checkpoint**: Counts re-anchor system stock to reality with an audit trail.

---

## Phase 8: User Story 6 — Configure inventory behavior (Priority: P2)

**Goal**: Inventory policy editable via the existing central settings UI; reads applied everywhere (FR-018/019/019a).
**Independent Test**: Toggle `inventory/auto_deduct_on_order` off in workspace settings → sales stop changing stock; on → resume. Policy not shown when module uninstalled.

- [ ] T051 [APP] [US6] Confirm the 5 `inventory/*` definitions surface in the existing `feature/store` settings screen for installed workspaces (no new screen). Verify edits push via `SyncEntity.STORE`.
- [ ] T052 [P] [US6] [APP] Inject `StoreSettingsProvider` into inventory ViewModels that need policy at display time (e.g., observe `inventory/allow_manual_override` to gate manual adjust UI); `observeBoolean("inventory", key, default)`.
- [ ] T053 [US6] [BE] Verify backend policy reads (T010/T021/T045) resolve via `SettingService` end-to-end (definition default when no override; override when set; per-device consistency via STORE sync).

**Checkpoint**: Policy is centrally managed, synced across devices, and honored by both clients.

---

## Phase 9: Routing, Navigation & Polish (Cross-Cutting)

- [ ] T054 [APP] Expand `InventoryRoute` in `shared/src/commonMain/.../Routes.kt` (Dashboard/List/Detail/Adjust/Count) and wire `InventoryEntryProvider.kt`; confirm `ModuleRegistry` maps `inventory-management` → `Route.Inventory`.
- [ ] T055 [P] [APP] Re-point `InventoryActionHandler` (agent) to the new repositories/DAOs (SEARCH/COUNT/GET_INVENTORY/LIST low-stock).
- [ ] T056 [P] [BE] Update `docs/modules/product.md` (inventory section) + `docs/guides/offline-sync-contract.md` "Resources on the contract" to list `inventory` items + transactions and note transactions are append-only.
- [ ] T057 [BE] Run `./gradlew testAll` (Docker up) — all inventory tests + existing suites green; `./gradlew :ampairs_service:flywayValidate`.
- [ ] T058 [APP] Final 3-target compile gate + `./gradlew check`.
- [ ] T059 Execute quickstart.md Scenarios A–H as acceptance; check off each success criterion (SC-001…SC-009).

---

## Dependencies & Execution Order

### Phase order
- **Phase 1 Setup** → **Phase 2 Foundational** (BLOCKS everything). Within Phase 2, migrations (T005–T011) and `/sync` (T012–T018) gate mobile.
- **Phase 3 (US1)** is backend-only and can start as soon as T005–T011 + T019 + **T019a** (order/invoice line→`StockLine` mapping, finding U1) land — it is the **MVP** and does not need mobile.
- **Phase 4 (US4)** needs Phase 2 `/sync` (T012–T018) stable; it is the **mobile foundation** and must precede US2/US3/US5 mobile UI.
- **Phase 5 (US2)**, **Phase 6 (US3)**, **Phase 7 (US5)** mobile UI all depend on Phase 4. Their backend parts (T045/T049) depend only on Phase 2.
- **Phase 8 (US6)** depends on T010 (definitions) + Phase 4 (mobile provider wiring).
- **Phase 9** last.

### Cross-repo gate
- APP Phases 4–8 are blocked on BE Phase 2 `/sync` endpoints (T017/T018) being deployed/stable. BE Phase 3 (US1) can proceed fully in parallel with APP Phase 4.

### Parallel opportunities
- Setup: T002/T003/T004 in parallel.
- Phase 2: DTOs T012/T013 in parallel; settings T010 parallel to sync work; T020 (APP cleanup) parallel to all BE.
- Phase 4: T028/T029/T031/T032/T034 in parallel (different files) before T033/T035/T036 wire them.
- US1 tests T026 parallel; dashboard T047 parallel to BE alerts T045.

---

## Implementation Strategy

- **MVP = Phase 1 + Phase 2 + Phase 3 (US1)**: backend auto-deduction on sale — the single highest-value outcome — demoable via API/quickstart B with zero mobile work.
- **Increment 2 = Phase 4 (US4)**: mobile offline-first foundation + items/movements sync.
- **Increment 3 = Phases 5–6 (US2, US3)**: adjustments/history + low-stock dashboard/alerts.
- **Increment 4 = Phases 7–8 (US5, US6)**: physical count + settings polish.
- **Finish = Phase 9**: routing, docs, full test + quickstart acceptance.

## Notes
- Every Flyway change ships under BOTH `db/migration/mysql/` and `db/migration/postgresql/` (verify version order, esp. the inventory_config backfill-before-drop across `product`/`setting`).
- No `inventory_config` table, `/config/sync`, `INVENTORY_CONFIG` SyncEntity, or mobile config entity/delegate/screen anywhere.
- Deferred (multi-warehouse, batch/serial, ledger) stays OUT; the model must not preclude it.
- Commit after each task or logical group; keep both repos on `claude/zealous-meitner-q8hovy`.
