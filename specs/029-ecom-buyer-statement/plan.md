# Implementation Plan: Ecom Buyer Account — invoices, statement & order↔invoice linking

**Branch**: `claude/store-workspace-access-tko2ey` (feature `029-ecom-buyer-statement`) | **Date**: 2026-08-20 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/029-ecom-buyer-statement/spec.md`

## Summary

Extend the storefront buyer account (which already exposes *orders* via
`GET /v1/ecom/account/orders`) with read-only access to the buyer's **invoices**, the
**order↔invoice link**, and their **money position** (outstanding, open bills + aging, and a
running account statement). No new persistence: invoices and the party ledger are read *through* the
`invoice` and `payment` modules via new thin `core` service interfaces (mirroring the existing
`OrderEcomService` / `EcomCustomerService`), and the order↔invoice link is a join over the existing
`EcomOrder.managementOrderRef == Order.uid == Invoice.orderRefId` chain. Access is gated by resolving
the buyer's login to a **linked CRM customer** (`partyUid`) — the same mechanism that already gates
order access — with no workspace membership and no `X-Workspace-ID`.

## Technical Context

**Language/Version**: Kotlin 2.4 / Java 21 (backend); Compose Multiplatform / Kotlin 2.4 (Phase 3 app)
**Primary Dependencies**: Spring Boot 4.1, Spring Data JPA, Hibernate (`@TenantId` filtering), Jackson (global SNAKE_CASE)
**Storage**: PostgreSQL (runtime/dev) + MySQL (parity) — **read-only** for this feature; no DDL
**Testing**: JUnit5 + Spring Boot test slices; Testcontainers for integration (`./gradlew testAll`)
**Target Platform**: Linux server (backend); Android/iOS/Desktop (Phase 3 app)
**Project Type**: web/mobile — multi-module Spring backend + separate KMP app repo
**Performance Goals**: Buyer reads are interactive; each endpoint ≤ 1 resolve + 1–2 bounded queries. Statement/open-bills scan the party's ledger/invoices only (bounded by the linked customer, not the workspace).
**Constraints**: No new tables/migrations; `ecom` may reach `invoice`/`payment` **only** via `core` interfaces; buyer never gains workspace membership; every read re-checked against the resolved `partyUid`.
**Scale/Scope**: Backend — 2 new `core` interfaces (+impls), ~6 buyer DTOs, 2 repo finders, 4 new endpoints + 1 extended response. App phase is optional/follow-on.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | How this feature complies |
|---|---|---|
| I. Type Safety (`Instant`) | ✅ | All DTO timestamps are `Instant`; no `LocalDateTime`. No new columns. |
| II. DTO & Contract Isolation | ✅ | New **buyer-safe** response DTOs in `core`; no JPA entity is returned. `InvoiceEcomService`/`PartyLedgerEcomService` map entities→DTOs. Buyer DTOs omit cost/margin/audit/tenant fields. |
| III. Global JSON (SNAKE_CASE) | ✅ | No `@JsonProperty`; standard camelCase fields. |
| IV. Multi-Tenant Isolation | ✅ | Tenant context set in the **controller** (`storefront.ownerId`) in try/finally; services never mutate it. `@TenantId` auto-filtering scopes every read. Buyer surface is exempted from `X-Workspace-ID` in `SessionUserFilter` (same as existing order endpoints). |
| V. API Response Standardization | ✅ | Every endpoint returns `ApiResponse<T>`; the invoice list uses `PageResponse`. |
| VI. Centralized Exception Handling | ✅ | No try/catch for business errors; `NOT_LINKED`→403 and wrong-party→404 bubble to the global handler. |
| VII. Efficient Data Loading | ✅ | Invoice detail reuses the invoice `@EntityGraph` for items; derived queries preferred. |
| VIII. Angular Material 3 | N/A | No web change in this feature. |
| IX. Module Boundaries | ✅ | Cross-module access only through `core` service interfaces; no cross-module repo/entity imports. Dependency edges (`ecom→core←invoice`, `ecom→core←payment`) already exist. |
| X. Compose Parity | ✅ (Phase 3) | App work lands in `shared/commonMain`; parity tracked here before merge. |
| XI. Security & Secrets | ✅ | No secrets; access is link-gated and tenant-scoped. |

**Result**: PASS — no violations, Complexity Tracking not required.

## Project Structure

### Documentation (this feature)

```
specs/029-ecom-buyer-statement/
├── plan.md              # This file
├── spec.md              # Feature spec (already present)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (read-model / DTO shapes; no schema change)
├── quickstart.md        # Phase 1 output (validation guide)
├── contracts/           # Phase 1 output (endpoint contracts)
│   └── buyer-account-api.md
└── tasks.md             # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root — backend)

```
core/src/main/kotlin/com/ampairs/core/service/
├── InvoiceEcomService.kt          # NEW interface + buyer invoice DTOs
└── PartyLedgerEcomService.kt      # NEW interface + buyer ledger DTOs

invoice/src/main/kotlin/com/ampairs/invoice/
├── service/InvoiceEcomServiceImpl.kt   # NEW — maps invoices → buyer DTOs (customerId-scoped)
└── repository/InvoiceRepository.kt     # + findByCustomerIdAndStatusIn(...), findByOrderRefIdAndStatusIn(...)

payment/src/main/kotlin/com/ampairs/payment/
└── service/PartyLedgerEcomServiceImpl.kt   # NEW — delegates to Statement/Outstanding/Aging + PartyBalance

ecom/src/main/kotlin/com/ampairs/ecom/
├── controller/CustomerAccountController.kt   # + invoices, invoices/{uid}, orders/{ref}/invoices
├── service/EcomOrderService.kt               # order detail gains invoices[] (via InvoiceEcomService)
├── repository/EcomOrderRepository.kt         # + findByManagementOrderRef(orderRefId)
└── domain/dto/EcomOrderResponse.kt           # + invoices: List<BuyerInvoiceSummary>
```

### Source Code (KMP app repo — Phase 3, optional)

```
ampairs-app/feature/ecom/src/commonMain/kotlin/com/ampairs/ecom/
├── data/api/EcomApi.kt                 # + getInvoices/getInvoice/getOrderInvoices/getOutstanding/getStatement
├── data/repository/                    # BuyerInvoiceRepository, StatementRepository (live reads)
└── ui/account/                         # InvoiceListScreen, InvoiceDetailScreen, AccountStatementScreen + VMs
```

**Structure Decision**: Backend-first. The feature is a read-through over existing `invoice`/`payment`
data exposed to `ecom` through two new `core` interfaces, plus a join to surface the order↔invoice
link. No new module, no schema change. Phases 1a (invoices+linking) and 1b (money position) are
independent; the app phase follows and is tracked separately.

## Complexity Tracking

*No constitution violations — section intentionally empty.*
