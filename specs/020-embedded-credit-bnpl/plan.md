# Implementation Plan: Embedded Working-Capital Credit / BNPL

**Branch**: `claude/indian-retail-ecosystem-877med` (spec dir `020-embedded-credit-bnpl`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/020-embedded-credit-bnpl/spec.md`

## Summary

Give Ampairs merchants embedded access to working-capital credit — a **term loan** for growth and **B2B
BNPL** for deferred payables — underwritten on the transaction data Ampairs already holds (the spec-013
party ledger, finalized GST invoices, order/sales velocity). The product thesis is the **credit-signal
export**: aggregate behavioural features that let a partner NBFC/bank underwrite thin-file kirana/SMEs a
bureau cannot see.

The hard architectural constraint: **Ampairs is a Lending Service Provider (LSP) / technology layer,
never a lender.** A new `credit` bounded context **originates** applications, captures **DPDP-grade
consent**, derives a **consented aggregate signal snapshot**, hands KYC + underwriting + disbursement +
collection to a partner **Regulated Entity (RE)** over a pluggable `LenderAdapter` (OCEN / Account
Aggregator aware), and keeps only a **read-only mirror** of the partner-owned loan for merchant UX and
repayment reminders. Ampairs never decides, never prices, never moves funds, and never stores
licence/KYC/bureau/raw-statement data. Unlike the rest of the platform this lifecycle is **online +
partner-dependent**; the mobile module is online-only with a pull-only status cache. Full design
rationale in [research.md](./research.md).

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), `core`
(`OwnableBaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`); **public service interfaces**
of `payment` (party-ledger aggregates), `invoice`, `order` for signals; Spring `ApplicationEventPublisher`
for in-process events; an outbound HTTP client (Spring `RestClient`/WebClient) + HMAC/mTLS for partner
adapters; webhook receiver with signature verification; a scheduler (`@Scheduled`) for reconciliation.
Mobile — Room KMP (read-only status cache only), Ktor, Metro DI, Navigation3, kotlinx.datetime.
**Storage**: Backend — PostgreSQL/MySQL via Flyway, money `DECIMAL(19,4)`, timestamps `TIMESTAMPTZ`/
`TIMESTAMP`; append-only event tables. Mobile — Room (pull-only status mirror), money as `Long` minor
units. **Secrets** (partner keys/mTLS certs) in env/secret store — never in DB or source.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :credit:test`), incl. state-machine transition
tests, webhook idempotency/dedup tests, and a consent-gate test (no export without live consent). Mobile —
`./gradlew :feature:credit:check`; 3-target compile gates.
**Target Platform**: Backend service (Linux); Mobile Android (minSdk 24) / iOS / Desktop (JVM).
**Project Type**: Mobile + API. **This feature is online-only** (no offline `/sync` for the lifecycle).
**Performance Goals**: Application submit + signal export perceived responsive (<3 s server work, partner
latency excluded); webhook handling idempotent and <500 ms; reconciliation poll batched.
**Constraints**: **LSP boundary is absolute** — no decisioning, no pricing, no funds movement, no
prohibited-data storage (R2). DPDP consent gates every export. Exactly-once partner effects under retries.
Online-only lifecycle; partner availability is a dependency, surfaced explicitly in UX.
**Scale/Scope**: Phase 1 = one or two partner REs, term loan only, merchant-as-borrower. Per workspace:
small N of applications/loans (not high-volume). ~7–9 backend entities (mostly append-only), ~4–5 mobile
screens, **0 offline-sync entities** for the lifecycle (1 pull-only mirror).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | All timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP`; money `BigDecimal`/`DECIMAL(19,4)`; no `LocalDateTime`/`Double`. |
| II. DTO & Contract Isolation | ✅ PASS | Request/Response DTOs in `credit/domain/dto/`; entities never exposed; partner-API DTOs are a separate internal contract layer in `adapter/`, never leaked to clients. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Global Jackson strategy; no `@JsonProperty` for standard fields. Partner adapters may map non-standard partner casing **inside** the adapter only. |
| IV. Multi-Tenant Isolation | ✅ PASS | All entities extend `OwnableBaseDomain` (`@TenantId ownerId`); tenant set by `SessionUserFilter` from `X-Workspace-ID`; controllers set context, services never mutate it. Borrower = workspace owner. |
| V. API Response Standardization | ✅ PASS | All app-facing endpoints return `ApiResponse<T>`. Webhook receiver returns a partner-expected ack shape (documented as an intentional, non-`ApiResponse` integration endpoint). |
| VI. Centralized Exception Handling | ✅ PASS | No business try/catch in controllers; typed `CreditException`/`ConsentRequiredException`/`PartnerException` bubble to the global handler. |
| VII. Efficient Data Loading | ✅ PASS | `@NamedEntityGraph` for application+events+consent; derived queries; `@Query` only for the signal-aggregate reads and reconciliation scans. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web UI deferred; if added, Angular Material 3 only. Tracked as follow-up. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `credit` bounded context; reads `payment`/`invoice`/`order` **only** via public service interfaces; partner integration isolated behind `LenderAdapter`. |
| X. Compose Multiplatform Parity | ✅ PASS | Shared logic/UI in `feature/credit/src/commonMain`; thin platform DI. Web parity tracked as follow-up. |
| XI. Security & Secrets Hygiene | ✅ PASS | Partner keys/mTLS certs in env/secret store; webhooks signature-verified; **prohibited-data list** enforced (no licence/KYC-image/bureau/PAN/raw-statement/score in any store). |
| Flyway | ✅ PASS | Versioned migration in **both** `mysql/` and `postgresql/`; `credit` added to `migrationModules`; next version via `flywayInfo`. |
| Offline `/sync` contract | ✅ N/A (by design) | Lifecycle is online + partner-gated (R11); it does **not** ride `/sync`. One pull-only status mirror on mobile is read-only. Documented exception, not a violation. |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% on state machine + consent gate + idempotency; mobile `check` + 3-target compile. |

**Result**: PASS — no violations. The offline-`/sync` exception and the partner-webhook (non-`ApiResponse`)
endpoint are deliberate, documented design decisions, not principle breaches. Complexity Tracking notes
the partner-integration layer.

## Project Structure

### Documentation (this feature)

```
specs/020-embedded-credit-bnpl/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 — LSP boundary, consent, partner abstraction, idempotency
├── data-model.md        # Phase 1 — entities, state machine, event tables, prohibited-data list
├── quickstart.md        # Phase 1 — exercise application→offer→disburse with a stub lender
├── contracts/
│   ├── README.md
│   ├── credit-app-actions.md      # apply / consent / accept-offer / status (ApiResponse<T>)
│   ├── lender-adapter.md          # internal LenderAdapter port contract (canonical offer/status)
│   └── partner-webhooks.md        # inbound webhook shapes + signature + idempotency
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
credit/
└── src/main/
    ├── kotlin/com/ampairs/credit/
    │   ├── domain/
    │   │   ├── model/          # CreditApplication, CreditApplicationEvent (append-only),
    │   │   │                   # CreditConsent (immutable), CreditSignalSnapshot, CreditProduct,
    │   │   │                   # CreditLine (BNPL), LoanMirror, Disbursement, RepaymentSchedule, RepaymentInstalment
    │   │   ├── enums/          # ApplicationStatus, CreditProductType, ConsentPurpose, ConsentStatus,
    │   │   │                   # LoanStatus, RepaymentStatus, LenderPartnerCode
    │   │   └── dto/            # request/response DTOs + converters (app-facing)
    │   ├── repository/         # Spring Data repos (+ @EntityGraph, append-only event repo)
    │   ├── service/            # CreditApplicationService (state machine), ConsentService,
    │   │                       # CreditSignalService (aggregate export), LoanMirrorService,
    │   │                       # ReconciliationService (@Scheduled), RepaymentReminderService
    │   ├── adapter/            # LenderAdapter (port), LenderRouter, {Partner}Adapter impls,
    │   │                       # canonical CreditOffer/LoanStatus/RepaymentSchedule, AA/OCEN client
    │   ├── controller/         # CreditController (app actions), PartnerWebhookController (signed)
    │   ├── config/             # Constants, PROHIBITED_DATA guard, partner config (env-bound)
    │   └── event/              # listeners on payment/invoice events for signal freshness (read-only)
    └── resources/db/migration/
        ├── mysql/V1.0.x__create_credit_module_tables.sql
        └── postgresql/V1.0.x__create_credit_module_tables.sql
