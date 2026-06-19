# Implementation Plan: Payment & Collection (Party Ledger)

**Branch**: `claude/affectionate-bohr-er32cm` (spec dir `013-payment-collection`) | **Date**: 2026-06-19 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/013-payment-collection/spec.md`

## Summary

Build a payment & collection capability on top of existing invoices/orders so a retail or wholesale
owner can manage money owed per party: an **opening balance**, movements from **sales, returns and
adjustments**, **payments** in many modes (cash, cheque, UPI, NEFT, RTGS, IMPS, net banking, card,
bank transfer), and an always-correct **closing balance** (receivable or payable) per party.

Technical approach: a new backend bounded context (`payment` module) and a new Compose Multiplatform
feature module (`feature/payment`) implementing a **subsidiary party ledger**. A single signed
`LedgerEntry` per movement is the sole driver of the balance; a cached `PartyBalance` is always
recomputable (`opening + ΣDr − ΣCr = closing`). Payments are recorded as vouchers with mode/instrument
details and a realisation status; allocations match receipts to bills for outstanding/aging only.
Everything is workspace-scoped and rides the canonical offline `/sync` contract; the document-authoring
side posts the ledger movement so balances are correct offline. Money is exact precision
(`DECIMAL(19,4)` backend, integer minor units on mobile). Full design rationale in
[research.md](./research.md); entities in [data-model.md](./data-model.md); APIs in
[contracts/](./contracts/).

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), `core`
(`OwnableBaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`), `sequence` module;
consumes an invoice-finalized domain event from the `invoice` module. Mobile — Room KMP, Ktor, Metro
DI, Navigation3, kotlinx.datetime, existing `data/sync` (`CentralSyncService`, `SyncDelegate`),
`data/common` (`ApiUrlBuilder`, `WorkspaceAwareDatabaseFactory`), `customer`/`invoice`/`order` feature
modules (read-only references).
**Storage**: Backend — PostgreSQL/MySQL via Flyway, money `DECIMAL(19,4)`, timestamps
`TIMESTAMPTZ`/`TIMESTAMP`. Mobile — Room (workspace-scoped DB `payment`), money as `Long` minor units.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :payment:test`), incl. the ledger foot-to-zero
invariant. Mobile — `./gradlew :feature:payment:check`; compile gates for all three targets.
**Target Platform**: Backend service (Linux); Mobile Android (minSdk 24) / iOS / Desktop (JVM).
**Project Type**: Mobile + API (Option 3) — backend module + KMP feature module. Web (Angular) deferred
to a follow-up (see Constitution Check · Principle X).
**Performance Goals**: Record-collection flow perceived instant (<30 s end-to-end, US/SC-001); statement
and aging render without noticeable lag on a party with thousands of entries; sync batches 100
records/page like existing entities.
**Constraints**: Offline-capable (balances update on-device immediately, reconcile deterministically);
ledger MUST always foot with zero rounding drift (SC-002/006); no hard-delete of posted movements;
gap-free voucher numbering; workspace data isolation.
**Scale/Scope**: Phase 1 = customer receivables (+ purchases/returns as adjustments). Per workspace:
thousands of parties, tens of thousands of ledger entries. ~4–5 backend entities, ~5 sync entities and
~4 primary screens on mobile.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | All timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP`; no `LocalDateTime`. Money uses `BigDecimal`/`DECIMAL(19,4)` (not floating point). |
| II. DTO & Contract Isolation | ✅ PASS | Request/Response DTOs in `payment/domain/dto/`; entities never exposed; `entity.asResponse()` / `request.toEntity()` converters with validation annotations. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Rely on global Jackson strategy; no `@JsonProperty` for standard fields; clients trust snake_case. |
| IV. Multi-Tenant Isolation | ✅ PASS | All entities extend `OwnableBaseDomain` (`@TenantId ownerId`); tenant set by `SessionUserFilter`; controllers honor `X-Workspace-ID`; services never mutate tenant context. |
| V. API Response Standardization | ✅ PASS | All endpoints return `ApiResponse<T>`; sync pull returns `ApiResponse<PageResponse<T>>` via `PageResponse.from(page)`. |
| VI. Centralized Exception Handling | ✅ PASS | No business try/catch in controllers; typed domain exceptions bubble to the global handler. |
| VII. Efficient Data Loading | ✅ PASS | `@NamedEntityGraph` for voucher+allocations; derived queries preferred; `@Query` only for sync feed/aging where needed. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web UI deferred; when added it will use Angular Material 3 only. Tracked as follow-up. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `payment` bounded context; cross-module access (invoice/customer) via public service interfaces + a published domain event, never repositories. |
| X. Compose Multiplatform Parity | ✅ PASS | Shared logic/UI in `feature/payment/src/commonMain`; thin platform DI only. Web parity tracked as deferred follow-up. |
| XI. Security & Secrets Hygiene | ✅ PASS | No secrets introduced; standard JWT/workspace auth reused. |
| Flyway | ✅ PASS | Versioned migration in **both** `mysql/` and `postgresql/`; module added to `migrationModules`; next version checked via `flywayInfo`. |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% on ledger/posting logic incl. foot-to-zero invariant; mobile `check` + 3-target compile gates. |

**Result**: PASS — no violations; Complexity Tracking not required. Web deferral is a scope decision
(documented), not a principle violation.

## Project Structure

### Documentation (this feature)

```
specs/013-payment-collection/
├── plan.md              # This file (/speckit.plan output)
├── spec.md              # Feature specification (/speckit.specify output)
├── research.md          # Phase 0 output — design decisions + rationale
├── data-model.md        # Phase 1 output — entities, fields, relationships, state machines
├── quickstart.md        # Phase 1 output — how to exercise the feature
├── contracts/           # Phase 1 output — API contracts
│   ├── README.md
│   ├── payment-sync.md          # canonical /sync endpoints
│   └── payment-actions.md       # statement / open-bills / aging / recompute / bounce
├── checklists/
│   └── requirements.md  # spec quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
payment/
└── src/main/
    ├── kotlin/com/ampairs/payment/
    │   ├── domain/
    │   │   ├── model/          # PartyBalance, LedgerEntry, PaymentVoucher, PaymentAllocation, AdjustmentVoucher
    │   │   ├── enums/          # EntryType, Direction, PaymentMode, ClearanceStatus, PaymentDirection, AdjustmentType
    │   │   └── dto/            # request/response DTOs + converters
    │   ├── repository/         # Spring Data repos (+ @EntityGraph, sync feed queries)
    │   ├── service/            # bulkUpsert, posting, balance recompute, aging, statement, open-bills
    │   ├── controller/         # PaymentController (sync + actions)
    │   ├── config/             # Constants, PaymentSettingDefinitions
    │   └── event/              # InvoiceFinalizedEvent listener (posts SALES_INVOICE entry)
    └── resources/db/migration/
        ├── mysql/V1.0.x__create_payment_module_tables.sql
        └── postgresql/V1.0.x__create_payment_module_tables.sql
