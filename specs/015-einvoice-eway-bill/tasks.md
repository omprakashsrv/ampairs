---
description: "Task list for GST E-Invoicing (IRN) & E-Way Bill"
---

# Tasks: GST E-Invoicing (IRN) & E-Way Bill

**Input**: Design documents from `/specs/015-einvoice-eway-bill/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: INCLUDED — the constitution mandates Testing & Quality Gates (backend critical logic ≥80%,
endpoints ≥90%) and the plan calls for INV-01 golden / idempotency / cancel-window / retry-window /
authz tests. Test tasks are written before the implementation they cover.

**Two repos, one branch** (`claude/einvoice-eway-bill-specs-9s8ugx`):
- **Backend** tasks live in `ampairs/` (this repo), paths under `einvoice/src/...`.
- **Mobile** tasks are tagged **(app)** and live in the sibling repo `ampairs-app/`, paths under
  `feature/einvoice/src/...`. Commit/push each repo on the same branch name.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: can run in parallel (different files, no dependency)
- **[Story]**: US1 / US2 / US3 / US4 (maps to spec user stories)

---

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Create backend Gradle module `einvoice/build.gradle.kts` (Spring Boot, Kotlin; depends on `core`, `event`, and the public service interfaces of `invoice`, `setting`, `workspace`); add `include("einvoice")` to `settings.gradle.kts`.
- [ ] T002 Wire the aggregator: in `ampairs_service/build.gradle.kts` add `implementation(project(":einvoice"))` and add `"einvoice"` to the `migrationModules` list.
- [ ] T003 [P] Create package skeleton under `einvoice/src/main/kotlin/com/ampairs/einvoice/` (`domain/model`, `domain/enums`, `domain/dto`, `repository`, `service`, `provider`, `controller`, `event`, `config`) and `config/Constants.kt` (UID prefixes `EIN`/`EWB`, base path `/einvoice/v1`).

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: must complete before ANY user story.

- [ ] T004 Flyway migration creating `e_invoice_document`, `e_invoice_job`, `e_invoice_credential` tables with unique constraints `(owner_id, invoice_uid)` and unique `irn` (where non-null), and the pending-job poll index — written in **BOTH** `einvoice/src/main/resources/db/migration/postgresql/` and `.../mysql/`. Run `./gradlew :ampairs_service:flywayInfo` first to pick the next free global version (current max `V1.0.112` → likely `V1.0.113`). `TIMESTAMPTZ`/`TIMESTAMP` for all time columns.
- [ ] T005 [P] Enums `IrnStatus`, `JobStatus`, `JobType`, `CancelReason` in `einvoice/.../domain/enums/`.
- [ ] T006 [P] `EInvoiceCredential` entity (`OwnableBaseDomain`) + repository + AES credential encryption util (KMS/env-provided key) in `einvoice/.../config/` — secrets never in source (Principle XI).
- [ ] T007 [P] Typed exceptions `EInvoiceException` / `GspException` + module `@RestControllerAdvice` returning `ApiResponse` errors (Principle VI).
- [ ] T008 `EInvoiceProvider` port interface (`authenticate`, `generateIrn`, `cancelIrn`, `getIrnByDoc`, `generateEwayBill`, `cancelEwayBill`) + NIC INV-01 / EWB payload model shells using `@JsonProperty` PascalCase (Principle III documented exception) in `einvoice/.../domain/dto/` and `einvoice/.../provider/`.
- [ ] T009 `NicSessionTokenCache` (per-GSTIN token + expiry, refresh-on-demand) and `EInvoiceProviderResolver` (per-workspace provider selection) in `einvoice/.../provider/` (depends on T006, T008).
- [ ] T010 `NicDirectProvider` against the NIC **sandbox**: implement `authenticate`, `generateIrn`, `getIrnByDoc` (cancel + EWB methods are added in their stories) in `einvoice/.../provider/` (depends on T008, T009).

**Checkpoint**: provider/persistence/credential foundation ready — user stories can begin.

---

## Phase 3: User Story 1 — Automatic e-invoice (IRN) registration on finalize (Priority: P1) 🎯 MVP

**Goal**: Finalizing an eligible B2B invoice auto-registers it with the IRP and stamps IRN + signed QR
+ ack on the invoice; status surfaces as Pending/Generated/Failed; QR prints offline.

**Independent Test**: Enable e-invoicing for a workspace, finalize an eligible B2B invoice, and verify
IRN/QR/ack appear on the invoice + print, with automatic retry on outage and idempotent re-trigger.

### Tests for User Story 1

- [ ] T011 [P] [US1] INV-01 builder golden test against the NIC sandbox schema (IGST vs CGST+SGST by place-of-supply; `TotInvVal` ±1 tolerance; round-off line) in `einvoice/src/test/.../Inv01PayloadBuilderTest.kt`.
- [ ] T012 [P] [US1] Idempotency test: `getIrnByDoc` pre-check + NIC 3029 duplicate → success storing existing IRN; no second document row, in `einvoice/src/test/.../EInvoiceIdempotencyTest.kt`.
- [ ] T013 [P] [US1] Retry-window test: transient failures retry then `windowExpiresAt` passes → `FAILED`; validation rejection → `FAILED` immediately (no retry), in `einvoice/src/test/.../EInvoiceQueueWorkerTest.kt`.
- [ ] T014 [P] [US1] Authz test: `POST /documents/{uid}/generate` requires admin/owner (403 otherwise) in `einvoice/src/test/.../EInvoiceControllerAuthTest.kt`.

### Implementation for User Story 1

- [ ] T015 [P] [US1] `EInvoiceDocument` entity (`OwnableBaseDomain`, `@NamedEntityGraph` for invoice+eway) + repository (`@EntityGraph` detail, `/sync` feed query, failed-list query) in `einvoice/.../domain/model/` + `einvoice/.../repository/`.
- [ ] T016 [P] [US1] `EInvoiceJob` entity + repository with the pending-job poll query (`PENDING` OR (`FAILED` AND `now < windowExpiresAt`) AND `nextAttemptAt <= now`) in `einvoice/.../domain/model/` + `repository/`.
- [ ] T017 [US1] `Inv01PayloadBuilder` mapping `Invoice`/`InvoiceItem` → INV-01 (place-of-supply IGST vs CGST+SGST; `BigDecimal` scale-2 once; `RndOffAmt`; HSN/GSTIN/PIN/total validation) in `einvoice/.../service/` (depends on T008, T015).
- [ ] T018 [US1] Request/Response DTOs + converters: list-safe `EInvoiceDocumentResponse` (no signed/audit blobs) and `EInvoiceDocumentDetailResponse` (with `signed_invoice`/audit) in `einvoice/.../domain/dto/`.
- [ ] T019 [US1] `EInvoiceService`: upsert `PENDING`, generate via provider with get-by-doc pre-check, persist IRN/QR/ack → `GENERATED`, classify transient vs permanent failure, manual generate/retry entry point (depends on T010, T015, T016, T017).
- [ ] T020 [US1] `EInvoiceSettingDefinitions` declaring `einvoice_enabled`, `einvoice_provider`, `einvoice_retry_window_hours` (default 48) via the `setting` module.
- [ ] T021 [US1] Invoice event listener `@TransactionalEventListener` on `InvoiceFinalizedEvent` → if `einvoice_enabled`, upsert `EInvoiceDocument(PENDING)` + enqueue `GENERATE_IRN` job (depends on T019, T020).
- [ ] T022 [US1] `EInvoiceQueueWorker` (`@Scheduled`): poll due jobs, generate with exponential backoff until `windowExpiresAt` → `FAILED`; serialize per `invoiceUid` (depends on T019).
- [ ] T023 [US1] `EInvoiceController`: `POST /documents/{invoiceUid}/generate` (admin/owner), `GET /documents/sync` (pull, `PageResponse`), `GET /documents/{invoiceUid}` (detail, signed/audit), `GET /documents/failed` (paginated). Tenant context at controller boundary (depends on T019).
- [ ] T024 [P] [US1] **(app)** Add `SyncEntity.EINVOICE_DOC` and `ApiUrlBuilder.einvoiceUrl("v1/...")` in `ampairs-app/`.
- [ ] T025 [P] [US1] **(app)** Room `EInvoiceDocument` entity + DAO + workspace-scoped DB `einvoice` + platform DI modules (`androidMain`/`iosMain`/`desktopMain`, `@SingleIn(WorkspaceScope::class)`, registered with `WorkspaceClosableRegistry`) in `ampairs-app/feature/einvoice/src/`.
- [ ] T026 [US1] **(app)** `EInvoiceApi`(+Impl) + `EInvoiceRepository` (local-only pull-mirror; no `markPendingPush`) (depends on T024, T025).
- [ ] T027 [US1] **(app)** `EInvoiceDocumentSyncDelegate` — pull-only, `@ContributesIntoMap(WorkspaceScope::class)` + `@SyncEntityKey(SyncEntity.EINVOICE_DOC)` (depends on T026).
- [ ] T028 [P] [US1] **(app)** QR-render helper (qrose) in `commonMain` rendering `signed_qr_code` → bitmap offline.
- [ ] T029 [US1] **(app)** Invoice-detail IRN/ack/QR overlay + status chip (Pending/Generated/Failed) + "Generate now" online command + ViewModel (`metroViewModel`) (depends on T027, T028).
- [ ] T030 [US1] **(app)** Embed QR + IRN in the invoice PDF/print template (offline) (depends on T028).
- [ ] T031 [P] [US1] **(app)** `strings.xml` resources for the e-invoice UI in `feature/einvoice/src/commonMain/composeResources/values/`.

**Checkpoint**: US1 fully functional and independently testable — **MVP**.

---

## Phase 4: User Story 2 — Generate an E-Way Bill (Priority: P2)

**Goal**: Capture transporter/vehicle/distance for an invoice (above threshold), generate the EWB off
the IRN, show number + validity on the invoice and print; support standalone-later and Part-B update.

**Independent Test**: For an invoice above the threshold, enter transport details, generate the EWB, and
confirm number + validity appear on the invoice and print; update vehicle without a new EWB number.

### Tests for User Story 2

- [ ] T032 [P] [US2] EWB tests: generate-off-IRN, 24h cancel-window guard, update-vehicle keeps `ewb_no`, admin/owner authz, in `einvoice/src/test/.../EwayBillServiceTest.kt`.

### Implementation for User Story 2

- [ ] T033 [US2] Flyway migration (next free version, BOTH vendors) creating `e_way_bill` table in `einvoice/src/main/resources/db/migration/{postgresql,mysql}/`.
- [ ] T034 [P] [US2] Enums `EwbStatus`, `TransMode`, `VehicleType` in `einvoice/.../domain/enums/`.
- [ ] T035 [P] [US2] `EwayBill` entity + repository (`/sync` feed query) in `einvoice/.../domain/model/` + `repository/`.
- [ ] T036 [US2] EWB request/response DTOs + NIC EWB payload model + `EwbPayloadBuilder` in `einvoice/.../domain/dto/` + `service/` (depends on T008).
- [ ] T037 [US2] `EwayBillService`: generate (off IRN when present), cancel (24h), update-vehicle (Part-B, no new number), extend-validity (depends on T010, T035, T036).
- [ ] T038 [US2] Add `generateEwayBill` / `cancelEwayBill` / update-vehicle / extend to `NicDirectProvider` (depends on T010).
- [ ] T039 [US2] Add `eway_enabled` + `eway_value_threshold` (default 50000) to `EInvoiceSettingDefinitions` (depends on T020).
- [ ] T040 [US2] Controller endpoints: `POST /eway-bills` (admin/owner), `POST /eway-bills/{uid}/cancel`, `.../update-vehicle`, `.../extend-validity`, `GET /eway-bills/sync` (depends on T037).
- [ ] T041 [P] [US2] **(app)** `SyncEntity.EWAY_BILL` + Room `EwayBill` entity/DAO in `ampairs-app/feature/einvoice/src/`.
- [ ] T042 [US2] **(app)** `EwayBillSyncDelegate` (pull-only) (depends on T041).
- [ ] T043 [US2] **(app)** Transporter/vehicle entry sheet → online generate command + EWB chip/validity on invoice detail + ViewModel (depends on T042).
- [ ] T044 [US2] **(app)** Embed EWB number in the invoice print template (depends on T043).

**Checkpoint**: US1 + US2 both work independently.

---

## Phase 5: User Story 3 — Cancel an e-invoice or e-way bill within the window (Priority: P3)

**Goal**: Admin/owner cancels an IRN within 24h of ack (with a reason code) or an EWB within its window;
after the window the system blocks and explains the credit-note remedy; cancelled IRNs can't reprint.

**Independent Test**: Generate an IRN, cancel within 24h with a reason → `CANCELLED` and reprint blocked;
attempt >24h → 409 with credit-note message; non-admin → 403.

### Tests for User Story 3

- [ ] T045 [P] [US3] Cancel-window tests: within 24h → CANCELLED; >24h → 409 (credit-note message); admin/owner authz; cancelled-IRN reprint guard, in `einvoice/src/test/.../EInvoiceCancellationTest.kt`.

### Implementation for User Story 3

- [ ] T046 [US3] Add `cancel` to `EInvoiceService`: 24h guard from `ackDate`, NIC reason code, mark `CANCELLED`, set cancel fields (depends on T019).
- [ ] T047 [US3] Add `cancelIrn` to `NicDirectProvider` (depends on T010).
- [ ] T048 [US3] Invoice event listener on `InvoiceCancelledEvent`: if IRN exists within 24h → enqueue `CANCEL_IRN`; else flag for credit-note follow-up (depends on T021, T046).
- [ ] T049 [US3] Controller `POST /documents/{invoiceUid}/cancel` (admin/owner + reason) and a reprint guard rejecting valid-e-invoice export when `CANCELLED` (depends on T046).
- [ ] T050 [US3] **(app)** Cancel action (online command, admin/owner only) on invoice detail + block valid-e-invoice reprint locally when status is `CANCELLED` (depends on T029).

**Checkpoint**: US1 + US2 + US3 independently functional.

---

## Phase 6: User Story 4 — Configure e-invoicing applicability per business (Priority: P3)

**Goal**: Admin enables/disables e-invoicing + EWB and sets the threshold; the system only registers for
enabled workspaces and skips ineligible (B2C/sub-threshold) invoices without error.

**Independent Test**: Disabled workspace → finalize attempts no registration; enable → subsequent
eligible invoices register; an ineligible B2C/sub-threshold invoice is skipped silently.

### Tests for User Story 4

- [ ] T051 [P] [US4] Gating tests: disabled → no enqueue; enabled → enqueue; ineligible B2C/sub-threshold → skip (no error), in `einvoice/src/test/.../EInvoiceApplicabilityTest.kt`.

### Implementation for User Story 4

- [ ] T052 [US4] `EInvoiceApplicabilityService`: evaluate per-invoice eligibility (B2B vs B2C/sub-threshold skip — FR-003) reading the settings (depends on T020).
- [ ] T053 [US4] Replace the simple `einvoice_enabled` flag check in the finalize listener with the full `EInvoiceApplicabilityService` gate (depends on T021, T052).
- [ ] T054 [US4] Confirm all settings (`einvoice_enabled`/`einvoice_provider`/`einvoice_retry_window_hours`, `eway_enabled`/`eway_value_threshold`) are admin-configurable with documented defaults via the `setting` registry (depends on T020, T039).

**Checkpoint**: all four stories independently functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T055 [P] Module docs `docs/modules/einvoice.md`; note the intentional pull-only `/sync` deviation in `docs/guides/offline-sync-contract.md`.
- [ ] T056 [P] Fill backend coverage to constitution gates (≥80% on builder/idempotency/cancel-window/retry-window/authz; endpoints ≥90%).
- [ ] T057 [P] **(app)** 3-target compile gates (`shared:compileKotlinIosSimulatorArm64`, `androidApp:compileDebugKotlinAndroid`, `desktopApp:compileKotlin`) + QR-render snapshot test + `:feature:einvoice:check`.
- [ ] T058 Reconciliation/observability: stuck-queue logging that feeds the failed-document view; (plan Phase 3) daily get-by-doc heal for lost-ack `PENDING`/`FAILED`.
- [ ] T059 Run `quickstart.md` end-to-end against the NIC sandbox (finalize→IRN, idempotency, bounded retry, validation fail, cancel, EWB, offline QR/print).

---

## Dependencies & Execution Order

### Phase order
- **Setup (P1)** → **Foundational (P2)** → **US1 (P3)** → **US2 (P4)** → **US3 (P5)** → **US4 (P6)** → **Polish (P7)**.
- Foundational BLOCKS all stories. After it, stories can proceed in parallel if staffed (US2/US3/US4 each only depend on Foundational + a few US1 anchors noted below).

### Cross-story dependencies (kept minimal)
- **US2** reuses the provider (T010) and `EInvoiceDocument`/IRN (T015/T019) to generate off the IRN, and `EInvoiceSettingDefinitions` (T020). It is otherwise self-contained (own table/entity/delegate/UI).
- **US3** extends `EInvoiceService` (T019/T046) and the finalize listener (T021/T048); EWB cancel ships in US2 (T037/T040).
- **US4** extends the settings (T020) and the finalize listener (T021). The simple flag gate in US1 (T021) is replaced by US4's full eligibility (T053) — US1 remains testable on the simple flag alone.

### Within a story: tests → models → services → endpoints → mobile delegate → UI/print.

---

## Parallel Execution Examples

```bash
# Foundational — independent files:
T005 (enums)  ‖  T006 (credential+crypto)  ‖  T007 (exceptions)

