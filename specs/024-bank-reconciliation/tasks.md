---
description: "Task list for Bank Reconciliation (spec 024)"
---

# Tasks: Bank Reconciliation

**Input**: Design documents from `/specs/024-bank-reconciliation/`
**Prerequisites**: plan.md (required), spec.md (user stories US1–US5), research.md (R1–R10)

**Tests**: INCLUDED — plan.md "Testing" + Constitution "Testing & Quality Gates" require backend ≥80%
coverage on parsers/matching/idempotency, the tie-out invariant, and bounce routing; mobile `check` +
3-target compile.

**Organization**: Tasks are grouped by user story. Two repos are involved — paths are prefixed
`ampairs/…` (backend, this repo) and `ampairs-app/…` (mobile, sibling repo).

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1–US5 maps to the spec's user stories
- Exact file paths are included in each task

## Path Conventions (from plan.md "Project Structure")
- **Backend**: `ampairs/banking/src/main/kotlin/com/ampairs/banking/{domain/{model,enums,dto},repository,service,parser,controller,config}`, migrations under `ampairs/banking/src/main/resources/db/migration/{mysql,postgresql}/`, tests under `ampairs/banking/src/test/kotlin/com/ampairs/banking/`
- **Mobile**: `ampairs-app/feature/banking/src/{commonMain,androidMain,iosMain,desktopMain}/kotlin/com/ampairs/banking/{data/{api,db,repository},domain,di,sync,ui}`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create both new modules and wire them into their builds.

- [ ] T001 Create backend `banking` module skeleton (package dirs `domain/{model,enums,dto}`, `repository`, `service`, `parser`, `controller`, `config`) and `ampairs/banking/build.gradle.kts` mirroring an existing module (e.g. `payment`), depending on `core`
- [ ] T002 Register the module in `ampairs/settings.gradle.kts` (`include("banking")`) and add `implementation(project(":banking"))` + `"banking"` to `migrationModules` in `ampairs/ampairs_service/build.gradle.kts`
- [ ] T003 Add `ampairs/banking/CLAUDE.md` documenting the bounded context (overlay on `payment`, server-side import+match, never posts ledger entries)
- [ ] T004 [P] Create mobile `feature/banking` module skeleton (`commonMain/kotlin/com/ampairs/banking/{data/{api,db,repository},domain,di,sync,ui}` + platform mains) and `ampairs-app/feature/banking/build.gradle.kts` mirroring `feature/payment`
- [ ] T005 [P] Register `:feature:banking` in `ampairs-app/settings.gradle.kts` and add it as a dependency where feature modules are aggregated (shared/navigation)
- [ ] T006 [P] Add `ApiUrlBuilder.bankingUrl("v1/…")` in `ampairs-app/data/common/.../ApiUrlBuilder.kt`

**Checkpoint**: Both modules compile empty and are on their respective build graphs.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, enums, settings, and mobile DI/DB that every user story depends on.

**⚠️ CRITICAL**: No user-story work can begin until this phase is complete.

