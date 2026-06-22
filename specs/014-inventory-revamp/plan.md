# Implementation Plan: Inventory Module Revamp (Pragmatic Core)

**Branch**: `claude/zealous-meitner-q8hovy` (feature key `014-inventory-revamp`) | **Date**: 2026-06-22 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/014-inventory-revamp/spec.md`

## Summary

Replace the incoherent inventory implementation with a single, offline-first, **single-warehouse**
inventory product spanning the backend (`ampairs/product` → `com.ampairs.inventory`) and the mobile app
(`ampairs-app/feature/inventory`). The core outcomes: (1) **stock moves automatically** when a sale is
confirmed/returned, idempotently and policy-driven; (2) every change is an **immutable, auditable
movement** with running balance; (3) **low-stock visibility + alerts**; (4) the mobile module is genuinely
**offline-first** via the canonical `/sync` contract and the gold-standard customer-module architecture.

Technical approach: standardize the backend on the existing rich entities (`InventoryItem`,
`InventoryTransaction`) constrained to a single default `Warehouse`; retire the legacy flat `Inventory`
entity **and the dedicated `InventoryConfig` entity/table**; expose the canonical `/sync` contract for
items and transactions (movements); move inventory policy into the **central `setting` module** (declare an
`InventorySettingDefinitions` provider; read effective values via `SettingService.getBoolean("inventory",
…)`); implement order/invoice→stock deduction in `InventoryTransactionService` behind a cross-module public
service interface with an **idempotency key per source document + line**; wire the dormant low-stock/expiry
schedulers to the notification module. On mobile, rebuild `feature/inventory` to the customer template: new
Room model (`InventoryItemEntity`, `InventoryTransactionEntity`), **local-only repository** +
`markPendingPush`, dedicated **SyncDelegates** owning all API traffic, **MVI ViewModels**, new UX
(dashboard, list, detail+history, adjust, count), Navigation3 wiring, and a Room migration off the old flat
entity. Inventory policy on mobile is read via the existing `StoreSettingsProvider`
(`getBoolean/observeBoolean("inventory", …)`) and edited through the existing generic workspace-settings
screen — **no inventory-specific settings screen, sync delegate, or DB table**.

Deferred (explicitly out of scope; model must not preclude them): multi-warehouse/transfers, batch/lot +
expiry (FIFO/FEFO/LIFO), serial tracking, ledger/valuation snapshots.

## Technical Context

**Language/Version**: Backend — Kotlin 2.3 / Java 21 (Spring Boot 4.0). Mobile — Kotlin 2.4 (KMP),
Compose Multiplatform 1.11.
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), `core` (ApiResponse,
PageResponse, OwnableBaseDomain, TenantContextHolder). Mobile — Metro DI, Room KMP 2.8, Ktor 3.5,
Navigation3, Paging3, kotlinx.datetime/serialization, `data/sync` (CentralSyncService, SyncDelegate,
SyncEntity, SyncStateDao).
**Storage**: Backend — PostgreSQL (runtime) + MySQL (migrations written for both), `TIMESTAMPTZ`/`Instant`.
Mobile — Room (workspace-scoped SQLite per workspace slug).
**Testing**: Backend — JUnit5 + Testcontainers (`./gradlew testAll`). Mobile — `./gradlew check` plus the
3-target compile gate (`androidApp:compileDebugKotlinAndroid`, `shared:compileKotlinIosSimulatorArm64`,
`desktopApp:compileKotlin`).
**Target Platform**: Backend service; mobile Android/iOS/Desktop (Wasm best-effort).
**Project Type**: Mobile + API (cross-repo).
**Performance Goals**: Sync batches of 100 records, ≤10k/cycle; movement history paged; dashboard
aggregates computed from local DB. Auto-deduction adds negligible latency to order/invoice confirmation.
**Constraints**: Offline-capable on mobile; multi-tenant isolation; idempotent deduction (zero
double-counts); local-edit-wins conflict resolution; all money/date via workspace business locale.
**Scale/Scope**: Per-workspace inventory (typically hundreds–low-thousands of SKUs); ~6 mobile screens
(no bespoke settings screen — reuses the central workspace-settings UI); **2** backend `/sync` resources
(items, transactions) + inventory policy via the central `setting` module + 1 cross-module stock
integration + alerts.

**Resolved decision (order/invoice trigger)**: the order/invoice lifecycle event(s) that trigger
deduction/restoration are **confirmed as the R3 default** — deduct on order-confirm (or invoice-finalize
for invoice-first workspaces), restore on order-cancel / return-credit-note / invoice-void. Deduction is
idempotent regardless of which event fires (so a belt-and-suspenders event+call combination is safe). The
remaining implementation detail (the exact order/invoice line → `StockLine` mapping and service call sites)
is a discovery task in tasks.md (T023/T024 + U1), not an open design question.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type safety (`Instant`) | ✅ | All new/changed entity & DTO timestamps are `Instant`; Postgres `TIMESTAMPTZ`. |
| II. DTO isolation | ✅ | `/sync` Request/Response DTOs in `inventory/domain/dto/`; never expose JPA entities. |
| III. Global SNAKE_CASE | ✅ | No `@JsonProperty` for standard fields; snake_case query params. |
| IV. Multi-tenancy | ✅ | Entities extend `OwnableBaseDomain`; tenant context set in controllers; sync queries auto-filtered. |
| V. ApiResponse | ✅ | `ApiResponse<PageResponse<T>>` (pull) / `ApiResponse<List<T>>` (push). |
| VI. Centralized exceptions | ✅ | Reuse existing `InventoryExceptionHandler`; no try/catch in controllers. |
| VII. EntityGraph (N+1) | ✅ | Reuse `InventoryItem.withRelations` graph; add graphs for transaction reads as needed. |
| VIII. Angular M3 | ➖ N/A | No web work in this feature. |
| IX. Module boundaries | ⚠️→✅ | Inventory stays inside `product` (its current home) but order/invoice→stock goes through a **public service interface** (`InventoryStockService`), never direct repository/entity access. Inventory policy is consumed via the `setting` module's public `SettingService`, not a parallel store. Justified in Complexity Tracking. |
| Setting reuse (no parallel config) | ✅ | Inventory policy is declared as `SettingDefinition`s and stored/synced through the central `setting` module (precedent: `common/prices_include_tax`, `invoice/show_discount_options`); the dedicated `InventoryConfig` entity/table is retired. |
| X. CMP parity | ✅ | All UI/VM/logic in `commonMain`; platform code only for DB factories; parity tracked in tasks. |
| XI. Secrets hygiene | ✅ | No secrets introduced. |
| Flyway (both vendors) | ✅ | Every migration written under `db/migration/mysql/` and `db/migration/postgresql/`. |
| Testing gates | ✅ | Backend `testAll` (idempotency/concurrency/sync round-trip); mobile 3-target compile + `check`. |

**Gate result: PASS** (one justified note under Complexity Tracking re: module placement).

## Project Structure

### Documentation (this feature)

```
specs/014-inventory-revamp/
├── plan.md              # This file
├── research.md          # Phase 0 — decisions (idempotency, trigger event, legacy retirement, SyncEntity additions)
├── data-model.md        # Phase 1 — entities (backend JPA + mobile Room), fields, relationships, state
├── quickstart.md        # Phase 1 — end-to-end validation guide
├── contracts/
│   ├── inventory-sync-api.md        # /sync contracts: items, transactions
│   ├── inventory-settings.md        # inventory policy via the central setting module
│   └── stock-integration.md         # order/invoice → InventoryStockService contract
├── checklists/
│   └── requirements.md  # spec quality checklist (done)
└── tasks.md             # Phase 2 — produced by /speckit-tasks (NOT here)
```

### Source Code (the two repositories)

```
# Backend — ampairs/product/src/main/kotlin/com/ampairs/inventory/
inventory/
├── domain/
│   ├── model/         # InventoryItem, InventoryTransaction (KEEP); Warehouse (single default)
│   │                  # RETIRE: legacy Inventory.kt AND InventoryConfig.kt (→ central setting module)
│   ├── dto/           # ADD: ItemSync(Request/Response), TransactionSync(Request/Response)
│   └── enums/         # TransactionType/Reason (KEEP)
├── config/            # ADD: InventorySettingDefinitions : SettingDefinitionProvider (5 inventory/* keys)
├── controller/        # ADD: /sync endpoints on Item/Transaction controllers (remove map-shaped GET /items)
├── service/           # KEEP InventoryItem/Transaction services; ADD getAfterSync + bulkUpsert;
│                      # ADD InventoryStockService (public interface for order/invoice integration);
│                      # RETIRE InventoryConfigService → read via core SettingService.getBoolean("inventory", …)
├── repository/        # ADD findUpdatedAfter / findAllForSync (sync feeds incl. soft-deleted)
├── listener/          # InventoryOrderEventListener → call InventoryStockService (or replace with explicit call)
├── scheduler/         # wire low-stock/expiry → notification module (gate on inventory/enable_low_stock_alerts)
└── src/main/resources/db/migration/{mysql,postgresql}/   # new Vx migrations (idempotency col, sync fields,
                                                          #   retire legacy Inventory + inventory_config,
                                                          #   optional backfill inventory_config → store_setting)

