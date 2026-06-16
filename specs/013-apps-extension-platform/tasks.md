# Tasks: Apps & Extensions Connector Platform

**Input**: Design documents from `/specs/013-apps-extension-platform/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/connector-api.yaml

**Tests**: Targeted tests are included for the data-integrity crux (sparse upsert / no-data-loss) and the install/config/mapping contracts, since those are the feature's core acceptance criteria. They are marked `[TEST]` and may be skipped for a spike, but are recommended.

**Repos**: This `tasks.md` lives in the backend repo (`/home/user/ampairs`). Tasks in the KMP client repo are marked `[CLIENT ampairs-app]` and the Angular web repo `[WEB ampairs-web]` (web repo is NOT present in this environment — plan/execute separately).

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1–US6 (maps to spec user stories); `SETUP`/`FOUND`/`POLISH` for cross-cutting

## Path Conventions
- Backend module root: `connector/src/main/kotlin/com/ampairs/connector/`
- Backend migrations: `connector/src/main/resources/db/migration/{postgresql,mysql}/`
- Client: `/home/user/ampairs-app/...`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Register the new `connector` bounded-context module so it compiles and migrates.

- [X] T001 [SETUP] Add `include("connector")` to `settings.gradle.kts`.
- [X] T002 [SETUP] Create `connector/build.gradle.kts` by copying `customer/build.gradle.kts` (Spring Boot library: `bootJar` disabled, `jar` enabled with empty classifier, `api(project(":core"))`, `allOpen` for JPA, group `com.ampairs`, web/data-jpa/validation/security starters).
- [X] T003 [SETUP] In `ampairs_service/build.gradle.kts`: add `implementation(project(mapOf("path" to ":connector")))` and add `"connector"` to the `migrationModules` list.
- [X] T004 [P] [SETUP] Create package skeleton `connector/src/main/kotlin/com/ampairs/connector/{domain/model,domain/dto,domain/catalogue,repository,service,controller,config,exception}` and a `package-info`/`CLAUDE.md` stub mirroring `setting`.
- [X] T005 [P] [SETUP] Create empty Flyway migration dirs `connector/src/main/resources/db/migration/postgresql/` and `.../mysql/`. Run `./gradlew :ampairs_service:flywayInfo` to confirm the module is picked up and to choose the starting version.

**Checkpoint**: `./gradlew :connector:compileKotlin` succeeds (empty module).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure every story depends on — installation entity, catalogue/entitlement, secret encryption, cross-module write SPI.

**⚠️ No user-story work begins until this phase is complete.**

- [X] T006 [FOUND] Create `ConnectorInstallation` entity in `domain/model/ConnectorInstallation.kt` extending `OwnableBaseDomain` with `connectorType`, `status` (enum `InstallationStatus`: NEEDS_CONFIG/ENABLED/PAUSED/ERROR/UNINSTALLED), `autoStart`, `scheduleSeconds?`, `lastErrorMessage?`, `active`; add `@NamedEntityGraph` for config+mappings. (data-model.md §ConnectorInstallation)
- [X] T007 [FOUND] Flyway `V1.0.0__connector_installation.sql` in BOTH `postgresql/` and `mysql/` (TIMESTAMPTZ vs TIMESTAMP), with unique index on `(owner_id, connector_type)` filtered to active rows (FR-005).
- [X] T008 [P] [FOUND] `repository/ConnectorInstallationRepository.kt` (Spring Data; derived queries `findByOwnerIdAndActiveTrue`, `findByUidAndOwnerId`, `findByActiveTrueAndStatus`).
- [X] T009 [P] [FOUND] Installation DTOs + converters in `domain/dto/` (`InstallationResponse`, `asResponse()`); no entity exposed (Principle II).
- [X] T010 [FOUND] Catalogue infra in `domain/catalogue/`: `CatalogueConnector` data class (type, displayName, hostingType, supportedEntities, supportedDirections, connectionSchema, defaultMapping, requiredTier/requiredModule, multipleInstancesAllowed) + `ConnectorCatalogueProvider` SPI + a registry bean aggregating providers. (data-model.md §Connector)
- [ ] T011 [FOUND] Entitlement gating service in `service/ConnectorCatalogueService.kt`: list catalogue filtered by `InstalledModulesProvider.enabledModuleCodes()` + `MasterModule.requiredTier`/`SubscriptionTier`; reuse existing `SubscriptionAddon.TALLY_INTEGRATION`. Register a `connector` MasterModule entry (dependencies: customer-management, product-management). (research.md R6)
- [X] T012 [P] [FOUND] Secret encryption util in `config/ConnectorSecretCipher.kt` (encrypt/decrypt with key from env var, e.g. `CONNECTOR_SECRET_KEY`); never log plaintext (FR-008, Principle XI).
- [X] T013 [P] [FOUND] `exception/` typed domain exceptions (`ConnectorNotFoundException`, `ConnectorAlreadyInstalledException`, `InvalidMappingException`, `ConnectorConfigInvalidException`) — bubble to `GlobalExceptionHandler` (Principle VI).
- [X] T014 [FOUND] **Cross-module write SPI** `ConnectorEntityWriter` (public interface in `connector` api package or `core`): `entityType: String`; `applySparse(refId: String, presentColumns: Map<String, Any?>): WriteOutcome` (CREATED/UPDATED/SKIPPED/FAILED + appliedColumns). Connector dispatches by `entityType`; target modules implement it. (research.md R3, Principle IX — no direct cross-module repo access.)

**Checkpoint**: Module compiles with installation persistence; catalogue + SPI defined.

---

## Phase 3: User Story 1 — Install a connector (Priority: P1) 🎯 MVP

**Goal**: Browse the workspace-gated catalogue and install/uninstall a connector; per-workspace, tenant-isolated, persisted.

**Independent Test**: Install one connector → appears `NEEDS_CONFIG` for that workspace only; uninstall returns it to catalogue; second workspace unaffected.

- [ ] T015 [P] [US1] [TEST] Contract test `connector/src/test/.../CatalogueAndInstallContractTest.kt`: `GET /connector/v1/catalogue` (tier-gated), `POST /connector/v1/installations`, `DELETE`, `GET /installations` — assert `ApiResponse` shape, tenant isolation, NEEDS_CONFIG status, duplicate-install rejected (FR-005).
- [X] T016 [US1] `controller/ConnectorCatalogueController.kt`: `GET /connector/v1/catalogue` → `ApiResponse<List<CatalogueConnector>>`; set tenant context (try/finally); flag `installed` per workspace.
- [X] T017 [US1] `service/ConnectorInstallationService.kt`: install (enforce one active per type unless `multipleInstancesAllowed`, FR-005), uninstall (soft-delete `active=false`, stop sync), list, status transitions (FR-006).
- [X] T018 [US1] `controller/ConnectorInstallationController.kt`: `GET/POST /installations`, `DELETE /installations/{uid}` → `ApiResponse`; tenant context per method.
- [X] T019 [US1] Wire `EntityChangePublisher` broadcast on install/uninstall so clients pull (entity name `connector`).

**Checkpoint**: US1 independently testable — catalogue + install/uninstall work.

---

## Phase 4: User Story 2 — Configure & map data (Priority: P1)

**Goal**: Provide connection details (secrets encrypted) and define/customise per-entity field mappings; all persisted server-side; secrets never returned.

**Independent Test**: Save config + edit mapping for `customer`; reload → returned unchanged with secrets masked; invalid mapping target rejected.

- [X] T020 [US2] `domain/model/ConnectorConfig.kt` (installationUid, nonSecretValues map, secretValuesEncrypted text, lastValidatedAt) + `domain/model/ConnectorFieldMapping.kt` (installationUid, entityType, rules JSON, version). (data-model.md §§Config, FieldMapping)
- [X] T021 [US2] Flyway `V1.0.1__connector_config_mapping.sql` (both vendors): `connector_config`, `connector_field_mapping`; unique `(installation_uid, entity_type)` on mapping.
- [X] T022 [P] [US2] `repository/ConnectorConfigRepository.kt` + `repository/ConnectorFieldMappingRepository.kt`.
- [X] T023 [P] [US2] DTOs in `domain/dto/`: `ConfigRequest`/`ConfigResponse` (response excludes secrets, exposes `secret_keys_set` masked, FR-008), `FieldMapping`/`FieldMappingRule` DTOs + converters.
- [X] T024 [US2] `service/ConnectorConfigService.kt`: upsert config (encrypt secrets via `ConnectorSecretCipher`), get (mask), `testConnection` (validate w/o echoing secrets, FR-009 — for client-side connectors this records intent/last_validated; actual reachability is client-tested).
- [X] T025 [US2] `service/ConnectorMappingService.kt`: upsert mapping, get; **validate each `ampairs_field` against the live target-entity schema and type** (FR-013) → `InvalidMappingException` on bad target; bump `version`.
- [X] T026 [US2] `controller/ConnectorConfigController.kt` (`GET/PUT /installations/{uid}/config`, `POST .../config/test`) + `controller/ConnectorMappingController.kt` (`GET/PUT /installations/{uid}/mappings`); tenant context per method.
- [X] T027 [US2] Transition installation `NEEDS_CONFIG → ENABLED` once a valid config + at least one mapping exist (FR-006).
- [ ] T028 [P] [US2] [TEST] Contract test `ConfigAndMappingContractTest.kt`: config round-trips with secrets masked; mapping persists; invalid `ampairs_field` rejected (FR-013).
- [ ] T028a [CLIENT ampairs-app] [US2] Client-side **connection/reachability test** (FR-009, G2): the desktop app tests reachability of the local external system (Tally host:port) and reports the result to the backend `POST /connector/v1/installations/{uid}/config/test`; backend records `last_validated_at`. Server never reaches a client-side connector's external system.
- [ ] T028b [CLIENT ampairs-app] [US2] (also [WEB ampairs-web] T050) **Data-mapping + formatting/converter UI** (FR-014, G1): non-developer editor to map external↔Ampairs fields per entity, mark unmapped, AND define value formatting/conversion (transformation) rules (phone/GSTIN sanitisation, unit conversion, date/number formatting); persists via `PUT /connector/v1/installations/{uid}/mappings`.

**Checkpoint**: US1+US2 work — a connector can be installed, configured, mapped.

---

## Phase 5: User Story 3 — Incremental stateful sync + sparse upsert (Priority: P1)

**Goal**: The data path. Connector-scoped sparse upsert (per-row presence ∩ mapping allowlist; omitted columns preserved; explicit null clears); backend-persisted checkpoints + run history; metadata `/sync` pull. **This is the data-loss fix.**

**Independent Test**: Sparse-upsert row1 `{ref_id:1, values:{column1,column2}}` and row2 `{ref_id:2, values:{column3,column4}}` → each writes only its columns, all others preserved; omit a previously-set column → preserved; explicit null → cleared; re-sync unchanged data → 0 writes.

- [X] T029 [US3] `domain/model/ConnectorSyncCheckpoint.kt` (installationUid, entityType, direction, watermark, lastSyncedAt) + `domain/model/ConnectorSyncRun.kt` (installationUid, entityType?, trigger, startedAt, finishedAt?, status, processed/created/updated/failed, errorDetail). (data-model.md §§Checkpoint, Run)
- [X] T030 [US3] Flyway `V1.0.2__connector_sync_state.sql` (both vendors): `connector_sync_checkpoint` (unique `(installation_uid, entity_type, direction)`), `connector_sync_run`.
- [X] T031 [P] [US3] Repositories `ConnectorSyncCheckpointRepository.kt`, `ConnectorSyncRunRepository.kt`.
- [X] T032 [US3] **Sparse upsert request/result model**: `SparseUpsertRow` carrying `ref_id`, `entity_type`, and `values: Map<String, JsonNode?>` (or `Map<String, Any?>`) where **key presence is the signal** (FR-018c) — do NOT use a fixed nullable DTO. `SparseUpsertResult` (ref_id, ampairs_uid?, outcome, applied_columns). (research.md R4)
- [X] T033 [US3] `service/ConnectorSparseUpsertService.kt` (the core, reuse `ProductService.updateProducts()` merge pattern): match the existing record by `refId` (or `uid`) **only** — no business-key reconciliation; a non-matching row creates a new record (FR-019). For each row → `writable = values.keys ∩ mappingAllowlist(installation, entityType)`; dispatch to `ConnectorEntityWriter` for `entityType` with the present, in-allowlist columns; key present ⇒ write (incl. explicit null = clear), key absent ⇒ leave untouched; keys outside allowlist ignored (FR-018/FR-018b). Overlapping-connector writes to the same field resolve last-write-wins (FR-019b).
- [X] T034 [US3] `controller/ConnectorDataController.kt`: `POST /installations/{uid}/data/{entity_type}/upsert` (body `List<SparseUpsertRow>`) → `ApiResponse<List<SparseUpsertResult>>`; tenant context; this is SEPARATE from global `/{module}/v1/{resource}/sync` (FR-018a — leave that untouched).
- [X] T035 [P] [US3] Implement `ConnectorEntityWriter` per target module via `core.connector.AbstractRefIdEntityWriter` (refId/uid match → apply present columns → save; Principle IX). Done: customer, customer_group (customer module), product, product_group, product_category (product module), unit (unit module). Catalogue split `product_catalog` → `product_group`/`product_category`; product references target `*Id` fields. DEFERRED: `stock_balance` writer (Product.inventory is a `List<Inventory>` needing custom row logic).
- [X] T036 [US3] `service/ConnectorCheckpointService.kt` + endpoint `PUT /installations/{uid}/checkpoints` (advance watermark, FR-017); incremental semantics (FR-016).
- [X] T037 [US3] `service/ConnectorSyncRunService.kt` + endpoints `GET/POST /installations/{uid}/runs` (record + list run history, FR-020); admin visibility (FR-007).
- [X] T038 [US3] `controller/ConnectorSyncController.kt`: `GET /connector/v1/sync` metadata pull (installations + masked config + mappings + checkpoints) for client mirroring; canonical `/sync` style with `last_sync/page/size/sort_by/sort_dir` → `ApiResponse<PageResponse<...>>`.
- [X] T039 [US3] Serialise concurrent sparse-upserts per (installation, entity) and ensure checkpoint only advances past accounted rows (FR-021/FR-022); partial-failure → per-row FAILED in result, run status PARTIAL.
- [ ] T040 [US3] [TEST] Integration test `SparseUpsertDataIntegrityTest.kt` (Testcontainers): verifies SC-004 (row1≠row2 columns, unmapped/omitted preserved), explicit-null clears, out-of-allowlist ignored, no duplicates by refId, and SC-008 resume-after-interruption (checkpoint).

**Checkpoint**: US1+US2+US3 = full server-side platform for one connector; data integrity proven.

---

## Phase 6: User Story 4 — Auto-start configured connectors (Priority: P2)

**Goal**: Configured connectors sync automatically on schedule; client auto-starts from backend-persisted config; pause/resume.

**Independent Test**: Configure a connector, do nothing → background sync runs on schedule; restart client → resumes from backend checkpoint; pause → no sync.

- [X] T041 [US4] Backend `controller`/`service`: `POST /installations/{uid}/pause` + `/resume` (FR-025) toggling status PAUSED⇄ENABLED; honor `autoStart`/`scheduleSeconds`.
- [ ] T042 [CLIENT ampairs-app] [US4] `shared/src/desktopMain/com/ampairs/tallysync/TallySyncScheduler`: auto-start background sync when an `ENABLED` connector is discovered from pulled config; resume from backend checkpoint on app start (FR-023/FR-024); stop when PAUSED.
- [ ] T042a [CLIENT ampairs-app] [US4] **Connector status UI** (FR-H04, G3): show connector state (NEEDS_CONFIG/ENABLED/PAUSED/ERROR) and last sync run result (time, counts, error) in the client; surface pause/resume and on-demand sync trigger (FR-025).

**Checkpoint**: Configured connectors run hands-free.

---

## Phase 7: User Story 5 — Migrate Tally onto the platform (Priority: P2)

**Goal**: Tally becomes a client-side connector; config/mapping/checkpoints/run-history move to backend; existing records match (no dup); legacy DataStore path retired.

**Independent Test**: A workspace using client-side Tally enables the Tally connector → customers/products/units/groups keep syncing, no duplicates, prior data preserved.

- [X] T043 [US5] Backend: register the **Tally** `CatalogueConnector` provider (`domain/catalogue/TallyConnectorProvider.kt`): `type="tally"`, `hostingType=CLIENT_SIDE`, supportedEntities (customer, customer_group, product, product_catalog, unit, stock_balance), `supportedDirections=[INBOUND]`, connectionSchema (host, port), default mapping template mirroring current `TallyCustomerMapper`/`TallyProductMapper`.
- [ ] T044 [P] [CLIENT ampairs-app] [US5] Add `SyncEntity.CONNECTOR`; create `ConnectorSyncDelegate` (mirror `feature/store/.../StoreSyncDelegate`) pulling `/connector/v1/sync` into a workspace-scoped Room cache; `@ContributesIntoMap(WorkspaceScope::class)` + `@SyncEntityKey`.
- [ ] T045 [P] [CLIENT ampairs-app] [US5] `ConnectorConfigProvider` (mirror `feature/store/.../StoreSettingsProvider`) exposing host/port/mapping/watermark from the cache.
- [ ] T046 [CLIENT ampairs-app] [US5] Repoint `TallySyncService`/`TallySyncScheduler` to read config+mapping+watermark from `ConnectorConfigProvider` instead of `AppPreferencesDataStore`; after each Tally cycle push business data via `POST .../data/{entity_type}/upsert` and report checkpoint + run to backend.
- [ ] T047 [CLIENT ampairs-app] [US5] Migrate/seed: on first run with the connector, hydrate backend config from existing DataStore Tally host/port + alterId watermarks so no full re-sync; ensure refId (Tally GUID) match prevents duplicates (FR-027).
- [ ] T048 [CLIENT ampairs-app] [US5] Retire legacy client-side Tally config path: replace `TallySettingsScreen` persistence (DataStore) with connector config; remove dead DataStore keys after migration (FR-028).

**Checkpoint**: Tally fully runs on the platform; no workspace loses connectivity.

---

## Phase 8: User Story 6 — Two-way sync readiness (Priority: P3, future)

**Goal**: Platform-ready for bidirectional sync without re-architecture; no execution in this release.

- [X] T049 [US6] Ensure `supportedDirections` (catalogue) and a per-installation `direction` setting exist and are persisted; default INBOUND only; document OUTBOUND execution + most-recent-update-wins conflict (FR-030/FR-031) as deferred. No outbound execution code.

---

## Phase 9: Polish & Cross-Cutting

- [ ] T050 [P] [POLISH] [WEB ampairs-web] Stub the web workstream (separate repo, not in env): Apps catalogue + install + connection-config form + data-mapping editor under workspace-settings/module-management, Angular Material 3 only. Document required endpoints (from `contracts/connector-api.yaml`).
- [X] T051 [P] [POLISH] Update `docs/guides/offline-sync-contract.md` and `connector/CLAUDE.md`: note the connector sparse-upsert is OFF the canonical `/sync` contract by design (like tax/file), and the global `/sync` is unchanged.
- [ ] T052 [POLISH] Backend gate: `./gradlew :ampairs_service:flywayInfo` (versions sequential), `./gradlew :connector:compileKotlin`, `./gradlew testAll` (Docker), `./gradlew ciBuild`.
- [ ] T053 [P] [CLIENT ampairs-app] [POLISH] Validate all targets: `./gradlew shared:compileKotlinIosSimulatorArm64 androidApp:compileDebugKotlinAndroid desktopApp:compileKotlin`.
- [ ] T054 [P] [POLISH] Verify `EntityChangePublisher` broadcasts on connector metadata + data writes so other devices pull (multi-device consistency).

---

## Dependencies & Execution Order

- **Setup (P1) → Foundational (P2)** block everything.
- **US1 (P3)** depends only on Foundational → **MVP**.
- **US2 (P4)** depends on US1 (needs an installation to configure).
- **US3 (P5)** depends on US2 (needs mapping allowlist) + T014 SPI + T035 writers.
- **US4 (P6)** depends on US2/US3 (config + sync exist).
- **US5 (P7)** depends on US1–US4 (full platform) + client infra T044/T045.
- **US6 (P8)** is additive metadata; can land any time after Foundational.
- **Polish (P9)** last.

### Parallel opportunities
- Setup: T004, T005 [P]. Foundational: T008, T009, T012, T013 [P] after T006/T007.
- US2: T022, T023 [P]. US3: T031 [P], T035 [P] (per target module), test T040 after T033/T034.
- US5 client: T044, T045 [P]; web T050 [P] anytime.

## Implementation Strategy

- **MVP = Setup + Foundational + US1 + US2 + US3** (install → configure → map → server-side sparse-upsert sync with the no-data-loss guarantee). This delivers the core platform and fixes the data-loss bug for one connector.
- **Increment 2 = US4 + US5**: auto-start + migrate Tally fully onto the platform (retire legacy DataStore path).
- **Increment 3 = US6 + web UI + outbound two-way** (future).

## MVP scope (suggested)

Phases 1–5 (through US3). At that point a connector can be installed, configured, mapped, and run a server-persisted, incremental, **non-destructive** sparse upsert — the feature's reason for existing.
</content>
