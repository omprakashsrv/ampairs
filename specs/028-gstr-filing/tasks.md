---
description: "Task list for GST Return Filing & Reconciliation (GSTR)"
---

# Tasks: GST Return Filing & Reconciliation (GSTR)

**Input**: Design documents from `/specs/028-gstr-filing/`
**Prerequisites**: plan.md, spec.md, research.md (R1–R12), data-model.md (8 entities / 7 enums), contracts/ (20 endpoints)

**Tests**: INCLUDED — the project constitution ("Testing & Quality Gates") and plan.md mandate ≥80%
backend coverage on aggregation, reconciliation, portal-JSON and idempotency/lock logic. Test tasks are
therefore required, not optional.

**Organization**: Tasks are grouped by user story (US1–US7 from spec.md) so each story is an
independently testable increment. Phasing also tracks plan.md's delivery phases: **P1 export-first**
(US1–US4), **P2 GSP filing + 2A/2B** (US5–US6), **mobile surface** (US7).

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1–US7, or `SETUP`/`FOUND`/`POLISH`
- All backend paths are under `gstr/` in this repo (`/home/user/ampairs`). Mobile (US7) paths are in the
  sibling repo `ampairs-app/` (`/home/user/ampairs-app`), called out per task.

## Path Conventions
- **Backend module**: `gstr/src/main/kotlin/com/ampairs/gstr/{domain/model,domain/enums,domain/dto,repository,service,provider,controller,event,config}/`
- **Backend migrations**: `gstr/src/main/resources/db/migration/{mysql,postgresql}/`
- **Backend tests**: `gstr/src/test/kotlin/com/ampairs/gstr/`
- **Mobile**: `ampairs-app/feature/gstr/src/{commonMain,androidMain,iosMain,desktopMain}/...`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Stand up the empty `gstr` bounded context so any story can build in it.

- [ ] T001 [SETUP] Create the `gstr` Gradle module: `gstr/build.gradle.kts` (Spring Boot/Kotlin module mirroring `einvoice/build.gradle.kts`: depends on `:core`, `:invoice`, `:tax`, `:einvoice`, `:setting`, `:event`, Spring Data JPA, Flyway, Jackson, validation, Spring scheduling, an HTTP client (RestClient/WebClient) for P2).
- [ ] T002 [SETUP] Register the module: add `include("gstr")` to `settings.gradle.kts`; add `implementation(project(":gstr"))` and `"gstr"` to `migrationModules` in `ampairs_service/build.gradle.kts`.
- [ ] T002a [SETUP] Resolve the Flyway version band BEFORE writing any migration: run `./gradlew :ampairs_service:flywayInfo`; confirm `V1.0.110/111/112` are unused (specs 015/016/024/026 may have claimed 105–107 or higher). If any is taken, pick the next free contiguous triple and update the version numbers referenced in T008/T009 (110), T062 (111) and T052 (112) consistently.
- [ ] T003 [P] [SETUP] Create the package skeleton under `gstr/src/main/kotlin/com/ampairs/gstr/`: `domain/model`, `domain/enums`, `domain/dto`, `repository`, `service`, `provider`, `controller`, `event`, `config`; and the test root `gstr/src/test/kotlin/com/ampairs/gstr/`.
- [ ] T004 [P] [SETUP] Add module doc stub `docs/modules/gstr.md` and a module `gstr/CLAUDE.md` summarizing the bounded context (reads invoice/tax/einvoice/setting via services + events; off-`/sync` pull-only on mobile).

**Checkpoint**: `./gradlew :gstr:compileKotlin` succeeds on an empty module.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, enums, core entities, integration seams and error handling that EVERY story needs.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Enums & exceptions