# Mobile — ampairs-app/feature/inventory/src/
inventory/ (commonMain/kotlin/com/ampairs/inventory/)
├── data/
│   ├── db/            # REBUILD: InventoryItemEntity, InventoryTransactionEntity (+ synced flags),
│   │                  #   InventoryDatabase, DAOs (reactive Flow + Paging sources), migration off old flat InventoryEntity
│   ├── api/           # InventoryItemApi/TransactionApi (+ impls) → /sync endpoints
│   └── repository/    # local-only repos: Room write synced=false + syncStateDao.markPendingPush
├── domain/            # Inventory item/movement domain models + mappers
├── sync/              # InventoryItemSyncDelegate, InventoryTransactionSyncDelegate
├── ui/
│   ├── dashboard/     # low-stock / out-of-stock / total value
│   ├── list/          # item list + search (Paging3)
│   ├── detail/        # item detail + movement history (Paging3)
│   ├── adjust/        # stock-adjustment flow
│   └── count/         # physical-count flow
│                      # (NO settings screen — policy edited via the existing feature/store settings UI)
├── viewmodel/         # MVI ViewModels (StateFlow UiState + SharedFlow events);
│                      #   inject StoreSettingsProvider to read inventory/* policy (getBoolean/observeBoolean)
├── di/                # @ContributesTo(WorkspaceScope) DAO + (android/ios/desktop) DB providers
└── composeResources/values/strings.xml   # all UI strings

