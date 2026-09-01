---
description: "Task list for WhatsApp Commerce (conversational ordering)"
---

# Tasks: WhatsApp Commerce (conversational ordering)

**Input**: Design documents from `/specs/023-whatsapp-commerce/`
**Prerequisites**: plan.md ✅, spec.md ✅ (clarified 2026-06-28), research.md ✅ (data-model.md / contracts/ to be authored — see Polish)

**Tests**: INCLUDED. The project's Constitution Check (plan.md) mandates ≥80% coverage on the
webhook / session / ingestion paths, and plan.md enumerates concrete test scenarios (webhook signature
verification, session state-machine transitions, 24h-window template selection, opt-out handling,
chat-cart→Order ingestion idempotency). Test tasks are therefore part of each story.

**Organization**: Tasks are grouped by user story. The two P1 stories are sequenced **US4 → US1**
because browsing/ordering (US1) requires a connected number and a synced catalog (US4).

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1 (browse+order), US2 (payment), US3 (status), US4 (connect+catalog), US5 (consent)
- All backend paths are under `ampairs/whatsapp/...`; mobile paths are in the sibling repo `ampairs-app/...`.

## Path Conventions (from plan.md — Mobile + API, backend-dominant)
- **Backend (this repo `ampairs/`)**: `whatsapp/src/main/kotlin/com/ampairs/whatsapp/{domain,repository,gateway,service,provider,controller,listener,security,config}/`
- **Backend migrations**: `whatsapp/src/main/resources/db/migration/{mysql,postgresql}/`
- **Backend tests**: `whatsapp/src/test/kotlin/com/ampairs/whatsapp/`
- **Mobile (sibling repo `ampairs-app/`)**: `feature/whatsapp/src/commonMain/kotlin/com/ampairs/whatsapp/{data/api,domain,di,ui}/`

> **⚠️ Two-repo scope:** Tasks **T022, T033, T047** are **mobile** work in the **separate `ampairs-app`
> repo** and are **out of scope for this backend PR** — they are listed here only for end-to-end
> traceability and MUST be executed/tracked on an `ampairs-app` branch (or filed as issues there), not on
> this `ampairs` branch. Every other task (T001–T052 + the T014a isolation test = 50 backend tasks) is
> backend work in this repo. A story's backend
> tasks are independently completable without its mobile task.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Stand up the new `whatsapp` bounded context and wire it into the build.

