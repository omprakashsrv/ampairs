# Implementation Plan: WhatsApp Commerce (conversational ordering)

**Branch**: `claude/indian-retail-ecosystem-877med` (spec dir `023-whatsapp-commerce`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/023-whatsapp-commerce/spec.md`

## Summary

Bring **conversational commerce over WhatsApp** to every Ampairs workspace: connect the merchant's
**WhatsApp Business Account (WABA)**, sync the catalog into the **Meta Commerce catalog**, let customers
browse via interactive **list/button/product** messages, build a **cart over chat**, capture the
confirmed order into the existing `order` module, attach a **UPI payment link** (reuse spec 016), and push
**order-status updates** back into the thread — all policy-compliant with opt-in and the 24-hour window.

A new backend `whatsapp` bounded context owns the **inbound webhook**, conversation **session state**,
catalog mapping and order capture. It **reuses** the `notification` module purely as the outbound
transport (implementing the missing `WhatsAppNotificationProvider` behind the existing
`NotificationChannel.WHATSAPP`), and reuses the proven `EcomOrderIngestionService` to turn a chat cart
into a management `Order` (`orderType="WHATSAPP"`). Like ONDC, **the live conversation is always-online and
not offline-synced** — the merchant's app surface is config + inbox + fulfilment, and orders reach the app
through the normal offline `order` `/sync`. Full rationale in [research.md](./research.md); entities in
[data-model.md](./data-model.md); APIs in [contracts/](./contracts/).

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), Spring `@Async`/
`ApplicationEventPublisher`, an HTTP client (`RestClient`/`WebClient`) for the Meta Graph API, HMAC-SHA256
verification, `core` (`OwnableBaseDomain`, `ApiResponse`, `TenantContextHolder`); consumes `notification`
(`NotificationService`, `NotificationChannel.WHATSAPP`, `NotificationQueue`, templates), `ecom`
(`EcomListedProduct`, `ProductCatalogChangedEvent`), `order` (`EcomOrderIngestionService`,
`OrderStatusChangedEvent`), `customer` (`CustomerService` phone match), `payment` (spec 016 UPI links /
spec 013 ledger settlement event), `product`/`tax` (pricing/GST via the ecom path). Mobile — read/write
`/whatsapp/v1/...` via Ktor; existing `feature/order` sync for WhatsApp-originated orders.
**Storage**: Backend — PostgreSQL/MySQL via Flyway; `Instant`→`TIMESTAMPTZ`/`TIMESTAMP`; money
`DECIMAL(19,4)`; raw webhook payloads persisted as `TEXT`/`jsonb` for audit/replay. Mobile — no new Room
DB; WhatsApp config fetched live.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :whatsapp:test`), incl. webhook signature
verification, session state-machine transitions, 24h-window template selection, opt-out handling, and
chat-cart→`Order` ingestion idempotency. Mobile — `./gradlew :feature:whatsapp:check`.
**Target Platform**: Backend service (publicly reachable HTTPS for Meta webhooks); Mobile
Android/iOS/Desktop (config + inbox + fulfilment).
**Project Type**: Mobile + API — backend-dominant; mobile is config/monitoring + normal order fulfilment.
**Performance Goals**: Webhook fast-ack (200 returned immediately, processed async); interactive reply →
next message round-trip feels instant; catalog feed sync for thousands of items batched.
**Constraints**: **Always-online** conversation path (no offline); every webhook HMAC-verified;
merchant-initiated sends gated on opt-in + 24h-window (template vs free-form); idempotent on Meta `wamid`
and order `ecomOrderRef`; access tokens encrypted at rest (rule 10).
**Scale/Scope**: Per workspace one WABA, thousands of conversations; ~6–8 backend entities; Phase 1 =
inbound + catalog + order capture; Phase 2 = payment + status loop; Phase 3 = campaigns/automation.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | All timestamps `Instant`; money `DECIMAL(19,4)`; no `LocalDateTime`. |
| II. DTO & Contract Isolation | ✅ PASS | Meta payloads are their own DTOs in `whatsapp/domain/dto/meta/`; internal entities never exposed; converters with validation. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Meta JSON is snake_case (matches global Jackson); the few non-standard keys use explicit `@JsonProperty` (documented). |
| IV. Multi-Tenant Isolation | ⚠️ PARTIAL → justified | The webhook has **no `X-Workspace-ID`** (Meta-facing). Tenant is resolved from inbound `phone_number_id` → `WhatsAppAccount`, then set via `TenantContextHolder.withTenant {}`. All `whatsapp` entities extend `OwnableBaseDomain`. Documented in Complexity Tracking. |
| V. API Response Standardization | ✅ PASS | Internal `/whatsapp/v1/config|inbox` endpoints return `ApiResponse<T>`. The webhook returns Meta's expected 200/`hub.challenge` (external contract) — documented exception. |
| VI. Centralized Exception Handling | ✅ PASS | Config endpoints let exceptions bubble; the webhook fast-acks 200 and handles failures async with logging, not business try/catch in the controller. |
| VII. Efficient Data Loading | ✅ PASS | `@NamedEntityGraph` for conversation+cart+messages; inbox paginated; catalog feed batched. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web WhatsApp console deferred; Angular Material 3 when added. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `whatsapp` context; reuses `notification`/`ecom`/`order`/`customer`/`payment` via public service interfaces + Spring events, never repositories. |
| X. Compose Multiplatform Parity | ✅ PASS | Mobile config/inbox UI shared in `commonMain`; thin platform DI. |
| XI. Security & Secrets Hygiene | ✅ PASS | WABA access tokens + app secret encrypted at rest via env KEK; never committed; webhook HMAC-verified. |
| Flyway | ✅ PASS | Migration in **both** `mysql/` and `postgresql/`; `whatsapp` in `migrationModules`; version via `flywayInfo`. |
| Canonical /sync | ✅ N/A (justified) | Conversation path is off-`/sync` by design (always-online webhook). Orders reach the app via existing `order` sync. Documented like `tax`/`file`. |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% on webhook/session/ingestion; mobile `check`. |