- [ ] T005 [P] [FOUND] Create all 7 enums in `gstr/src/main/kotlin/com/ampairs/gstr/domain/enums/` (one file each): `ReturnType` (GSTR1/GSTR3B/CMP08/GSTR9/GSTR9C), `ReturnStatus` (NOT_STARTED/PREPARED/RECONCILED/FILED/ACKNOWLEDGED), `FilingStatus` (INITIATED/SUBMITTED/EVC_REQUESTED/FILED/ACKNOWLEDGED/FAILED), `RegistrationType` (REGULAR/COMPOSITION/SEZ/CASUAL), `FilingFrequency` (MONTHLY/QUARTERLY), `Gstr1Section` (B2B/B2CL/B2CS/CDNR/CDNUR/EXP/NIL/HSN/DOCS), `MismatchType` (MATCHED/MISMATCH_VALUE/MISMATCH_GSTIN/MISSING_IN_2B/MISSING_IN_BOOKS/PROBABLE_MATCH). Values verbatim from data-model.md.
- [ ] T006 [P] [FOUND] Create the exception hierarchy in `gstr/src/main/kotlin/com/ampairs/gstr/config/`: `GstrException`, `GstnFilingException`, `PeriodLockedException`; add a module `@RestControllerAdvice` handler that maps them to `ApiResponse` error envelopes (no business try/catch in controllers).
- [ ] T007 [P] [FOUND] Create `gstr/src/main/kotlin/com/ampairs/gstr/config/GstrConstants.kt` and a money helper `GstrMoney` (BigDecimal scale-4 internal; `roundRupee()` = HALF_UP scale 0 at the section boundary; ±₹1 tolerance constant — R12).

### Schema (Flyway V1.0.110 — P1 core tables)

- [ ] T008 [FOUND] Write `gstr/src/main/resources/db/migration/postgresql/V1.0.110__create_gstr_tables.sql`: tables `gstin_registration`, `gst_return_period` (UNIQUE `(owner_id, gstin, return_type, financial_year, period)`), `gst_return_snapshot`, `purchase_register_entry`; `TIMESTAMPTZ` timestamps, `DECIMAL(19,4)` money, JSON column for snapshot section data, `owner_id` tenant column + indexes.
- [ ] T009 [FOUND] Write the MySQL twin `gstr/src/main/resources/db/migration/mysql/V1.0.110__create_gstr_tables.sql` (same schema; `TIMESTAMP`, `LONGTEXT`/`JSON` for snapshot). **Both vendors required** (rule 07-migrations).

### Core entities & repositories (P1)

- [ ] T010 [P] [FOUND] `domain/model/GstinRegistration.kt` — `OwnableBaseDomain` (`@TenantId ownerId`); fields gstin(15), stateCode, legalName, tradeName, registrationType, filingFrequency, active. Per data-model.md.
- [ ] T011 [P] [FOUND] `domain/model/GstReturnPeriod.kt` — `OwnableBaseDomain`; gstin, returnType, financialYear, period, status, arn?, filedAt?, snapshotId?; `@NamedEntityGraph` including the snapshot. Unique key per T008.
- [ ] T012 [P] [FOUND] `domain/model/GstReturnSnapshot.kt` — `OwnableBaseDomain`; sectionData (JSON), headline totals (DECIMAL 19,4), preparedAt; immutable-once-FILED note.
- [ ] T013 [P] [FOUND] `domain/model/PurchaseRegisterEntry.kt` — `OwnableBaseDomain`; supplierGstin, supplierInvoiceNo, supplierInvoiceDate, taxable, cgst/sgst/igst/cess, itcEligibility (schema now; matching is US6).
- [ ] T014 [P] [FOUND] Repositories in `repository/` for the four entities: `GstinRegistrationRepository`, `GstReturnPeriodRepository` (derived finders by gstin/type/fy/period; `@EntityGraph`), `GstReturnSnapshotRepository`, `PurchaseRegisterEntryRepository`. Derived queries; `@Query` only where unavoidable.
- [ ] T015 [FOUND] Add the period↔invoice aggregation query and the pull-only sync-feed query on `GstReturnPeriodRepository` (`@Query` justified: cross-cutting period aggregation + `last_sync` feed), plus the pending-prepare poll.

### DTOs & converters (base)

- [ ] T016 [P] [FOUND] `domain/dto/` base request/response DTOs + extension converters for `GstinRegistration` and `GstReturnPeriod` (snake_case via global Jackson; `asResponse()`/`toEntity()`; validation annotations). Raw filed/2B blobs omitted from list DTOs.

### Cross-module integration seams (R1)