- [ ] T001 Create the `whatsapp` Gradle module: `whatsapp/build.gradle.kts` (depends on `core`; declares deps on `notification`, `ecom`, `order`, `customer`, `payment`, `product`, `tax` per plan.md) and the `com.ampairs.whatsapp.{domain,repository,gateway,service,provider,controller,listener,security,config}` package skeleton.
- [ ] T002 Wire the module: add `include("whatsapp")` in `settings.gradle.kts`; add `implementation(project(":whatsapp"))` and `"whatsapp"` to `migrationModules` in `ampairs_service/build.gradle.kts`.
- [ ] T003 [P] Add `WhatsAppProperties` in `whatsapp/.../config/WhatsAppProperties.kt` — Meta app id, app secret, webhook verify token, Graph API base URL — sourced from env vars (rule 10: no secrets committed).
- [ ] T004 [P] Create empty Flyway migration folders `whatsapp/src/main/resources/db/migration/{mysql,postgresql}/`; confirm the next free global version via `./gradlew :ampairs_service:flywayInfo`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Webhook intake, signature verification, tenant resolution, gateway, and the base entities every conversational story depends on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T005 Core Flyway migration `V1.0.x__create_whatsapp_module_tables.sql` in **both** `mysql/` and `postgresql/` — tables `whatsapp_account`, `whatsapp_conversation`, `whatsapp_inbound_message` (Instant→TIMESTAMPTZ/TIMESTAMP; `owner_id` for tenant; unique `wamid` on inbound; money DECIMAL(19,4) where present; raw payload as TEXT/jsonb).
- [ ] T006 [P] Enums in `whatsapp/.../domain/enums/`: `ConversationState` (IDLE, BROWSING, CART, AWAITING_ADDRESS, AWAITING_PAYMENT, ORDER_PLACED), `OptInStatus` (OPTED_IN, OPTED_OUT, UNKNOWN), `MessageDirection`, `TemplateStatus`, `TemplateCategory`.
- [ ] T007 [P] `WhatsAppAccount` entity (`OwnableBaseDomain`) + repository — `phoneNumberId`, `wabaId`, encrypted `accessToken`, catalog id, `catalogSyncEnabled`, connection status. Derived finder `findByPhoneNumberId(...)` (rule 06).
- [ ] T008 [P] `WhatsAppConversation` entity (`OwnableBaseDomain`) + repository — keyed by (workspace, customer phone); fields: `state: ConversationState`, `lastInboundAt: Instant` (24h window), `optInStatus`, `ecomOrderRef`. `@NamedEntityGraph` over cart+messages (rule 07/Constitution VII).
- [ ] T009 [P] `WhatsAppInboundMessage` entity (`OwnableBaseDomain`) + repository — `wamid` (unique), direction, type, raw payload, processed flag.
- [ ] T010 `MetaSignatureVerifier` in `whatsapp/.../security/` — HMAC-SHA256 over the raw request body using the app secret, compared against `X-Hub-Signature-256` (constant-time compare).
- [ ] T011 `WhatsAppGateway` interface + `CloudApiWhatsAppGateway` impl in `whatsapp/.../gateway/` — send text / interactive (list/button) / product messages and templates via Graph API `/{phone_number_id}/messages`; BSP-swappable. Meta request/response DTOs in `domain/dto/meta/` (rule 02/03).
- [ ] T012 `WhatsAppWebhookController` in `whatsapp/.../controller/` — `GET /whatsapp/v1/webhook` returns `hub.challenge` on verify-token match; `POST /whatsapp/v1/webhook` verifies signature (T010), **fast-acks 200**, dispatches processing async (`@Async`/`ApplicationEventPublisher`). Documented non-`ApiResponse` external contract (plan Complexity Tracking).
- [ ] T013 Add `/whatsapp/v1/webhook` to the `workspace` `SessionUserFilter` skip-list (`WebhookPathSecurityConfig` allow-list) so the Meta-facing webhook bypasses the `X-Workspace-ID`/JWT requirement.
- [ ] T014 Tenant resolution + inbound dedup: resolve workspace from `metadata.phone_number_id` → `WhatsAppAccount.workspaceId`, run handling inside `TenantContextHolder.withTenant {}` (rule 05 — never in a service); persist `WhatsAppInboundMessage` idempotently on `wamid` (skip duplicates) before processing.
- [ ] T014a [P] **Cross-workspace isolation test** `whatsapp/src/test/.../TenantIsolationTest.kt` (covers FR-021 / SC-007) — with two workspaces each owning a `WhatsAppAccount`/conversation/catalog item, assert that a webhook routed to workspace A's `phone_number_id` never reads or mutates workspace B's conversations, catalog mapping, or orders, and that `OwnableBaseDomain` tenant filtering blocks any cross-tenant repository read.

**Checkpoint**: Webhook verifies, signature-checks, fast-acks, resolves tenant, dedupes, and proves tenant isolation — stories can begin.

---

## Phase 3: User Story 4 - Connect WhatsApp number & keep catalog in sync (Priority: P1) 🎯 MVP (enabler)

**Goal**: A merchant connects their WABA + phone number and shares their listed catalog into WhatsApp; listed-product changes stay reflected. (FR-001..004, FR-002 variant grain.)

**Independent Test**: Connect a WhatsApp Business number from the app, enable catalog sharing, and verify connection status shows connected and listed products (one entry per variant/SKU) become browsable in a test chat.

### Tests for User Story 4

- [ ] T015 [P] [US4] Integration test `whatsapp/src/test/.../WebhookVerifyAndConnectTest.kt` — `GET /whatsapp/v1/webhook` challenge round-trip + `POST/GET /whatsapp/v1/config/account` happy path.
- [ ] T016 [P] [US4] Test `CatalogSyncTest.kt` — `EcomListedProduct` (with variants) maps to **one catalog item per variant/SKU**; `ProductCatalogChangedEvent` updates the feed.

### Implementation for User Story 4

