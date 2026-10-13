# Implementation Plan: GST Return Filing & Reconciliation (GSTR)

**Branch**: `claude/gstr-filing-specs-rsi2pm` (spec dir `028-gstr-filing`) | **Date**: 2026-06-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/028-gstr-filing/spec.md`

> **Clarifications incorporated (spec Session 2026-06-28):** (1) a finalized invoice dated in an
> already-FILED period still finalizes (keeps its real date) and is reported in the **next open** period's
> GSTR-1 — `PeriodLockService` routes, never blocks (R8); (2) the electronic-**file** action and
> credential setup are restricted to the workspace **owner/admin** role (Principle IV/XI); (3) QRMP
> quarterly filers file **one GSTR-1 per quarter** — the optional monthly IFF is out of scope for now.

## Summary

Add end-to-end **GST return filing & reconciliation** for Indian businesses on top of the data the
platform already produces. A new backend bounded context (`gstr`) **auto-prepares GSTR-1** (outward
supplies — B2B invoice-wise, B2CL vs B2CS state+rate summaries, credit/debit notes, HSN summary,
exports/nil-rated, document series) and **GSTR-3B** (the monthly/quarterly summary, derived *from* the
GSTR-1 totals + RCM + ITC so the two tie by construction) by **aggregating finalized invoices** from
the `invoice` module and their **immutable tax audit snapshots** (spec 026 R8), pulling in IRN data from
`einvoice` (spec 015) where present. A **reconciliation engine** runs invoice⟷GSTR-1 self-checks and
(Phase 2) books⟷2A/2B ITC matching with a typed mismatch taxonomy, gating filing on **data-quality
preconditions** (missing GSTIN/HSN/place-of-supply block a return). **CMP-08** covers composition
dealers (spec 026); **GSTR-9/9C** are a later phase.

Filing is **export-first**: Phase 1 generates a **GST-portal-compatible JSON / offline-utility Excel** for
GSTR-1 & 3B (real compliance value with no GSP onboarding); Phase 2 adds a **`GstnFilingProvider`**
port (pluggable GSP/ASP, per-workspace resolver — the same abstraction spec 015 used for the IRP) for
API filing with **EVC/OTP** auth, **ARN** tracking and the **2A/2B pull**. Because a business may hold
multiple GSTINs (one per state), the filing aggregate is a **`GstReturnPeriod`** keyed by
`(gstin, returnType, financialYear, period)` over a first-class **`GstinRegistration`** branch model, with
a `NOT_STARTED → PREPARED → RECONCILED → FILED → ACKNOWLEDGED` lifecycle, period-locking and an immutable
filed snapshot. **Filing and 2A/2B pull are online-only** GSTN operations (backend queue + retry); the
mobile app is a **read-only status/summary surface**. Money is `BigDecimal`; portal return totals round to
the rupee. Full design rationale in [research.md](./research.md).

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), Spring scheduling
(`@Scheduled` filing/pull retry worker), an HTTP client for GSP calls (WebClient/RestClient), `core`
(`OwnableBaseDomain`, `BaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`); consumes
`InvoiceFinalizedEvent` / `InvoiceCancelledEvent` from `event`/`invoice`; reads `InvoiceService`
(finalized invoices: `series`/`sequenceNumber`, `customerGst`/`sellerGst`,
`placeOfSupply`/`sellerPlaceOfSupply`, `taxInfos`, `totalCost`/`totalTax`, tax audit snapshot), the
`tax` GST definitions, the `einvoice` `EInvoiceDocument` (IRN/ackNo, spec 015), and `SettingService`.
Mobile — Room KMP, Ktor, Metro DI, Navigation3, kotlinx.datetime, existing `data/sync`
(`CentralSyncService`, `SyncDelegate`), `data/common` (`ApiUrlBuilder`), `feature/invoice` (read-only
reference).
**Storage**: Backend — PostgreSQL/MySQL via Flyway; the computed return JSON (`GstReturnSnapshot`),
section data and GSTN 2A/2B feeds as `JSON`/`TEXT`/`LONGTEXT`; timestamps `TIMESTAMPTZ`/`TIMESTAMP`;
money `DECIMAL(19,4)` (portal totals rupee-rounded at the boundary). Mobile — Room (workspace-scoped DB
`gstr`), display-only strings/longs.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :gstr:test`) incl. GSTR-1 aggregation golden
tests (B2B/B2CL/B2CS/HSN/DOCS classification), 3B⟷GSTR-1 tie-out, reconciliation mismatch taxonomy,
portal-JSON schema conformance, period-lock/immutability and filing idempotency. Mobile —
`./gradlew :feature:gstr:check` + 3-target compile gates.
**Target Platform**: Backend service (Linux); Mobile Android (minSdk 24) / iOS / Desktop (JVM).
**Project Type**: Mobile + API — new backend module + thin pull-only KMP feature surface. Web (Angular)
is the **primary filing UI** and a tracked follow-up (see Constitution Check · Principle VIII).
**Performance Goals**: Prepare a month's GSTR-1 for a workspace with thousands of invoices in seconds
(server aggregation); export generation streams; the filing/pull retry worker drains its queue with
exponential backoff; mobile status sync batches 100 rows/page.
**Constraints**: Filing & 2A/2B pull are **online-only** (queue + retry); one return per
`(gstin, type, fy, period)` (idempotent); a `FILED` period is **immutable** and **source-locked** — but a
late invoice dated in a filed period is **routed to the next open period, never blocked** (R8); the
electronic-**file** action and credential setup are **owner/admin-only** (RBAC; prepare/export may be
broader); GSP/GSTN credentials never leave the server; EVC OTP goes to the signatory (never to the app);
workspace + per-GSTIN data isolation; portal totals in whole rupees; QRMP filers file one GSTR-1 per
quarter (monthly IFF deferred).
**Scale/Scope**: Per workspace: 1–N GSTINs × ~12 monthly (or 4 quarterly) periods/year × return types.
~7 backend entities (`GstinRegistration`, `GstReturnPeriod`, `GstReturnSnapshot`, `PurchaseRegisterEntry`,
`Gstn2bRecord`, `ReconciliationResult`, `GstFilingAttempt` + Phase-2 `GstnCredential`), ~2 pull-only sync
entities + ~6 action/export endpoints, ~1–2 mobile status screens.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | All timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP` (preparedAt, filedAt, ackDate, pulledAt); money `BigDecimal`/`DECIMAL(19,4)`, never floating point — invoice `Double` converted once at aggregation. |
| II. DTO & Contract Isolation | ✅ PASS | Request/Response DTOs in `gstr/domain/dto/`; entities never exposed; the **portal JSON / 2A-2B payloads** are isolated builder DTOs (III), not entities; raw filed/2B blobs omitted from list DTOs, exposed only on detail. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Internal API uses global Jackson SNAKE_CASE. The **GSTN portal return JSON** (GSTR-1/3B offline-utility schema) and **2A/2B** payloads use GSTN's required field names (PascalCase/mixed: `b2b`, `inv`, `itms`, `txval`, `iamt`/`camt`/`samt`/`csamt`, `pos`, `rt`, `hsn_sc`) via isolated `*PortalBuilder` DTOs with explicit `@JsonProperty` — a genuinely non-standard external contract, documented inline (exactly as spec 015 did for INV-01). |
| IV. Multi-Tenant Isolation | ✅ PASS | All workspace entities extend `OwnableBaseDomain` (`@TenantId ownerId`); tenant set by `SessionUserFilter` via `X-Workspace-ID`; per-GSTIN scoping is an explicit `gstin` column under the tenant; GSP creds resolved per workspace; services never mutate tenant context. |
| V. API Response Standardization | ✅ PASS | All endpoints return `ApiResponse<T>`; sync pull returns `ApiResponse<PageResponse<T>>`; exports stream the artifact via a typed download endpoint. |
| VI. Centralized Exception Handling | ✅ PASS | Typed `GstrException`/`GstnFilingException`/`PeriodLockedException` bubble to a module handler; no business try/catch in controllers (filing retry lives in the worker/service). |
| VII. Efficient Data Loading | ✅ PASS | `@NamedEntityGraph` for period+snapshot+attempts; derived queries; `@Query` only for the period-invoice aggregation, the sync feed and the pending-filing/pull poll. |
| VIII. Angular Material 3 Exclusivity | ⚠️ DEFERRED | Web is the **primary** filing/reconciliation UI; it will be Angular Material 3 only. Tracked follow-up; this plan delivers backend + a read-only mobile surface. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `gstr` bounded context; reads `invoice`/`tax`/`einvoice`/`setting` via public service interfaces + domain events, never repositories. |
| X. Compose Multiplatform Parity | ✅ PASS | Shared status/summary logic in `feature/gstr/src/commonMain`; thin platform DI; pull-only — no on-device return computation. |
| XI. Security & Secrets Hygiene | ✅ PASS | Phase-2 GSP/GSTN credentials env-provided + encrypted per-GSTIN row; EVC OTP never touches the app; the electronic-**file** action + credential setup are **owner/admin-only** (workspace RBAC; prepare/export may be broader); filed/2B blobs ACL'd; no secrets in source/`keys/`. |
| Flyway | ✅ PASS | Versioned migrations in **both** `mysql/` and `postgresql/`; `gstr` added to `settings.gradle.kts` + `migrationModules`. **Version coordination**: max applied is `V1.0.104`, but specs 015/016/024/026 already claim `V1.0.105`–`V1.0.107`; this module uses a **distinct higher band `V1.0.110`/`V1.0.111`/`V1.0.112`** (verify with `./gradlew :ampairs_service:flywayInfo` before merge and bump if taken). |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% on aggregation + reconciliation + portal-JSON + idempotency/lock logic; mobile `check` + 3-target compile. |

**Result**: PASS — no constitution violations. Web deferral (VIII) and the deliberately off-`/sync`
filing/return entities are documented scope/architecture decisions, recorded in **Complexity Tracking**.

## Project Structure

### Documentation (this feature)

```
specs/028-gstr-filing/
├── plan.md              # This file
├── spec.md              # Feature specification (/speckit.specify output)
├── research.md          # Phase 0 — design decisions + rationale
├── data-model.md        # Phase 1 — entities, period lifecycle, GSTR-1/3B section mapping
├── quickstart.md        # Phase 1 — prepare + export a month's GSTR-1/3B; reconcile a sample 2B
├── contracts/
│   ├── README.md
│   ├── gstr-sync.md            # pull-only /sync feeds (return-periods, reconciliation summaries)
│   ├── gstr-prepare.md         # prepare/recompute/readiness + GSTR-1/3B/CMP-08 retrieval
│   ├── gstr-export.md          # portal JSON / Excel export endpoints + the GSTN JSON shape
│   └── gstr-filing.md          # Phase 2 — file (EVC/OTP), status/ARN, 2A/2B pull, reconciliation
├── checklists/requirements.md
└── tasks.md             # Phase 2 (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
gstr/
└── src/main/
    ├── kotlin/com/ampairs/gstr/
    │   ├── domain/
    │   │   ├── model/      # GstinRegistration, GstReturnPeriod, GstReturnSnapshot,
    │   │   │               #   PurchaseRegisterEntry, Gstn2bRecord, ReconciliationResult,
    │   │   │               #   GstFilingAttempt, GstnCredential (P2)
    │   │   ├── enums/      # ReturnType (GSTR1/GSTR3B/CMP08/GSTR9/GSTR9C),
    │   │   │               #   ReturnStatus (NOT_STARTED→PREPARED→RECONCILED→FILED→ACKNOWLEDGED),
    │   │   │               #   FilingStatus (INITIATED→SUBMITTED→EVC_REQUESTED→FILED→ACKNOWLEDGED|FAILED),
    │   │   │               #   RegistrationType (REGULAR/COMPOSITION/SEZ/CASUAL),
    │   │   │               #   FilingFrequency (MONTHLY/QUARTERLY), Gstr1Section (B2B/B2CL/B2CS/CDNR/
    │   │   │               #   CDNUR/EXP/NIL/HSN/DOCS), MismatchType (MATCHED/MISMATCH_VALUE/
    │   │   │               #   MISMATCH_GSTIN/MISSING_IN_2B/MISSING_IN_BOOKS/PROBABLE_MATCH)
    │   │   └── dto/        # request/response DTOs + converters; *PortalBuilder DTOs (GSTN JSON shape)
    │   ├── repository/     # Spring Data repos (+ @EntityGraph, period-invoice aggregation, sync feed,
    │   │                   #   pending-filing/pull poll)
    │   ├── service/        # GstReturnService (period lifecycle), Gstr1Aggregator, Gstr3bAggregator,
    │   │                   #   Cmp08Service, ReconciliationEngine, ReturnReadinessService,
    │   │                   #   Gstr1PortalBuilder/Gstr3bPortalBuilder (JSON), GstrExcelExporter,
    │   │                   #   PeriodLockService, PurchaseRegisterImportService,
    │   │                   #   GstnFilingWorker (@Scheduled retry, P2), GstrSettingDefinitions
    │   ├── provider/       # GstnFilingProvider port + ClearTaxGspProvider/MasterIndiaGspProvider/
    │   │                   #   GstnSandboxProvider, GstnProviderResolver, GstnSessionTokenCache (P2)
    │   ├── controller/     # GstrController (prepare/readiness/retrieve/export + sync),
    │   │                   #   GstrFilingController (file/status/ARN/2A-2B, P2),
    │   │                   #   GstinRegistrationController
    │   ├── event/          # InvoiceFinalizedEvent/InvoiceCancelledEvent listener → mark period dirty
    │   │                   #   (re-prepare on next request); consult PeriodLockService on finalize
    │   └── config/         # Constants, credential encryption (P2)
    └── resources/db/migration/
        ├── mysql/V1.0.110__create_gstr_tables.sql
        ├── postgresql/V1.0.110__create_gstr_tables.sql
        ├── mysql/V1.0.111__create_gstr_reconciliation_tables.sql        # P2
        ├── postgresql/V1.0.111__create_gstr_reconciliation_tables.sql   # P2
        ├── mysql/V1.0.112__create_gstr_filing_credential_tables.sql     # P2
        └── postgresql/V1.0.112__create_gstr_filing_credential_tables.sql