**Result**: PASS with two **documented platform-driven deviations** (no `X-Workspace-ID` on the Meta
webhook; webhook response shape ≠ `ApiResponse`), inherent to consuming Meta's external contract; tracked
in Complexity Tracking.

## Project Structure

### Documentation (this feature)

```
specs/023-whatsapp-commerce/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 — design decisions + rationale
├── data-model.md        # Phase 1 — entities, conversation state machine, Meta payload mappings
├── quickstart.md        # Phase 1 — connect a WABA, sync catalog, run a browse→order→pay walkthrough
├── contracts/
│   ├── README.md
│   ├── whatsapp-webhook.md      # GET verify + POST inbound (Meta contract)
│   ├── whatsapp-config.md       # /whatsapp/v1/config (account, catalog sync, templates, opt-in)
│   └── whatsapp-inbox.md        # conversation/inbox read + merchant reply
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
whatsapp/
└── src/main/
    ├── kotlin/com/ampairs/whatsapp/
    │   ├── domain/
    │   │   ├── model/      # WhatsAppAccount, WhatsAppConversation, WhatsAppCartItem,
    │   │   │               # WhatsAppCatalogItem, WhatsAppTemplate, WhatsAppInboundMessage
    │   │   ├── enums/      # ConversationState, OptInStatus, TemplateStatus, TemplateCategory, MessageDirection
    │   │   └── dto/
    │   │       ├── meta/   # WebhookEvent, InboundMessage, InteractiveReply, SendMessageRequest, … (Meta DTOs)
    │   │       └── config/ # account/template/catalog config request+response DTOs (+ converters)
    │   ├── repository/     # Spring Data repos (+ @EntityGraph)
    │   ├── gateway/        # WhatsAppGateway (iface) + CloudApiWhatsAppGateway (Meta Graph API); BSP impl later
    │   ├── service/        # WhatsAppConversationService (state machine), WhatsAppCatalogService,
    │   │   │               # WhatsAppOrderCaptureService, WhatsAppTemplateService, WhatsAppOptInService
    │   ├── provider/       # WhatsAppNotificationProvider (implements notification's provider iface)
    │   ├── controller/     # WhatsAppWebhookController (/whatsapp/v1/webhook), WhatsAppConfigController, WhatsAppInboxController
    │   ├── listener/       # OrderStatusChangedListener → status reply; ProductCatalogChangedListener → catalog feed;
    │   │   │               # PaymentSettledListener (spec 016/013) → "payment received" reply
    │   ├── security/       # MetaSignatureVerifier (X-Hub-Signature-256)
    │   └── config/         # WhatsAppProperties (Meta app id/secret, verify token), WebhookPathSecurityConfig (allow-list)
    └── resources/db/migration/
        ├── mysql/V1.0.x__create_whatsapp_module_tables.sql
        └── postgresql/V1.0.x__create_whatsapp_module_tables.sql
# wiring: settings.gradle.kts (include "whatsapp"); ampairs_service/build.gradle.kts
#         (implementation(project(":whatsapp")) + "whatsapp" in migrationModules);
#         workspace SessionUserFilter skip-list += "/whatsapp/v1/webhook"
# notification module: WhatsAppNotificationProvider plugs into the existing NotificationProvider registry

# Mobile — ampairs-app/ (sibling repo) — CONFIG/INBOX ONLY, no SyncDelegate
feature/whatsapp/src/
├── commonMain/kotlin/com/ampairs/whatsapp/
│   ├── data/api/          # WhatsAppApi(+Impl), ApiUrlBuilder.whatsappUrl(...)  (plain authed reads/writes)
│   ├── domain/            # WABA connection status, conversation/inbox models, template models
│   ├── di/                # WhatsAppModule.kt
│   └── ui/                # connect WABA, catalog-sync toggle, template list, opt-in stats, conversation inbox, merchant reply
# WhatsApp-originated orders surface through the EXISTING feature/order /sync pull (orderType="WHATSAPP").
# wiring: settings.gradle.kts (:feature:whatsapp); shared/ Routes + entry provider;
#         ModuleRegistry ("whatsapp-commerce" → Route.WhatsApp); data/common ApiUrlBuilder.whatsappUrl(...)
```

