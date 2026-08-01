# Phase 0 Research — WhatsApp Commerce (conversational ordering)

This feature brings **conversational commerce over WhatsApp** to every Ampairs workspace — 72% of Indian
product discovery happens on WhatsApp, and merchants want customers to browse, order and pay inside the
chat. It integrates the **WhatsApp Business Cloud API** (Meta), shares the workspace catalog into the
**WhatsApp product catalog**, drives interactive list/button messages, captures orders into the existing
`order` module, attaches a **UPI payment link** (reuse spec 016), pushes status updates, and reuses the
`notification` module's WhatsApp sending and template machinery. Each item: **Decision · Rationale ·
Alternatives considered**. These supersede inline assumptions in `spec.md`.

Core constraint up front: WhatsApp inbound messaging is **webhook-driven and always-online**. The
24-hour customer-service window, opt-in/consent rules and template policy are Meta's, not ours. Like
ONDC, the live conversation is a **backend** concern; the merchant's app surface is configuration +
monitoring + fulfilment (orders arrive through the normal offline `order` sync).

---

## R1. Extend `notification` vs new `whatsapp` bounded context

- **Decision**: A **new backend module `whatsapp`** (`com.ampairs.whatsapp`) owns the Cloud API
  integration, the inbound **webhook**, conversation **session state**, catalog mapping and the order-capture
  flow. It **reuses** the existing `notification` module strictly as the **outbound transport** (the
  `WHATSAPP` channel already exists in `NotificationChannel`) by implementing the missing
  `WhatsAppNotificationProvider` (there is currently no provider behind the enum). Inbound + commerce logic
  is too large and too stateful to live in `notification`, whose job is fire-and-forget queued delivery.
- **Rationale**: `notification` is a one-way delivery queue (`NotificationQueue`, retry, providers); it has
  no concept of inbound messages, conversation sessions, catalogs or order capture. WhatsApp commerce is a
  stateful, bidirectional, webhook-driven bounded context — rule 08 says it gets its own module. We still
  honour reuse: outbound sends go through `NotificationService` with a real WhatsApp provider, so template
  management and delivery tracking are shared.
- **Alternatives considered**: Put everything in `notification` (rejected — conflates a delivery queue with
  a commerce engine; bloats `notification` with sessions/catalog/orders). A standalone microservice
  (rejected for Phase 1 — modular monolith suffices; webhook latency is forgiving vs ONDC).

## R2. WhatsApp Business Cloud API directly vs a BSP

