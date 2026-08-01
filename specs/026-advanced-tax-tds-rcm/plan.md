# Implementation Plan: Advanced Indian Tax (TDS / TCS / RCM / Composition / ITC)

**Branch**: `claude/indian-retail-ecosystem-877med` (spec dir `026-advanced-tax-tds-rcm`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/026-advanced-tax-tds-rcm/spec.md`

## Summary

Extend Ampairs' GST engine to the rest of Indian indirect/withholding tax that Tier-2/3 wholesalers
and B2B sellers actually hit: **TDS** (income-tax withheld by the buyer at settlement), **TCS**
(collected by the seller at settlement), **RCM** (reverse charge — buyer liable for GST),
**composition scheme** (bill of supply, no tax line), **nil/exempt/zero-rated/non-GST** treatment, and
**ITC / ITC-reversal** tracking — plus an **immutable per-transaction tax audit snapshot**.

Per [research.md](./research.md), each is placed where it belongs rather than forced into one
mechanism: TDS/TCS are **ledger postings** (new `payment` `EntryType`s, computed at invoice/payment
time, *not* GST components); RCM is a **liability-flip flag** that changes posting/document shape but
**reuses the existing place-of-supply calculation verbatim**; composition is a `TaxConfiguration` mode
that switches the invoice to `documentType = BILL_OF_SUPPLY` and suppresses the tax stack;
nil/exempt/zero-rated is a per-line `gstTreatment` enum; ITC reversal is a separate input-credit
ledger. The work **extends the existing `tax` module** (definitions + the on-device pluggable
calculator) and reaches the `invoice`/`payment` aggregates through their existing event/service seams.
The hard constraint — **offline-deterministic tax calculation** — is preserved: all amount-affecting
logic runs in the on-device strategy fed by synced definitions, so a device and the backend compute
identical amounts.

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), `core`
(`OwnableBaseDomain`, `BaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`); extends
`tax`; consumes/extends `invoice` (`InvoiceFinalizedEvent`, `documentType`) and `payment`
(`LedgerEntry`, `EntryType`) via public service interfaces + domain events. Mobile — Room KMP, Ktor,
Metro DI, kotlinx.datetime, existing `data/sync` (`CentralSyncService`, `SyncDelegate`), existing
`feature/tax` pluggable strategy (`IndiaGSTStrategy`, `TransactionContext.isReverseCharge`),
`feature/invoice` (`InvoiceItem.taxInfos/taxSpec`), `feature/payment` (ledger, minor-units money).
**Storage**: Backend — PostgreSQL/MySQL via Flyway; advanced-tax amounts `DECIMAL(19,4)`; tax audit
snapshot as a JSON column on invoice; timestamps `TIMESTAMPTZ`/`TIMESTAMP`. Mobile — Room
(workspace-scoped `tax` DB extended), ledger amounts as `Long` minor-units (via `payment`), GST line
calc `Double` with half-up + round-off at the line boundary.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :tax:test :payment:test :invoice:test`), incl.
**device-vs-backend amount parity** and **ledger foot-to-zero** with TDS/TCS. Mobile —
`./gradlew :feature:tax:check`; **3-target compile gate**; deterministic-calc golden tests
(same inputs → same amounts on Android/iOS/Desktop).
**Target Platform**: Backend service (Linux); Mobile Android (minSdk 24) / iOS / Desktop (JVM).
**Project Type**: Mobile + API — extend backend `tax` module + app `feature/tax` (with additive
touches to `invoice`/`payment` on both sides).
**Performance Goals**: On-device calc perceived-instant per line (pure function, no I/O); definition
sync batches 100 rows/page; audit snapshot write is one row at finalize.
**Constraints**: **Offline-deterministic** — backend recompute MUST match device byte-for-byte on
amounts (R9); ledger MUST foot to zero with TDS/TCS (exact money); composition invoices MUST NOT carry
a tax line; RCM MUST NOT change the rate, only liability; finalized tax snapshot is immutable; KMP-safe
(`commonMain` no `java.*`).
**Scale/Scope**: ~3–4 new `tax` entities (`TdsSection`, `TcsSection`, composition config, gstTreatment
overrides), ~3 new `payment` `EntryType`s, an `invoice` `documentType`/`rcmApplicable`/snapshot
extension, ~3 new sync entities on device, and an extended on-device calculator.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | All timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP`; advanced-tax amounts `BigDecimal`/`DECIMAL(19,4)` (never floating point on the ledger). |
| II. DTO & Contract Isolation | ✅ PASS | New request/response DTOs in `tax/domain/dto/`; entities never exposed; `entity.asResponse()` / `request.toEntity()` with validation. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Flat snake_case (`tds_section`, `rcm_applicable`, `gst_treatment`, `document_type`); audit snapshot is a typed JSON DTO. |
| IV. Multi-Tenant Isolation | ✅ PASS | New definition entities extend `OwnableBaseDomain` (`@TenantId`); TDS/TCS *master* sections (statutory, shared) extend `BaseDomain`; tenant set by `SessionUserFilter`; controllers honor `X-Workspace-ID`. |
| V. API Response Standardization | ✅ PASS | All endpoints return `ApiResponse<T>`; sync pull → `ApiResponse<PageResponse<T>>` via `PageResponse.from(page)`. |
| VI. Centralized Exception Handling | ✅ PASS | No business try/catch in controllers; typed tax exceptions bubble to the global handler. |
| VII. Efficient Data Loading | ✅ PASS | `@EntityGraph` where definitions join; derived queries; `@Query` only for sync feeds. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web UI deferred; M3-only when added. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | Definitions/calc **extend `tax`** (its bounded context); effects reach `invoice`/`payment` via `InvoiceFinalizedEvent` + public services, never repositories. |
| X. Compose Multiplatform Parity | ✅ PASS | Shared calc/logic in `feature/tax/commonMain`; thin platform DI; on-device deterministic strategy. |
| XI. Security & Secrets Hygiene | ✅ PASS | No secrets; reuses JWT/workspace auth. |
| Flyway | ✅ PASS | Versioned migrations in **both** `mysql/` and `postgresql/` (next ≥ V1.0.105 per `flywayInfo`); `tax`/`invoice`/`payment` already in `migrationModules`. |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% on TDS/TCS/RCM/composition calc + ledger foot-to-zero + device-parity; mobile `check` + 3-target compile + deterministic golden tests. |

**Result**: PASS — no violations; Complexity Tracking not required. Extending `tax` (vs a new module)
is the constitution-aligned choice (R1): advanced tax is the same bounded context, and the cross-aggregate
effects use existing module seams.

## Project Structure

### Documentation (this feature)

```
specs/026-advanced-tax-tds-rcm/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 — TDS/TCS/RCM/composition/ITC placement + determinism
├── data-model.md        # Phase 1 — TdsSection, TcsSection, composition config, gstTreatment, snapshot
├── quickstart.md        # Phase 1 — finalize an RCM invoice / a TDS receipt end-to-end
├── contracts/
│   ├── README.md
│   ├── tds-tcs-sync.md       # canonical /sync for TDS/TCS section config
│   └── advanced-tax-actions.md  # composition mode, gst-treatment, audit snapshot
└── tasks.md             # Phase 2 (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
tax/src/main/kotlin/com/ampairs/tax/
├── domain/
│   ├── model/        # + TdsSection (BaseDomain master) + WorkspaceTdsConfig (OwnableBaseDomain),
│   │                 #   TcsSection + WorkspaceTcsConfig, CompositionConfig (on TaxConfiguration),
│   │                 #   GstTreatmentOverride
│   ├── enums/        # + GstTreatment (TAXABLE/NIL_RATED/EXEMPT/ZERO_RATED/NON_GST),
│   │                 #   WithholdingType (TDS/TCS), TdsSectionCode helpers
│   └── dto/          # + section/config request+response DTOs, TaxAuditSnapshot DTO
├── repository/       # + Tds/Tcs/composition repos (+ sync feed queries incl. soft-deleted)
├── service/          # + WithholdingCalcService (TDS/TCS amount + threshold), RcmResolver,
│                     #   CompositionService; extend GstRuleTemplateService for treatment
├── controller/       # + /tax/v1/tds-sections/sync, /tcs-sections/sync, /composition config
└── resources/db/migration/{mysql,postgresql}/V1.0.105__create_advanced_tax_tables.sql