**Structure Decision**: Mobile + API, backend-dominant. The `whatsapp/` module mirrors existing bounded
contexts but adds `gateway/` (Meta transport, BSP-swappable), `security/` (HMAC verify) and a `provider/`
that plugs into `notification`'s provider registry. The mobile `feature/whatsapp/` is config + inbox only;
fulfilment reuses `feature/order`.

## Phased Delivery

### Phase 1 — MVP: connect WABA, sync catalog, capture an order from chat
- **Entities**: `WhatsAppAccount`, `WhatsAppConversation`, `WhatsAppCartItem`, `WhatsAppCatalogItem`,
  `WhatsAppInboundMessage`.
- **Connect**: `WhatsAppConfigController` — `POST /whatsapp/v1/config/account` (WABA `phone_number_id` +
  token via embedded signup), `GET .../account`. Webhook verify (`GET /whatsapp/v1/webhook`).
- **Inbound**: `WhatsAppWebhookController` (`POST`) → `MetaSignatureVerifier` → resolve workspace by
  `phone_number_id` → async `WhatsAppConversationService` state machine; persist `WhatsAppInboundMessage`
  (idempotent on `wamid`).
- **Catalog**: `WhatsAppCatalogService` syncs `EcomListedProduct` → Meta Commerce catalog;
  `WhatsAppCatalogItem` id map; `ProductCatalogChangedListener` keeps it fresh.
- **Browse + cart + order**: interactive list/button/product messages via `CloudApiWhatsAppGateway`; on
  confirm, `WhatsAppOrderCaptureService` → reuse `EcomOrderIngestionService` → management `Order`
  (`orderType="WHATSAPP"`, phone-matched `Customer`), idempotent on `ecomOrderRef`.
- **Mobile**: connect WABA, catalog-sync toggle, conversation inbox (orders arrive via existing order sync).

### Phase 2 — Payment link, status loop, templates, opt-in
- `WhatsAppTemplate` + `WhatsAppNotificationProvider`: outbound via `NotificationService`, template-vs-
  free-form chosen by 24h window; template approval status from `message_template_status_update` webhook.
- **Payment**: generate spec-016 UPI link on confirm, send in chat; `PaymentSettledListener` (spec 013/016)
  → "payment received" reply + advance conversation; COD path.
- **Status loop**: `OrderStatusChangedListener` → window-aware status updates.
- **Opt-in**: `WhatsAppOptInService` — opt-in capture, STOP/opt-out handling, gating of merchant-initiated
  sends; auditable.

### Phase 3 — Campaigns, automation, BSP, multi-number
- Bulk MARKETING template campaigns to opted-in customers (segment from `customer`/spec 022 analytics).
- Automated re-engagement / abandoned-cart nudges (window-aware).
- Optional **BSP** gateway implementation behind `WhatsAppGateway`; multi-number / multi-WABA per workspace.
- WhatsApp Pay native (when broadly available) behind the payment abstraction.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Webhook lacks `X-Workspace-ID` and is excluded from `SessionUserFilter` | Meta calls the webhook with no notion of our workspace; the inbound `phone_number_id` is the only routing key. Tenant is resolved from it, then set via `TenantContextHolder.withTenant {}`, after HMAC verification. | Requiring the header (rejected — Meta won't send it; every call 401s). |
| Webhook response is Meta's `hub.challenge`/200, not `ApiResponse<T>` | The verify + delivery contract is dictated by Meta; internal `/whatsapp/v1/config|inbox` endpoints still use `ApiResponse<T>`. | Wrapping in `ApiResponse` (rejected — Meta verification would fail). |
| WhatsApp conversation off the canonical `/sync` contract | The conversation is live, webhook-driven and bounded by the 24h window; offline Room mirroring is impossible and pointless. Orders reach the app through the existing `order` sync. | Forcing a `SyncDelegate` (rejected — same class of exception as `tax`/`file`/ONDC). |
| Commerce logic in a new module, not in `notification` | `notification` is a one-way delivery queue with no inbound/session/catalog/order concepts; WhatsApp commerce is a stateful bidirectional context (rule 08). Reuse is preserved by sending outbound through `notification`'s `WHATSAPP` channel via a new provider. | Putting it all in `notification` (rejected — conflates a queue with a commerce engine). |
