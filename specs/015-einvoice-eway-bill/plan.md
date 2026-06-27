# Implementation Plan: GST E-Invoicing (IRN) & E-Way Bill

**Branch**: `claude/indian-retail-ecosystem-877med` (spec dir `015-einvoice-eway-bill`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/015-einvoice-eway-bill/spec.md`

## Summary

Add GST **e-invoicing** (Invoice Reference Number generation via the NIC IRP through a GSP) and
**e-way bill** generation on top of the existing `invoice` module. When an invoice is finalized
(`InvoiceFinalizedEvent`), the system queues IRN generation, builds the NIC **INV-01** JSON from the
invoice's already-modelled GST fields (CGST/SGST/IGST, place-of-supply), submits it to the IRP via a
pluggable **GSP provider**, and persists the returned **IRN + signed QR + acknowledgement number** in a
sidecar `EInvoiceDocument`. An optional **e-way bill** (transporter, vehicle, distance, validity) is
generated off the IRN. Because IRN/EWB cannot be minted offline, generation is **online-only, queued
and retried** with strict idempotency (NIC get-by-doc pre-check; duplicate-IRN treated as success) and
statutory **24-hour cancellation windows**. The mobile app is **pull-only**: it displays IRN, QR and
EWB on the invoice and embeds them in the PDF/print template; it never authors compliance state.

Technical approach: a new backend bounded context (`einvoice` module) that reads finalized invoices via
`InvoiceService` and reacts to invoice events; a `EInvoiceProvider` port with per-GSP implementations; a
sidecar persistence model + outbound retry queue; and a thin offline-first Compose feature
(`feature/einvoice`) that pulls and renders. Applicability (₹2 Cr AATO from Oct 2025, ₹50k EWB
threshold) is per-workspace `setting` config, not hardcoded. Full design rationale in
[research.md](./research.md).

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), Spring scheduling
(`@Scheduled` retry worker), an HTTP client for GSP calls (WebClient/RestClient), `core`
(`OwnableBaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`); consumes
`InvoiceFinalizedEvent` / `InvoiceCancelledEvent` from `event`/`invoice`; reads `InvoiceService`,
`SettingService`. Mobile — Room KMP, Ktor, Metro DI, Navigation3, a QR-render lib (e.g. qrose) in
`commonMain`, existing `data/sync` (`CentralSyncService`, `SyncDelegate`), `data/common`
(`ApiUrlBuilder`), `feature/invoice` (read-only reference), the existing `printing`/PDF path.
**Storage**: Backend — PostgreSQL/MySQL via Flyway; signed JWS/QR as `TEXT`/`LONGTEXT`; timestamps
`TIMESTAMPTZ`/`TIMESTAMP`; money in payload builders `BigDecimal` scale 2. Mobile — Room
(workspace-scoped DB `einvoice`), display-only strings/longs.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :einvoice:test`) incl. INV-01 builder golden
tests against the NIC sandbox schema, idempotency/duplicate-IRN handling, cancellation-window guards.
Mobile — `./gradlew :feature:einvoice:check` + 3-target compile gates; QR-render snapshot.
**Target Platform**: Backend service (Linux); Mobile Android (minSdk 24) / iOS / Desktop (JVM).
**Project Type**: Mobile + API — new backend module + KMP feature module.
**Performance Goals**: Finalize stays instant (IRN is async); IRN generation completes < 10 s when GSP
healthy; retry worker drains the queue with exponential backoff; QR renders offline with no lag.
**Constraints**: IRN/EWB are **online-only** (queue + retry); one IRN per invoice (idempotent);
statutory 24h cancel windows enforced; GSP credentials never leave the server; signed payloads
access-controlled; workspace data isolation.
**Scale/Scope**: Per workspace: thousands of e-invoices/month. ~4 backend entities (`EInvoiceDocument`,
`EwayBill`, `EInvoiceJob`/queue, `EInvoiceCredential`), ~2 pull-only sync entities + ~3 action
endpoints, ~2 mobile screens/overlays + PDF embed.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | All timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP` (ackDate, validUpto, cancelledAt); payload money `BigDecimal` scale 2 — never floating point in the IRP payload. |
| II. DTO & Contract Isolation | ✅ PASS | Request/Response DTOs in `einvoice/domain/dto/`; entities never exposed; signed JWS/QR omitted from list DTOs, exposed only on detail. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Internal API uses global Jackson SNAKE_CASE. The **INV-01 / EWB** payloads use NIC's PascalCase schema via an isolated `Inv01PayloadBuilder` with explicit `@JsonProperty` (genuinely non-standard external contract — documented inline). |
| IV. Multi-Tenant Isolation | ✅ PASS | All entities extend `OwnableBaseDomain` (`@TenantId ownerId`); tenant set by `SessionUserFilter` via `X-Workspace-ID`; provider/creds resolved per workspace; services never mutate tenant context. |
| V. API Response Standardization | ✅ PASS | All endpoints return `ApiResponse<T>`; sync pull returns `ApiResponse<PageResponse<T>>`. |
| VI. Centralized Exception Handling | ✅ PASS | Typed `EInvoiceException`/`GspException` bubble to a module exception handler; no business try/catch in controllers (provider retry logic lives in the worker/service, not the controller). |
| VII. Efficient Data Loading | ✅ PASS | `@NamedEntityGraph` for invoice+e-invoice+e-way joins; derived queries; `@Query` only for the sync feed and the pending-job poll. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web UI deferred; tracked follow-up. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `einvoice` bounded context; reads `invoice`/`setting` via public service interfaces + domain events, never repositories. |
| X. Compose Multiplatform Parity | ✅ PASS | Shared logic/UI in `feature/einvoice/src/commonMain`; thin platform DI; QR render in `commonMain`. |
| XI. Security & Secrets Hygiene | ✅ PASS | GSP/NIC credentials env-provided + encrypted per-workspace row; cached NIC session token; signed-blob ACL; no secrets in source/`keys/`. |
| Flyway | ✅ PASS | Versioned migration in **both** `mysql/` and `postgresql/`; `einvoice` added to `migrationModules`; next version after `V1.0.104` (check `flywayInfo`). |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% on payload-builder + idempotency + cancel-window logic; mobile `check` + 3-target compile. |

**Result**: PASS — no violations. Web deferral is a documented scope decision. Complexity Tracking not
required.

## Project Structure

### Documentation (this feature)

```
specs/015-einvoice-eway-bill/
├── plan.md              # This file
├── spec.md              # Feature specification (/speckit.specify output)
├── research.md          # Phase 0 — design decisions + rationale
├── data-model.md        # Phase 1 — entities, states, INV-01 field mapping
├── quickstart.md        # Phase 1 — exercise IRN/EWB against the NIC sandbox
├── contracts/
│   ├── README.md
│   ├── einvoice-sync.md         # pull-only /sync feeds (documents, eway-bills)
│   └── einvoice-actions.md      # generate/cancel IRN, generate/cancel/update EWB
├── checklists/requirements.md
└── tasks.md             # Phase 2 (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
einvoice/
└── src/main/
    ├── kotlin/com/ampairs/einvoice/
    │   ├── domain/
    │   │   ├── model/      # EInvoiceDocument, EwayBill, EInvoiceJob, EInvoiceCredential
    │   │   ├── enums/      # IrnStatus, EwbStatus, TransMode, VehicleType, CancelReason, JobStatus
    │   │   └── dto/        # request/response DTOs + converters; Inv01* + Ewb* payload models
    │   ├── repository/     # Spring Data repos (+ @EntityGraph, sync feed, pending-job poll)
    │   ├── service/        # EInvoiceService, EwayBillService, Inv01PayloadBuilder, EwbPayloadBuilder,
    │   │                   #   EInvoiceQueueWorker (@Scheduled retry), EInvoiceSettingDefinitions
    │   ├── provider/       # EInvoiceProvider port + MasterIndiaProvider/ClearTaxProvider/NicDirectProvider,
    │   │                   #   EInvoiceProviderResolver, NicSessionTokenCache
    │   ├── controller/     # EInvoiceController (sync + actions)
    │   ├── event/          # InvoiceFinalizedEvent/InvoiceCancelledEvent listener → enqueue/cancel
    │   └── config/         # Constants, credential encryption
    └── resources/db/migration/
        ├── mysql/V1.0.105__create_einvoice_tables.sql
        └── postgresql/V1.0.105__create_einvoice_tables.sql