# wiring: settings.gradle.kts (include "credit"); ampairs_service/build.gradle.kts
#         (implementation(project(":credit")) + "credit" in migrationModules)
# reads payment/invoice/order PUBLIC SERVICE INTERFACES only — no repo access across modules

# Mobile — ampairs-app/ (sibling repo) — ONLINE-ONLY feature
feature/credit/src/
├── commonMain/kotlin/com/ampairs/credit/
│   ├── data/api/          # CreditApi(+Impl), ApiUrlBuilder.creditUrl (live calls, no SyncDelegate)
│   ├── data/db/           # Room: LoanStatusCache only (pull-only mirror, synced=true)
│   ├── data/repository/   # CreditRepository (online; explicit Loading/RequiresInternet states)
│   ├── domain/            # Money (minor units), models, ConsentPurpose, status enums
│   ├── di/                # CreditModule.kt
│   └── ui/                # screens + ViewModels (offer, consent capture, KYC handoff webview,
│                          #   application status, repayment schedule + reminders)
├── androidMain/ iosMain/ desktopMain/   # CreditModule.{platform}.kt (@SingleIn(WorkspaceScope::class))
# wiring: settings.gradle.kts (:feature:credit); shared/ Routes + entry provider;
#         ModuleRegistry ("embedded-credit" → Route.Credit); data/common ApiUrlBuilder.creditUrl(...)
# NOTE: NOT registered as a SyncEntity / SyncDelegate — lifecycle is online-only (R11)
```

**Structure Decision**: Mobile + API. The backend `credit/` module mirrors existing bounded contexts in
layout but adds an `adapter/` package for the partner-integration port and a webhook controller. The
mobile `feature/credit/` deliberately departs from the offline-first template: **no SyncDelegate, no
push** — every state change is a live call; only a pull-only `LoanStatusCache` is persisted. Web (Angular)
is a tracked follow-up.

## Phased Delivery

### Phase 1 (MVP) — Term loan origination with one partner, signals + consent
- **Entities**: `CreditApplication` (+ `CreditApplicationEvent` append-only), `CreditConsent` (immutable),
  `CreditSignalSnapshot`, `CreditProduct`, `LoanMirror`, `Disbursement`, `RepaymentSchedule`/
  `RepaymentInstalment`.
- **Services**: `CreditApplicationService` (state machine R7), `ConsentService` (R6 — granular, revocable),
  `CreditSignalService` (R5 — aggregate-only export, consent-gated), `LoanMirrorService`,
  `ReconciliationService` (R10 `@Scheduled`).
- **Endpoints** (`/credit/v1/**`, all `ApiResponse<T>`): `POST /applications` (DRAFT),
  `POST /applications/{uid}/consent`, `POST /applications/{uid}/submit` (export signals + adapter submit),
  `GET /applications/{uid}` (status), `POST /applications/{uid}/accept-offer`, `GET /loans/{uid}`,
  `GET /loans/{uid}/repayments`; plus `POST /partner/webhooks/{partner}` (signed, idempotent — **not**
  `ApiResponse`).
- **Adapter**: `LenderAdapter` port + one real partner adapter + a **stub adapter** for tests/dev.
- **Events**: in-process listeners keep the latest signal snapshot fresh from `payment`/`invoice` events;
  outbound effects via the adapter only.
- **Mobile**: online-only screens — apply, capture consent, view offer + Key Fact Statement, KYC handoff
  (partner webview/redirect), application status, repayment schedule; pull-only status cache.
- **Compliance gates (tests)**: no signal export without a live matching `CreditConsent`; prohibited-data
  guard; webhook dedup/idempotency; "partner wins" reconciliation.

### Phase 2 — B2B BNPL line + multi-partner routing + AA rail
- `CreditLine` (approved/available/utilisation) + per-transaction **drawdown** against the line; BNPL at
  the merchant's purchase-order/payables flow (R8).
- `LenderRouter` with eligibility/geography rules + decline-fallback re-routing to a second partner (fresh
  consent).
- **Account Aggregator** consent initiation + handle exchange (raw statement AA→RE direct; R4); OCEN-LAP
  canonical mapping for OCEN-native lenders.
- Repayment reminders + delinquency-aware UX; revocation propagation to partner.

### Phase 3 — Consumer BNPL at checkout + web parity + analytics
- Consumer-facing BNPL at the merchant's counter (the customer is borrower; tighter KYC/consent at point
  of sale) — a distinct `CreditProduct`, same rail.
- Angular web parity (Material 3) for the merchant credit dashboard.
- Portfolio analytics for the merchant (utilisation, upcoming dues) — display-only, sourced from the
  mirror; **no** Ampairs-side scoring.

## Complexity Tracking

| Violation / Added complexity | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| `adapter/` partner-integration layer (port + per-RE impls + router) | REs differ and churn; multi-partner + fallback is required for approval rates and resilience | A single hard-wired NBFC client is a SPOF with no fallback on decline and couples the lifecycle to one partner's API |
| Append-only event tables (`CreditApplicationEvent`) | Loan origination is an async, partner-gated saga needing idempotent webhooks + audit + resumability | Boolean flags on a flat row cannot express the saga safely and lose the regulatory audit trail |
| Online-only mobile module (off the `/sync` contract) | The lifecycle is partner-gated and money-adjacent; offline authoring would create unreconcilable phantom loan state | Forcing `/sync` would fake offline capability the regulated flow cannot safely provide |
| Partner-webhook endpoint not wrapped in `ApiResponse<T>` | Partners require their own ack shape/signature contract | Wrapping in `ApiResponse` would break partner expectations; isolating it as a documented integration endpoint is cleaner |
