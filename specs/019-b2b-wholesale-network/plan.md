# Implementation Plan: B2B Wholesale Network (workspace-to-workspace ordering)

**Branch**: `claude/indian-retail-ecosystem-877med` (spec dir `019-b2b-wholesale-network`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/019-b2b-wholesale-network/spec.md`

## Summary

Build a **private B2B ordering graph between Ampairs workspaces** (Udaan/Jumbotail-style): a retailer
workspace connects to a distributor/wholesaler workspace that is also on Ampairs, browses that seller's
per-buyer **price-list**, and places a **purchase order** that — on the seller's acceptance — becomes a
sales `Order` in the seller's existing pipeline (order → invoice → payment ledger), with credit terms,
status round-trip and notifications.

The defining challenge is **cross-tenant data flow**: one transaction spans two `ownerId` tenants. The
plan contains that in a new `b2b` bounded context: a double opt-in **`B2bConnection`** is the consent
gate; a single **`B2bAccessGuard`** is the only place tenant isolation is deliberately crossed, always
`nativeQuery=true` and connection-scoped (rule 05). The buyer is mirrored as a **`Customer`** inside the
seller's workspace so the seller's customer-keyed order/invoice/credit/ledger machinery works unchanged.
PO *drafting* is offline-first (canonical `/sync`); *submission* is an online cross-tenant action. Full
rationale in [research.md](./research.md); entities in [data-model.md](./data-model.md); APIs in
[contracts/](./contracts/).

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA (incl. `nativeQuery` cross-tenant repos), Flyway,
Jackson (SNAKE_CASE), `core` (`OwnableBaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`,
`TenantContextHolder.withTenant`), Spring `ApplicationEventPublisher`; consumes `order`
(`EcomOrderIngestionService` pattern, `OrderStatusChangedEvent`, `OrderService`), `customer`
(`CustomerService` for mirror creation), `invoice` (`InvoiceFinalizedEvent`), `payment` (spec-013 ledger,
aging, credit), `workspace` (`WorkspaceMemberService` for role checks), `notification`
(`NotificationService`), `ecom` (`EcomListedProduct` as shareable catalog). Mobile — Room KMP, Ktor,
Metro DI, existing `data/sync`/`data/common`, `feature/order`/`feature/customer` references.
**Storage**: Backend — PostgreSQL/MySQL via Flyway; `Instant`→`TIMESTAMPTZ`/`TIMESTAMP`; money
`DECIMAL(19,4)`. Mobile — Room (workspace-scoped DB `b2b`) for buyer-side POs + cached catalog.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :b2b:test`), incl. **cross-tenant isolation
tests** (a non-connected/revoked tenant must be denied), consent state machine, PO→`Order` projection
idempotency, credit-gate. Mobile — `./gradlew :feature:b2b:check` + 3-target compile gates.
**Target Platform**: Backend service; Mobile Android/iOS/Desktop.
**Project Type**: Mobile + API — backend `b2b` module + KMP `feature/b2b`.
**Performance Goals**: Catalog browse + price-list apply renders without lag for thousands of items; PO
submit→seller-order projection completes synchronously within a normal request; sync batches 100/page.
**Constraints**: **Cross-tenant access only via `B2bAccessGuard` + `nativeQuery` + `ACTIVE` connection**;
never combine `@TenantId` with an explicit `workspaceId` param; consented field-minimised DTOs; revocation
is immediate; every cross-tenant access audited; offline draft / online submit split.
**Scale/Scope**: Per workspace tens–hundreds of connections, thousands of POs; ~7 backend entities, ~3–4
mobile sync entities, ~5 primary screens.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | All timestamps `Instant`; money `DECIMAL(19,4)`; no `LocalDateTime`. |
| II. DTO & Contract Isolation | ✅ PASS | Request/Response DTOs in `b2b/domain/dto/`; **cross-tenant DTOs are field-minimised** (no cost/MRP/other-buyer data); converters with validation. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Global Jackson strategy; no `@JsonProperty` for standard fields. |
| IV. Multi-Tenant Isolation | ⚠️ DELIBERATE CROSS-TENANT → justified | The feature's purpose is consented cross-tenant flow. All access funnels through `B2bAccessGuard` → `nativeQuery=true` repos scoped by an `ACTIVE` `B2bConnection`, with `TenantContextHolder.withTenant {}` for seller-context reads. Never mixes `@TenantId` with explicit `workspaceId`. All `b2b` entities extend `OwnableBaseDomain` where single-tenant; bi-tenant entities (`B2bConnection`, `B2bPurchaseOrder`) carry both ids explicitly and live only in `b2b`. Documented in Complexity Tracking. |
| V. API Response Standardization | ✅ PASS | All endpoints return `ApiResponse<T>`; sync pull `ApiResponse<PageResponse<T>>`. |
| VI. Centralized Exception Handling | ✅ PASS | No business try/catch in controllers; `B2bAccessGuard` throws typed `ConnectionNotActiveException`/`ForbiddenCrossTenantException` that bubble to the global handler. |
| VII. Efficient Data Loading | ✅ PASS | `@NamedEntityGraph` for PO+lines+link; catalog browse paginated; native cross-tenant queries are explicit and parameterised. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web B2B portal deferred; Angular Material 3 when added. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `b2b` context brokers; reuses `order`/`invoice`/`payment`/`customer`/`notification` via public service interfaces + events, never their repositories (cross-tenant native reads live in `b2b`'s own repos). |
| X. Compose Multiplatform Parity | ✅ PASS | Shared logic/UI in `feature/b2b/src/commonMain`; thin platform DI. |
| XI. Security & Secrets Hygiene | ✅ PASS | No secrets; standard JWT/workspace auth; cross-tenant audited. |
| Flyway | ✅ PASS | Migration in **both** `mysql/` and `postgresql/`; `b2b` in `migrationModules`; version via `flywayInfo`. |
| Canonical /sync | ✅ PASS | Buyer-side PO uses canonical `GET/POST /b2b/v1/purchase-orders/sync`; **submit** is a deliberate non-sync online action (like an import/UI action exception). |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% incl. isolation + consent + projection tests; mobile `check` + 3-target compile. |

**Result**: PASS with the **deliberate, consented cross-tenant deviation** from default `@TenantId`
isolation — the feature's raison d'être — fully contained in `B2bAccessGuard` and documented in Complexity
Tracking.

## Project Structure

### Documentation (this feature)

```
specs/019-b2b-wholesale-network/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 — design decisions + rationale
├── data-model.md        # Phase 1 — entities, connection/PO state machines, cross-tenant DTOs
├── quickstart.md        # Phase 1 — connect two workspaces, share a price-list, place + accept a PO
├── contracts/
│   ├── README.md
│   ├── b2b-connection.md        # connect/approve/revoke
│   ├── b2b-catalog.md           # guarded cross-tenant catalog + price-list browse
│   ├── b2b-purchase-order.md    # PO /sync + submit/accept/reject + status
│   └── b2b-pricelist.md         # seller price-list CRUD + assignment
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
b2b/
└── src/main/
    ├── kotlin/com/ampairs/b2b/
    │   ├── domain/
    │   │   ├── model/      # B2bConnection, B2bPriceList, B2bPriceListItem, B2bPurchaseOrder,
    │   │   │               # B2bPurchaseOrderLine, B2bOrderLink, B2bAuditLog
    │   │   ├── enums/      # ConnectionStatus, PoStatus, ConnectionInitiator, AccessDirection
    │   │   └── dto/        # request/response DTOs (cross-tenant ones field-minimised) + converters
    │   ├── repository/     # Spring Data repos; cross-tenant ones @Query(nativeQuery = true)
    │   ├── security/       # B2bAccessGuard (THE single cross-tenant gate), audit hooks
    │   ├── service/        # B2bConnectionService, B2bCatalogShareService, B2bPriceListService,
    │   │   │               # B2bPurchaseOrderService, B2bOrderIngestionService (PO→seller Order),
    │   │   │               # B2bMirrorCustomerService (mirror Customer on activation)
    │   ├── controller/     # B2bConnectionController, B2bCatalogController, B2bPurchaseOrderController, B2bPriceListController
    │   ├── listener/       # OrderStatusChangedListener (seller Order → PO status); InvoiceFinalizedListener (link invoice)
    │   └── config/         # B2bProperties
    └── resources/db/migration/
        ├── mysql/V1.0.x__create_b2b_module_tables.sql
        └── postgresql/V1.0.x__create_b2b_module_tables.sql
# wiring: settings.gradle.kts (include "b2b"); ampairs_service/build.gradle.kts
#         (implementation(project(":b2b")) + "b2b" in migrationModules)
# order module: reuse ingestion pattern (B2bOrderIngestionService mirrors EcomOrderIngestionService)

# Mobile — ampairs-app/ (sibling repo)
feature/b2b/src/
├── commonMain/kotlin/com/ampairs/b2b/
│   ├── data/api/          # B2bApi(+Impl), ApiUrlBuilder.b2bUrl(...)
│   ├── data/db/           # Room: B2bPurchaseOrder(+lines), cached shared-catalog entities + DAOs + B2bRoomDatabase
│   ├── data/repository/   # B2bPurchaseOrderRepository (local-only), B2bConnectionRepository
│   ├── domain/            # PO models, connection/Po status enums, price-list models
│   ├── di/                # B2bModule.kt
│   ├── sync/              # B2bPurchaseOrderSyncDelegate (draft POs); catalog/connection are online reads
│   └── ui/                # connections, catalog browse (with negotiated prices), build PO, my POs, incoming POs (seller)
├── androidMain/ iosMain/ desktopMain/   # B2bModule.{platform}.kt (@SingleIn(WorkspaceScope::class))
# wiring: settings.gradle.kts (:feature:b2b); SyncEntity.B2B_PURCHASE_ORDER; shared/ Routes + entry provider;
#         ModuleRegistry ("b2b-wholesale" → Route.B2b); data/common ApiUrlBuilder.b2bUrl(...)
```

**Structure Decision**: Mobile + API. The `b2b/` backend module mirrors existing bounded contexts but
adds a `security/` package containing the **single** `B2bAccessGuard` — the only audited place tenant
isolation is crossed. The mobile `feature/b2b/` is offline-first for PO drafting (one `SyncDelegate`) and
online for catalog/connection.

## Phased Delivery

### Phase 1 — MVP: connect, share price-list, place + accept a PO
- **Entities**: `B2bConnection`, `B2bPriceList`, `B2bPriceListItem`, `B2bPurchaseOrder`,
  `B2bPurchaseOrderLine`, `B2bOrderLink`, `B2bAuditLog`.
- **Connection**: `B2bConnectionService` — `POST /b2b/v1/connections` (request), `POST
  /b2b/v1/connections/{uid}/approve|reject|revoke`. On `ACTIVE`, `B2bMirrorCustomerService` creates the
  buyer's mirror `Customer` in the seller's workspace (via `CustomerService`, under `withTenant`).
- **Catalog/price-list**: `B2bPriceListService` CRUD + assignment; `B2bCatalogController.GET
  /b2b/v1/catalog?connection={uid}` — **guarded cross-tenant** read of the seller's `EcomListedProduct`
  with the connection's price-list applied server-side (field-minimised, no cost/MRP).
- **PO**: buyer drafts offline (`/b2b/v1/purchase-orders/sync`), `POST .../{uid}/submit` (online) →
  `B2bAccessGuard` re-checks consent + credit (spec-013 aging) → `B2bOrderIngestionService` projects a
  seller `Order` (`orderType="B2B"`, `customerId`=mirror) idempotent on `b2bPoRef`; `B2bOrderLink` records
  the correlation. Seller sees it as `PENDING_MERCHANT_REVIEW`; accept/reject via the seller's order flow.
- **Status round-trip**: `OrderStatusChangedListener` → update `B2bPurchaseOrder` status → notify buyer.
- **Notifications**: connection + PO events via `NotificationService`.
- **Mobile**: connections list, catalog browse with negotiated prices, build PO (offline), my POs.

### Phase 2 — Credit, ledger, returns, seller-side incoming-PO console
- Credit-limit gating on submit; invoice link via `InvoiceFinalizedListener` → buyer sees invoice +
  receivable mirrors into buyer's payment ledger party (R5/R7).
- Seller-side mobile "incoming POs" management; partial fulfilment, edits (`update`), B2B returns/credit
  notes flowing through the seller's existing adjustment path.
- Tiered pricing / MOQ / pack-size enforcement on PO lines.

### Phase 3 — Network discovery, multi-distributor, analytics
- Seller "connect code" / discoverable directory (opt-in), bulk buyer onboarding.
- Buyer ordering from multiple distributors; reorder suggestions (feeds spec 027 replenishment).
- B2B GMV/aging analytics (feeds spec 022 dashboard); optional embedded-credit/BNPL hook (spec 020).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Deliberate cross-tenant access bypassing `@TenantId` | The feature *is* a transaction spanning two tenants; rule 05 explicitly permits this via `nativeQuery=true` + explicit consent. Contained in one `B2bAccessGuard` checking an `ACTIVE` `B2bConnection`, role, direction, and auditing every access. | Staying purely single-tenant (rejected — then there is no B2B network at all). Per-tenant data duplication with async copy (rejected — two sources of truth, consent/state divergence, reconciliation cost). |
| Bi-tenant entities (`B2bConnection`, `B2bPurchaseOrder`) carry two workspace ids, not a single `@TenantId ownerId` | These rows belong to *both* parties / to the link itself; a single `ownerId` can't represent a relationship. They live only in the `b2b` module and are always accessed through the guard. | Forcing a single `ownerId` (rejected — would hide the row from the other legitimate party). |
| Buyer mirrored as a `Customer` in the seller's tenant | The seller's order/invoice/credit/ledger pipeline is customer-keyed; without a mirror, none of the reuse (the whole value) is possible. Flagged + back-referenced to the connection to avoid desync. | Teaching every downstream consumer that a `customerId` may be a workspace (rejected — invasive, leaks B2B concerns across modules). |
| PO `submit` is an online action off the canonical `/sync` contract | Cross-tenant projection + consent re-check + credit gate require the seller's live tenant state and cannot run offline. Drafting/editing remains offline-synced. | Fully-offline submit (rejected — impossible to project into another tenant offline). |
