---
description: "Task list for Brand → Distributor DMS + Sales Force Automation"
---

# Tasks: Brand → Distributor DMS + Sales Force Automation

**Input**: Design documents from `/specs/021-brand-distributor-dms-sfa/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, **module-boundaries.md**

> **Module placement** is governed by `module-boundaries.md` (contexts `trade`/`sfa`/`dms`/`claim`); the `ampairs/trade/...` paths below predate the four-module split — map each per its capability/module row.

**Tests**: INCLUDED — the spec's Constitution Check and Testing & Quality Gates explicitly require them
(cross-tenant consent gate, snapshot-recompute determinism, claim lifecycle, retailer-PII projection,
offline author→sync round-trip; backend ≥80% critical / ≥90% endpoints).

**Two repositories** — this feature spans both:
- **Backend** `ampairs/` (this repo) — new `trade` bounded context.
- **Mobile** `ampairs-app/` (sibling repo) — new `feature/trade` offline-first SFA module + `shared/` wiring.
  Paths below prefixed `ampairs-app/` belong to the mobile repo (separate branch/PR; same branch name
  `claude/brand-distributor-dms-sfa-12692h`).

**Organization**: by user story (spec.md priorities). US1 (offline rep app) is the standalone MVP.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: parallelizable (different files, no dependency)
- **[Story]**: US1..US6 (maps to spec user stories); `—` = setup/foundational/polish

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the backend `trade` module and the mobile `feature/trade` module skeletons + build wiring.

- [x] T001 [—] Create backend module `ampairs/trade/` with `build.gradle.kts` (depends on `core`, `workspace`, `order`, `invoice`, `product`, `customer`, `event` as needed via api/implementation), and package skeleton `src/main/kotlin/com/ampairs/trade/{domain/model,domain/dto,domain/enums,repository,service,controller,config,listener}` + `src/test/kotlin/com/ampairs/trade/`.
- [x] T002 [—] Register the module: add `include("trade")` to `ampairs/settings.gradle.kts`; add `implementation(project(":trade"))` to `ampairs/ampairs_service/build.gradle.kts`; add `"trade"` to `migrationModules` in `ampairs/ampairs_service/build.gradle.kts`.
- [x] T003 [P] [—] Add `ampairs/trade/src/main/resources/db/migration/{postgresql,mysql}/` directories; confirm next free global Flyway version with `./gradlew :ampairs_service:flywayInfo` (plan assumes **V1.0.117**; bump if taken).
- [ ] T004 [P] [—] Create mobile module `ampairs-app/feature/trade/` with `build.gradle.kts` (KMP targets android/ios/desktop, Room/Ktor/Metro, Moko Permissions + Play Services Location on androidMain) and package skeleton `src/commonMain/kotlin/com/ampairs/trade/{data/api,data/db,data/repository,domain,di,sync,ui}` + platform source sets; add `include(":feature:trade")` to `ampairs-app/settings.gradle.kts`.
- [ ] T005 [P] [—] Add `ApiUrlBuilder.tradeUrl(path)` in `ampairs-app/data/common/.../ApiUrlBuilder.kt` (mirrors existing `customerUrl`/`orderUrl`).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Cross-cutting trade infrastructure every story needs. **⚠️ No user story can start until this is done.**

- [x] T006 [—] Backend: typed exception hierarchy in `ampairs/trade/.../config/TradeExceptions.kt` — `TradeException` (422), `ConsentRequiredException` (403), `LinkStateException` (409), `ClaimStateException` (409); ensure each maps in the global handler (verify `core` GlobalExceptionHandler picks them up or register mappers).
- [x] T007 [P] [—] Backend: shared enums in `ampairs/trade/.../domain/enums/` — `TradeTier`, `SalesType` (PRIMARY/SECONDARY/TERTIARY) used across stories.
- [x] T008 [—] Backend: add `FIELD_REP("Field Representative", 30, "...")` to `ampairs/workspace/.../model/enums/WorkspaceRole.kt` (level 30, between GUEST=20 and MEMBER=40); update any role-permission matrices/tests that enumerate roles.
- [x] T009 [P] [—] Backend: `CrossTenantReadGuard` scaffold in `ampairs/trade/.../service/CrossTenantReadGuard.kt` — interface + method `requireActiveLink(brandWorkspaceId, distributorWorkspaceId, category): TradeLink` throwing `ConsentRequiredException`; real link lookup wired in US2/US3 (stub returns deny until TradeLink exists).
- [ ] T010 [P] [—] Mobile: register sync entities — add `VISIT, FIELD_ORDER, ATTENDANCE, BEAT, JOURNEY_PLAN` to `ampairs-app/data/sync/.../SyncEntity.kt`.
- [ ] T011 [P] [—] Mobile: add `Route.Trade` + sub-routes in `ampairs-app/shared/.../Routes.kt`; register `"dms-sfa" → Route.Trade` in `ModuleRegistry.kt`; create empty `TradeEntryProvider` wired into `CombinedEntryProvider`.
- [ ] T012 [P] [—] Mobile: `TradeRoomDatabase` + workspace-scoped DI skeleton — `ampairs-app/feature/trade/.../data/db/TradeRoomDatabase.kt` and `di/TradeModule.{android,ios,desktop}.kt` (`@ContributesTo(WorkspaceScope::class)`, `@SingleIn(WorkspaceScope::class)`, `createDatabase<TradeRoomDatabase>(...)`, register with `WorkspaceClosableRegistry`).
- [ ] T013 [P] [—] Mobile: geo/permissions expect/actual — `ampairs-app/feature/trade/.../domain/GeoLocationProvider.kt` (expect) + android (Play Services + Moko Permissions), ios, desktop (stub) actuals; returns nullable lat/lng (never throws on denial).

**Checkpoint**: Trade modules build on all targets; role + sync entities + DB skeleton exist. User stories can begin.

---

## Phase 3: User Story 1 — Field rep runs the daily beat offline (Priority: P1) 🎯 MVP

**Goal**: A distributor's FIELD_REP can run today's beat fully offline — visits (geo-flagged), counter orders, attendance, ad-hoc visits, offline new-outlet — syncing idempotently when online.

**Independent Test**: In airplane mode, open today's beat, check in (out-of-radius still saved+flagged), take a counter order, add a new outlet, check out; re-enable network → every record uploads exactly once and appears in the distributor's data. (SC-001/002/003/010)

### Tests for User Story 1 ⚠️
- [x] T014 [P] [US1] Backend contract test for `GET/POST /trade/v1/visits/sync` (UID-keyed idempotent upsert, soft-deletes in pull feed) in `ampairs/trade/src/test/.../VisitSyncContractTest.kt`.
- [x] T015 [P] [US1] Backend contract tests for `field-orders`, `attendance`, `beats`, `journey-plans` `/sync` in `ampairs/trade/src/test/.../{FieldOrder,Attendance,Beat,JourneyPlan}SyncContractTest.kt`.
- [x] T016 [P] [US1] Backend test: ad-hoc validation (`ad_hoc=false`⇒planned_visit required; `ad_hoc=true`⇒null) + geo-fence flag is informational (out-of-radius/no-location row still upserts) in `ampairs/trade/src/test/.../VisitRulesTest.kt`.
- [x] T016a [P] [US1] Backend test for adherence (FR-017/SC-010): a Visit referencing a PlannedVisit marks it VISITED; a passed day with no Visit marks MISSED; ad-hoc visits are excluded from planned-adherence % but counted separately, in `ampairs/trade/src/test/.../AdherenceTest.kt`.
- [ ] T016b [P] [US1] Backend test for rep-removed-from-beat scoping (FR-015 / Edge Cases): after a rep loses a beat assignment they can no longer read/act on those outlets, but Visits they already authored remain valid, in `ampairs/trade/src/test/.../BeatScopingTest.kt`.
- [ ] T016c [P] [US1] Backend test for outlet deactivation (Edge Cases): a deactivated customer drops off future beats/planned visits while already-captured Visit/FieldOrder history is unaffected, in `ampairs/trade/src/test/.../OutletDeactivationTest.kt`.
- [ ] T017 [P] [US1] Mobile offline round-trip test: author visit+order+attendance+new-outlet offline → push → assert single upsert + re-push idempotent, in `ampairs-app/feature/trade/src/commonTest/.../OfflineSyncRoundTripTest.kt`. Also assert/record (or manual-QA per SC-001) that a full offline visit capture (check-in + outcome + counter order) completes in **< 60 s** with no network.
- [ ] T017a [P] [US1] Mobile multi-device merge test: the same rep on two devices (and out-of-order sync) authors visits/orders offline → after sync all records merge with no loss and latest-authoritative wins, in `ampairs-app/feature/trade/src/commonTest/.../MultiDeviceMergeTest.kt`.

### Implementation for User Story 1 — Backend
- [x] T018 [P] [US1] Enums in `ampairs/trade/.../domain/enums/` — `VisitOutcome`, `GeoFenceStatus`, `AttendanceType`, `PlannedVisitStatus`.
- [x] T019 [P] [US1] Entities `Beat`, `BeatOutlet` (+ `@NamedEntityGraph("Beat.outlets")`) in `ampairs/trade/.../domain/model/`.
- [x] T020 [P] [US1] Entities `JourneyPlan`, `PlannedVisit` in `ampairs/trade/.../domain/model/`.
- [x] T021 [P] [US1] Entities `Visit` (geoFenceStatus, distanceMeters, adHoc, lat/lng, synced/active), `Attendance`, `FieldOrder` in `ampairs/trade/.../domain/model/`.
- [x] T022 [US1] Flyway `V1.0.117__create_trade_module_tables.sql` in BOTH `postgresql/` and `mysql/` — network+SFA tables for US1+US2 (beats, beat_outlets, journey_plans, planned_visits, visits, attendance, field_orders; + trade_networks, trade_links, network_retailers, network_brands, network_products from US2). TIMESTAMPTZ/TIMESTAMP, DECIMAL(19,4) money, owner_id column.
- [x] T023 [P] [US1] Request/Response DTOs + converters for visit/field-order/attendance/beat/journey-plan in `ampairs/trade/.../domain/dto/` (snake_case, `@field:` validation, `asResponse()`/`toEntity()`).
- [x] T024 [P] [US1] Spring Data repositories for the 7 SFA entities in `ampairs/trade/.../repository/` (derived queries; `getXAfterSync(Instant?, Pageable)` incl. soft-deleted rows).
- [x] T025 [US1] `BeatService` + `JourneyPlanService` in `ampairs/trade/.../service/` — CRUD + today's-planned-visits derivation; FIELD_REP beat-scoping enforcement (rep sees only their distributor's beats).
- [x] T026 [US1] `VisitService` + `AttendanceService` + `FieldOrderService` in `ampairs/trade/.../service/` — bulk UID-keyed upsert; geo-fence flag compute (distance to outlet, never block); FieldOrder ties to `order` module via `OrderService` + tags SECONDARY; ad-hoc rule enforcement.
- [x] T027 [US1] `TradeSyncController` in `ampairs/trade/.../controller/` — `GET/POST /trade/v1/{visits|field-orders|attendance|beats|journey-plans}/sync` returning `ApiResponse<PageResponse<>>`/`ApiResponse<List<>>`; sets/clears tenant via X-Workspace-ID; per `contracts/trade-sfa-sync.md`.
- [x] T027a [US1] Adherence in `JourneyPlanService` (or `AdherenceService`) in `ampairs/trade/.../service/` — reconcile PlannedVisit → VISITED/MISSED from authored Visits; compute visit % / on-time % per rep × period with ad-hoc counted separately (FR-017); planned visits on a `Leave`-excused day (T085/T088) are excused, not missed; expose `GET /trade/v1/adherence?rep_member_uid&period_from&period_to` on `TradeSyncController` returning `ApiResponse<AdherenceSummary>` (SC-010).

### Implementation for User Story 1 — Mobile (offline-first)
- [ ] T028 [P] [US1] Room entities + DAOs for visit/field-order/attendance/beat/beat-outlet/journey-plan/planned-visit in `ampairs-app/feature/trade/.../data/db/` (synced/active flags; client-generated uids via `UidGenerator`).
- [ ] T029 [P] [US1] `TradeApi` + impl (`tradeUrl`, multipart not needed) in `ampairs-app/feature/trade/.../data/api/`.
- [ ] T030 [US1] Local-only repositories (`VisitRepository`, `FieldOrderRepository`, `AttendanceRepository`, `BeatRepository`) in `ampairs-app/feature/trade/.../data/repository/` — write Room `synced=false` + `syncStateDao.markPendingPush(...)`; NO Api in write path.
- [ ] T031 [US1] SyncDelegates (`Visit/FieldOrder/Attendance/Beat/JourneyPlan`) in `ampairs-app/feature/trade/.../sync/` — `@ContributesIntoMap(WorkspaceScope::class)` + `@SyncEntityKey`; bulk push (synced=false rows, batch 100), batched pull (hard-delete server-DELETED), per canonical contract.
- [ ] T032 [US1] ViewModels (`metroViewModel`) — today's beat, outlet visit (capture geo+time, flag), add-outlet (creates `customer` via existing customer sync), check-in/out — in `ampairs-app/feature/trade/.../ui/`.
- [ ] T032a [US1] Counter-order capture: author the order through the **existing `feature/order` offline flow** (the order rides the `order` module's `/sync`); the trade `FieldOrder` only stores the resulting `orderUid` as a SECONDARY-tagged reference — no parallel order entity in `feature/trade`. Wire the take-order action from the visit screen into `feature/order`'s create path.
- [ ] T033 [US1] Compose screens (commonMain, stringResource, collectAsStateWithLifecycle) for the above + `TradeEntryProvider` wiring; geo capture via `GeoLocationProvider`; offline confirmations (no network block).
- [ ] T034 [US1] 3-target compile + check: `./gradlew :feature:trade:check shared:compileKotlinIosSimulatorArm64 androidApp:compileDebugKotlinAndroid desktopApp:compileKotlin` (mobile repo).

**Checkpoint**: A distributor can run the offline SFA rep app end-to-end and sync — shippable MVP, no brand required.

---

## Phase 4: User Story 2 — Brand ↔ distributor consented link (Priority: P1)

**Goal**: A brand invites a distributor; the distributor accepts with an agreed scope (coded outlets by default); either can revoke. No data flows before acceptance; isolation preserved.

**Independent Test**: Brand invites D → before accept, brand snapshot read = 403; D accepts (CODED) → link ACCEPTED; D revokes → brand read = 403 again. Multiple brands see only their own link. (SC-004/006/009, FR-001..007)

### Tests for User Story 2 ⚠️
- [x] T035 [P] [US2] Contract tests for `POST /trade/v1/links`, `/accept`, `/decline`, `/revoke` (state machine, `ApiResponse`) in `ampairs/trade/src/test/.../TradeLinkContractTest.kt`. Include authority (FR-031): invite/publish require brand ADMIN/OWNER; accept/decline/revoke are distributor-side; a non-admin actor is rejected.
- [x] T036 [P] [US2] Consent-gate test: with no ACCEPTED link, a cross-tenant read throws `ConsentRequiredException`; illegal transition (accept REVOKED) throws `LinkStateException`, in `ampairs/trade/src/test/.../ConsentGateTest.kt`.
- [x] T037 [P] [US2] Scope-default test: invite without `retailer_visibility` ⇒ CODED; multi-brand isolation (brand A cannot see brand B's link), in `ampairs/trade/src/test/.../ConsentScopeTest.kt`.
- [ ] T037a [P] [US2] Two-level product-linking test (FR-018a/018b): (Hop A) designating a `ProductBrand` label via `NetworkBrand` attributes all products under it to the brand; other-brand/untagged products are excluded; (Hop B) barcode/SKU auto-match proposes SUGGESTED `NetworkProduct`s and CONFIRMED ones itemize by brand SKU (same SKU across two distributors aggregates under one `brand_product_uid`); an attributed-but-unmapped product is **counted** in the brand's aggregated "unmapped" total, not dropped, in `ampairs/trade/src/test/.../ProductLinkingTest.kt`.
- [ ] T037b [P] [US2] NPI import test (FR-018c/018d): `available-for-import` lists only brand SKUs under a designated label not already carried (a barcode/SKU match or existing mapping excludes it — no duplicates); `import` creates the distributor product (pre-filled, tagged with the label) + a CONFIRMED `NetworkProduct`; a brand publishing a SKU emits the distributor notification, in `ampairs/trade/src/test/.../NpiImportTest.kt`.

### Implementation for User Story 2
- [x] T038 [P] [US2] Enums `LinkStatus`, `RetailerVisibility`, `DesignationStatus` (ACTIVE/REMOVED), `MatchSource` (AUTO_BARCODE/AUTO_SKU/MANUAL — no HSN), `MappingStatus` (SUGGESTED/CONFIRMED) in `ampairs/trade/.../domain/enums/`.
- [x] T039 [P] [US2] Entities `TradeNetwork`, `TradeLink`, `ConsentScope` (embeddable, defaults: retailerVisibility=CODED), `NetworkRetailer` in `ampairs/trade/.../domain/model/` (tables already in V1.0.117 migration T022).
- [x] T039a [P] [US2] (Hop B) Entity `NetworkProduct` (link, distributorProductUid, brandProductUid, brandSkuCode, matchSource, status; ≤1 CONFIRMED per (link, distributorProductUid)) in `ampairs/trade/.../domain/model/` + DTOs (`NetworkProductRow`, `BrandProductRow`) in `domain/dto/` (table in V1.0.117, T022).
- [x] T039b [P] [US2] (Hop A) Entity `NetworkBrand` (link, distributorProductBrandUid → existing `product_brand`, brandWorkspaceId, status; ≤1 brand per (link, label)) + `NetworkBrandRow` DTO in `ampairs/trade/.../domain/{model,dto}/` (table in V1.0.117, T022).
- [x] T040 [P] [US2] DTOs + converters (`TradeLinkResponse`, `ConsentScope`, invite/accept requests) in `ampairs/trade/.../domain/dto/` per `contracts/trade-network-actions.md`.
- [x] T041 [P] [US2] Repositories for network/link/network-retailer in `ampairs/trade/.../repository/` — incl. `findActiveLink(brand, distributor)` and uniqueness (≤1 non-revoked link per pair).
- [x] T042 [US2] `TradeLinkService` in `ampairs/trade/.../service/` — invite/accept/decline/revoke state machine; scope default CODED; distributor may tighten on accept; emit no data until ACCEPTED.
- [x] T043 [US2] Wire `CrossTenantReadGuard` (T009) to real `TradeLinkService.findActiveLink` + category check; replace deny stub.
- [x] T044 [US2] `TradeNetworkController` in `ampairs/trade/.../controller/` — link endpoints; brand ADMIN/OWNER for invite, distributor ADMIN/OWNER for accept/revoke; `ApiResponse<TradeLinkResponse>`.
- [x] T044a [US2] (Hop A) `NetworkBrandService` + endpoints — distributor designates one of its `ProductBrand` labels for a linked brand (`POST/DELETE /trade/v1/network-brands`), brand reads designations read-only (`GET /trade/v1/network-brands`); requires ACTIVE link; in `ampairs/trade/.../{service,controller}/` per `contracts/trade-network-actions.md`.
- [x] T044b [US2] (Hop B, optional) `NetworkProductService` + endpoints — brand publishes/serves its catalog (`GET /trade/v1/brand-catalog`, consent-gated), **barcode/SKU** auto-match (no HSN) producing SUGGESTED mappings over products under a designated label, distributor confirm/override (`GET/POST /trade/v1/network-products`).
- [ ] T044c [US2] (NPI, FR-018c) New-product import in `NetworkProductService` — searchable brand-catalog (`search`/`category`/`barcode`/`since`), `GET /trade/v1/brand-catalog/available-for-import` (brand SKUs under a designated label NOT already carried — no distributor product matching barcode/SKU and no existing mapping), and `POST /trade/v1/network-products/import` which creates the distributor `product` (pre-filled from the brand entry, tagged with the designated `ProductBrand` label) via the `product` module public service + a CONFIRMED `NetworkProduct`; in `ampairs/trade/.../{service,controller}/`.
- [ ] T044d [US2] (NPI notify, FR-018d) New-product notification — listen for the brand's product-published/added event (`ProductCreatedEvent`/`ProductCatalogChangedEvent` from the `event` module) and signal each linked distributor with a matching `NetworkBrand` designation ("N new products available to import") via the existing event/notification rail; in `ampairs/trade/.../listener/`.
- [ ] T045 [P] [US2] Mobile (distributor): minimal "Trade links" screen + ViewModel to accept/decline/revoke invitations in `ampairs-app/feature/trade/.../ui/links/` (online action over `tradeUrl`).
- [ ] T045a [P] [US2] Mobile/Web (distributor): linking screens + ViewModels — (Hop A) designate which of my `ProductBrand` labels = this linked brand; (Hop B, optional) review barcode/SKU auto-suggestions against the brand catalog and confirm/override to a brand SKU, in `ampairs-app/feature/trade/.../ui/mapping/`.
- [ ] T045b [P] [US2] Mobile/Web (distributor): "Available for import" screen + ViewModel — searchable list of the brand's new SKUs not yet carried, one-click import (creates the distributor product + mapping), and a "new products available" notification entry point, in `ampairs-app/feature/trade/.../ui/import/`.

**Checkpoint**: The consent edge works end-to-end; cross-tenant reads are gated. Foundation for all brand-facing stories.

---

## Phase 5: User Story 3 — Brand secondary-sales rollup (Priority: P2)

**Goal**: Distributor secondary sales (from their order/invoice docs, tagged SECONDARY) roll into versioned, recomputable `SecondarySalesSnapshot`s (event-triggered, ≤~5 min coalesced); the brand reads them aggregated across links, retailer dimension projected per scope.

**Independent Test**: Two linked distributors record retailer sales → brand secondary-sales view shows combined totals by SKU×period; unlinked distributor excluded; backdated/cancelled invoice self-corrects with no double count within ~5 min. (SC-005/011, FR-018..023)

### Tests for User Story 3 ⚠️
- [ ] T046 [P] [US3] Snapshot-determinism test: same source docs ⇒ identical snapshot; a backdated/cancelled invoice triggers a recompute that supersedes the prior version (no double count), in `ampairs/trade/src/test/.../SnapshotRecomputeTest.kt`.
- [ ] T047 [P] [US3] PII-projection test: scope CODED ⇒ `outlet_code` only (no name/area); scope IDENTIFIED ⇒ name/area present, never full contact PII, in `ampairs/trade/src/test/.../RetailerProjectionTest.kt`.
- [ ] T048 [P] [US3] Endpoint test: `GET /trade/v1/snapshots/secondary-sales?...=all-linked` aggregates only ACCEPTED links; no link ⇒ 403, in `ampairs/trade/src/test/.../SecondarySalesReadTest.kt`.
- [ ] T048a [P] [US3] Backend test for distributor-offline read (Edge Cases): the brand's secondary-sales/stock read returns the last published snapshot version (no live dependency on the distributor being online), in `ampairs/trade/src/test/.../SnapshotAvailabilityTest.kt`.
- [ ] T048c [P] [US3] Backend test for area derivation (FR-020a): SKU×PERIOD×AREA rollup keys `areaCode` off the retailer's `customer.pincode`; two distributors selling into the same pincode aggregate under one `area_code` for the brand (no per-distributor area mapping); city/state coarser rollups derive from the same address, in `ampairs/trade/src/test/.../AreaRollupTest.kt`.
- [ ] T048b [P] [US3] Backend test for two-level resolution (FR-018a/018b): an **other-brand/untagged** distributor sale never appears in the brand's view; an **attributed** sale appears even with no Hop B mapping (counted in the aggregated "unmapped" row); a Hop B-mapped sale is itemized by `brand_product_uid` and cross-distributor totals aggregate by it; a re-tag does not move historical attribution (point-in-time), in `ampairs/trade/src/test/.../BrandProductResolutionTest.kt`.
- [ ] T049 [P] [US3] Coalescing test: N rapid invoice events ⇒ ≤1 rebuild per ~5 min window per distributor, in `ampairs/trade/src/test/.../SnapshotDebounceTest.kt`.

### Implementation for User Story 3
- [ ] T050 [P] [US3] Enum `SnapshotGrain` + `SecondarySalesSnapshot` entity (key `(distributorWorkspaceId, grain, periodKey, sku, outlet/area, version)`) in `ampairs/trade/.../domain/model/`.
- [ ] T051 [US3] Flyway `V1.0.118__create_trade_snapshot_target_tables.sql` (both vendors) — secondary_sales_snapshots, distributor_stock_snapshots (US4), sales_targets (US5), primary_order_links (US5). Snapshot tables include `attributed_brand_workspace_id` (Hop A, set as-of-sale) + nullable `brand_product_uid` + `brand_sku_code` (Hop B) columns.
- [ ] T052 [P] [US3] Snapshot DTOs (`SecondarySalesRow`) + repository (versioned read, latest-version-per-key) in `ampairs/trade/.../{domain/dto,repository}/`. The cross-distributor `all-linked` aggregate query uses `nativeQuery = true` (bypassing `@TenantId`); single-distributor reads stay tenant-filtered.
- [ ] T053 [US3] `SnapshotService` in `ampairs/trade/.../service/` — deterministic recompute from distributor order/invoice (via `InvoiceService`/`OrderService` public reads); **Hop A**: attribute each row to a brand via `NetworkBrand` using the product's brand label **as of sale time** (point-in-time — capture/freeze attribution, don't derive from current label), exclude other-brand/untagged; **Hop B**: set `brandProductUid`/`brandSkuCode` where a CONFIRMED `NetworkProduct` exists, else count the row in the aggregated "unmapped" bucket (never drop); the **AREA grain** derives `areaCode` from each sale's retailer `customer.pincode` (city/district/state as coarser rollups) — comparable across distributors with no mapping; **debounced ≤ once/~5 min per (distributor, grain)**; bumps `version`.
- [ ] T054 [US3] Event listener in `ampairs/trade/.../listener/TradeSalesListener.kt` — on existing `InvoiceFinalizedEvent`/`InvoiceCancelledEvent` (+ order events) tag SECONDARY and enqueue debounced rebuild.
- [ ] T055 [US3] `SnapshotController` (secondary-sales read) in `ampairs/trade/.../controller/` — every `all-linked` cross-tenant aggregation runs `nativeQuery = true` strictly behind `CrossTenantReadGuard` (active link + scope checked first); retailer projection per scope; `ApiResponse<PageResponse<SecondarySalesRow>>`.
- [ ] T056 [P] [US3] Mobile/Web brand dashboard (online, pull-only): secondary-sales by SKU/beat/area screen + ViewModel in `ampairs-app/feature/trade/.../ui/dashboard/` (reads `tradeUrl("v1/snapshots/secondary-sales")`).

**Checkpoint**: Brand sees consented, self-correcting secondary-sales rollups across distributors.

---

## Phase 6: User Story 4 — Distributor stock & replenishment (Priority: P2)

**Goal**: Brand sees each linked distributor's on-hand stock per SKU (from their inventory) as versioned snapshots, plus days-of-stock / out-of-stock signals.

**Independent Test**: A linked distributor's on-hand qty appears in the brand stock view; unlinked excluded; reducing qty reflects on refresh. (SC + FR-020/021)

### Tests for User Story 4 ⚠️
- [ ] T057 [P] [US4] Endpoint + consent test for `GET /trade/v1/snapshots/distributor-stock` (gated; unlinked excluded) and days-of-stock/out-of-stock derivation, in `ampairs/trade/src/test/.../DistributorStockReadTest.kt`.

### Implementation for User Story 4
- [ ] T058 [P] [US4] `DistributorStockSnapshot` entity + DTO (`DistributorStockRow`) + repository in `ampairs/trade/.../{domain/model,domain/dto,repository}/` (table in V1.0.118, T051).
- [ ] T059 [US4] Extend `SnapshotService` — stock recompute from distributor `inventory` (via product/inventory public service); inventory-change event hook in `TradeSalesListener` (debounced rebuild).
- [ ] T060 [US4] Add `distributor-stock` read to `SnapshotController` — derive days-of-stock/out-of-stock from stock + secondary-sales run rate; consent-gated (`scope.share_stock`).
- [ ] T061 [P] [US4] Mobile/Web brand dashboard: distributor days-of-stock / OOS screen + ViewModel in `ampairs-app/feature/trade/.../ui/dashboard/`.

**Checkpoint**: Brand sees consented distributor stock + replenishment signals.

---

## Phase 7: User Story 5 — Targets vs achievement + primary-order handshake (Priority: P2)

**Goal**: Targets set per tier/grain with derived achievement from the same sales rollups; reps see a personal scorecard. Brand places primary orders via the brand→distributor confirm handshake (FR-024a). *(Primary-order handshake is grouped here as the primary-tier brand feature.)*

**Independent Test**: A distributor-period target shows correct achievement % as sales accrue; a rep scorecard shows only their own achievement. Brand places a primary order → distributor confirms → a normal order appears in the distributor tenant; no link ⇒ rejected. (SC-007, FR-024/024a/025)

### Tests for User Story 5 ⚠️
- [ ] T062 [P] [US5] Target-achievement test: achievement derived from SecondarySalesSnapshot (secondary) / primary orders (primary) agrees brand-side and distributor-side, in `ampairs/trade/src/test/.../TargetAchievementTest.kt`.
- [x] T063 [P] [US5] Primary-order handshake test: place (requires ACTIVE link) → confirm creates distributor-tenant order via `OrderService`; reject leaves no distributor order; no link ⇒ `ConsentRequiredException`, in `ampairs/trade/src/test/.../PrimaryOrderHandshakeTest.kt`.

### Implementation for User Story 5
- [ ] T064 [P] [US5] Enums `TargetGrain`, `PrimaryOrderStatus`; entities `SalesTarget`, `PrimaryOrderLink` in `ampairs/trade/.../domain/{enums,model}/` (tables in V1.0.118, T051).
- [ ] T065 [P] [US5] DTOs + repositories for targets + primary-order-link in `ampairs/trade/.../{domain/dto,repository}/`.
- [ ] T066 [US5] `TargetService` — CRUD + derived achievement (no stored achievement) from snapshots/primary orders, per tier/grain/period; product-grain targets/achievement attribute via `NetworkBrand` (Hop A) and key on the **brand SKU** where a `NetworkProduct` (Hop B) mapping exists, else the attributed "unmapped" total.
- [x] T067 [US5] `PrimaryOrderService` — place (brand, ACTIVE link, references brand-tenant order uid) / confirm (distributor → create order via `OrderService`, set distributorOrderUid) / reject; state machine.
- [x] T068 [US5] `TargetController` (`GET /trade/v1/targets`, consent-gated `scope.share_targets`) + `PrimaryOrderController` (`POST /trade/v1/primary-orders`, `/confirm`, `/reject`) in `ampairs/trade/.../controller/`.
- [ ] T069 [P] [US5] Mobile: rep "My targets / scorecard" screen + ViewModel in `ampairs-app/feature/trade/.../ui/scorecard/` (own achievement only).

**Checkpoint**: Targets/achievement + primary-order handshake working; tenants stay authoritative.

---

## Phase 8: User Story 6 — Trade schemes & claims settlement (Priority: P3)

**Goal**: Brand authors/publishes schemes; qualifying secondary sales accrue claims computed from the shared rollup; distributor submits, brand approves/rejects/settles with a reconcilable reference.

**Independent Test**: Published scheme accrues correct claim from qualifying sales; distributor submits → brand approves → settles (reference recorded); rejected claims don't settle; claim amount matches both sides. (SC-008, FR-026..029)

### Tests for User Story 6 ⚠️
- [ ] T070 [P] [US6] Claim-lifecycle test: DRAFT→SUBMITTED→APPROVED→SETTLED happy path + illegal transitions ⇒ `ClaimStateException`; reject records reason and does not settle, in `ampairs/claim/src/test/.../ClaimLifecycleTest.kt`. Include authority (FR-031): submit is distributor-side, approve/reject/settle are brand-side; a wrong-tier actor is rejected.
- [ ] T071 [P] [US6] Claim-amount parity test: `computed_amount` from SecondarySalesSnapshot is identical for brand and distributor; zero qualifying sales ⇒ zero claim (no error), in `ampairs/trade/src/test/.../ClaimComputationTest.kt`.

### Implementation for User Story 6
- [ ] T072 [P] [US6] Enum `ClaimStatus`; entities `SchemeClaim` (FK = `pricing` scheme/offer uid + `fundingBrandId`), `ClaimSettlement` in `ampairs/claim/.../domain/{enums,model}/`. Scheme definition is **reused from `pricing`/spec 015** — do NOT create a `TradeScheme` entity or `SchemeType` enum.
- [x] T072a [P] [US6] (publication edge, `trade` module) Enum `PublicationStatus` (PUBLISHED/WITHDRAWN) + entity `SchemePublication` (schemeRef = pricing offer uid, link, status; ≤1 PUBLISHED per (link, schemeRef)) + DTO + repo + `SchemePublicationService` (publish/withdraw; requires ACTIVE link; auto-withdraw on revoke) in `ampairs/trade/.../{domain/{enums,model},domain/dto,repository,service}/`. Publish endpoints `POST/DELETE /trade/v1/links/{uid}/schemes` + distributor `GET /trade/v1/schemes` on `TradeNetworkController` per `contracts/trade-network-actions.md`; consent-gated. Table in a `trade` migration (V1.0.117 set or a follow-up).
- [ ] T073 [US6] Flyway `V1.0.119__create_claim_settlement_tables.sql` (both vendors) — `scheme_claims`, `claim_settlements` **only** (no `trade_schemes` — scheme-definition tables live in `pricing`/015).
- [ ] T074 [P] [US6] DTOs + repositories for claim/settlement in `ampairs/claim/.../{domain/dto,repository}/`.
- [ ] T075 [US6] `ClaimService` only — accrue claims from qualifying `SecondarySalesSnapshot` rows carrying `pricing`'s `fundingBrandId` attribution (015 FR-020), scoped via `NetworkBrand` (Hop A) and keyed on the **brand SKU** via `NetworkProduct` (Hop B); submit/approve/reject/settle lifecycle; optional spec-013 ledger ref on settle. **No `SchemeService`** — scheme authoring/application is `pricing`/015; the optional scheme **publish-down-link** action (per the spec.md publication clarify) only references a pricing scheme uid, it never authors a scheme.
- [ ] T076 [US6] `ClaimController` — claim submit/approve/reject/settle endpoints per `contracts/trade-network-actions.md`; consent-gated. **No scheme create/publish authoring here** — scheme definition is `pricing`/015 and scheme publish/withdraw is the `trade`-module `SchemePublication` edge (T072a).
- [ ] T077 [P] [US6] Mobile (distributor): "Schemes & claims" screen + ViewModel (view published schemes **(from `pricing`)**, submit claim) in `ampairs-app/feature/dms/.../ui/claims/`.

**Checkpoint**: Full claims settlement loop works; figures reconcile across tenants.

---

## Phase 8b: Field-Ops Reporting, Survey & Leave (US1 extensions, Priority: P2)

**Goal**: The management/reporting layer over the offline-captured SFA data — attendance summaries + leave,
visit productivity, and store-visit surveys — per `sub-specs/field-ops-reporting/` (FR-AS1–7, FR-VP1–7) and
`sub-specs/sfa-field-operations/` (FR-AT2/4/5/6/8, FR-SV6/SV7). Depends on Foundational + US1 capture; reports
are online server-computed reads, survey responses are offline via `/sync`.

**Independent Test**: A manager opens a rep's attendance summary (days/hours/late/absent, leave excused) and
visit productivity (productive-call %, coverage) for a period; a rep captures a store survey offline that
syncs and rolls up.

### Migration
- [x] T084 [P2] Flyway `V1.0.120__create_trade_reporting_survey_leave_tables.sql` in BOTH `postgresql/` and `mysql/` — `leaves` (rep×day excused) + `visit_survey_responses` (structured per-visit answers). Summaries/productivity are derived (no tables). Confirm next free version with `flywayInfo`.

### Attendance Summary & Leave (FR-AS1–7)
- [x] T085 [P] [P2] Entity `Leave` (repMemberUid, date, reason, markedBy, status) + DTO + repo in `ampairs/trade/.../domain/{model,dto},repository/` (table in V1.0.120, T084).
- [x] T086 [P2] `AttendanceSummaryService` (read-only `@Transactional`) + `GET /trade/v1/attendance/summary?rep_member_uid&from&to` → `ApiResponse<AttendanceSummaryResponse>` (days present, working hours, late/absent/excused, business tz) in `ampairs/trade/.../{service,controller}/`. Mirror `payment` `AgingService` (`payment/.../service/AgingService.kt`).
- [x] T087 [P2] Attendance integrity in `AttendanceService` — enforce single-open attendance (reject/auto-close prior on push, FR-AS3) + scheduled auto-close past a configurable cutoff, flag `AUTO_CLOSED` (FR-AS4); register cutoff in `TradeSettingDefinitions`.
- [x] T088 [P2] `LeaveController` — manager CRUD `POST/GET/DELETE /trade/v1/leaves`; wire leave into adherence so excused days are not counted "missed" (update `JourneyPlanService` adherence, T027a).

### Visit Productivity & Survey (FR-VP1–7)
- [ ] T089 [P] [P2] Add `EntityType.VISIT_SURVEY` to `form/.../domain/model/EntityType.kt` + `VisitSurveyStandardFieldProvider` (`@Component`, baseline fields `shelf_availability`/`competitor_present`/`planogram_compliance`) — reuses the `form` module template engine; survey template rides `GET/POST /form/v1/config/schema/sync`.
- [x] T090 [P] [P2] Entity `VisitSurveyResponse` (visitUid, structured per-question values, synced/active) + DTO + repo + offline `/sync` (`VisitSurveyResponseSyncDelegate`, `@SyncEntityKey(SyncEntity.VISIT_SURVEY_RESPONSE)`, canonical contract) in `ampairs/trade/.../{domain/model,domain/dto,repository,sync}/`.
- [x] T091 [P2] `VisitProductivityService` (read-only) + `GET /trade/v1/visits/productivity?rep_member_uid&from&to&area` → `ApiResponse<VisitProductivityResponse>` (productive-call %, lines/value per call, avg duration, unique-outlet coverage with revisit dedupe). Mirror `AgingService`.
- [ ] T092 [P2] Survey rollup — `GET /trade/v1/visits/survey-rollup?from&to&area` aggregating structured responses (counts/percentages per question) by period/area; point-in-time (FR-VP4/5).

### Mobile (`ampairs-app/feature/trade`)
- [ ] T093 [P2] Visit survey capture (rep, offline) — Room `VisitSurveyResponse` entity/DAO + `VisitSurveyResponseSyncDelegate` + survey form UI rendering the `VISIT_SURVEY` schema; required-blank flagged, not blocked (FR-VP2/6).
- [ ] T094 [P] [P2] Manager dashboards (online reads) — attendance summary, visit productivity, survey rollup screens + ViewModels, and a leave-marking screen, in `ampairs-app/feature/trade/.../ui/reports/`.
- [ ] T095 [P] [P2] Add `VISIT_SURVEY_RESPONSE` to mobile `ampairs-app/data/sync/.../SyncEntity.kt`.

### Tests
- [x] T096 [P] [P2] Backend test: attendance summary correctness + leave excusal (not absent / planned visits excused) + late/absent + auto-close bounds hours + single-open enforcement (FR-AS1–7), in `ampairs/trade/src/test/.../AttendanceSummaryTest.kt`.
- [x] T097 [P] [P2] Backend test: visit productivity (productive-call %, coverage dedupe) + survey capture/sync + survey rollup + point-in-time on template change (FR-VP1–7), in `ampairs/trade/src/test/.../VisitProductivitySurveyTest.kt`.
- [ ] T098 [P] [P2] Mobile test: offline survey-capture round-trip (idempotent re-push, multi-device merge), in `ampairs-app/feature/trade/src/commonTest/.../SurveySyncRoundTripTest.kt`.

**Checkpoint**: Managers get attendance/productivity/survey reporting + leave; reps capture surveys offline.

---

## Phase 9: Polish & Cross-Cutting Concerns

- [ ] T078 [P] [—] Run `quickstart.md` end-to-end against a dev instance; fix any drift between contracts and implementation.
- [ ] T079 [P] [—] Coverage check: backend ≥80% critical / ≥90% endpoints on consent gate, snapshot recompute, claim lifecycle, PII projection (`./gradlew :trade:test`); document gaps.
- [ ] T079a [P] [—] Performance check (SC-006): load-test `GET /trade/v1/snapshots/{secondary-sales,distributor-stock}` with a brand linked to ≥200 distributors; assert first-page p95 < 2s; record results in the PR. Tune snapshot read indexes if needed.
- [ ] T080 [P] [—] `TradeSettingDefinitions` in `ampairs/trade/.../config/` — register the geo-fence radius + snapshot-coalesce-window as workspace settings (per `setting` module pattern).
- [ ] T081 [P] [—] Docs: add `ampairs/docs/modules/trade.md` (module overview, cross-tenant boundary, snapshot model) and update module ownership table in `.claude/rules/08-module-boundaries.md`.
- [ ] T082 [—] Backend CI gate: `./gradlew :ampairs_service:flywayInfo` (confirm V1.0.117–120 applied cleanly on Postgres + MySQL) + `./gradlew ciBuild`.
- [ ] T083 [P] [—] Mobile final parity gate: `./gradlew :feature:trade:check` + 3-target compile; verify offline new-outlet + ad-hoc visit round-trip in an integration test.

---

## Dependencies & Execution Order

### Phase dependencies
- **Setup (P1: T001–T005)** → no deps.
- **Foundational (P2: T006–T013)** → depends on Setup; **blocks all stories**.
- **US1 (P3)** and **US2 (P4)** are both spec-P1 and **independent of each other** — US1 (offline rep app) is the standalone MVP; US2 (link/consent) is the foundation for brand-facing stories.
- **US3 (P5)** depends on **US2** (consent gate) + the snapshot infra it introduces.
- **US4 (P6)** depends on **US2**; reuses `SnapshotService`/`SnapshotController` from US3 (do US3 first or share the scaffold).
- **US5 (P7)** depends on **US2**; secondary achievement reads US3 snapshots (primary achievement + primary-order handshake do not).
- **US6 (P8)** depends on **US2 + US3** (claims computed from secondary-sales snapshots).
- **Field-Ops Reporting (P8b)** depends on **Foundational + US1 capture** (reports/leave/survey over the
  attendance/visit data); independent of the brand-facing stories. P2 — can be built any time after US1.
- **Polish (P9)** depends on all desired stories.

### Migration ordering (global Flyway versions)
- `V1.0.117` (T022) — network + SFA tables (US1 + US2).
- `V1.0.118` (T051) — snapshot + target + primary-order tables (US3/US4/US5).
- `V1.0.119` (T073) — claim/settlement tables (US6); scheme-definition tables live in `pricing`/spec 015.
- `V1.0.120` (T084) — leave + visit-survey-response tables (Phase 8b reporting/survey/leave).

### Within each story
Tests (write first, expect fail) → enums/entities → migration → DTOs/repos → services → controllers → mobile.

### Parallel opportunities
- Setup: T003/T004/T005 in parallel.
- Foundational: T007/T009/T010/T011/T012/T013 in parallel (T006, T008 touch shared files — sequential-ish).
- Within a story, `[P]` entity/DTO/test tasks in different files run in parallel; services/controllers that share a file are sequential.
- After Foundational, **US1 and US2 can be built in parallel** by different developers (and backend vs mobile within US1 are largely parallel).

---

## Parallel Example: User Story 1

```bash
# Tests first (different files):
T014 VisitSyncContractTest ; T015 {FieldOrder,Attendance,Beat,JourneyPlan}SyncContractTest
T016 VisitRulesTest ; T017 (mobile) OfflineSyncRoundTripTest