- [ ] T017 [P] [FOUND] `service/InvoiceReadAdapter.kt` — read finalized, non-cancelled invoices + CDNs for `(gstin, period)` via the public `InvoiceService` (series/sequenceNumber, customerGst/sellerGst, placeOfSupply/sellerPlaceOfSupply, taxInfos, totals, tax audit snapshot). No repository access into `invoice`.
- [ ] T018 [P] [FOUND] `service/ComplianceReadAdapters.kt` — read `tax` GST definitions and `einvoice` `EInvoiceDocument` IRN/ackNo via their public services; read `SettingService` for module toggles.
- [ ] T019 [P] [FOUND] `config/GstrSettingDefinitions.kt` — register `gstr_enabled`, `filing_frequency`, `b2cl_threshold` (default ₹1,00,000, configurable — FR-004) with the central `setting` registry.
- [ ] T020 [FOUND] `event/InvoiceEventListener.kt` — listen for `InvoiceFinalizedEvent`/`InvoiceCancelledEvent`; mark the affected `GstReturnPeriod` dirty (re-prepare on next request); consult `PeriodLockService` on finalize (skeleton; full routing in US5).
- [ ] T021 [FOUND] `service/PeriodLockService.kt` — skeleton `isPeriodLocked(gstin, date): Boolean` (returns false until a period is FILED; the seam ships now so the `invoice` finalize path can call it). Wire the additive call from the invoice finalize path.

### Foundational tests

- [ ] T022 [P] [FOUND] Flyway migrate+validate test (Testcontainers, PostgreSQL) asserting V1.0.110 applies and the unique constraint holds: `gstr/src/test/kotlin/com/ampairs/gstr/migration/GstrMigrationTest.kt`.
- [ ] T023 [P] [FOUND] Repository slice tests for the four repos incl. tenant isolation + unique-period constraint: `gstr/src/test/kotlin/com/ampairs/gstr/repository/`.

**Checkpoint**: Schema applies on both vendors; entities/repos/seams compile and pass; `./gradlew :gstr:test` green for foundational tests. **User stories can now begin.**

---

## Phase 3: User Story 1 — Auto-prepare GSTR-1 + portal-ready export (Priority: P1) 🎯 MVP

**Goal**: Aggregate a period's finalized invoices into GSTR-1 sections and produce a portal-compatible
JSON/Excel export, with no manual data entry.

**Independent Test**: Finalize a representative invoice mix in a period, `POST …/prepare`, confirm every
invoice lands in exactly one correct section with footing totals, and `GET …/export` downloads a
portal-ready file (quickstart Steps 2 & 5; SC-001, SC-002, SC-005).

### Tests for User Story 1 ⚠️ (write first, ensure they fail)

- [ ] T024 [P] [US1] Golden aggregation tests for `Gstr1Aggregator`: B2B invoice-wise, B2CL vs B2CS threshold split, CDNR/CDNUR, EXP/NIL, HSN rollup, DOCS series — `gstr/src/test/kotlin/com/ampairs/gstr/service/Gstr1AggregatorTest.kt`.
- [ ] T025 [P] [US1] Rate-as-of-issue test: a backdated rate change must not alter the prepared snapshot (uses the immutable tax audit snapshot; FR-005) — same dir.
- [ ] T026 [P] [US1] Portal-JSON schema-conformance test for `Gstr1PortalBuilder` (GSTN field names, rupee-rounded section totals) — `…/service/Gstr1PortalBuilderTest.kt`.
- [ ] T027 [P] [US1] Controller/web-layer test for `prepare` + `gstr1` retrieve + `export?format=json|xlsx` returning `ApiResponse` / streamed artifact — `…/controller/GstrControllerTest.kt`.
- [ ] T027a [P] [US1] NIL-period test: a period with zero finalized invoices prepares as a valid NIL GSTR-1 (empty sections, zero rupee totals) and exports without error (FR-008, edge "empty/nil period") — `gstr/src/test/kotlin/com/ampairs/gstr/service/Gstr1NilReturnTest.kt`.

### Implementation for User Story 1