- [ ] T017 [P] [US4] `WhatsAppCatalogItem` entity + repository + migration (`V1.0.x__create_whatsapp_catalog_item.sql`, mysql+postgresql) — maps `EcomListedProduct.uid`(+variant/SKU) ↔ Meta `retailer_id`/`product_id`.
- [ ] T018 [US4] `WhatsAppConfigController` `/whatsapp/v1/config/account` — `POST` (connect: `phone_number_id` + token via embedded signup), `GET` (status), `DELETE` (disconnect → stop send/receive, FR-004). Request/Response DTOs + converters in `domain/dto/config/`; returns `ApiResponse<T>` (rule 04).
- [ ] T019 [US4] `WhatsAppCatalogService` in `whatsapp/.../service/` — push the workspace's `EcomListedProduct` set to the Meta Commerce catalog as one product per variant/SKU (retailer_id, name, price, currency=INR, image_url, availability, description); persist the `WhatsAppCatalogItem` id map; batch for thousands of items.
- [ ] T020 [US4] `ProductCatalogChangedListener` in `whatsapp/.../listener/` — on `ProductCatalogChangedEvent`, re-sync affected catalog items (cross-module via event, not repository — rule 08).
- [ ] T021 [US4] Catalog-sync toggle in `WhatsAppConfigController` — `catalogSyncEnabled` on/off on `WhatsAppAccount`.
- [ ] T022 [P] [US4] **Mobile** (`ampairs-app` repo — out of scope for this backend PR; tracked for traceability): `feature/whatsapp` module — `WhatsAppApi(+Impl)` + `ApiUrlBuilder.whatsappUrl(...)`, connect-WABA + catalog-sync-toggle + connection-status UI in `ui/`; `WhatsAppModule.kt` DI; `settings.gradle.kts` + `shared/` Routes/entry provider + `ModuleRegistry("whatsapp-commerce" → Route.WhatsApp)`.

**Checkpoint**: Merchant can connect, see status, share catalog (per variant), and disconnect.

---

## Phase 4: User Story 1 - Customer browses & places an order over WhatsApp (Priority: P1) 🎯 MVP

**Goal**: Customer messages the number, browses the catalog, builds a cart, confirms, and a WhatsApp order is captured in a **pending merchant-review** state with the right items/totals/customer. (FR-005..011, FR-008a; depends on US4 + Foundational.)

**Independent Test**: From a WhatsApp account, message the connected number, browse, add items, confirm; verify a matching order appears in the merchant's order list (channel=WhatsApp, pending review) with correct items/quantities/totals and a phone-matched customer; verify the merchant can accept it into fulfilment.

### Tests for User Story 1

- [ ] T023 [P] [US1] Test `ConversationStateMachineTest.kt` — interactive replies advance IDLE→BROWSING→CART→AWAITING_ADDRESS→ORDER_PLACED; reply ids resolve against the issuing session.
- [ ] T024 [P] [US1] Test `OrderCaptureIdempotencyTest.kt` — chat cart → management `Order` is idempotent on `ecomOrderRef` (repeated confirmation creates exactly one order); phone-matched customer is reused, not duplicated.

### Implementation for User Story 1

- [ ] T025 [P] [US1] `WhatsAppCartItem` entity + repository + migration (`V1.0.x__create_whatsapp_cart_item.sql`, mysql+postgresql) — conversation-scoped, linked to `WhatsAppCatalogItem`, quantity.
- [ ] T026 [US1] `WhatsAppConversationService` in `whatsapp/.../service/` — the session state machine; interpret inbound interactive (list/button/product) replies against `WhatsAppConversation` state; mutate cart; transition states.
- [ ] T027 [US1] Outbound browse/cart messages via `WhatsAppGateway` — interactive product/list/button messages referencing catalog `product_retailer_id`s; cart-review message.
- [ ] T028 [US1] Unavailable-item handling — at add/review, flag items removed/out-of-stock (catalog item inactive) and leave the cart unchanged (spec edge case).
- [ ] T029 [US1] `WhatsAppOrderCaptureService` in `whatsapp/.../service/` — on confirm, build an `EcomOrderPlacedEvent`-shaped payload and drive the existing `EcomOrderIngestionService.ingest(...)` to create a management `Order` (`orderType="WHATSAPP"`, `ecomOrderRef`, status `PENDING_MERCHANT_REVIEW`); idempotent on `ecomOrderRef` (FR-008/008a/010).
- [ ] T030 [US1] Customer match/create by phone via `CustomerService` (no duplicate customer) (FR-009); pricing/GST resolve through the ecom path (FR-011).
- [ ] T031 [US1] Merchant-acceptance path — accept a `PENDING_MERCHANT_REVIEW` WhatsApp order so it enters normal fulfilment (FR-008a). Acceptance endpoint/action surfaced for the merchant (config/inbox or reuse order workflow).
- [ ] T032 [US1] `WhatsAppInboxController` `/whatsapp/v1/inbox` — paginated conversation/message read (`ApiResponse`/`PageResponse`, `@EntityGraph`) **and** merchant manual free-form reply (human takeover) gated on the 24h window (FR-022/022a).
- [ ] T033 [P] [US1] **Mobile** (`ampairs-app` repo — out of scope for this backend PR; tracked for traceability): conversation inbox + merchant reply UI in `feature/whatsapp/ui/`; verify WhatsApp orders surface in the existing `feature/order` `/sync` pull (`orderType="WHATSAPP"`) — no new SyncDelegate (plan R11).