# Then entities/enums/DTOs in parallel (different files):
T018 enums ; T019 Beat/BeatOutlet ; T020 JourneyPlan/PlannedVisit ; T021 Visit/Attendance/FieldOrder
T023 DTOs ; T024 repositories
# Mobile in parallel with backend:
T028 Room+DAO ; T029 TradeApi
```

---

## Implementation Strategy

### MVP first (User Story 1 only)
1. Phase 1 Setup → 2. Phase 2 Foundational → 3. Phase 3 US1 → **STOP & VALIDATE** the offline rep app
(airplane-mode round-trip) → ship to a distributor. Delivers standalone SFA value with no brand needed.

### Incremental delivery
US1 (MVP, distributor SFA) → US2 (consent link) → US3 (secondary rollup) → US4 (stock) → US5 (targets +
primary order) → US6 (schemes/claims). Each is an independently testable, deployable increment.

### Parallel team strategy
After Foundational: Dev A → US1 (mobile-heavy), Dev B → US2 then US3/US4 (backend snapshot rail), Dev C →
US5/US6 once US2/US3 land. Backend and mobile within US1 split cleanly across two people.

---

## Notes
- `[P]` = different files, no dependency. `[Story]` traces a task to its spec user story.
- Tests included per the spec's Constitution Check; write them to fail first.
- Cross-tenant reads ALWAYS pass `CrossTenantReadGuard` (active link + scope) — never query another tenant's live tables directly.
- Snapshots are recomputed (versioned), never incrementally mutated — backdated docs trigger a rebuild.
- Money: `BigDecimal`/`DECIMAL(19,4)` backend, `Long` minor units mobile. Timestamps `Instant`/`TIMESTAMPTZ`.
- Commit after each task or logical group; keep backend (`ampairs`) and mobile (`ampairs-app`) commits in their own repos.