invoice/src/main/kotlin/com/ampairs/invoice/
├── domain/model/Invoice.kt        # + documentType (TAX_INVOICE|BILL_OF_SUPPLY|EXPORT),
│                                  #   rcmApplicable, taxAuditSnapshot (JSON)
├── domain/enums/DocumentType.kt   # new
├── domain/model/InvoiceItem.kt    # + gstTreatment, rcmApplicable (line override)
└── resources/db/migration/{mysql,postgresql}/V1.0.106__add_document_type_rcm_snapshot.sql

payment/src/main/kotlin/com/ampairs/payment/
├── domain/enums/EntryType.kt      # + TDS_WITHHELD (CR), TCS_COLLECTED (DR), ITC_REVERSAL
├── service/                       # InvoiceLedgerListener: on RCM → self-assess posting; post TDS/TCS
└── resources/db/migration/{mysql,postgresql}/V1.0.107__itc_ledger.sql   # ItcEntry (input-credit ledger)
# wiring: all three modules already in migrationModules; events already published (InvoiceFinalizedEvent)

# Mobile — ampairs-app/ (sibling repo)
feature/tax/src/commonMain/kotlin/com/ampairs/tax/
├── domain/model/        # + TdsSection, TcsSection, CompositionConfig, GstTreatment enum
├── calculation/
│   ├── strategy/IndiaGSTStrategy.kt   # extend: read gstTreatment (suppress components for
│   │                                  #   nil/exempt/non-GST; 0% IGST+ITC for zero-rated);
│   │                                  #   honor TransactionContext.isReverseCharge (RCM) for posting hint
│   ├── WithholdingCalculator.kt       # TDS/TCS amount (pure fn) — minor-units, threshold-aware
│   └── model/                         # TaxAuditSnapshot (captured at finalize)
├── data/db/             # + Tds/Tcs/composition Room entities + DAOs
├── sync/                # + TdsSectionSyncDelegate, TcsSectionSyncDelegate (@SyncEntity.TDS_SECTION/…)
└── ui/                  # section pickers, composition toggle, RCM toggle, withholding line