# US1 tests (all [P], before impl):
T011 (INV-01 golden)  ‖  T012 (idempotency)  ‖  T013 (retry-window)  ‖  T014 (authz)

# US1 backend models in parallel, then service:
T015 (EInvoiceDocument)  ‖  T016 (EInvoiceJob)   →   T017 (builder)  →  T019 (service)

# US1 mobile scaffolding in parallel:
T024 (SyncEntity+UrlBuilder)  ‖  T025 (Room+DI)  ‖  T028 (QR helper)  ‖  T031 (strings)
```

---

## Implementation Strategy

### MVP first (US1 only)
1. Phase 1 Setup → 2. Phase 2 Foundational → 3. Phase 3 US1 → **STOP & validate** IRN end-to-end
   (backend generate + pull + mobile display/print) → demo. This is the compliant-billing core.

### Incremental delivery
US1 (MVP) → US2 (e-way bill) → US3 (cancellation) → US4 (applicability config). Each ships and is
testable without breaking the previous.

### Notes
- Backend builds/tests locally with system JDK 21 (`./gradlew :einvoice:compileKotlin :einvoice:test`).
  The **app** KMP build can't run in the sandbox — rely on CI (the coverage bot posts only after
  compile+tests pass).
- Commit after each task or logical group; push backend on `ampairs`, mobile on `ampairs-app`, both on
  branch `claude/einvoice-eway-bill-specs-9s8ugx`.
- No client-authored push for these entities — sync is pull-only by design.
