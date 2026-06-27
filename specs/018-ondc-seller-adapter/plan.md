# Implementation Plan: ONDC Seller Adapter (Beckn BPP)

**Branch**: `claude/indian-retail-ecosystem-877med` (spec dir `018-ondc-seller-adapter`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/018-ondc-seller-adapter/spec.md`

## Summary

Make every Ampairs workspace an **ONDC Seller Network Participant (SNP)** — a Beckn **BPP** — so its
catalog is discoverable and its orders are transactable on India's open commerce network. A new backend
`ondc` bounded context implements the Beckn protocol surface (receive `select`/`init`/`confirm`/`status`/
`track`/`cancel`/`update`/`rating`, send the matching signed `on_*` callbacks; respond `on_search` to
gateway `search`), the ONDC registry **subscription** lifecycle (Ed25519/X25519 keys, site-verification,
registry lookup), a **catalog projection** from the existing `EcomListedProduct` set into the ONDC RET
taxonomy, **GST/HSN/price/serviceability** mapping via the `tax` and `inventory` services, **order
ingestion** that reuses the existing `EcomOrderIngestionService` to create a management `Order`
(`orderType="ONDC"`), and a **settlement** ledger (full RSF deferred).

Technical approach honours the protocol's nature: ONDC is **always-online and callback-driven**, so the
Beckn surface is a backend **webhook** authenticated by Ed25519 signature (not `X-Workspace-ID`), with a
synchronous `ACK` + asynchronous signed callback. **Nothing in the protocol path is offline-synced.** The
mobile side is configuration + monitoring only; the *outcome* — an order to fulfil — flows through the
normal offline-first `order` `/sync` path the merchant already uses. Full rationale in
[research.md](./research.md); entities in [data-model.md](./data-model.md); APIs in [contracts/](./contracts/).

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), Spring `@Async` /
`ApplicationEventPublisher`, `core` (`OwnableBaseDomain`, `ApiResponse`, `TenantContextHolder`),
**BouncyCastle** (Ed25519 sign/verify, X25519, BLAKE2b-512), an HTTP client (`RestClient`/`WebClient`)
for registry lookup + outbound `on_*` callbacks; consumes `ecom` (`EcomListedProduct`,
`ProductCatalogChangedEvent`, `CatalogSyncService`), `order` (`EcomOrderIngestionService`,
`OrderStatusChangedEvent`), `tax` (`TaxRule` composition), `product`/`inventory`
(`InventoryStockService`). Mobile — read-only `/ondc/v1/...` REST via Ktor; existing `feature/order`
sync for ONDC-originated orders.
**Storage**: Backend — PostgreSQL/MySQL via Flyway; timestamps `TIMESTAMPTZ`/`TIMESTAMP` (`Instant`);
money `DECIMAL(19,4)`; raw Beckn JSON persisted as `TEXT`/`jsonb` for audit/replay. Mobile — no new Room
DB; ONDC config is fetched live.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :ondc:test`), incl. signature round-trip,
schema-validation, idempotency, and `confirm`→`Order` ingestion; ONDC **pre-prod log-verification** test
scenarios run against the staging registry. Mobile — `./gradlew :feature:ondc:check`.
**Target Platform**: Backend service (Linux, publicly reachable HTTPS endpoint for gateway callbacks);
Mobile Android/iOS/Desktop (monitoring UI only).
**Project Type**: Mobile + API — backend module is primary; mobile is a thin monitoring surface.
**Performance Goals**: Synchronous `ACK` within the ONDC latency budget (well under ~1 s); `on_search`
catalog build for a few-thousand-SKU provider returns within the network's response window; callback
delivery retried with backoff.
**Constraints**: **Always-online** protocol path (no offline); every inbound/outbound message
signature-verified + schema-validated; `quote.breakup` MUST foot to `quote.price`; idempotent on
`(transaction_id, message_id, action)`; prod transactions gated on `SUBSCRIBED` + env match; keys
encrypted at rest, never in `keys/`.
**Scale/Scope**: Per workspace one `OndcSubscription`, one provider, thousands of items; ~7–9 backend
entities; Phase 1 = discovery + order; Phase 2 = fulfilment/issue-grievance; Phase 3 = RSF.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | All timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP`; money `DECIMAL(19,4)`; no `LocalDateTime`. |
| II. DTO & Contract Isolation | ✅ PASS | Beckn payloads are their own request/response DTOs in `ondc/domain/dto/beckn/`; internal entities never exposed; converters with validation. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Beckn JSON is snake_case (matches global Jackson). The few ONDC-namespaced keys (`@ondc/org/...`) use explicit `@JsonProperty` (documented non-standard case per rule 03). |
| IV. Multi-Tenant Isolation | ⚠️ PARTIAL → justified | Beckn endpoints have **no `X-Workspace-ID`** (network-facing). Tenant is resolved from `bpp_id`/provider id, then set via `TenantContextHolder.withTenant {}` before any repo access. All `ondc` entities extend `OwnableBaseDomain`. Deviation documented in Complexity Tracking. |
| V. API Response Standardization | ✅ PASS | Internal `/ondc/v1/admin|config` endpoints return `ApiResponse<T>`. Beckn endpoints return the protocol-mandated `{message:{ack}}` shape (external contract, not ours) — documented exception. |
| VI. Centralized Exception Handling | ✅ PASS | Config/admin endpoints let exceptions bubble. Beckn handlers translate failures to `NACK` with an error block (protocol requirement) inside the handler, not business try/catch in controllers. |
| VII. Efficient Data Loading | ✅ PASS | `@NamedEntityGraph` for order+links+settlement; catalog projection paginated; registry key cache. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web ONDC console deferred; when added, Angular Material 3 only. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `ondc` context; reuses `ecom`/`order`/`tax`/`inventory` via public service interfaces + Spring events, never repositories. |
| X. Compose Multiplatform Parity | ✅ PASS | Mobile monitoring UI shared in `commonMain`; thin platform DI. |
| XI. Security & Secrets Hygiene | ✅ PASS | Ed25519/X25519 private keys encrypted at rest via env-provided KEK; never committed, never in `keys/`; registry creds via env. |
| Flyway | ✅ PASS | Versioned migration in **both** `mysql/` and `postgresql/`; `ondc` added to `migrationModules`; next version via `flywayInfo`. |
| Canonical /sync | ✅ N/A (justified) | ONDC is off-`/sync` by design (always-online protocol). Documented like `tax`/`file` exceptions. |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% on signing/mapping/ingestion; ONDC pre-prod scenarios; mobile `check`. |

**Result**: PASS with two **documented protocol-driven deviations** (no `X-Workspace-ID` on Beckn paths;
Beckn response shape ≠ `ApiResponse`). Both are inherent to speaking an external standard and are tracked
in Complexity Tracking, not principle violations of our own surfaces.

## Project Structure

### Documentation (this feature)

```
specs/018-ondc-seller-adapter/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 — design decisions + rationale
├── data-model.md        # Phase 1 — entities, Beckn payload mappings, state machines
├── quickstart.md        # Phase 1 — staging onboarding + a search→confirm walkthrough
├── contracts/
│   ├── README.md
│   ├── beckn-bpp.md         # /ondc/v1/beckn/* receive + on_* send contracts
│   ├── ondc-config.md       # /ondc/v1/config (subscription, serviceability, category map)
│   └── ondc-settlement.md   # settlement report endpoints
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
ondc/
└── src/main/
    ├── kotlin/com/ampairs/ondc/
    │   ├── domain/
    │   │   ├── model/      # OndcSubscription, OndcProvider, OndcCategoryMapping, OndcServiceability,
    │   │   │               # BecknTransaction, BecknOrderLink, BecknCallbackLog, OndcSettlement
    │   │   ├── enums/      # SubscriberType, SubscriptionStatus, OndcEnvironment, BecknAction, CallbackStatus
    │   │   └── dto/
    │   │       ├── beckn/  # Context, SearchMessage, OnSearch, Select, OnSelect, Confirm, OnConfirm, … (protocol DTOs)
    │   │       └── config/ # subscription/serviceability/category request+response DTOs (+ converters)
    │   ├── repository/     # Spring Data repos (+ @EntityGraph)
    │   ├── service/        # OndcSubscriptionService, OndcCatalogMapper, OndcCategoryService,
    │   │   │               # OndcServiceabilityService, OndcSettlementService, OndcOrderIngestionService
    │   ├── beckn/          # BecknSigner (Ed25519/BLAKE-512), BecknRegistryClient (lookup cache),
    │   │   │               # BecknCallbackDispatcher (@Async, retry), BecknSchemaValidator
    │   ├── handler/        # SelectHandler, InitHandler, ConfirmHandler, StatusHandler, … (per action)
    │   ├── controller/     # BecknController (/ondc/v1/beckn/*), OndcConfigController, OndcSettlementController
    │   ├── listener/       # OrderStatusChangedListener → unsolicited on_status; ProductCatalogChangedListener (catalog dirty)
    │   ├── config/         # OndcProperties (registry URLs per env), BecknPathSecurityConfig (allow-list)
    │   └── web/            # /ondc-site-verification.html challenge controller (unauthenticated)
    └── resources/db/migration/
        ├── mysql/V1.0.x__create_ondc_module_tables.sql
        └── postgresql/V1.0.x__create_ondc_module_tables.sql