- [ ] T007 Confirm the next free Flyway version via `./gradlew :ampairs_service:flywayInfo` (plan assumes `V1.0.105`; adjust if taken)
- [ ] T008 Write Flyway migration creating ALL banking tables — `bank_account`, `bank_statement_line`, `bank_match`, `bank_match_line` (N:M join), `bank_match_voucher` (N:M join), `statement_import`, `column_mapping` — in `ampairs/banking/src/main/resources/db/migration/postgresql/V1.0.105__create_banking_tables.sql` (owner_id, timestamps `TIMESTAMPTZ`, money `BIGINT` paise, narration `TEXT`; unique index on statement-line fingerprint uid; indexes on owner_id+value_date, amount, ref/utr)
- [ ] T009 [P] Write the byte-identical MySQL migration in `ampairs/banking/src/main/resources/db/migration/mysql/V1.0.105__create_banking_tables.sql` (`TIMESTAMP`, `BIGINT`)
- [ ] T010 [P] Create enums `DrCr`, `MatchStatus` (SUGGESTED/CONFIRMED/REJECTED), `MatchType` (AUTO/MANUAL), `Confidence` (EXACT/HIGH/MEDIUM/LOW), `StatementFormat` (CSV/MT940/ACCOUNT_AGGREGATOR), `ImportStatus` (PARSING/MATCHING/COMPLETE/FAILED), `LineMatchStatus` (UNMATCHED/SUGGESTED/MATCHED/REJECTED) in `ampairs/banking/src/main/kotlin/com/ampairs/banking/domain/enums/`
- [ ] T011 Create `BankingSettingDefinitions` provider in `ampairs/banking/src/main/kotlin/com/ampairs/banking/service/BankingSettingDefinitions.kt` gated by the installed `banking` module, exposing `banking_enabled`, `auto_match_min_confidence` (default HIGH), `match_date_window_days` (default 3), `match_amount_tolerance_paise` (default 0), `default_csv_profile`, `bounce_narration_patterns` (FR-023; research R10)
- [ ] T012 Add `config/Constants.kt` (uid prefixes `BSL_`/`BMT_`/`BAC_`/`SIMP_`, batch size 100, max sync cycle) in `ampairs/banking/src/main/kotlin/com/ampairs/banking/config/`
- [ ] T013 [P] Add `SyncEntity` entries `BANK_ACCOUNT`, `BANK_STATEMENT_LINE`, `BANK_MATCH`, `STATEMENT_IMPORT` to `ampairs-app/data/sync/.../SyncEntity.kt`
- [ ] T014 Create the mobile workspace-scoped Room DB `BankingDatabase` and platform DI (`BankingModule.{android,ios,desktop}.kt`, `@ContributesTo(WorkspaceScope::class)`, `@SingleIn(WorkspaceScope::class)`, registered with `WorkspaceClosableRegistry`, path `banking`) in `ampairs-app/feature/banking/src/`
- [ ] T015 [P] Create `BankingApi` interface + skeleton `BankingApiImpl` (Ktor, including a multipart upload method) and `domain/` model/enum mirrors in `ampairs-app/feature/banking/src/commonMain/kotlin/com/ampairs/banking/data/api/`

**Checkpoint**: Schema, enums, settings, and mobile DB/DI exist; stories can begin.

---

## Phase 3: User Story 1 — Import a bank statement for an account (Priority: P1) 🎯 MVP

**Goal**: Configure a bank account, upload a statement file (CSV), and see every transaction parsed
(date, amount, dr/cr, narration, reference) without duplicates on re-import.

**Independent Test**: With an account configured, upload a CSV; every line appears with correct fields
and the import summary count matches the file; re-uploading an overlapping file adds zero duplicates.

### Tests for User Story 1 ⚠️

- [ ] T016 [P] [US1] CSV parser golden test (column-mapping profile → canonical `ParsedLine`s; date formats, signed vs dr/cr columns, ₹ formatting) in `ampairs/banking/src/test/kotlin/com/ampairs/banking/parser/CsvStatementParserTest.kt`
- [ ] T017 [P] [US1] Import idempotency test (re-import overlapping range → no duplicate lines; summary reports inserted/skipped) in `ampairs/banking/src/test/kotlin/com/ampairs/banking/service/ImportServiceIdempotencyTest.kt`
- [ ] T018 [P] [US1] Running-balance continuity test (gap/truncation flagged) in `ampairs/banking/src/test/kotlin/com/ampairs/banking/service/RunningBalanceContinuityTest.kt`

### Implementation for User Story 1 (Backend)

