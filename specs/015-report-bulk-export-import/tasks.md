# Tasks: Data Export & Bulk Upload (Reports + Round-Trip Bulk Edit)

**Input**: Design documents from `/home/user/ampairs/specs/015-report-bulk-export-import/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/report-api.md

**Tests**: This is not a TDD request, so most tests are **OPTIONAL** (Polish phase). **Exception (constitution "Testing & Quality Gates" — API endpoints ≥90%)**: the backend contract/integration tests for the **new endpoints** added in US3 and US4 are **REQUIRED** and live inside those story phases (T034a, T048a). Unit/perf tests stay optional.

**Cross-repo**: This feature spans two repos. Tasks are tagged:
- **[BE]** → backend repo `/home/user/ampairs` (Spring Boot, new `report` module)
- **[APP]** → mobile repo `/home/user/ampairs-app` (KMP, new `feature/export` module)

## Format: `[ID] [P?] [BE|APP] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependency on another unfinished task in the phase)
- **[Story]**: US1–US5 (or none for Setup/Foundational/Polish)
- All paths are absolute.

## Path roots
- Backend module: `/home/user/ampairs/report/src/main/kotlin/com/ampairs/report/`
- Backend SPI (shared): `/home/user/ampairs/core/src/main/kotlin/com/ampairs/core/export/`
- Backend per-module descriptors: `/home/user/ampairs/{module}/src/main/kotlin/com/ampairs/{module}/export/`
- App feature: `/home/user/ampairs-app/feature/export/src/`
- App SPI (shared): `/home/user/ampairs-app/data/common/src/commonMain/kotlin/com/ampairs/common/export/`
- App per-module exporters: `/home/user/ampairs-app/feature/{module}/src/commonMain/kotlin/com/ampairs/{module}/export/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Stand up the new app feature module and global hooks needed by the MVP (US1/US2 are app-only and offline). The backend `report` module is created later, in Phase 5 (US3) — no earlier story needs it.

- [ ] T001 [P] [APP] Create `feature/export` module: add `include(":feature:export")` to `/home/user/ampairs-app/settings.gradle.kts` and write `/home/user/ampairs-app/feature/export/build.gradle.kts` (KMP targets android/ios/desktop/wasmJs; deps: `:data:common`, `:data:sync`, `:feature:file`, Room KMP, kotlinx.serialization, FileKit, Metro). Add JVM-only `poi-ooxml` to `androidMain`+`desktopMain` source sets via version catalog.
- [ ] T002 [P] [APP] Add `fun reportUrl(path: String)` to `/home/user/ampairs-app/data/common/src/commonMain/kotlin/com/ampairs/common/ApiUrlBuilder.kt` (pattern: `${apiBaseUrl}/api/report/$path`).
- [ ] T003 [P] [APP] Add `EXPORT_TEMPLATE` to the `SyncEntity` enum at `/home/user/ampairs-app/data/sync/src/commonMain/kotlin/com/ampairs/sync/SyncEntity.kt`.
- [ ] T004 [P] [APP] Add `poi-ooxml` (and `jackson-dataformat-csv`/`-xml` for backend reuse) to the version catalogs: `/home/user/ampairs-app/gradle/libs.versions.toml` and `/home/user/ampairs/gradle/libs.versions.toml`.
- [ ] T005 [P] [APP] Create `feature/export/src/commonMain/composeResources/values/strings.xml` skeleton (export/import/template UI strings).

**Checkpoint**: App module compiles empty; global hooks (URL, SyncEntity, deps) in place.

---

## Phase 2: Foundational (Blocking Prerequisites for ALL app stories)

**Purpose**: The generic engine + SPI + format core that every story builds on. ⚠️ No app user story can start until this is done.

- [ ] T006 [APP] Define export value types in `feature/export/src/commonMain/kotlin/com/ampairs/export/domain/`: `ExportFormat` (CSV/JSON/XML/EXCEL), `GenerationLocation` (CLIENT/SERVER), `ImportMode` (UPDATE_ONLY/UPSERT), `ColumnType`, `ExportColumn`, `ExportFilter`, `ExportSort`, `ImportOutcome`, `RowError`.
- [ ] T007 [P] [APP] Define the **per-module SPI** in `data/common/.../common/export/`: `ModuleExporter` interface (`moduleKey`, `columns`, `readRows(...)`, `writeRows(rows, mode)`) and `@ModuleExporterKey` Metro `@MapKey`. (Lives in `data/common` so feature modules don't depend on `feature/export`.)
- [ ] T008 [APP] Implement `ModuleExporterRegistry` (Metro multibinding `Map<String, ModuleExporter>`) in `feature/export/src/commonMain/kotlin/com/ampairs/export/engine/`.
- [ ] T009 [P] [APP] `FormatWriter` dispatcher + `CsvWriter` (RFC-4180 quoting; batch-streamed) in `feature/export/.../engine/format/`.
- [ ] T010 [P] [APP] `JsonWriter` (kotlinx.serialization, array-of-objects) + `XmlWriter` (pure commonMain string building) in `.../engine/format/`.
- [ ] T011 [P] [APP] `FormatReader` dispatcher + `CsvReader`, `JsonReader`, `XmlReader` (pure commonMain) in `.../engine/format/`.
- [ ] T012 [APP] `expect class SpreadsheetWriter` and `expect class SpreadsheetReader` in commonMain + wire Excel branch into `FormatWriter`/`FormatReader`. (Actuals land in US1.)
- [ ] T013 [APP] File I/O helpers in `feature/export/.../engine/io/`: reuse `feature/file` `FileManager` (save bytes to device) + `FilePicker` (pick file); add `expect saveExportFile(name, bytes)` / `shareFile(path)` actuals (FileKit save dialog / share sheet) per platform if not already covered by `feature/file`.
- [ ] T014 [APP] `ExportDatabase` (Room, `@SingleIn(WorkspaceScope::class)`) + platform DB factory modules `ExportModule.{android,ios,desktop}.kt` registered with `WorkspaceClosableRegistry` (explicit reified type param). Empty schema for now (entities added in US2/US3).

**Checkpoint**: Generic engine compiles on all 3 targets; SPI + registry ready; no module wired yet.

---

## Phase 3: User Story 1 — Export a module's data to a file, offline (Priority: P1) 🎯 MVP

**Goal**: From the customer module, produce a CSV/JSON/XML/Excel file on-device with a standard report (all primary columns, active rows, incl. `uid`), with no connectivity.

**Independent Test**: With network disabled, export customers to CSV → file has a header row, one row per active customer, a `uid` column, values matching the app. Repeat for JSON/XML/Excel.

### Implementation (US1)
- [ ] T015 [APP] [US1] `SpreadsheetWriter` **actual** in `feature/export/src/androidMain/` using Apache POI (`SXSSF` streaming).
- [ ] T016 [P] [APP] [US1] `SpreadsheetWriter` **actual** in `feature/export/src/desktopMain/` using Apache POI.
- [ ] T017 [P] [APP] [US1] `SpreadsheetWriter` **actual** in `feature/export/src/iosMain/`: build the **minimal pure-Kotlin OOXML writer over a KMP zip** (decision A1 — the default, so "all formats on client" incl. offline Excel holds on iOS per FR-001/FR-007). The SERVER fallback (T055) is the contingency only if this writer slips, **not** the baseline. CSV/JSON/XML stay offline on iOS regardless. Tabular `.xlsx` = a small fixed set of XML parts in a zip — keep the writer to the tabular subset (see research R4).
- [ ] T018 [APP] [US1] Add `CustomerDao.queryForExport(filters, sort, includeInactive)` in `/home/user/ampairs-app/feature/customer/.../data/db/CustomerDao.kt`.
- [ ] T019 [APP] [US1] `CustomerExporter` in `/home/user/ampairs-app/feature/customer/src/commonMain/kotlin/com/ampairs/customer/export/CustomerExporter.kt`: `columns` (uid[isMatchKey], name, phone, email, group_uid, active[isActiveFlag], display-only balance), `readRows()` (entity→row), registered `@Inject @ContributesIntoMap(WorkspaceScope::class) @ModuleExporterKey("customer")`. (Add `:data:common` dep to customer module if absent.)
- [ ] T020 [APP] [US1] `ExportViewModel` + `ExportUiState` (MVI) in `feature/export/.../ui/`: module pick (from registry), format pick, standard report, run → `FormatWriter` → `saveExportFile`/`shareFile`.
- [ ] T021 [APP] [US1] `ExportScreen` (Compose, `AppScreenWithHeader`, `metroViewModel()`, strings from resources) + progress + Save/Share.
- [ ] T022 [APP] [US1] Add `Route.Export` to `Routes.kt`, create `ExportEntryProvider`, register in `CombinedEntryProvider`/`ModuleRegistry`; also surface "Export" as an action on the customer list screen.
- [ ] T023 [APP] [US1] Compile gate: `androidApp:compileDebugKotlinAndroid`, `shared:compileKotlinIosSimulatorArm64`, `desktopApp:compileKotlin`.

**Checkpoint**: US1 fully functional and independently testable — offline export of customers in all 4 formats. **This is the MVP.**

---

## Phase 4: User Story 2 — Round-trip bulk edit (export → edit → re-upload by UID) (Priority: P1)

**Goal**: Import an edited file; each row updates the existing record matched by `uid` via the existing offline-sync push; partial success with a per-row error report. Offline.

**Independent Test**: Export customers to CSV, edit `name`+`phone` of 5 rows, re-import → exactly those 5 customers updated (matched by `uid`), no new records, summary 5 updated / 0 failed; re-importing the same file is idempotent.

### Implementation (US2)
- [ ] T024 [APP] [US2] Implement `CustomerExporter.writeRows(rows, mode)`: parse+validate via `applyExportRow` (email/required checks → `RowError`), `UPDATE_ONLY` skips unknown/blank `uid`, `UPSERT` generates `UidGenerator.generateUid("CUS")`; write `synced=false` then `syncStateDao.markPendingPush(SyncEntity.CUSTOMER, now)`. Returns `ImportOutcome`. **Never call the API here** — the existing `CustomerSyncDelegate` push delivers to `/customer/v1/customers/sync`. **Unsynced-local conflict (spec FR-024 / edge case):** before overwriting, check the target row; if it already has `synced=false` (a pending local edit not yet pushed), do **not** silently clobber it — apply the configured `ConflictPolicy` (default `SKIP_WITH_WARNING`: leave the local edit, add the row to `ImportOutcome.conflicts`; alternative `OVERWRITE_LOCAL`) and surface the count in the result summary.
- [ ] T025 [APP] [US2] `DataJobEntity` + `DataJobDao` (local-only, **not** synced) in `feature/export` Room DB (bump `ExportDatabase` version); `DataJobRepository` (pure Room) for tracking import/export runs + progress.
- [ ] T026 [APP] [US2] `ImportViewModel` + state: `FilePicker` pick → `FormatReader` parse → preview first N rows + detected columns → mode toggle (Update-only/Upsert) → run `writeRows` → build `ImportOutcome` + generate local error-report CSV.
- [ ] T027 [APP] [US2] `ImportScreen` (Compose) + result summary (total/updated/created/skipped/failed) + "Download/Share error report"; entry action on customer list + `Route`/entry provider.
- [ ] T028 [APP] [US2] Compile gate (all 3 targets).

**Checkpoint**: US1 + US2 deliver the full offline round-trip on customer with zero backend work.

---

## Phase 5: User Story 3 — Configure & save custom reports (Export Templates, synced) (Priority: P2)

**Goal**: Per-module column-select + filters + sort saved as a named **Export Template** that syncs across devices (canonical `/sync`) and drives both CLIENT export and import column shape.

**Independent Test**: Create a customer template (subset columns, group filter, name sort, default CSV/CLIENT), run it → output has exactly those columns/rows/sort; reopen on another device → template present and reproduces config.

### Backend — create the `report` module (first use of backend)
- [X] T029 [BE] [US3] Create `/home/user/ampairs/report/` module: `build.gradle.kts` (deps: `:core`, `:file`, `:event`; `jackson-dataformat-csv`, `jackson-dataformat-xml`, `poi-ooxml`). Add `implementation(project(":report"))` to `/home/user/ampairs/ampairs_service/build.gradle.kts` and `"report"` to its `migrationModules` list.
- [X] T030 [BE] [US3] `ExportTemplate` entity (`OwnableBaseDomain`, uid prefix `EXT`) in `report/domain/model/` (moduleKey, name, selectedColumns JSON, filters JSON, sortBy, sortDir, defaultFormat, defaultLocation, includeInactive, active) + `ExportTemplateRepository` (Spring Data, derived queries).
- [X] T031 [BE] [US3] DTOs in `report/domain/dto/`: `ExportTemplateResponse`, `ExportTemplateUpdateRequest` (validation) + `asResponse()`/`toEntity()`/`applyRequest()` mappers.
- [X] T032 [BE] [US3] `ExportTemplateService` in `report/domain/service/`: `bulkUpsert(List<UpdateRequest>)` (UID-keyed, preserve refId) + paged `sync` query that **includes soft-deleted** rows since `last_sync`.
- [X] T033 [BE] [US3] `ExportTemplateController` in `report/controller/`: `GET /report/v1/templates/sync` (→ `ApiResponse<PageResponse<…>>`, snake_case params) + `POST /report/v1/templates/sync` (→ `ApiResponse<List<…>>`), tenant context set/cleared at controller. Match `docs/guides/offline-sync-contract.md`.
- [X] T034 [BE] [US3] `ExportTemplateCheckpointContributor` in `report/sync/` (mirror `CustomerCheckpointContributor`).
- [X] T035 [BE] [US3] Flyway `report/src/main/resources/db/migration/{mysql,postgresql}/V<next>__report_init.sql` creating `export_template` (run `./gradlew :ampairs_service:flywayInfo` to pick the version). Write **both** vendors.
- [~] **T034a [BE] [US3] (REQUIRED test)** *(partial: service-level test done; MockMvc+Testcontainers contract test pending)* Contract test for `GET/POST /report/v1/templates/sync` (Testcontainers): pull feed includes soft-deleted rows; UID-keyed upsert preserves `uid`/`refId`; snake_case params + `ApiResponse<PageResponse<…>>` shape; tenant isolation (a template under workspace A is invisible to workspace B). Satisfies the constitution ≥90% endpoint-coverage gate for the new sync endpoint.

### App — template store + sync + editor
- [ ] T036 [APP] [US3] `ExportTemplateEntity` + `ExportTemplateDao` in `feature/export` Room DB (bump version).
- [ ] T037 [APP] [US3] `ExportTemplateApi(+Impl)` (Ktor) for `/report/v1/templates/sync` using `reportUrl(...)`.
- [ ] T038 [APP] [US3] `ExportTemplateRepository` — **local-only**: write `synced=false` + `markPendingPush(SyncEntity.EXPORT_TEMPLATE)`.
- [ ] T039 [APP] [US3] `ExportTemplateSyncDelegate` `@ContributesIntoMap(WorkspaceScope::class) @SyncEntityKey(EXPORT_TEMPLATE)` (copy `CustomerSyncDelegate`: bulk push, batched pull, hard-delete server-DELETED).
- [ ] T040 [APP] [US3] `TemplateEditorScreen` + ViewModel: column checklist + reorder, typed filter rows, sort, default format/location; save → repository.
- [ ] T041 [APP] [US3] Apply templates in `ExportViewModel`/`ImportViewModel` (selectedColumns/filters/sort feed `readRows`/`writeRows`); template picker on both screens.
- [ ] T042 [APP] [US3] Compile gates: app 3 targets + `./gradlew :report:compileKotlin :report:test` (backend builds locally with system JDK 21).

**Checkpoint**: Saved custom reports sync across devices and drive offline export/import.

---

## Phase 6: User Story 4 — Large / validated SERVER jobs (Priority: P2)

**Goal**: `generation_location=SERVER` runs export/import as async backend jobs (any format incl. rich Excel, strict validation, row-level error report, completion events).

**Independent Test**: SERVER export of a large module → job PENDING→RUNNING→COMPLETED + download URL. SERVER import with mixed valid/invalid rows → COMPLETED/PARTIAL + downloadable error report enumerating bad rows; valid rows upserted via the module's `/sync` service.

### Backend — jobs, engine, descriptors, endpoints
- [ ] T043 [BE] [US4] `DataJob` + `ImportRowError` entities + repositories in `report/domain/model/`; Flyway `V<next>__report_jobs.sql` (`data_job`, `import_row_error`) both vendors.
- [ ] T044 [BE] [US4] `ModuleExportDescriptor` SPI in `/home/user/ampairs/core/.../core/export/` (moduleKey, columns, `fetch(filters,sort,page,size)`, `importRows(rows,mode)`) + `ExportColumn`/`ExportFilter`/`ImportMode`/`ImportOutcome`; `ModuleExportRegistry` (Spring `Map<String, ModuleExportDescriptor>`).
- [ ] T045 [BE] [US4] `CustomerExportDescriptor` in `/home/user/ampairs/customer/.../customer/export/`: `fetch` via customer repo (tenant-scoped, `@EntityGraph`), `importRows` → existing `CustomerService.bulkUpsert` (batch 100).
- [ ] T046 [BE] [US4] Backend `FormatWriter`/`FormatReader` in `report/engine/format/`: Jackson CSV/XML/JSON + POI `SXSSF` Excel.
- [ ] T047 [BE] [US4] `DataExportService` (create job, run via worker, store artifact in `file` module, return download ref) + `DataExportController`: `POST /report/v1/exports`, `GET /report/v1/exports/{uid}`, `GET /report/v1/exports/{uid}/download`.
- [ ] T048 [BE] [US4] `DataImportService` (parse → validate per descriptor → map valid rows → module `bulkUpsert` in 100-batches → record `ImportRowError` → build error report) + `DataImportController`: multipart `POST /report/v1/imports/{moduleKey}`, `GET /report/v1/imports/{uid}`, `GET /report/v1/imports/{uid}/errors`.
- [ ] T049 [BE] [US4] `DataJobWorker` (`VirtualThreadTaskExecutor` + `@Scheduled` queue drain, like `notification`) + `ReportConfig` (executor bean, `@Scheduled` retention/TTL purge of jobs+artifacts). **Tenant scope (constitution IV):** the worker runs outside a controller request, so before any repository/service access it MUST establish tenant context from the job's `ownerId` — `TenantContextHolder.setCurrentTenant(job.ownerId)` wrapped in `try { … } finally { TenantContextHolder.clear() }` per job (the sanctioned manual-override case). The job's `ownerId` is the only source of workspace scope; never infer it from a request header here.
- [ ] T050 [BE] [US4] `DataJobCompletionListener` → publish `DATA_JOB_COMPLETED` to `/topic/workspace/{workspaceId}` (STOMP/Kafka) + optional `notification`.
- [ ] T051 [BE] [US4] `GET /report/v1/modules` returning `ModuleExportInfo` (label, supports_import, columns) from the registry.
- [ ] **T048a [BE] [US4] (REQUIRED test)** Integration test for the SERVER import job (Testcontainers): a file with a known mix of valid/invalid rows → valid rows upserted via the module service, invalid rows recorded, job ends `PARTIAL` with the expected counts and a downloadable error report; re-uploading the same file is **idempotent by `uid`** (no duplicates); the worker honors tenant scope (job under workspace A never reads/writes workspace B). Covers FR-010/012/013/016/019 + the constitution ≥90% endpoint gate for `/exports` + `/imports`.
- [ ] **T050a [BE] [US4] (REQUIRED test)** Lightweight perf smoke (SC-005): a SERVER export and import of ~50k synthetic rows completes as an async job (PENDING→COMPLETED/PARTIAL) without blocking the request thread; assert the request returns immediately with a job id and the worker drains in batches of 100. Bound the assertion to completion + non-blocking, not a hard wall-clock SLA.

### App — server toggle + status
- [ ] T052 [APP] [US4] `DataJobApi(+Impl)` in `feature/export/data/api/`: start export (`POST /report/v1/exports`), start import (multipart `POST /report/v1/imports/{module}`), poll status, download artifact/error report.
- [ ] T053 [APP] [US4] Add `GenerationLocation` toggle to Export/Import VMs; SERVER path calls `DataJobApi`, persists/refreshes `DataJobEntity`; CLIENT path unchanged.
- [ ] T054 [APP] [US4] `JobStatusScreen` (progress, counts, download buttons); subscribe to `DATA_JOB_COMPLETED` via the existing STOMP/event handler to refresh job state.
- [ ] T055 [APP] [US4] Excel-on-iOS **contingency** fallback (only if T017's native writer is deferred/unavailable at runtime): force `GenerationLocation=SERVER` for `EXCEL` and inform the user. With T017 shipped this path is dormant; keep it as a guarded degradation, not the default.
- [ ] T056 [APP] [US4] Compile gates (app 3 targets) + `./gradlew :report:compileKotlin :report:test`.

**Checkpoint**: Scale + rich Excel + authoritative validation available online; offline CLIENT path still default.

---

## Phase 7: User Story 5 — One mechanism, many modules (Priority: P3)

**Goal**: Prove genericity — onboarding a module = adding only its descriptor(s), no engine changes.

**Independent Test**: After customer works, enable product by adding only `ProductExporter` (app) + `ProductExportDescriptor` (backend) → product export and round-trip import work with zero engine edits; then add a third (order/offers).

- [ ] T057 [P] [APP] [US5] `ProductExporter` (+ `ProductDao.queryForExport`) in `feature/product`, registered `@ModuleExporterKey("product")`.
- [ ] T058 [P] [BE] [US5] `ProductExportDescriptor` in `product` module (importRows → product `bulkUpsert`).
- [ ] T059 [P] [APP] [US5] Third module exporter (`order`) in `feature/order` + [BE] `OrderExportDescriptor` in `order` module.
- [ ] T060 [APP] [US5] Confirm module picker lists all registered exporters; `supports_import` reflects `/sync` availability (export-only when no upsert).
- [ ] T061 [DOC] [US5] "Onboard a module in one descriptor" guide: `/home/user/ampairs/docs/guides/data-export-onboarding.md` + a note in the app design doc; validate SC-006.

**Checkpoint**: ≥3 modules export + round-trip import using only descriptors.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T062 [APP] Row-threshold heuristic: CLIENT export streams in batches (default 1k rows/batch); above a **configurable row threshold (default 25,000)** the UI recommends SERVER (and, for `EXCEL` on a platform without the native writer, forces it). Surface the threshold in settings.
- [ ] T063 [BE] Tenant-isolation guard: assert every job read/write is scoped to the active workspace; reject `uid` prefixes that don't belong to `moduleKey` ("wrong entity type").
- [ ] T064 [APP+BE] Locale fidelity: machine columns (uid, ISO-UTC timestamps, money-minor, enum codes, FK uids) stay raw; only `isDisplayOnly` columns use `formatMoney`/`formatDate` (research R9).
- [ ] T065 [DOC] Update `/home/user/ampairs/docs/modules/` (new `report` module) and refresh agent-context if used.
- [ ] ~~T066~~ → promoted to **T034a** (REQUIRED, US3): `/report/v1/templates/sync` contract test.
- [ ] ~~T067~~ → promoted to **T048a** (REQUIRED, US4): SERVER import partial-failure + idempotency + tenant scope.
- [ ] T068 [APP] *(optional test)* Unit tests: CSV/XML/JSON writer↔reader round-trip (incl. iOS OOXML writer); `CustomerExporter` `readRows`/`writeRows` mapping incl. modes and the unsynced-local conflict policy (T024).
- [ ] T069 [BE] *(optional test)* Retention scheduler purges expired jobs+artifacts; tenant-isolation negative test.
- [ ] ~~T070~~ → promoted to **T050a** (REQUIRED, US4): 50k-row non-blocking perf smoke.

---

## Dependencies & Execution Order

**Phase order**: Setup (P1) → Foundational (P2) → US1 → US2 → US3 → US4 → US5 → Polish.

**Hard prerequisites**:
- Foundational (T006–T014) blocks every app story.
- **US1 → US2**: US2's `writeRows`/import UI reuse US1's `CustomerExporter`, registry, and format engine. (US2 readers are foundational T011, so US2 mainly needs US1's exporter + import VM.)
- **US3** introduces the backend `report` module (T029) — prerequisite for all later backend tasks. App template sync (T036–T039) depends on T030–T035.
- **US4** depends on US3's backend module existing; the descriptor SPI (T044) + `CustomerExportDescriptor` (T045) gate the SERVER import/export services. App SERVER toggle (T053) depends on US1/US2 VMs.
- **US5** depends on US1/US2 (app exporter pattern) and US4 (backend descriptor SPI) being in place.

**Story independence**: US1 and US2 are pure-app and ship with **no backend**. US3 and US4 are backend-heavy but do not break US1/US2 (CLIENT remains the default). US5 is additive.

---

## Parallel Execution Examples

- **Setup**: T001–T005 all `[P]` (different files/repos).
- **Foundational**: T009, T010, T011 `[P]` (independent format files); T007 `[P]` with them.
- **US1**: T015/T016/T017 `[P]` (three platform actuals, different source sets).
- **US3**: backend T030–T034 are sequential-ish within the module; app T036/T037 `[P]` with backend once T029 lands.
- **US5**: T057 / T058 / T059 all `[P]` (different modules/repos).
- Across repos, any `[BE]` and `[APP]` task in the same phase with no shared file can run concurrently.

---

## Implementation Strategy

1. **MVP = US1** (Phase 1+2+3): offline export of customers in CSV/JSON/XML/Excel. No backend. Demoable on its own.
2. **Headline = US1+US2**: the full offline round-trip bulk-edit by `uid`, delivered entirely through the existing sync push — still no backend.
3. **US3**: introduce the backend `report` module and syncable templates (custom reports per module).
4. **US4**: add SERVER async jobs for scale, rich Excel everywhere (incl. iOS), and validated imports with error reports.
5. **US5**: prove genericity across ≥3 modules; lock in "one descriptor per module".
6. **Polish**: thresholds, tenant/locale fidelity, docs, and the optional test suite.

Each phase ends at a working, independently testable checkpoint.

---

## Summary

- **Total tasks**: 73 (T001–T070 + T034a, T048a, T050a; three Polish entries T066/T067/T070 are now redirects to those required tests).
- **Per story**: Setup 5 · Foundational 9 · US1 9 · US2 5 · US3 15 · US4 16 · US5 5 · Polish 6 (+3 required tests pulled into US3/US4).
- **Required tests** (constitution ≥90% endpoint gate): T034a, T048a, T050a. All other tests remain optional.
- **Parallel opportunities**: format writers/readers, the three Excel platform actuals, cross-repo BE/APP tasks per phase, and the per-module exporters in US5.
- **MVP scope**: **US1** (offline multi-format export of one module). Headline value at **US1+US2** (offline round-trip bulk edit) with zero backend.
- **Independent test per story**: stated in each phase header.