- [ ] T028 [US1] `service/Gstr1Aggregator.kt` — classify finalized invoices/CDNs (from T017 adapter) into Gstr1Section buckets using customerGst presence (registered) and placeOfSupply vs sellerPlaceOfSupply (intra/inter-state), reading rate-wise tax from the audit snapshot (fallback to live taxInfos for legacy). B2CL/B2CS split at `b2cl_threshold`. Produces section-structured data + HSN + DOCS. (R3)
- [ ] T029 [US1] `service/GstReturnService.kt` — `prepare(gstin, type, period)`: idempotent upsert of `GstReturnPeriod`, run the aggregator, persist an immutable `GstReturnSnapshot`, advance NOT_STARTED→PREPARED (re-prepare only while not FILED — R8); an activity-free period yields a valid NIL snapshot (FR-008). Reuse `GstrMoney` rounding once at section boundary (R12).
- [ ] T030 [P] [US1] `service/Gstr1PortalBuilder.kt` — render the snapshot into the GSTN offline-utility JSON shape via isolated `*PortalBuilder` DTOs with explicit `@JsonProperty` (b2b/inv/itms/txval/iamt.../pos/rt/hsn_sc) — documented external-contract exception.
- [ ] T031 [P] [US1] `service/GstrExcelExporter.kt` — render the same snapshot to the Excel (xlsx) form.
- [ ] T032 [US1] `controller/GstrController.kt` — `POST /gstr/v1/returns/{gstin}/{type}/{period}/prepare`, `GET …/gstr1`, `GET …/{type}/{period}/export?format=json|xlsx` (streamed); set tenant context at controller (try/finally), return `ApiResponse`. Per contracts/gstr-prepare.md + gstr-export.md.
- [ ] T033 [US1] `domain/dto/` GSTR-1 retrieve + readiness-less prepare response DTOs + converters.

**Checkpoint**: A month's GSTR-1 prepares and exports a portal-ready file end-to-end. **MVP deliverable.**

---

## Phase 4: User Story 2 — GSTR-3B tied to GSTR-1 (Priority: P2)

**Goal**: Derive GSTR-3B's outward liability from the GSTR-1 totals (+ RCM) so 3B and GSTR-1 tie to the
rupee; ITC section shown pending. Plus CMP-08 for composition dealers.

**Independent Test**: Prepare GSTR-1, then GSTR-3B; confirm 3B outward tax equals GSTR-1 outward totals
(rupee), RCM included, ITC marked pending (quickstart Step 4; SC-003).

### Tests for User Story 2 ⚠️

- [ ] T034 [P] [US2] 3B⟷GSTR-1 tie-out test: `Gstr3bAggregator` outward totals == GSTR-1 snapshot totals to the rupee; RCM reflected; ITC pending — `…/service/Gstr3bAggregatorTest.kt`.
- [ ] T035 [P] [US2] `Cmp08Service` summary test for a composition-registration GSTIN — `…/service/Cmp08ServiceTest.kt`.

### Implementation for User Story 2

- [ ] T036 [US2] `service/Gstr3bAggregator.kt` — build 3B (tables 3.1/3.2/3.1.1) FROM the GSTR-1 snapshot totals + RCM (from spec 026 `rcmApplicable`); ITC table stubbed/pending (FR-006/FR-007). Tie by construction. (R4)
- [ ] T037 [P] [US2] `service/Cmp08Service.kt` — CMP-08 summary for COMPOSITION registrations (ties to spec 026 composition mode).
- [ ] T038 [P] [US2] `service/Gstr3bPortalBuilder.kt` — 3B portal JSON shape (isolated `@JsonProperty` DTOs).
- [ ] T039 [US2] Extend `GstReturnService.prepare` to also produce the 3B/CMP-08 snapshot; extend `GstrController` with `GET …/gstr3b` and `GET …/cmp08/{period}` + 3B export. Per contracts.

**Checkpoint**: GSTR-1 + GSTR-3B (and CMP-08) prepare and tie out; both export.

---

## Phase 5: User Story 3 — Filing-readiness gate (Priority: P2)

**Goal**: Block returns containing portal-rejecting data errors (missing GSTIN/HSN/PoS, unfooted tax,
series gaps) with a blocking readiness report; never auto-edit invoices.

**Independent Test**: Introduce a B2B invoice missing customer GSTIN + a line missing HSN, prepare,
confirm both are blocking errors and the return cannot advance to ready; fix + re-prepare clears it
(quickstart Step 3; SC-004).

### Tests for User Story 3 ⚠️