**Checkpoint** 🎯 **MVP COMPLETE**: A customer can order over WhatsApp; the merchant sees, accepts, and fulfils it.

---

## Phase 5: User Story 2 - Pay via a link in the chat (Priority: P2)

**Goal**: After confirmation, the customer gets a UPI payment link (or COD); on settlement the payment is recorded and the customer is notified. (FR-012, FR-013; reuses spec 016/013.)

**Independent Test**: Place an order (US1), receive the payment link, complete payment; verify the order shows paid and the customer receives a confirmation message; verify COD proceeds without a link.

> **⚠️ Precondition (external dependency):** This story consumes **spec 016 (UPI collection / payment
> links)** and **spec 013 (payment ledger settlement event)**. Before starting T035, confirm both are
> implemented and live (the `payment`-module UPI link API + the settlement event are available). If they
> are not yet shipped, US2 is **blocked** — proceed with US1/US3/US5 first. The COD path (T037) has no
> such dependency and can land independently.

### Tests for User Story 2

- [ ] T034 [P] [US2] Test `PaymentLinkAndSettlementTest.kt` — link generated for the correct amount on confirm; settlement event → "payment received" reply + advance to `ORDER_PLACED`; COD path skips the link.

### Implementation for User Story 2

- [ ] T035 [US2] On confirm (or merchant action), generate a spec-016 UPI payment link for the order amount and send it in chat (free-form if in-window, else a UTILITY template with the link); persist the link reference on the conversation/order.
- [ ] T036 [US2] `PaymentSettledListener` in `whatsapp/.../listener/` — on the spec-013/016 settlement event, resolve the conversation by `ecomOrderRef`, send a "payment received" confirmation, and advance the state (cross-module via event — rule 08).
- [ ] T037 [US2] Cash-on-delivery path — confirm without a link, mark the order for collection on delivery (FR-012).
- [ ] T038 [US2] Migration if needed (`V1.0.x__add_whatsapp_payment_ref.sql`, mysql+postgresql) for the payment-link reference column.

**Checkpoint**: US1 + US2 work independently; the in-chat payment loop closes.

---

## Phase 6: User Story 3 - Order-status updates in the same thread (Priority: P2)

**Goal**: As the merchant advances the order, the customer receives status updates in the conversation, window-aware. (FR-014, FR-015.)

**Independent Test**: Advance an order through its statuses; verify the customer receives a message per meaningful status change, using an approved template when outside the recent-contact window.

### Tests for User Story 3

- [ ] T039 [P] [US3] Test `OrderStatusUpdateWindowTest.kt` — `OrderStatusChangedEvent` → window-open sends free-form, window-closed sends the approved UTILITY template; no out-of-window free-form is ever sent.

### Implementation for User Story 3

- [ ] T040 [US3] `OrderStatusChangedListener` in `whatsapp/.../listener/` — subscribe to `OrderStatusChangedEvent`, resolve the conversation by `ecomOrderRef`, and send the matching status update (confirmed/packed/dispatched/delivered).
- [ ] T041 [US3] 24h-window send selector — choose free-form vs approved template by `WhatsAppConversation.lastInboundAt`; withhold rather than violate policy if no suitable approved template exists (spec edge case + FR-015).

**Checkpoint**: US1–US3 each work independently; customers are kept informed.

---

## Phase 7: User Story 5 - Consent & opt-out (Priority: P2)

**Goal**: Merchant-initiated marketing requires explicit opt-in; transactional/order-status messages are allowed for the customer's own order; STOP/opt-out is honored and audited. (FR-016, FR-017, FR-015; templates via `notification`.)

**Independent Test**: Send an opt-out keyword from a customer; verify the merchant can no longer initiate marketing messages to them, transactional order updates still flow, and the opt-out is recorded.

### Tests for User Story 5

- [ ] T042 [P] [US5] Test `OptInOptOutGateTest.kt` — STOP keyword sets `OPTED_OUT` and blocks merchant-initiated **marketing**; transactional/order-status sends for an existing order remain allowed (FR-016); consent + opt-out events are recorded.

### Implementation for User Story 5