feature/invoice/src/commonMain/...   # InvoiceItem: gstTreatment; bill-of-supply rendering (no tax block)
feature/payment/src/commonMain/...   # post TDS_WITHHELD / TCS_COLLECTED ledger entries at settlement
# wiring: data/sync SyncEntity (+ TDS_SECTION, TCS_SECTION, COMPOSITION_CONFIG);
#         ApiUrlBuilder.taxUrl(...) already exists
```

**Structure Decision**: Mobile + API, **extending existing modules** rather than adding new ones. The
`tax` module gains definitions + calculator extensions (its bounded context); `invoice` gains
`documentType`/`rcmApplicable`/audit-snapshot; `payment` gains TDS/TCS `EntryType`s and an ITC ledger.
On device, `feature/tax` extends the pluggable strategy and adds `/sync` delegates for the new
definitions; `feature/invoice`/`feature/payment` consume them. This mirrors how spec 013 reached
across `invoice`→`payment` via events, and keeps the offline-deterministic calculator the single
source of amounts.

## Phased Delivery

### P1 — Withholding MVP: TDS/TCS at settlement + audit snapshot

- **Entities (tax)**: `TdsSection`/`TcsSection` (`BaseDomain` master: section code, rate, threshold,
  party-type applicability) + `WorkspaceTds/TcsConfig` (`OwnableBaseDomain`: enabled, default section).
- **Entities (payment)**: `EntryType.TDS_WITHHELD` (CR, reduces receivable), `TCS_COLLECTED` (DR,
  increases receivable) — posted by the ledger listener at invoice finalize / payment time.
- **Entities (invoice)**: `taxAuditSnapshot` JSON column (immutable at finalize, R8).
- **Endpoints**: `GET/POST /tax/v1/tds-sections/sync`, `/tcs-sections/sync` (canonical `/sync`).
- **Calc**: `WithholdingCalculator` pure fn (base × rate, threshold gate) in `feature/tax`; amounts in
  minor-units so they foot in the `payment` ledger.
- **Mobile/offline note**: `Tds/TcsSectionSyncDelegate` (`@ContributesIntoMap(WorkspaceScope::class)`),
  local-only config writes (`synced=false` + `markPendingPush`); device computes TDS/TCS offline and
  posts the ledger entry in the same Room txn as the receipt (spec-013 same-txn posting).
- **Flyway**: V1.0.105 (tax), V1.0.106 (invoice snapshot) — mysql + postgresql.

### P2 — RCM + composition scheme + gstTreatment

- **RCM**: `Invoice.rcmApplicable` + line override; `IndiaGSTStrategy` keeps the place-of-supply split
  but the ledger listener self-assesses on the buyer (liability + offsetting ITC) and the supplier
  invoice shows no collected GST (R4). Wire the app's existing `TransactionContext.isReverseCharge`.
- **Composition**: `TaxConfiguration.compositionScheme` + rate; `Invoice.documentType =
  BILL_OF_SUPPLY` suppresses `taxInfos`/components and adds the statutory declaration; bill-of-supply
  rendering on device (no tax block).
- **gstTreatment**: per-line enum (R7) deterministically shaping components (nil/exempt/non-GST → no
  components; zero-rated → 0% IGST + ITC flag).
- **Endpoints**: composition config under `/tax/v1/configurations` (extend existing); `gstTreatment`
  rides the existing tax-rule/code definitions.
- **Mobile/offline note**: composition mode + treatment are synced definitions read by the on-device
  calc; determinism preserved (R9).

### P3 — ITC ledger + return reconciliation hooks

- **ITC**: `ItcEntry` input-credit ledger (signed, R6) + ITC-reversal entries (Rule 42/43,
  180-day non-payment); decoupled from output-tax calc. Deepens when first-class purchase billing
  lands (today: `payment` adjustment vouchers).
- **Reconciliation**: aggregate the per-transaction audit snapshots into GST-return-shaped reports
  (GSTR-1/3B style summaries) — read-only, server-side.
- **Follow-up (flagged)**: converge invoice `Double` money onto minor-units to fully eliminate GST-line
  rounding risk (R10); ties to e-invoice (spec 015).

## Complexity Tracking

*No constitution violations — section intentionally empty. Advanced tax extends the existing `tax`
bounded context (R1); cross-aggregate effects use the established `InvoiceFinalizedEvent` + public
service seams rather than new boundaries.*