# wiring: settings.gradle.kts (include "ondc"); ampairs_service/build.gradle.kts
#         (implementation(project(":ondc")) + "ondc" in migrationModules);
#         workspace SessionUserFilter skip-list += "/ondc/v1/beckn", "/ondc-site-verification.html"

# Mobile — ampairs-app/ (sibling repo) — MONITORING/CONFIG ONLY, no SyncDelegate
feature/ondc/src/
├── commonMain/kotlin/com/ampairs/ondc/
│   ├── data/api/          # OndcApi(+Impl), ApiUrlBuilder.ondcUrl(...)  (plain authed reads/writes)
│   ├── domain/            # OndcSubscriptionStatus, ServiceabilityConfig, CategoryMapping models
│   ├── di/                # OndcModule.kt
│   └── ui/                # onboarding status, serviceability editor, category-map, live ONDC order list, settlement report
# ONDC-originated orders surface through the EXISTING feature/order /sync pull (orderType="ONDC"),
# not a new sync entity. wiring: settings.gradle.kts (:feature:ondc); shared/ Routes + entry provider;
# ModuleRegistry ("ondc-seller" → Route.Ondc); data/common ApiUrlBuilder.ondcUrl(...)
```

**Structure Decision**: Mobile + API, backend-dominant. The `ondc/` module mirrors existing bounded
contexts structurally but adds `beckn/` (crypto + transport) and `handler/` (per-action) packages unique
to protocol work. The mobile `feature/ondc/` is intentionally **not** offline-sync — it is a config +
monitoring surface; fulfilment reuses `feature/order`.

## Phased Delivery

### Phase 1 — MVP: discover + transact on staging (search → confirm → order)
- **Entities**: `OndcSubscription`, `OndcProvider`, `OndcCategoryMapping`, `OndcServiceability`,
  `BecknTransaction`, `BecknOrderLink`, `BecknCallbackLog`.
- **Onboarding**: `OndcSubscriptionService` — generate Ed25519/X25519 keys, serve
  `/ondc-site-verification.html`, register with the **staging** registry, cache `/lookup`. Endpoints:
  `POST /ondc/v1/config/subscription`, `GET /ondc/v1/config/subscription`.
- **Signing/transport**: `BecknSigner`, `BecknRegistryClient`, `BecknCallbackDispatcher`,
  `BecknSchemaValidator`.
- **Beckn surface** (`/ondc/v1/beckn`): receive `search`→send `on_search` (catalog via
  `OndcCatalogMapper` from `EcomListedProduct`); `select`→`on_select` and `init`→`on_init` (quote +
  GST via `tax`, stock via `inventory`); `confirm`→`on_confirm` (settlement_details) →
  `OndcOrderIngestionService` → management `Order` (`orderType="ONDC"`, reuse
  `EcomOrderIngestionService` idempotency). `status`→`on_status`.
- **Events**: listen `OrderStatusChangedEvent` → unsolicited `on_status`; listen
  `ProductCatalogChangedEvent` → mark catalog dirty.
- **Config**: serviceability + category mapping CRUD (`/ondc/v1/config/...`).
- **Mobile**: onboarding status + ONDC order list (orders arrive via existing `order` sync).
- **Gate**: pass ONDC **pre-prod** RET test scenarios; prod gated on `SUBSCRIBED` + env.

### Phase 2 — Fulfilment, cancel/update, ratings, issue & grievance
- `track`/`on_track`, `cancel`/`on_cancel`, `update`/`on_update` (e.g. quantity/fulfilment changes),
  `rating`/`on_rating`. Map fulfilment state transitions (`Packed → Order-picked-up → Out-for-delivery →
  Order-delivered`) onto management `Order` status, driving `on_status`.
- **IGM** (Issue & Grievance Management) `/issue`/`/on_issue`, `/issue_status` skeleton.
- Catalog incremental publish on `ProductCatalogChangedEvent` (push `on_search` to subscribed BAPs / full
  catalog refresh).

### Phase 3 — Settlement & reconciliation (RSF), multi-domain, prod scale
- `OndcSettlement` ledger → RSF `/settle`/`/on_settle`, `/recon`/`/on_recon`; feed collected amounts into
  the spec 013 payment ledger (`LedgerEntry`).
- Additional ONDC domains (beyond `RET10`), key rotation automation, callback-dispatch durability
  (optional Kafka bridge mirroring `EcomOrderKafkaProducer`).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Beckn endpoints lack `X-Workspace-ID` and are excluded from `SessionUserFilter` | ONDC authenticates participants by Ed25519 signature against the registry, not by our workspace header; the gateway/BAP will never send `X-Workspace-ID`. Tenant is resolved from `bpp_id`/provider id then set via `TenantContextHolder.withTenant {}`. | Requiring the header (rejected — not part of Beckn; every call would 401). |
| Beckn responses are `{message:{ack}}` / `on_*`, not `ApiResponse<T>` | The wire format is dictated by the external Beckn standard; we cannot wrap it. Internal `/ondc/v1/config|admin` endpoints still use `ApiResponse<T>`. | Wrapping in `ApiResponse` (rejected — non-compliant, BAP would reject). |
| ONDC off the canonical `/sync` contract | The protocol is live, online and bidirectional; offline Room mirroring is impossible and pointless. Config is server-authoritative; orders reach the app through the existing `order` sync. | Forcing a `SyncDelegate` (rejected — same class of exception as `tax`/`file`). |
| Per-workspace subscriber id (not one shared platform subscriber) in Phase 1 | ONDC seller liability + bank settlement are per-merchant; a shared subscriber would commingle catalogs/settlement across tenants. | Shared platform "seller-on-record" subscriber (deferred — a deliberate future model, not Phase 1). |