- [ ] T019 [P] [US1] `BankAccount` entity (extends `OwnableBaseDomain`: bank name, masked account no, IFSC, accountType, openingBalance paise, currency) in `ampairs/banking/src/main/kotlin/com/ampairs/banking/domain/model/BankAccount.kt` (FR-001)
- [ ] T020 [P] [US1] `BankStatementLine` entity (immutable; fingerprint uid, bankAccountUid, valueDate/txnDate `Instant`, amountMinor, drCr, narration, refNo/utr/chequeNo, runningBalance, sourceImportUid, lineMatchStatus) in `…/domain/model/BankStatementLine.kt` (FR-008; research R2)
- [ ] T021 [P] [US1] `StatementImport` entity (file name, account, format, period, inserted/skipped counts, ImportStatus) in `…/domain/model/StatementImport.kt`
- [ ] T022 [P] [US1] Request/Response DTOs + converters for account/line/import, plus `ParsedLine` and `ColumnMapping` profile DTOs in `…/domain/dto/` (Constitution II/III)
- [ ] T023 [US1] Spring Data repositories (`BankAccountRepository`, `BankStatementLineRepository` with fingerprint upsert + pull-feed `@Query`, `StatementImportRepository`) in `…/repository/` (depends on T019–T021)
- [ ] T024 [P] [US1] `StatementParser` port + `ParsedLine` canonical output in `…/parser/StatementParser.kt` (research R3)
- [ ] T025 [US1] `CsvStatementParser` driven by a `ColumnMapping` profile in `…/parser/CsvStatementParser.kt` (depends on T024)
- [ ] T026 [US1] `ImportService`: parse → deterministic fingerprint `BSL_<hash>` → upsert dedupe → running-balance continuity check → write `StatementImport` summary in `…/service/ImportService.kt` (FR-003/005/006/007; research R4)
- [ ] T027 [US1] `StatementImportController` multipart `POST /banking/v1/imports` (parse+import; per-line failures collected into the import report, not thrown) in `…/controller/StatementImportController.kt` (Constitution VI; plan Complexity Tracking)
- [ ] T028 [US1] Account sync endpoints `GET/POST /banking/v1/accounts/sync` and pull-only `GET /banking/v1/statement-lines/sync`, `/imports/sync` in `…/controller/BankingController.kt` (CLAUDE rule 9 canonical `/sync`)

### Implementation for User Story 1 (Mobile)

- [ ] T029 [P] [US1] Room entities + DAOs — `BankAccount` (synced), `BankStatementLine` & `StatementImport` (pull-only) — in `ampairs-app/feature/banking/src/commonMain/kotlin/com/ampairs/banking/data/db/`
- [ ] T030 [US1] `BankingRepository` (account config local-only: write `synced=false` + `syncStateDao.markPendingPush(BANK_ACCOUNT)`; never injects the Api for the config write path) in `…/data/repository/BankingRepository.kt` (offline-sync Rule 7)
- [ ] T031 [US1] `BankAccountSyncDelegate` (read-write config push/pull) + `BankStatementLineSyncDelegate` & `StatementImportSyncDelegate` (pull-only) in `…/sync/`, `@ContributesIntoMap(WorkspaceScope::class)` + `@SyncEntityKey`
- [ ] T032 [US1] Multipart statement upload in `BankingApiImpl` (FileKit pick → `postMultiPart(..., requestTimeoutMillis = 120_000L)`) in `…/data/api/BankingApiImpl.kt` (offline-sync Rule 3)
- [ ] T033 [P] [US1] Account setup screen + `BankAccountListViewModel`/`BankAccountFormViewModel` (`@ContributesIntoMap(WorkspaceScope::class)`, UID generated in VM) in `…/ui/account/`
- [ ] T034 [US1] Statement upload screen + `StatementUploadViewModel` (online upload action, import-in-progress state) in `…/ui/import/`

**Checkpoint**: US1 fully functional — accounts configurable offline; CSV uploads parse idempotently and lines/imports sync to the device.

---

## Phase 4: User Story 2 — Auto-match transactions & review the rest (Priority: P1)

**Goal**: After import, the server matches lines to recorded receipts/payments (exact ref/UTR → amount+date
→ fuzzy), auto-confirms unambiguous high-confidence matches, and surfaces the rest as suggestions the
owner confirms/rejects/manually links. Confirming annotates the voucher bank-reconciled (never posts).