- [ ] T040 [P] [US3] `ReturnReadinessService` tests: missing customer GSTIN (B2B), missing HSN/SAC, invalid place-of-supply, missing sellerGst, unfooted tax breakup, document-series gap each produce a BLOCKING error; warnings non-blocking — `…/service/ReturnReadinessServiceTest.kt`.
- [ ] T041 [P] [US3] Gate test: a period with blocking errors cannot advance PREPARED→RECONCILED (FR-010) — `…/service/ReconciliationEngineTest.kt`.

### Implementation for User Story 3

- [ ] T042 [US3] `service/ReconciliationEngine.kt` — invoice⟷GSTR-1 self-check: every finalized invoice lands in exactly one section; rate-wise totals foot; CDN references resolve; flag-only, no mutation (R9, FR-012/FR-014).
- [ ] T043 [US3] `service/ReturnReadinessService.kt` — produce a blocking `ReturnReadinessReport` (errors vs warnings) from the engine; advance to RECONCILED only when clear (R10, FR-009/FR-010). Never edit source invoices (FR-011).
- [ ] T044 [US3] `controller/GstrController.kt` — `GET …/{type}/{period}/readiness` returning the report; wire the gate into `prepare`/advance. Per contracts/gstr-prepare.md.
- [ ] T045 [P] [US3] `domain/dto/` `ReturnReadinessReportResponse` (+ late-fee/interest informational estimate field, FR-016) + converter.

**Checkpoint**: Readiness blocks bad data; clean periods reach RECONCILED.

---

## Phase 6: User Story 4 — Multi-GSTIN independent filing (Priority: P3)

**Goal**: Register multiple per-state GSTINs and prepare/track returns per GSTIN independently;
attribute each invoice to the GSTIN of the state it was billed from; quarterly filers file one GSTR-1
per quarter (no monthly IFF).

**Independent Test**: Register two GSTINs in different states, finalize invoices billed from each,
confirm each GSTIN's GSTR-1 contains only its state's invoices with independent status (quickstart
Step 6; SC-007).

### Tests for User Story 4 ⚠️

- [ ] T046 [P] [US4] Cross-state isolation test: invoices bucket to the correct GSTIN by seller-GSTIN state; zero cross-state leakage — `…/service/GstinBucketingTest.kt`.
- [ ] T047 [P] [US4] Period-representation test: MONTHLY frequency → months; QUARTERLY (QRMP) → one GSTR-1 per quarter (no IFF sub-periods) — `…/service/PeriodResolverTest.kt`.

### Implementation for User Story 4

- [ ] T048 [US4] Extend `Gstr1Aggregator`/`InvoiceReadAdapter` to bucket source documents to a `GstinRegistration` by seller-GSTIN state code (R2, FR-019).
- [ ] T049 [P] [US4] `service/PeriodResolver.kt` — represent periods by filing frequency (`MMYYYY` monthly, `Q{n}YYYY` quarterly = one GSTR-1/quarter; IFF deferred — FR-020).
- [ ] T050 [US4] `controller/GstinRegistrationController.kt` — full CRUD `POST/GET/PUT/DELETE /gstr/v1/registrations[/{gstin}]` + list (per contracts/gstr-sync.md); validation of the 15-char GSTIN + state code.
- [ ] T051 [P] [US4] `GstReturnPeriodRepository` finders + `GET /gstr/v1/returns/{gstin}` list endpoint for per-GSTIN independent status (filtered by financial_year/type).

**Checkpoint**: A multi-state business prepares each GSTIN's returns independently.

---

## Phase 7: User Story 5 — Electronic filing + EVC/ARN (Priority: P3) · plan Phase 2

**Goal**: File a reconciled return through the GST network with EVC/OTP (code to the signatory, never
the app), persist ARN, lock + freeze the filed period; queue + retry, idempotent on retries. File
action + credential setup are **owner/admin-only**.

**Independent Test**: Against the sandbox provider, file a prepared return, supply the EVC, confirm ARN
recorded and the period locked; a retry after a lost ack does not double-file (US5 scenarios; SC-006).

### Schema & provider