# wiring: settings.gradle.kts (include "payment"); ampairs_service/build.gradle.kts
#         (implementation(project(":payment")) + "payment" in migrationModules)
# invoice module: publish InvoiceFinalizedEvent on finalize (additive)

# Mobile — ampairs-app/ (sibling repo)
feature/payment/src/
├── commonMain/kotlin/com/ampairs/payment/
│   ├── data/api/          # PaymentApi(+Impl), ApiUrlBuilder.paymentUrl
│   ├── data/db/           # Room entities + DAOs + PaymentRoomDatabase
│   ├── data/repository/   # PaymentRepository, LedgerRepository (local-only)
│   ├── domain/            # Money (minor units), posting rules, models, enums
│   ├── di/                # PaymentModule.kt (DAOs)
│   ├── sync/              # PaymentVoucher/LedgerEntry/Allocation/PartyBalance/Adjustment SyncDelegates
│   └── ui/                # screens + ViewModels (dashboard, record payment, statement, open bills)
├── androidMain/ iosMain/ desktopMain/   # PaymentModule.{platform}.kt (@SingleIn(WorkspaceScope::class))
# wiring: settings.gradle.kts (:feature:payment); SyncEntity enum additions;
#         shared/ Routes + entry provider; ModuleRegistry ("payment-collection" → Route.Payment);
#         data/common ApiUrlBuilder.paymentUrl(...)
```

**Structure Decision**: Mobile + API. The backend `payment/` module mirrors existing bounded contexts
(`invoice`, `order`) exactly; the mobile `feature/payment/` module mirrors `feature/invoice` and
`feature/order` (offline-first, workspace-scoped, SyncDelegate-owned API). Web (Angular) is a tracked
follow-up and is out of scope for this plan.

## Complexity Tracking

*No constitution violations — section intentionally empty.*