- **Decision**: Integrate the **Meta WhatsApp Business Cloud API directly** (Graph API
  `/{phone_number_id}/messages`, webhooks on the app's verify-token + `messages` field), with the
  integration abstracted behind a `WhatsAppGateway` interface so a **BSP** (Business Solution Provider —
  e.g. Gupshup, Twilio, WATI, AiSensy) can be slotted in later without touching commerce logic. Phase 1
  assumes one platform-level Meta app; each workspace connects its own **WABA** (WhatsApp Business Account)
  + phone number id via OAuth/Embedded Signup and stores its `phoneNumberId` + access token.
- **Rationale**: The Cloud API is free of per-message BSP markup and is the canonical surface; abstracting
  it means we can later add a BSP for managed onboarding/number provisioning. Per-workspace WABA is
  required so messages come from the merchant's own number (trust, branding, opt-in legitimacy).
- **Alternatives considered**: BSP-first (rejected for Phase 1 — adds a paid dependency and a second
  contract; keep optional behind the gateway). One shared platform number for all merchants (rejected —
  customers must see *the merchant's* number; opt-in and template approval are per-WABA).

## R3. Inbound webhook surface & signature verification

- **Decision**: A `WhatsAppWebhookController` at a **network-facing `/whatsapp/v1/webhook`**: `GET` for
  Meta's `hub.challenge` verification (verify-token), `POST` for inbound events (messages, statuses,
  template status). It is **excluded from `SessionUserFilter`** (no `X-Workspace-ID`/JWT) and authenticated
  by Meta's **`X-Hub-Signature-256`** HMAC-SHA256 over the raw body using the app secret. The workspace is
  resolved from the inbound `metadata.phone_number_id` → `WhatsAppAccount.workspaceId`, then tenant context
  is set via `TenantContextHolder.withTenant {}`. The webhook returns 200 immediately and processes
  asynchronously.
- **Rationale**: Meta calls our webhook with no notion of our workspace header; the phone number id is the
  only routing key. HMAC signature verification is mandatory to reject spoofed callbacks. Fast-ack +
  async-process is required because Meta retries on slow/failed responses.
- **Alternatives considered**: Require `X-Workspace-ID` (rejected — Meta won't send it). Process inbound
  synchronously (rejected — Meta retries on timeout, causing duplicate handling). Skip signature check
  (rejected — open spoofing surface).

## R4. Conversation session state & the 24-hour window

- **Decision**: A `WhatsAppConversation` entity per (workspace, customer phone) holds the **session state
  machine** (`IDLE → BROWSING → CART → AWAITING_ADDRESS → AWAITING_PAYMENT → ORDER_PLACED`), the working
  **cart** (a `WhatsAppCartItem` list keyed to catalog items), the last inbound timestamp (for the **24-hour
  customer-service window**), and the `optInStatus`. Inbound interactive replies (list/button selections)
  advance the state machine. Within 24h of the customer's last message, the merchant may send free-form
  session messages; outside it, only **approved templates** (R6).
- **Rationale**: WhatsApp commerce is a multi-turn conversation; without persisted per-customer session
  state, list/button replies (which only carry an id) can't be interpreted. The 24-hour window is a hard
  Meta policy that dictates whether a send is free-form or must be a template — the session must track it.
- **Alternatives considered**: Stateless handling (rejected — interactive reply ids are meaningless without
  the session that issued them). Store cart only in `ecom` `EcomCart` (rejected — `EcomCart` is
  storefront/session-token scoped; a chat cart is phone-scoped and conversation-bound; though it can reuse
  pricing logic).

## R5. Catalog mapping — Ampairs product → WhatsApp product catalog

- **Decision**: A `WhatsAppCatalogService` syncs the workspace's **`EcomListedProduct`** set into the
  **Meta Commerce catalog** bound to the WABA (via the Catalog/Commerce Manager Graph API: product feed of
  `retailer_id`, `name`, `price`, `currency=INR`, `image_url`, `availability`, `description`). Interactive
  messages then reference catalog `product_retailer_id`s (Single/Multi-Product Messages). A
  `WhatsAppCatalogItem` maps `EcomListedProduct.uid` ↔ Meta `retailer_id`/`product_id`. Catalog stays in
  lock-step via the existing `ProductCatalogChangedEvent` → a `whatsapp` listener.
- **Rationale**: WhatsApp's native commerce (product messages, in-chat cart) requires products to exist in
  the Meta catalog. Reusing `EcomListedProduct` (already the curated public surface) keeps WhatsApp, the
  storefront and ONDC consistent automatically. Mapping the id pair lets us translate an in-chat cart back
  to Ampairs products.
- **Alternatives considered**: Send only text/image lists without a Meta catalog (rejected — loses native
  product messages, in-chat cart, and "View catalog" CTA). Project from raw `Product` (rejected — publishes
  unlisted SKUs; `EcomListedProduct` is the intended surface).

## R6. Template management — reuse `notification` templates

- **Decision**: Outbound **template** messages (order confirmation, payment reminder, dispatch, delivery)
  are modelled as `notification` templates and sent via `NotificationService` through the new
  `WhatsAppNotificationProvider`. A `WhatsAppTemplate` record mirrors each Meta-approved template
  (`name`, `language`, `category` MARKETING/UTILITY/AUTHENTICATION, `status` PENDING/APPROVED/REJECTED,
  component/variable schema). Template approval status is updated from the webhook's
  `message_template_status_update` events. Free-form (in-window) replies bypass templates.
- **Rationale**: Outside the 24h window every send must be a pre-approved template; modelling them lets the
  merchant manage/submit templates and lets the backend pick template-vs-free-form correctly. Reusing
  `notification`'s template + delivery tracking avoids a parallel sender.
- **Alternatives considered**: Hardcode template names (rejected — approval status changes; categories
  affect pricing/policy). A separate WhatsApp template store divorced from `notification` (rejected —
  duplicates the existing template machinery).

## R7. Order capture — conversation cart → management `Order`

- **Decision**: When the customer confirms the chat cart (button reply / in-chat WhatsApp order webhook
  payload `order` object), a `WhatsAppOrderCaptureService` builds an `EcomOrderPlacedEvent`-shaped payload
  and drives the **existing `EcomOrderIngestionService.ingest(...)`** (or a thin delegate) to create a
  management `Order` (`orderType="WHATSAPP"`, `ecomOrderRef={conversation/order id}`, status
  `PENDING_MERCHANT_REVIEW`), idempotent on `ecomOrderRef`. The customer is matched/created as a `Customer`
  by phone (reuse `CustomerService`). Pricing/GST resolve through the same path ecom uses.
- **Rationale**: A confirmed chat order is a sales order and must enter the same fulfilment/invoice/ledger
  pipeline as ecom and ONDC orders. Reusing the idempotent `EcomOrderIngestionService` avoids a third
  order-creation code path. Phone-keyed customer match fits WhatsApp's identity (the phone *is* the id).
- **Alternatives considered**: A WhatsApp-only order table (rejected — forks fulfilment/invoice/ledger).
  Map to `EcomCart`/`EcomOrder` (rejected — storefront-scoped; the management `Order` is the right grain,
  same as ONDC R7).

## R8. Payment — reuse spec 016 UPI payment links

- **Decision**: On order confirmation (or merchant action), generate a **UPI payment link via spec 016**
  (`016-upi-collection-payment-links` — a `payment`-module UPI link/intent) for the order amount and send
  it in the chat (free-form if in-window, else a UTILITY template with the link). Settlement is handled by
  spec 016's webhook → on success the spec-013 `payment` ledger posts the receipt against the customer; the
  `whatsapp` module listens for the settlement event and pushes a "payment received" confirmation + advances
  the conversation to `ORDER_PLACED`.
- **Rationale**: Payment collection is already (or imminently) solved by spec 016/013; WhatsApp commerce
  should *consume* it, not reinvent a payment integration. The link-in-chat flow is exactly how Indian
  WhatsApp commerce closes the loop.
- **Alternatives considered**: Build a WhatsApp-specific payment integration (rejected — duplicates spec
  016). WhatsApp Pay native (deferred — limited availability/rollout in India; the abstraction allows
  adding it later). Cash-on-delivery only (supported as a non-payment-link path, but link is the default).

## R9. Opt-in / consent

- **Decision**: A customer must be **opted-in** before the merchant may message them; opt-in is captured
  via (a) the customer messaging first (implicit, opens the 24h window), or (b) an explicit opt-in flow
  (keyword/QR/website). `WhatsAppConversation.optInStatus` (`OPTED_IN | OPTED_OUT | UNKNOWN`) gates all
  *merchant-initiated* template sends; a `STOP`/opt-out keyword sets `OPTED_OUT` and blocks further
  marketing. Consent + opt-out are auditable.
- **Rationale**: Meta policy and Indian DPDP-style consent norms require opt-in for business-initiated
  messaging and honouring opt-out; violating it risks WABA suspension. The session is the natural place to
  track it.
- **Alternatives considered**: Message anyone with a phone number (rejected — policy violation, ban risk).
  No opt-out handling (rejected — mandatory).

## R10. Status updates back to the customer

- **Decision**: The management `Order`'s lifecycle (`OrderStatusChangedEvent` — confirmed, packed,
  dispatched, delivered) drives **outbound WhatsApp updates** to the customer: free-form if the 24h window
  is open, else the appropriate approved UTILITY template. A `whatsapp` listener subscribes to
  `OrderStatusChangedEvent` and resolves the conversation by `ecomOrderRef`.
- **Rationale**: Closing the loop with order status is the headline WhatsApp-commerce benefit and reuses
  the order event stream already powering ecom/ONDC status. Window-aware send keeps it policy-compliant.
- **Alternatives considered**: SMS-only status (rejected — loses the conversational thread). Poll order
  status (rejected — event-driven is already available).

## R11. Offline boundary & the merchant app's role

- **Decision**: **The live conversation is not offline-synced.** WhatsApp config (WABA connection, catalog
  sync toggle, template list, opt-in stats, conversation/inbox view) is surfaced in the app via **plain
  authenticated `/whatsapp/v1/...` REST**, not a Room `SyncDelegate`. WhatsApp-originated **orders** arrive
  in the app through the **existing `order` `/sync`** (`orderType="WHATSAPP"`), so the merchant's
  offline-first fulfilment is unchanged. A light `feature/whatsapp` (or a tab in `feature/order`/an
  existing `feature/notification`) shows the WhatsApp inbox + config.
- **Rationale**: Inbound webhooks and the 24h window are always-online; Room-mirroring a live chat thread
  adds nothing and can't work offline. The order outcome, however, is a normal order the merchant fulfils
  offline.
- **Alternatives considered**: Model conversations/messages as `SyncDelegate` entities (rejected —
  high-churn, server-authoritative, online-only; sync is the wrong tool). Drive sending from the device
  (rejected — tokens, signatures, window state live on the backend).

## R12. Idempotency & duplicate suppression

- **Decision**: Each inbound message carries a Meta `message id` (`wamid...`); processing is idempotent on
  it (unique constraint on a `WhatsAppInboundMessage` log). Order capture stays idempotent on
  `ecomOrderRef`. Outbound sends are tracked by Meta message id + delivery status from `statuses` webhook
  events, mirrored into `NotificationQueue.providerMessageId`.
- **Rationale**: Meta redelivers webhook events on any non-200; without dedup we'd double-process messages
  and create duplicate orders. The `wamid` is the natural idempotency key.
- **Alternatives considered**: No dedup (rejected — duplicate orders/replies). Dedup on payload hash
  (rejected — `wamid` is the canonical, cheaper key).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Module placement | New `whatsapp` bounded context; `notification` reused as outbound transport only (R1) |
| Cloud API vs BSP | Meta Cloud API direct behind a `WhatsAppGateway`; BSP slot-in later; per-workspace WABA (R2) |
| Inbound webhook | `/whatsapp/v1/webhook`, HMAC-SHA256 verified, excluded from `SessionUserFilter`, async (R3) |
| Session & 24h window | `WhatsAppConversation` state machine + phone-scoped cart + window/opt-in tracking (R4) |
| Catalog mapping | `EcomListedProduct` → Meta Commerce catalog; `WhatsAppCatalogItem` id map (R5) |
| Templates | `notification` templates + `WhatsAppNotificationProvider`; approval status from webhook (R6) |
| Order capture | Chat cart → reuse `EcomOrderIngestionService` → management `Order` (`orderType="WHATSAPP"`) (R7) |
| Payment | Reuse spec 016 UPI links → spec 013 ledger on settlement (R8) |
| Opt-in/consent | `optInStatus` gate on merchant-initiated sends; honour STOP/opt-out, audited (R9) |
| Status updates | `OrderStatusChangedEvent` → window-aware WhatsApp send (R10) |
| Offline boundary | Conversation online-only; orders via existing `order` sync; config via plain REST (R11) |
| Idempotency | `wamid`-keyed inbound dedup; order idempotent on `ecomOrderRef` (R12) |