# wiring: settings.gradle.kts (include "gstr"); ampairs_service/build.gradle.kts
#         (implementation(project(":gstr")) + "gstr" in migrationModules)
# invoice finalize path: consult gstr PeriodLockService.isPeriodLocked(gstin, invoiceDate) (additive);
#   a locked hit does NOT block finalize — it tags the invoice's GSTR-1 reporting period to the next
#   open period (filing-period attribution), leaving the finalized invoice + its real date untouched

# Mobile — ampairs-app/ (sibling repo)  — thin, read-only status/summary
feature/gstr/src/
├── commonMain/kotlin/com/ampairs/gstr/
│   ├── data/api/          # GstrApi(+Impl), ApiUrlBuilder.gstrUrl
│   ├── data/db/           # Room entities (GstReturnPeriod status, headline totals) + DAOs + DB
│   ├── data/repository/   # GstrRepository (local-only, pull-mirror)
│   ├── domain/            # display models, status enums
│   ├── di/                # GstrModule.kt (DAOs)
│   ├── sync/              # GstReturnPeriodSyncDelegate (pull-only)
│   └── ui/                # return-period status list + summary screen, "prepare/file" online command VMs
├── androidMain/ iosMain/ desktopMain/   # GstrModule.{platform}.kt (@SingleIn(WorkspaceScope::class))
# wiring: SyncEntity addition (GSTR_RETURN_PERIOD); ApiUrlBuilder.gstrUrl;
#         ModuleRegistry ("gst-filing" → Route.Gstr); shared/ Routes + entry provider
```

**Structure Decision**: Mobile + API. The backend `gstr/` module mirrors the sibling compliance module
`einvoice` (spec 015) — same provider-abstraction posture, same online-queue + retry worker, same
encrypted-credential pattern — and reads `invoice`/`tax`/`einvoice` only through their public service
interfaces and the already-published `InvoiceFinalizedEvent`/`InvoiceCancelledEvent`. The mobile
`feature/gstr/` is intentionally thin (pull-only status/summary, like the read-only side of
`feature/einvoice`); the **rich preparation/reconciliation UI is web** (a tracked follow-up). No existing
module's data model is reshaped; `gstr` is purely additive and downstream.

## Phased Implementation

### Phase 1 — Export-first: GSTR-1 + 3B prep, reconciliation self-check, multi-GSTIN

- **Entities**: `GstinRegistration` (per-state GSTIN branch model — gstin, stateCode, legalName,
  registrationType, filingFrequency); `GstReturnPeriod` (`(gstin, returnType, fy, period)` + status);
  `GstReturnSnapshot` (immutable computed return JSON); `PurchaseRegisterEntry` (import-fed, the future
  ITC source — R5). Flyway `V1.0.110` (mysql + postgresql).
- **Aggregation**: `Gstr1Aggregator` classifies finalized invoices/snapshots into
  B2B/B2CL/B2CS/CDNR/CDNUR/EXP/NIL + HSN + DOCS sections (R3); `Gstr3bAggregator` builds the 3B summary
  *from* GSTR-1 totals + RCM so they tie (R4, ITC table stubbed). `Cmp08Service` for composition
  dealers (ties to spec 026 composition mode).
- **Reconciliation & readiness**: `ReconciliationEngine` invoice⟷GSTR-1 self-check + `ReturnReadiness
  Service` blocking report (missing GSTIN/HSN/PoS/series-gap) gating `PREPARED` (R9/R10).
- **Export**: `Gstr1PortalBuilder`/`Gstr3bPortalBuilder` → GSTN offline-utility **JSON**;
  `GstrExcelExporter` → Excel (R6). Endpoints: `POST /gstr/v1/returns/{gstin}/{type}/{period}/prepare`,
  `GET …/readiness`, `GET …/gstr1`, `GET …/gstr3b`, `GET …/export?format=json|xlsx`,
  `GET /gstr/v1/returns/sync` (pull-only); `GstinRegistration` CRUD; `GstrSettingDefinitions`
  (`gstr_enabled`, `filing_frequency`, `b2cl_threshold`).
- **Period lifecycle**: `NOT_STARTED → PREPARED → RECONCILED`; `PeriodLockService.isPeriodLocked`
  consulted by invoice finalize (no lock effective until a period is FILED in P2, but the seam ships now).
- **Mobile**: `GstReturnPeriodSyncDelegate` (pull-only); status list + headline-totals summary; "prepare"
  online command.

### Phase 2 — GSP API filing + 2A/2B pull + ITC reconciliation

- **Provider**: `GstnFilingProvider` port + one GSP impl + `GstnSandboxProvider` + `GstnProviderResolver`
  + `GstnSessionTokenCache`; encrypted `GstnCredential` per GSTIN. Flyway `V1.0.112`.
- **Filing**: `GstFilingAttempt` lifecycle (`INITIATED→SUBMITTED→EVC_REQUESTED→FILED→ACKNOWLEDGED`),
  EVC/OTP two-step (R7), with the `file`/`file/confirm` endpoints + credential setup **owner/admin-gated**
  (workspace RBAC); `GstnFilingWorker` (`@Scheduled` queue + backoff); period advances
  `RECONCILED → FILED → ACKNOWLEDGED` with persisted **ARN**; `getReturnStatus` idempotency pre-check +
  period locking/immutability at FILED (R8). Endpoints: `POST …/file` (request EVC), `POST …/file/confirm`
  (submit OTP), `GET …/filing-status`.
- **2A/2B + ITC recon**: pull `Gstn2bRecord` from GSTN via the provider (online-only queue);
  `ReconciliationEngine` books⟷2B matching into the MATCHED/MISMATCH/MISSING buckets (R9), feeding the 3B
  ITC table (R4); `PurchaseRegisterImportService` CSV/Excel import populates the books side (R5). Flyway
  `V1.0.111`. Endpoints: `POST …/2b/pull`, `GET …/reconciliation`, `POST /gstr/v1/purchase-register/import`.
- **Mobile**: filing status + ARN surfaced read-only; "file" online command (EVC entry); 2A/2B pull is
  backend-only (not on device).

### Phase 3 — Annual returns (GSTR-9 / 9C)

- `ReturnType.GSTR9/GSTR9C` annual aggregation rolling up the 12 monthly periods' snapshots;
  9C reconciliation statement scaffolding. Deeper ITC once a **first-class purchase/vendor module** lands
  (replaces the CSV-fed `PurchaseRegisterEntry`); GSTR-1 amendment flow (corrections file in the *next*
  period, never in-place — R8); the optional **monthly IFF** upload for QRMP quarterly filers (deferred
  from Phase 1, which files one quarter-end GSTR-1 per quarter).

### Mobile / offline considerations

- Return preparation, reconciliation and export are **server-computed** (whole-period aggregates); the
  app never computes a return on-device.
- **Filing and 2A/2B pull are online-only** GSTN/GSP operations driven by a backend queue + retry; the
  app surfaces status/ARN and can trigger prepare/file as **online commands** only.
- No client-authored push for `gstr` entities — the single mobile sync entity (`GSTR_RETURN_PERIOD`) is
  **pull-only**, like `einvoice`.

## Complexity Tracking

*Two deliberate deviations, both justified and consistent with the sibling `einvoice` (spec 015) and
`tax` modules:*

| Deviation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| `gstr` entities sit **off the canonical `/sync` push** (pull-only on mobile; filing/2A-2B are online commands) | Returns are server-authored period aggregates and filing/2A-2B are GSTN round-trips — there is no offline-authored state to push (research R11) | A full read/write `/sync` entity would imply the device can author/file returns offline, which is impossible (no full-period data on-device, no offline GSTN). Documented like `tax`/`file`/`einvoice` off-`/sync`. |
| New `gstr` **bounded context** rather than extending `tax`/`invoice`/`einvoice` | Filing has its own period lifecycle, external GSTN/GSP integration, encrypted secrets, retry queue and immutable filed snapshots — a distinct context (research R1, Principle IX) | Extending `tax` couples a regulated external integration + period model to the offline calculator; extending `einvoice` conflates per-document IRN minting with period aggregation. The clean seam is services + `InvoiceFinalizedEvent`. |