- [ ] T052 [US5] Flyway `V1.0.112__create_gstr_filing_credential_tables.sql` (postgresql + mysql): `gst_filing_attempt`, `gstn_credential` (encrypted per-GSTIN). **Verify the version is free via `:ampairs_service:flywayInfo` and bump the band if taken** (plan notes 110–112 may collide with 015/016/024/026).
- [ ] T053 [P] [US5] `domain/model/GstFilingAttempt.kt` (+ repo) — FilingStatus lifecycle, `clientRequestId` for idempotency. `domain/model/GstnCredential.kt` (+ repo) — encrypted fields.
- [ ] T054 [P] [US5] `provider/GstnFilingProvider.kt` port (`authenticate`, `saveGstr1`, `submitGstr1`, `fileGstr1`, `getReturnStatus`, `get2A`, `get2B`, `getFiledReturn`) + `GstnSandboxProvider` impl + `GstnProviderResolver` (per-workspace) + `GstnSessionTokenCache`. Credentials from env + encrypted row (R6, Principle XI).

### Tests for User Story 5 ⚠️

- [ ] T055 [P] [US5] Filing idempotency test: lost-ack retry calls `getReturnStatus` first and never double-files; `clientRequestId` dedupes — `…/service/FilingIdempotencyTest.kt` (R8).
- [ ] T056 [P] [US5] Period-lock/immutability test: at FILED the snapshot is frozen and `PeriodLockService.isPeriodLocked` returns true; a finalized invoice dated in the filed period is **routed to the next open period, never blocked** (clarification; FR-025) — `…/service/PeriodLockServiceTest.kt`.
- [ ] T057 [P] [US5] Authorization test: a non owner/admin member is denied `file`/`file/confirm` + credential setup, but may prepare/export (FR-031) — `…/controller/GstrFilingControllerAuthTest.kt`.

### Implementation for User Story 5