**Independent Test**: With recorded receipts and an imported statement, run matching; exact-ref lines
auto-match (flagged EXACT), ambiguous lines appear as suggestions, and confirm marks the voucher
bank-reconciled while reject returns the line to unmatched.

### Tests for User Story 2 ⚠️

- [ ] T035 [P] [US2] Matching-engine test: Pass 1 exact ref/UTR, Pass 2 amount+date window, Pass 3 fuzzy; confidence assignment in `ampairs/banking/src/test/kotlin/com/ampairs/banking/service/MatchingEngineTest.kt` (FR-011)
- [ ] T036 [P] [US2] Precision test: ambiguous (multi-candidate) lines are NEVER auto-confirmed (SC-003) in `…/service/MatchingPrecisionTest.kt`
- [ ] T037 [P] [US2] Confirm-annotates-voucher test (confirm marks voucher bank-reconciled, no ledger entry created) in `…/service/MatchConfirmationTest.kt` (FR-016)
- [ ] T038 [P] [US2] N:M match test (one credit ↔ several receipts; several lines ↔ one voucher) in `…/service/MatchNToMTest.kt` (FR-014)

### Implementation for User Story 2 (Backend)

- [ ] T039 [P] [US2] `BankMatch` entity + join tables (statementLineUids, voucherUids, amountMinor, Confidence, MatchType, MatchStatus, matchedBy, matchedAt) + `@NamedEntityGraph` to lines/vouchers in `ampairs/banking/src/main/kotlin/com/ampairs/banking/domain/model/BankMatch.kt` (research R6)
- [ ] T040 [P] [US2] `BankMatch` DTOs + converters in `…/domain/dto/`
- [ ] T041 [US2] `BankMatchRepository` (status queries, candidate joins, pull feed) in `…/repository/BankMatchRepository.kt`
- [ ] T042 [US2] Public interface on `payment` to annotate a `PaymentVoucher` bank-reconciled (record statement-line link + UTR) + its implementation in `ampairs/payment/.../service/` (research R1/R6; Constitution IX — cross-module via public service)
- [ ] T043 [US2] Candidate search via `payment`'s public services (receipts/payments by date/amount/ref/UTR), workspace-scoped, indexed in `…/service/CandidateSearchService.kt`
- [ ] T044 [US2] `MatchingEngine` multi-pass: Pass 1 exact ref/UTR → EXACT; Pass 2 amount+direction+date-window → HIGH (ambiguity lowers); Pass 3 fuzzy amount-tolerance + narration↔party token overlap → MEDIUM; single high-confidence candidate auto-matches else SUGGESTED in `…/service/MatchingEngine.kt` (FR-010/011/012/015; research R5)
- [ ] T045 [US2] Async run-match orchestration: `POST /banking/v1/accounts/{uid}/run-match` schedules matching asynchronously, sets `StatementImport`→MATCHING then COMPLETE, emits a backend sync event for `BANK_MATCH`/`BANK_STATEMENT_LINE` on completion in `…/service/ReconciliationOrchestrator.kt` (FR-024/025)
- [ ] T046 [US2] Match action + sync endpoints: `POST /banking/v1/matches/{uid}/confirm` (annotates voucher), `/reject`, `POST /matches` (manual), `POST /matches/{uid}/undo`, pull-only `GET /banking/v1/matches/sync` in `…/controller/BankingController.kt` (FR-013/016/017)

### Implementation for User Story 2 (Mobile)

- [ ] T047 [P] [US2] `BankMatch` Room entity (pull-only) + DAO in `ampairs-app/feature/banking/src/commonMain/kotlin/com/ampairs/banking/data/db/`
- [ ] T048 [US2] `BankMatchSyncDelegate` (pull-only) + confirm/reject/manual/undo online actions in `BankingApiImpl` + repository in `…/sync/` and `…/data/repository/`
- [ ] T049 [US2] Match-review list screen + `MatchReviewViewModel` (suggestions with confidence chips; confirm/reject/manual online actions; import-in-progress indicator driven by `StatementImport` status via sync) in `…/ui/match/` (FR-025)
- [ ] T050 [US2] Surface a confirmed match's bank-reconciled flag in `feature/payment` voucher display (read the synced annotation) in `ampairs-app/feature/payment/.../ui/`