- [ ] T043 [P] [US5] `WhatsAppTemplate` entity + repository + migration (`V1.0.x__create_whatsapp_template.sql`, mysql+postgresql) — `name`, `language`, `category` (MARKETING/UTILITY/AUTHENTICATION), `status` (PENDING/APPROVED/REJECTED), component/variable schema.
- [ ] T044 [US5] `WhatsAppNotificationProvider` in `whatsapp/.../provider/` — implements the `notification` provider interface behind `NotificationChannel.WHATSAPP`; routes outbound through `NotificationService` (template + delivery tracking, mirrors `wamid`/status into `NotificationQueue.providerMessageId`).
- [ ] T045 [US5] Template approval status — handle `message_template_status_update` webhook events → update `WhatsAppTemplate.status`.
- [ ] T046 [US5] `WhatsAppOptInService` in `whatsapp/.../service/` — capture opt-in (implicit on customer-initiated/order = transactional consent; explicit flow = marketing consent), handle STOP/opt-out → `OPTED_OUT`, gate all merchant-initiated sends by category (marketing requires explicit opt-in; transactional allowed for an existing order), auditable (FR-016/017).
- [ ] T047 [P] [US5] **Mobile** (`ampairs-app` repo — out of scope for this backend PR; tracked for traceability): template list + opt-in stats UI in `feature/whatsapp/ui/`.

**Checkpoint**: All five stories independently functional; messaging is policy-compliant.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T048 [P] Author `data-model.md` (entities + conversation state machine + Meta payload mappings) and `contracts/` (webhook, config, inbox) referenced by plan.md but not yet created.
- [ ] T049 [P] Author `quickstart.md` — connect a WABA, sync catalog, run a browse→order→pay walkthrough.
- [ ] T050 Coverage gate — ensure ≥80% on webhook/signature, session state machine, and order ingestion (`./gradlew :whatsapp:test`); add unit tests for gaps.
- [ ] T051 [P] Encrypt WABA access token + app secret at rest (env KEK), confirm nothing sensitive is logged (rule 10 / Constitution XI).
- [ ] T052 Run `./gradlew :whatsapp:compileKotlin :whatsapp:test` and `./gradlew :ampairs_service:flywayInfo` to confirm migrations validate on PostgreSQL and MySQL.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (P1)** → no deps.
- **Foundational (P2)** → depends on Setup; **BLOCKS all stories** (webhook, signature, tenant resolution, base entities, gateway).
- **US4 (P3)** → depends on Foundational. The enabling P1 story.
- **US1 (P4)** → depends on Foundational **and US4** (needs a connected number + synced catalog to browse/order).
- **US2 (P5)**, **US3 (P6)** → depend on Foundational + US1 (operate on a captured order).
- **US5 (P7)** → depends on Foundational; provides the template + consent gate used by US2/US3 outbound, but is independently testable for the opt-out gate.
- **Polish (P8)** → after the desired stories are complete.

### Within Each User Story

- Tests written first and failing → Models/migration → Services → Endpoints/listeners → Integration.
- Backend before the mobile UI task for that story.

### Parallel Opportunities

- Setup: T003, T004 in parallel.
- Foundational: T006–T009 (enums + three entities, different files) in parallel; T010/T011 in parallel after entities.
- Within a story, all `[P]` tasks (entity/migration vs mobile vs test files) run in parallel; the service/endpoint tasks that share the conversation/session are sequential.
- Across stories: once Foundational + US4 land, US1 is the critical path; US5 (templates/consent) can be built in parallel by a second developer since it's largely independent files.

---

## Parallel Example: User Story 1

```bash
# Tests for US1 together:
Task: "ConversationStateMachineTest.kt"        # T023
Task: "OrderCaptureIdempotencyTest.kt"         # T024

# Independent files for US1 together:
Task: "WhatsAppCartItem entity + repo + migration"   # T025
Task: "Mobile inbox + reply UI (ampairs-app)"        # T033
```

---

## Implementation Strategy

### MVP First (US4 + US1)

1. Phase 1 Setup → Phase 2 Foundational (CRITICAL — blocks everything).
2. Phase 3 US4 (connect + catalog) → Phase 4 US1 (browse + order capture).
3. **STOP and VALIDATE**: a customer can order over WhatsApp; the merchant accepts and fulfils. This is the demoable MVP.

### Incremental Delivery

1. Foundational → US4 → US1 = MVP (Phase 1 of plan.md).
2. + US2 (payment) + US3 (status) + US5 (consent/templates) = plan.md Phase 2 — each tested independently.
3. Campaigns/automation/BSP/multi-number (plan.md Phase 3) are **out of scope** for this tasks set.

### Notes

- `[P]` = different files, no dependencies. `[Story]` maps each task to a user story for traceability.
- Every migration is written in **both** `mysql/` and `postgresql/`; pick the global version via `flywayInfo` (rule 07).
- Commit after each task or logical group; the conversation path is always-online (no `SyncDelegate`).
- The mobile tasks (T022, T033, T047) live in the sibling repo `ampairs-app/` and are tracked here for traceability; they may be split into that repo's own branch.