- [ ] T058 [US5] Extend `GstReturnService`/new `GstFilingService` — two-step file: request EVC (INITIATED→SUBMITTED→EVC_REQUESTED) then confirm OTP (→FILED→ACKNOWLEDGED), persist ARN + filedAt on the period, freeze the snapshot (R7/R8).
- [ ] T059 [US5] Implement `PeriodLockService.isPeriodLocked` fully + the invoice-finalize next-open-period **routing** (tag the invoice's GSTR-1 reporting period to the next open period; never block finalize — clarification). Update `InvoiceEventListener`.
- [ ] T060 [P] [US5] `service/GstnFilingWorker.kt` — `@Scheduled` queue/poller over INITIATED/FAILED with exponential backoff (online-only; mirrors `EInvoiceQueueWorker`).
- [ ] T061 [US5] `controller/GstrFilingController.kt` — `POST …/file`, `POST …/file/confirm`, `GET …/filing-status` (ARN); **owner/admin-gated** (RBAC); EVC OTP never returned to caller. Per contracts/gstr-filing.md.

**Checkpoint**: A reconciled return files electronically, records ARN, and is immutable + source-locked.

---

## Phase 8: User Story 6 — 2A/2B ITC reconciliation (Priority: P3) · plan Phase 2

**Goal**: Match the purchase register against supplier-reported 2A/2B into a typed mismatch taxonomy,
summarize eligible-vs-at-risk ITC, and feed the 3B ITC table. Books side seeded by CSV/Excel import.

**Independent Test**: Import a purchase register + a sample 2B with one exact match, one value mismatch,
one missing-in-2B; confirm correct bucketing and the at-risk ITC summary (US6 scenarios; SC-008).

### Schema

- [ ] T062 [US6] Flyway `V1.0.111__create_gstr_reconciliation_tables.sql` (postgresql + mysql): `gstn_2b_record`, `reconciliation_result`. Verify version via `flywayInfo`.
- [ ] T063 [P] [US6] `domain/model/Gstn2bRecord.kt` (+ repo) and `domain/model/ReconciliationResult.kt` (+ repo) with `MismatchType`.

### Tests for User Story 6 ⚠️

- [ ] T064 [P] [US6] Mismatch-taxonomy tests: MATCHED, MISMATCH_VALUE (beyond ±₹1), MISMATCH_GSTIN, MISSING_IN_2B, MISSING_IN_BOOKS, PROBABLE_MATCH; ±₹1 rounding tolerance prevents false mismatches — `…/service/Books2bReconciliationTest.kt` (R9/R12, SC-008).
- [ ] T065 [P] [US6] Purchase-register import test (CSV/Excel → `PurchaseRegisterEntry`) — `…/service/PurchaseRegisterImportServiceTest.kt`.

### Implementation for User Story 6

- [ ] T066 [US6] Extend `ReconciliationEngine` with books⟷2B matching on `(supplier_gstin, invoice_no, invoice_date, taxable, tax)` + tolerance → the six buckets; aggregate eligible vs at-risk ITC; flag-only (R9).
- [ ] T067 [P] [US6] `service/PurchaseRegisterImportService.kt` — CSV/Excel import populating `PurchaseRegisterEntry` (FR-015).
- [ ] T068 [P] [US6] Extend `GstnFilingProvider` usage for the 2A/2B pull (`get2B`) into `Gstn2bRecord` via the online queue.
- [ ] T069 [US6] Feed eligible ITC from reconciliation into `Gstr3bAggregator`'s ITC table (lifts FR-007 "pending" once data exists).
- [ ] T070 [US6] `controller/GstrFilingController.kt` — `POST …/2b/pull`, `GET …/reconciliation?bucket=`, `POST /gstr/v1/purchase-register/import`. Per contracts/gstr-filing.md.

**Checkpoint**: ITC reconciliation classifies every line and informs 3B.

---

## Phase 9: User Story 7 — Mobile read-only filing status (Priority: P3) · mobile surface

**Goal**: Surface per-GSTIN/period status, headline totals and ARN read-only on the app via a pull-only
`/sync`; allow prepare/file as online commands only; no on-device return computation.

**Independent Test**: After a return is prepared/filed on the server, open the app and confirm the
period status, totals and ARN render read-only (US7 scenarios; SC-009).

### Backend (this repo)

- [ ] T071 [US7] `controller/GstrController.kt` — pull-only `GET /gstr/v1/returns/sync` (snake_case `last_sync/page/size/sort_by/sort_dir`, `PageResponse`, includes soft-/status rows the app mirrors); a `POST …/returns/sync` push returns `405` (server-authored — R11). Per contracts/gstr-sync.md.
- [ ] T072 [P] [US7] Web-layer test for the sync feed pagination + 405-on-push — `gstr/src/test/kotlin/com/ampairs/gstr/controller/GstrSyncControllerTest.kt`.

### Mobile (sibling repo `ampairs-app/`)

- [ ] T073 [P] [US7] `ampairs-app/feature/gstr/` module scaffold: `build.gradle.kts`, add to `settings.gradle.kts`; `data/api/GstrApi(+Impl)` + `ApiUrlBuilder.gstrUrl` in `data/common`.
- [ ] T074 [P] [US7] Room layer in `feature/gstr/src/commonMain/.../data/db/`: `GstReturnPeriod` entity (status + headline totals), DAO, DB; `domain/` display models + status enums.
- [ ] T075 [US7] `data/repository/GstrRepository.kt` (local-only pull-mirror) + `sync/GstReturnPeriodSyncDelegate.kt` (pull-only) wired to `CentralSyncService`; add `SyncEntity.GSTR_RETURN_PERIOD`.
- [ ] T076 [P] [US7] Platform DI `feature/gstr/src/{androidMain,iosMain,desktopMain}/.../GstrModule.{platform}.kt` (`@ContributesTo(WorkspaceScope::class)`, `@SingleIn(WorkspaceScope::class)` DB, register with `WorkspaceClosableRegistry`).
- [ ] T077 [US7] UI in `feature/gstr/src/commonMain/.../ui/`: return-period status list + headline-totals summary screen (read-only) + "prepare"/"file" online-command ViewModels (`metroViewModel()`); strings via Compose resources.
- [ ] T078 [US7] Wiring: `ModuleRegistry` (`"gst-filing" → Route.Gstr`), `shared/` Routes + entry provider; nav.
- [ ] T079 [US7] Mobile gates: `./gradlew :feature:gstr:check` + 3-target compile (`shared:compileKotlinIosSimulatorArm64`, `androidApp:compileDebugKotlinAndroid`, `desktopApp:compileKotlin`).

**Checkpoint**: The app shows live filing status read-only; prepare/file run as server online commands.

---

## Phase 10: Polish & Cross-Cutting Concerns

- [ ] T080 [P] [POLISH] Re-verify (post-merge) the Flyway versions resolved in T002a are still free via `./gradlew :ampairs_service:flywayInfo`; re-validate both vendors apply cleanly.
- [ ] T081 [P] [POLISH] Coverage gate: ensure ≥80% on aggregation + reconciliation + portal-JSON + idempotency/lock (`./gradlew :gstr:test` + report).
- [ ] T082 [P] [POLISH] Performance check: prepare a month's GSTR-1 for a workspace with thousands of invoices in seconds; export streams (plan Performance Goals; SC-001).
- [ ] T083 [P] [POLISH] Fill `docs/modules/gstr.md` (endpoints, lifecycle, GSTR-1/3B section mapping, off-`/sync` rationale).
- [ ] T084 [POLISH] Execute `specs/028-gstr-filing/quickstart.md` end-to-end (Steps 1–8) and fix any drift; tick SC-001..SC-009.
- [ ] T085 [P] [POLISH] Security pass: confirm credentials never logged/serialized; filed/2B blobs ACL'd; EVC OTP never returned; owner/admin gate enforced (Principle XI).

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (P1)** → no deps.
- **Foundational (P2)** → depends on Setup; **BLOCKS all user stories**.
- **US1 (P3)** → after Foundational. The MVP.
- **US2, US3 (P4–P5)** → after Foundational; both build on US1's snapshot (US2 reads GSTR-1 totals; US3 self-checks the aggregation). Sequence US1 → US2/US3.
- **US4 (P6)** → after Foundational; refines US1 aggregation (bucketing) — sequence after US1.
- **US5 (P7)** → after US1–US3 (files a RECONCILED return); introduces V1.0.112 + provider.
- **US6 (P8)** → after US2 (feeds 3B ITC) + US5 (shares the provider/queue); introduces V1.0.111.
- **US7 (P9)** → backend feed after Foundational + US1 (needs period status); mobile after the backend feed exists.
- **Polish (P10)** → after the targeted stories are complete.

### Within each story

- Tests written first and failing → models → services → endpoints → integration.
- Both Flyway vendors land together; never modify an applied migration.

### Parallel opportunities

- Setup: T003, T004 [P].
- Foundational: enums/exceptions/money (T005–T007) [P]; the four entities (T010–T013) [P]; integration seams (T017–T019) [P]; foundational tests (T022–T023) [P]. (T008/T009 schema and T014/T015 repos are sequential gates.)
- Within a story, all `[P]` test tasks run together, then `[P]` builders/models together.
- With staff: once Foundational is done, US1 → then US2/US3/US4 can proceed largely in parallel; US5/US6 (plan Phase 2) and US7-mobile are a second wave.

---

## Parallel Example: User Story 1

```bash
# Tests first (all [P], different files):
Task: "Gstr1Aggregator golden tests (T024)"
Task: "Rate-as-of-issue test (T025)"
Task: "Gstr1PortalBuilder schema-conformance test (T026)"
Task: "GstrController prepare/retrieve/export test (T027)"

# Then parallel builders:
Task: "Gstr1PortalBuilder (T030)"
Task: "GstrExcelExporter (T031)"
```

---

## Implementation Strategy

### MVP first (User Story 1 only)

1. Phase 1 Setup → 2. Phase 2 Foundational (CRITICAL gate) → 3. Phase 3 US1 → **STOP & VALIDATE**:
   prepare + export a real month's GSTR-1 (quickstart Steps 1–2, 5). This alone removes the monthly
   re-keying — a shippable export-first MVP with no GSP onboarding.

### Incremental delivery (plan phases)

1. Foundation → **US1 (MVP export-first)** → demo.
2. **US2 + US3** (3B tie-out + readiness gate) → the full export-first compliance slice.
3. **US4** (multi-GSTIN) → branch businesses.
4. **US5 + US6** (plan Phase 2: GSP filing + EVC/ARN, then 2A/2B ITC) → live filing + ITC.
5. **US7** (mobile status) → owners see filing status on the phone.
6. Phase 3 annual returns (GSTR-9/9C) and QRMP monthly IFF are explicitly out of scope here.

### Notes

- `[P]` = different files, no deps. `[Story]` maps to spec.md user stories for traceability.
- Backend tasks run in `/home/user/ampairs`; speckit scripts need `SPECIFY_FEATURE=028-gstr-filing` (branch is `claude/gstr-filing-specs-rsi2pm`). Mobile tasks (T073–T079) run in `/home/user/ampairs-app`.
- Commit after each task or logical group; stop at any checkpoint to validate a story independently.