**Checkpoint**: US1+US2 = Phase 1 MVP — CSV import + exact/amount/date(+fuzzy) matching + manual review, end-to-end across server and app.

---

## Phase 5: User Story 3 — Reconciliation status & exceptions per account (Priority: P2)

**Goal**: Per account+period, show matched / bank-only / books-only buckets, let the owner record a
voucher from an orphan bank line, and display the tie-out (opening + net = closing).

**Independent Test**: For an account with mixed matched/bank-only/books-only items, open the view;
buckets and totals are correct, the tie-out balances, and recording from a bank-only line matches it.

### Tests for User Story 3 ⚠️

- [ ] T051 [P] [US3] Tie-out invariant test (`opening + Σmatched + Σunmatched-bank = statement closing`) in `ampairs/banking/src/test/kotlin/com/ampairs/banking/service/TieOutInvariantTest.kt` (FR-020; research R7)
- [ ] T052 [P] [US3] Bucket-classification test (bank-only / books-only / suggested) in `…/service/ReconciliationBucketsTest.kt`

### Implementation for User Story 3

- [ ] T053 [US3] `ReconciliationReportService` computing the three buckets + tie-out per account+period in `ampairs/banking/src/main/kotlin/com/ampairs/banking/service/ReconciliationReportService.kt` (FR-018)
- [ ] T054 [US3] Report DTOs in `…/domain/dto/` and endpoint `GET /banking/v1/accounts/{uid}/reconciliation?period=…` in `…/controller/BankingController.kt`
- [ ] T055 [US3] Spawn-voucher-from-orphan: `POST /banking/v1/statement-lines/{uid}/create-voucher` → `payment` receipt/`AdjustmentVoucher` via public service, then auto-match the line in `…/service/OrphanVoucherService.kt` (FR-019; research R7)
- [ ] T056 [P] [US3] Reconciliation report screen + `ReconciliationReportViewModel` (pull-only buckets + tie-out display) in `ampairs-app/feature/banking/src/commonMain/kotlin/com/ampairs/banking/ui/reconciliation/`
- [ ] T057 [US3] "Record from this line" action wired to the spawn-voucher endpoint in `…/ui/reconciliation/`

**Checkpoint**: US3 done — exceptions and tie-out visible; orphan lines convertible to vouchers.

---

## Phase 6: User Story 4 — Reconcile bounced / returned payments (Priority: P2)

**Goal**: Detect a statement debit reversing an earlier receipt (cheque/NACH return), route it to
`payment`'s existing bounce flow (which posts the reversal), and record the return charge as a bank charge.

**Independent Test**: With a recorded receipt and an imported reversing debit, reconcile as a return;
the original receipt is marked bounced, the party's outstanding is restored, and any return fee is a
bank charge — not a customer payment.

### Tests for User Story 4 ⚠️

- [ ] T058 [P] [US4] Bounce-routing test (detected return → `payment` bounce command → receipt BOUNCED + outstanding restored) in `ampairs/banking/src/test/kotlin/com/ampairs/banking/service/BounceReconciliationTest.kt` (FR-021; research R8)
- [ ] T059 [P] [US4] Return-charge-as-bank-charge test in `…/service/ReturnChargeTest.kt` (FR-022)

### Implementation for User Story 4