# wiring: settings.gradle.kts (include "einvoice"); ampairs_service/build.gradle.kts
#         (implementation(project(":einvoice")) + "einvoice" in migrationModules)

# Mobile — ampairs-app/ (sibling repo)
feature/einvoice/src/
├── commonMain/kotlin/com/ampairs/einvoice/
│   ├── data/api/          # EInvoiceApi(+Impl), ApiUrlBuilder.einvoiceUrl
│   ├── data/db/           # Room entities (EInvoiceDocument, EwayBill) + DAOs + DB
│   ├── data/repository/   # EInvoiceRepository (local-only, pull-mirror)
│   ├── domain/            # display models, QR render helper, status enums
│   ├── di/                # EInvoiceModule.kt (DAOs)
│   ├── sync/              # EInvoiceDocumentSyncDelegate, EwayBillSyncDelegate (pull-only)
│   └── ui/                # IRN/QR overlay on invoice detail, EWB entry sheet (online command), VMs
├── androidMain/ iosMain/ desktopMain/   # EInvoiceModule.{platform}.kt (@SingleIn(WorkspaceScope::class))
# wiring: SyncEntity additions (EINVOICE_DOC, EWAY_BILL); ApiUrlBuilder.einvoiceUrl;
#         invoice detail screen + PDF/print template embed QR/IRN/EWB
```

**Structure Decision**: Mobile + API. The backend `einvoice/` module mirrors existing bounded contexts
(`invoice`, `payment`); the mobile `feature/einvoice/` mirrors a pull-only feature (like the read-only
side of `feature/payment`). The `invoice` module is untouched beyond the already-published events.

## Phased Implementation

### Phase 1 — MVP: IRN generation + display (single GSP)

- **Entities**: `EInvoiceDocument` (irn, ackNo, ackDate, signedInvoice, signedQrCode, irnStatus,
  gspProvider, request/response audit, cancel fields) 1:1 invoice; `EInvoiceJob` (queue: invoiceUid,
  jobStatus, attemptCount, nextAttemptAt, lastError); `EInvoiceCredential` (encrypted per-workspace GSP
  creds). Flyway `V1.0.105` in both vendors.
- **Provider**: `EInvoiceProvider` port + one implementation (NIC sandbox `NicDirectProvider` or one
  GSP) + `EInvoiceProviderResolver` + `NicSessionTokenCache`.
- **Pipeline**: `InvoiceFinalizedEvent` listener → upsert `EInvoiceDocument(PENDING)` + enqueue job;
  `EInvoiceQueueWorker` (`@Scheduled`) polls `PENDING`/`FAILED`, builds INV-01 via `Inv01PayloadBuilder`,
  calls provider, persists IRN/QR/ack, marks `GENERATED`; idempotent get-by-doc pre-check; 3029 →
  success.
- **Endpoints**: `POST /einvoice/v1/documents/{invoiceUid}/generate` (manual trigger / retry);
  `POST /einvoice/v1/documents/{invoiceUid}/cancel` (24h guard + reason); `GET /einvoice/v1/documents/sync`
  (pull-only). `EInvoiceSettingDefinitions` (`einvoice_enabled`, `einvoice_provider`).
- **Mobile**: `EInvoiceDocumentSyncDelegate` (pull-only); invoice detail shows IRN/ack/QR with
  PENDING/GENERATED/FAILED state; PDF/print template embeds QR + IRN; "Generate now" online command.

### Phase 2 — E-way bill

- **Entity**: `EwayBill` (ewbNo, ewbDate, validUpto, transporterId/name, transMode, vehicleNo,
  transDistance, transDocNo/date, vehicleType, ewbStatus) — Flyway follow-up version.
- **Service/endpoints**: `EwayBillService`; `POST /einvoice/v1/eway-bills` (generate, off IRN when
  present), `POST /einvoice/v1/eway-bills/{uid}/cancel` (24h), `POST .../update-vehicle` (Part-B),
  `POST .../extend-validity`; `GET /einvoice/v1/eway-bills/sync` (pull-only). `eway_enabled`,
  `eway_value_threshold` settings.
- **Mobile**: transporter/vehicle entry sheet → online command; EWB chip + validity on invoice detail;
  EWB no embedded in print.

### Phase 3 — Multi-GSP, resilience & reconciliation

- Second GSP implementation + per-workspace provider switch + fallback on GSP outage.
- Daily reconciliation job: for `PENDING`/`FAILED` older than N hours, re-query NIC get-by-doc to heal
  lost-ack cases; alert on stuck queue depth.
- Credit-note hand-off for late (>24h) cancellations (links to `payment` adjustments / future
  credit-note feature); cancelled-IRN reprint guard.

### Mobile / offline considerations

- IRN/EWB are **server-authored, online-only**; the app pulls and renders. A field user offline sees
  "IRN pending"; the backend generates on reconnect.
- QR is stored as a string and rendered to a bitmap **offline** — printing a compliant invoice never
  needs connectivity once the document has synced.
- No client-authored push for these entities; the only writes are explicit online commands
  (generate/cancel/EWB), never `markPendingPush` sync.

## Complexity Tracking

*No constitution violations. The only deviation from the canonical `/sync` contract is intentional:
e-invoice/e-way entities are **pull-only** (server-authored), so the `POST /sync` push half is omitted —
documented here and in `contracts/`, consistent with how `tax`/`file` sit off the standard push path.*