# Shared mobile wiring — ampairs-app/shared/src/commonMain/
├── Routes.kt                                  # expand InventoryRoute (dashboard/list/detail/adjust/count)
├── navigation/providers/InventoryEntryProvider.kt   # wire new routes
└── data/sync/.../SyncEntity.kt                # ADD INVENTORY_TRANSACTION (INVENTORY exists; config via SyncEntity.STORE)
```

**Structure Decision**: Mobile + API (cross-repo). Backend inventory stays within the `product` module
(its existing home per the module-ownership table) rather than spinning out a new module, to avoid a
large module-extraction yak-shave inside this feature; cross-module access is mediated by a public
`InventoryStockService` interface. Mobile follows the established per-feature module layout mirrored from
`feature/customer`.

## Cross-Repo Sequencing (critical)

1. **Backend first**: data-model alignment + migrations (A) → `/sync` contract (B) → stock integration (C)
   → alerts (D). The `/sync` endpoints and DTO field names MUST land and be stable before mobile sync can
   be built/tested against them.
2. **Mobile second**: rebuild data+sync layer (E) against the now-stable contract → UX/navigation (F).
3. **Both**: migration/rollout + tests (G). Backend `testAll` and mobile 3-target compile gates run
   continuously; idempotency/concurrency and sync round-trip tests gate completion.

Workstreams A/B can proceed in parallel with mobile UX scaffolding (F) that doesn't depend on live data,
but mobile **sync** (E) is blocked on B.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Inventory remains a sub-context inside `product` rather than its own module (slight tension with Principle IX "new bounded contexts get their own module") | Inventory already lives in `product` with substantial code; extracting a module is a separate, risky refactor that would balloon this feature and touch the build graph (`migrationModules`, `ampairs_service` wiring) | Spinning out a new `inventory` module now multiplies surface area and migration risk for no user-facing benefit; mitigated by exposing only a **public service interface** for cross-module use, satisfying the boundary intent. Module extraction can be a clean follow-up. |
| Idempotency key column added to `inventory_transaction` | Auto-deduction must never double-count on retried events / at-least-once delivery (SC-002) | A "check if a transaction already references this doc+line" query is racy under concurrency; a unique constraint on `(source_type, source_id, source_line_uid, owner_id)` enforces it at the DB and is simpler to reason about. |