- [ ] T060 [US4] `BounceDetector` (narration-pattern detection from `bounce_narration_patterns` setting; match reversing debit to original receipt's voucher) in `ampairs/banking/src/main/kotlin/com/ampairs/banking/service/BounceDetector.kt`
- [ ] T061 [US4] Route a confirmed return to `payment`'s existing `POST /vouchers/{uid}/bounce`; reconcile the return-charge line as a bank charge in `…/service/ReconciliationOrchestrator.kt` (research R8 — `banking` never reverses itself)
- [ ] T062 [US4] Surface "reconcile as return/bounce" action in the match-review UI; bounced result appears via `feature/payment` sync in `ampairs-app/feature/banking/src/commonMain/kotlin/com/ampairs/banking/ui/match/`

**Checkpoint**: US4 done — returns restore party dues through the single ledger bounce path.

---

## Phase 7: User Story 5 — Multiple accounts & statement formats (Priority: P3)

**Goal**: Reconcile several accounts independently; import MT940 and account-aggregator formats yielding
the same canonical transactions; reuse saved CSV column profiles; support N:M settlement matching.

**Independent Test**: Configure two accounts and confirm transactions/matches/reconciliation never mix;
import an MT940 file and confirm identical canonical lines to CSV; re-import a known bank's CSV and
confirm the saved column profile is reused.

### Tests for User Story 5 ⚠️

- [ ] T063 [P] [US5] MT940 parser golden test (`:61:`/`:86:` tags → canonical lines) in `ampairs/banking/src/test/kotlin/com/ampairs/banking/parser/Mt940ParserTest.kt`
- [ ] T064 [P] [US5] Account-aggregator parser test (RBI AA JSON → canonical lines) in `…/parser/AccountAggregatorParserTest.kt`
- [ ] T065 [P] [US5] Column-mapping profile reuse test (same bank → saved profile applied) in `…/service/ColumnMappingReuseTest.kt`
- [ ] T066 [P] [US5] N:M settlement test (one UTR-keyed payout ↔ many receipts; ties to feature-016 collections) in `…/service/SettlementMatchingTest.kt` (research R5/R9)
- [ ] T067 [P] [US5] Account-isolation test (lines/matches scoped per `bankAccountUid`, never cross-account/workspace) in `…/service/AccountIsolationTest.kt` (FR-002)

### Implementation for User Story 5

- [ ] T068 [P] [US5] `Mt940Parser` (SWIFT `:61:`/`:86:`) in `ampairs/banking/src/main/kotlin/com/ampairs/banking/parser/Mt940Parser.kt` (research R3)
- [ ] T069 [P] [US5] `AccountAggregatorParser` (RBI AA JSON); consent tokens env-provided + encrypted, masked account numbers in `…/parser/AccountAggregatorParser.kt` (Constitution XI; research R3)
- [ ] T070 [US5] Persist + reuse `ColumnMapping` profiles per bank/workspace (CRUD + default selection) in `…/service/ColumnMappingService.kt` + `…/controller/BankingController.kt` (FR-009)
- [ ] T071 [US5] Extend `MatchingEngine` with N:M settlement matching (one bank credit ↔ many receipts via UTR from feature 016) in `…/service/MatchingEngine.kt` (research R5)
- [ ] T072 [US5] Wire format selection into `StatementImportController` (CSV/MT940/AA via `StatementFormat`) in `…/controller/StatementImportController.kt`
- [ ] T073 [P] [US5] Multi-account management UI + format picker on upload in `ampairs-app/feature/banking/src/commonMain/kotlin/com/ampairs/banking/ui/account/` and `…/ui/import/`

**Checkpoint**: All user stories independently functional.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T074 [P] Author `specs/024-bank-reconciliation/data-model.md` (entities + state machines) and `contracts/` (banking-sync, banking-import, banking-actions) to match the implementation
- [ ] T075 [P] Author `specs/024-bank-reconciliation/quickstart.md` (import a CSV → run matching → confirm a match → view tie-out)
- [ ] T076 Verify backend coverage ≥80% on parsers + matching + idempotency via `./gradlew :banking:test` and close gaps
- [ ] T077 [P] Mobile validation: `./gradlew :feature:banking:check` + 3-target compile (`androidApp:compileDebugKotlinAndroid`, `shared:compileKotlinIosSimulatorArm64`, `desktopApp:compileKotlin`)
- [ ] T078 Performance check: import + match a few-thousand-line statement within seconds; verify candidate-search indexes (SC-001)
- [ ] T079 [P] Add navigation entry to `feature/banking` from finance/settings (`ModuleRegistry`/entry provider) in `ampairs-app/feature/workspace/.../ModuleRegistry.kt` and `ampairs-app/shared/.../navigation/`
- [ ] T080 Run `./gradlew :ampairs_service:flywayInfo` + `:banking:test` (Testcontainers) and `ciBuild` gate before marking complete

---

## Dependencies & Execution Order

### Phase Dependencies
- **Setup (P1)**: no dependencies.
- **Foundational (P2)**: depends on Setup — BLOCKS all stories (migration T008/T009, enums T010, settings T011, mobile DB/DI T014 are hard prerequisites).
- **US1 (P3)**: depends on Foundational. The MVP.
- **US2 (P4)**: depends on Foundational + US1 (matches reference imported lines and accounts).
- **US3 (P5)**: depends on US1+US2 (buckets/tie-out need lines + matches).
- **US4 (P6)**: depends on US2 (reconciles a confirmed/identified line to a voucher) + US1 (lines).
- **US5 (P7)**: depends on US1 (parser port, import path) + US2 (engine, for N:M); MT940/AA/profile reuse otherwise independent.
- **Polish (P8)**: after the desired stories.

### Within Each Story
- Tests first (write failing) → entities (`[P]`) → repositories → services/parsers → controllers → mobile DB/sync → mobile UI.

### Parallel Opportunities
- Setup: T004/T005/T006 in parallel with T001–T003 (different repos).
- Foundational: T009/T010/T013 `[P]`; T008 before both migrations' dependents.
- US1 entities T019/T020/T021/T022/T024 `[P]`; US2 T039/T040 and tests T035–T038 `[P]`.
- Different stories can proceed in parallel once Foundational is done (US1 must lead since US2–US5 build on it).
- Backend (`ampairs/`) and mobile (`ampairs-app/`) tracks within a story can run by different developers.

---

## Parallel Example: User Story 1

```bash
# Tests (write first, ensure they fail):
Task: "CSV parser golden test in banking/src/test/.../CsvStatementParserTest.kt"   # T016
Task: "Import idempotency test in banking/src/test/.../ImportServiceIdempotencyTest.kt"  # T017
Task: "Running-balance continuity test in banking/src/test/.../RunningBalanceContinuityTest.kt"  # T018

# Entities together:
Task: "BankAccount entity"        # T019
Task: "BankStatementLine entity"  # T020
Task: "StatementImport entity"    # T021
Task: "DTOs + ParsedLine + ColumnMapping"  # T022
```

---

## Implementation Strategy

### MVP First (Phase 1 of plan.md = US1 + US2)
1. Phase 1 Setup → Phase 2 Foundational (CRITICAL — blocks all).
2. Phase 3 US1 (CSV import) → **validate independently** (upload, parse, idempotent re-import).
3. Phase 4 US2 (exact/amount/date matching + review) → **validate** → this is the demoable MVP per plan.md Phase 1.

### Incremental Delivery (maps to plan.md phasing)
- **Plan Phase 1**: US1 + US2 (CSV + amount/date matching + manual review).
- **Plan Phase 2**: US3 (exception report + tie-out) + US4 (bounce) + fuzzy pass (in T044).
- **Plan Phase 3**: US5 (MT940, account-aggregator, N:M settlement, multi-account dashboards).

### Notes
- `[P]` = different files, no dependency. `[Story]` = traceability to US1–US5.
- Backend builds/tests locally (system JDK 21): `./gradlew :banking:test`. Mobile build needs CI (JetBrains-vendor toolchain blocked in sandbox) — rely on PR CI / coverage bot.
- Commit after each task or logical group; stop at any checkpoint to validate a story independently.
- Watch the ledger invariant: matching only **annotates** vouchers; only US4 touches the ledger, and only via `payment`'s existing bounce command.
