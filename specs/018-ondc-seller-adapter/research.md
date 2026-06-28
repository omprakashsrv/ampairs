# Phase 0 Research — ONDC Seller Adapter (Beckn BPP)

This feature turns every Ampairs workspace into an **ONDC Seller Network Participant (SNP)** —
specifically a Beckn **BPP** (Buyer-side counterpart is the BAP; ONDC routes through a gateway). The
existing `ecom` module already owns the storefront catalog, cart, checkout and order; this adapter
exposes that same catalog/order capability onto the **open network** by speaking the Beckn protocol
over signed HTTP. Each item below: **Decision · Rationale · Alternatives considered**. These
supersede inline assumptions in `spec.md`.

The single most important constraint, stated up front: **ONDC is an always-online, callback-driven
protocol.** A BPP MUST receive `search` and respond `on_search` to a gateway, and accept
`select`/`init`/`confirm`/`status`/`track`/`cancel`/`update` and answer the matching `on_*` callback
to a specific BAP within seconds. None of this can ride the offline `/sync` engine — it is a backend
**webhook surface**, not a Room-mirrored entity. The mobile app's role is configuration, monitoring
and order fulfilment, never live protocol handling.

---

## R1. New `ondc` bounded context vs extending `ecom`

- **Decision**: A **new backend module `ondc`** (`com.ampairs.ondc`), a peer of `ecom`/`order`. It owns
  the Beckn transaction surface, the ONDC registry/subscription record, the catalog projection, signing
  keys, and settlement records. It **reuses** `ecom`'s catalog projection (`EcomListedProduct`,
  `CatalogSyncService`) and the `order` module's `EcomOrderIngestionService` ingestion path — it does not
  re-implement them.
- **Rationale**: ONDC is a distinct bounded context with its own protocol, lifecycle, compliance surface
  and external party (the network). Module-boundary rule (08) says new bounded contexts get their own
  module; mixing Beckn endpoints into `ecom` would couple the storefront (a self-hosted concern) to the
  network (a federated concern) and bloat `ecom`'s controllers. The two share *data* (the listed catalog,
  the management order) through public service interfaces + Spring `ApplicationEvent`s, never repositories.
- **Alternatives considered**: Put Beckn endpoints inside `ecom` (rejected — couples two contexts,
  violates rule 08, makes `ecom` carry crypto/registry concerns). A standalone microservice outside the
  monolith (rejected for Phase 1 — the modular monolith already gives clean boundaries; a separate
  deployable adds ops surface with no near-term payoff; revisit only if callback latency SLAs force it).

## R2. Beckn endpoint surface — unauthenticated path, auth via signature

- **Decision**: A `BecknController` exposing the BPP callback surface under a **network-facing base path
  `/ondc/v1/beckn`** (`/search` handler is actually received by the BPP only as `on_search` is *sent*;
  the BPP *receives* `/select`, `/init`, `/confirm`, `/status`, `/track`, `/cancel`, `/update`, `/rating`
  and *sends* the `on_*` callbacks to the BAP `bpp_uri`). These endpoints are **excluded from
  `SessionUserFilter`** (no `X-Workspace-ID`, no JWT) and instead authenticated by the Beckn
  `Authorization`/`X-Gateway-Authorization` **Ed25519 signature header** (R5). The workspace is resolved
  from `context.bpp_id` + the provider id in the message, **not** from a header.
- **Rationale**: ONDC participants authenticate each other by signature against the registry, not by our
  JWT/workspace header. `SessionUserFilter` enforces `X-Workspace-ID` and would 401 every gateway call;
  the Beckn paths must be allow-listed exactly like `/auth/v1` already is. Tenant context is set
  *programmatically* by the adapter after it resolves the provider, inside `TenantContextHolder.withTenant {}`.
- **Alternatives considered**: Force the gateway to send `X-Workspace-ID` (rejected — not part of Beckn,
  the network won't do it). Reuse the public storefront controller path (rejected — different auth model,
  different lifecycle).

## R3. Synchronous ACK + asynchronous callback (the Beckn two-phase exchange)

- **Decision**: Every BPP action endpoint returns an **immediate `{ "message": { "ack": { "status":
  "ACK" } } }`** synchronously (HTTP 200, within the network's ms-budget), persists the inbound request
  as a `BecknTransaction` row, then **asynchronously** computes and **POSTs the `on_*` callback** to the
  BAP's `bpp_uri` (from `context`) on a bounded worker pool. Correlation is by Beckn `transaction_id` +
  `message_id`.
- **Rationale**: Beckn is explicitly an asynchronous protocol: the synchronous response is only an `ACK`
  (or `NACK` on a malformed/unauthenticated request); the real answer (`on_search`, `on_select`,
  `on_confirm`, …) is a *separate* signed HTTP call back to the BAP. Conflating them (computing the whole
  catalog inside the request thread) blows the ACK latency budget and breaks on slow catalogs.
- **Alternatives considered**: Synchronous full response (rejected — not protocol-compliant, latency).
  Kafka for the async leg (deferred — the codebase already runs ecom ingestion on an in-process
  `ApplicationEvent` worker after commit; the same pattern, a `@Async` callback dispatcher, is enough for
  Phase 1; a Kafka bridge can later subscribe to the same internal event, mirroring `EcomOrderKafkaProducer`).

## R4. ONDC registry & subscription lifecycle

- **Decision**: A `OndcSubscription` entity (per workspace) holds `subscriber_id`, `subscriber_url`,
  `ukId` (unique key id), `signing_public_key`, `encryption_public_key`, network domain(s) (e.g.
  `ONDC:RET10`), `subscriber_type = BPP`, `status` (`DRAFT → INITIATED → UNDER_SUBSCRIPTION → SUBSCRIBED
  → EXPIRED`), `validFrom`/`validUntil`. Onboarding is a backend flow: generate Ed25519 + X25519 keypairs,
  serve the `/ondc-site-verification.html` challenge (signed request id), call the registry `/subscribe`,
  then resolve other participants' keys via the registry `/lookup` (cached). Staging vs pre-prod vs prod
  registries are environment config.
- **Rationale**: ONDC mandates registry subscription + the site-verification challenge before a
  participant can transact; key rotation and `validUntil` expiry must be tracked. Modelling it as an
  entity (not just config) lets the app surface onboarding state to the merchant and lets the backend
  refuse to sign when not `SUBSCRIBED`.
- **Alternatives considered**: Manual one-off onboarding per workspace via ops (rejected — doesn't scale to
  many workspaces, no in-app visibility). One shared platform subscriber for all workspaces (rejected for
  Phase 1 — ONDC seller liability and settlement are per-merchant; a shared subscriber id would commingle
  catalogs and bank settlement across tenants; revisit only if ONDC's "seller-on-record platform" model is
  adopted, which is a deliberate scope choice, see Complexity Tracking in plan.md).

## R5. Signing — Ed25519 request signing + registry-backed verification

- **Decision**: **Ed25519** detached signatures over the **BLAKE-512 digest** of the raw request body, with
  the Beckn `Authorization` header in the prescribed `Signature keyId="{subscriber_id}|{ukId}|ed25519",
  algorithm="ed25519", created=…, expires=…, headers="(created) (expires) digest", signature="…"` format
  (and `X-Gateway-Authorization` for gateway-relayed calls). Outbound: sign with our private key. Inbound:
  fetch the caller's `signing_public_key` from the registry `/lookup` (by `subscriber_id` + `ukId`), verify,
  reject with `NACK` on failure. Keys stored encrypted at rest; **never** in `keys/` (rule 10). Crypto via
  **BouncyCastle** (Ed25519, X25519, BLAKE2b-512) — already JVM-available.
- **Rationale**: This is the exact ONDC signing spec; there is no alternative wire format. Registry lookup
  (cached with TTL) is mandatory because keys rotate. BouncyCastle is the standard, audited provider and
  avoids hand-rolling EdDSA.
- **Alternatives considered**: JOSE/JWS (rejected — ONDC uses its own HTTP-Signatures-derived scheme, not
  JWS). Hardcode counterpart keys (rejected — they rotate; registry lookup is required). Roll our own
  Ed25519 (rejected — never hand-roll crypto).

## R6. Catalog mapping — Ampairs product → ONDC item/provider/fulfillment/category

- **Decision**: A `OndcCatalogMapper` projects the workspace's **`EcomListedProduct`** set (already the
  storefront-listed subset of `Product`) into a Beckn **`on_search` catalog**: the workspace becomes a
  `provider` (id = workspace `slug`/`uid`, with provider-level `locations`, `fulfillments`, `serviceability`
  `tags`); each `EcomListedProduct`/`ProductVariant` becomes an `item` with `id`, `descriptor`, `price`
  (`currency=INR`, `value`), `quantity.available`, `@ondc/org/returnable`, `@ondc/org/cancellable`,
  `@ondc/org/seller_pickup_return`, `@ondc/org/time_to_ship`, `@ondc/org/available_on_cod`. **HSN + GST**
  ride as item `tags` (`tax`/`hsn_code`) sourced from `Product.taxCode` → `tax` module `TaxRule`
  composition (CGST/SGST/IGST). Categories map to the **ONDC retail taxonomy** (e.g. `RET10` grocery) via
  an `OndcCategoryMapping` lookup keyed on `ProductCategory`.
- **Rationale**: `EcomListedProduct` is *already* the merchant-curated public catalog — reusing it means
  ONDC and the self-hosted storefront stay in lock-step automatically via the existing
  `ProductCatalogChangedEvent` → `CatalogSyncService` path. ONDC requires a strict taxonomy + the
  `@ondc/org/*` item tags; modelling the category map explicitly keeps it auditable and per-workspace
  overridable.
- **Alternatives considered**: Project from raw `Product` (rejected — would publish unlisted/internal SKUs
  to the open network; `EcomListedProduct` is the intended public surface). Auto-derive ONDC category from
  free-text `ProductCategory.name` (rejected — ONDC taxonomy codes are fixed; a fuzzy mapping causes
  search-mismatch rejections; require an explicit mapping with a sensible default).

## R7. Order ingestion — `confirm` → management `Order` reuse

- **Decision**: On a verified `confirm`, the adapter builds an `EcomOrderPlacedEvent`-shaped payload and
  drives the **existing `EcomOrderIngestionService.ingest(...)`** (or a thin `OndcOrderIngestionService`
  that delegates to the same logic) to create a management **`Order`** with `orderType = "ONDC"`,
  `ecomOrderRef = {beckn order_id}`, status `PENDING_MERCHANT_REVIEW`, idempotent on `ecomOrderRef`. A
  `BecknOrderLink` row maps Beckn `transaction_id`/`order_id` ↔ management `Order.uid` ↔ BAP `bpp_uri` so
  subsequent `status`/`track`/`cancel`/`update` and outbound `on_status` map both ways. Order
  state-machine changes (via `OrderStatusChangedEvent`) drive **unsolicited `on_status`** callbacks to the BAP.
- **Rationale**: A confirmed ONDC order *is* a sales order — it must land in the same `order` pipeline that
  feeds invoicing, inventory deduction (spec 014 `InventoryStockService.applySale`) and the payment ledger
  (spec 013), exactly as ecom orders do today. Reusing `EcomOrderIngestionService` (already idempotent on
  `ecomOrderRef`) avoids a second order-creation code path and inherits its event publishing.
- **Alternatives considered**: A separate ONDC order table that never enters the `order` module (rejected —
  forks fulfilment, breaks invoice/ledger reuse, double-counts inventory). Map ONDC order directly to an
  ecom `EcomOrder` (rejected — `EcomOrder` is storefront-scoped with a cart token; ONDC has no cart, the
  management `Order` is the right grain).

## R8. Serviceability, inventory & price freshness

- **Decision**: Serviceability is a per-provider config (`OndcServiceability`: pin-code ranges /
  GPS-radius / pan-India flag, plus `@ondc/org/time_to_ship`) surfaced into provider `tags`. `on_search`
  filters items by live availability from **`InventoryStockService`/`EcomListedProduct.stockStatus`**;
  `on_select`/`on_init` re-validate price + stock at quote time. Price is read from the listed catalog
  (`EcomListedProduct.price`/`mrp`); GST is computed at `on_init`/`on_confirm` via the `tax` module so the
  ONDC `quote.breakup` (item + tax + delivery) foots to `quote.price`.
- **Rationale**: ONDC penalises sellers for fulfilment failures, so serviceability and stock must reflect
  reality at quote time, not catalog-publish time. The breakup must foot exactly or the BAP rejects the
  quote. Reusing the existing inventory + tax services keeps one source of truth.
- **Alternatives considered**: Static serviceability (rejected — over-promises). Skip stock re-check at
  select (rejected — leads to confirm-then-cancel and ONDC penalty). Recompute tax on the client (rejected —
  GST authority is the `tax` module backend).

## R9. Settlement & reconciliation (RSF)

- **Decision**: Phase 1 stores **settlement terms in `on_confirm`** (`@ondc/org/settlement_details` —
  settlement bank account, IFSC, UPI, settlement window) and persists each order's settlement-relevant
  amounts (item, tax, delivery, commission, collected-by) into an `OndcSettlement` ledger row. The
  **Reconciliation & Settlement Framework (RSF)** message exchange (`/settle`, `/on_settle`, `/recon`,
  `/on_recon`) and automated payout reconciliation are **deferred to Phase 3**; Phase 1 emits a settlement
  report the merchant can reconcile manually and feeds collected amounts into the spec 013 payment ledger.
- **Rationale**: RSF is a heavy, separately-versioned ONDC spec; correct order transaction + a recorded
  settlement basis is the MVP. Wiring RSF before the transaction flow is proven is premature.
- **Alternatives considered**: Full RSF in Phase 1 (rejected — scope; RSF depends on a stable order flow).
  Ignore settlement entirely (rejected — `settlement_details` is mandatory in `on_confirm`; we must at
  least carry and persist it).

## R10. Idempotency, retries & duplicate suppression

- **Decision**: Idempotency key = Beckn **`(transaction_id, message_id, action)`**; a unique constraint on
  `BecknTransaction` makes re-delivered requests no-ops that re-emit the prior callback. Outbound `on_*`
  callbacks retry with bounded exponential backoff and are logged in `BecknCallbackLog` with
  delivery status. Order creation stays idempotent on `ecomOrderRef` (R7).
- **Rationale**: Networks redeliver; the gateway may retry; without dedup we'd create duplicate orders or
  send conflicting callbacks. `(transaction_id, message_id, action)` is the natural Beckn idempotency grain.
- **Alternatives considered**: Idempotency on `transaction_id` only (rejected — one transaction has many
  messages/actions). No persistence of inbound (rejected — can't replay callbacks or audit).

## R11. Offline boundary & the mobile app's role

- **Decision**: **Nothing in the Beckn transaction path is offline-synced.** The app's `feature/order`
  already receives ONDC-originated orders (they arrive as management `Order`s with `orderType="ONDC"`) via
  the existing order `/sync` pull — so fulfilment is offline-first *after* ingestion. ONDC-specific config
  (subscription status, serviceability, category mappings, settlement report) is surfaced in the app via
  **plain authenticated `/ondc/v1/...` REST reads**, not via a Room `SyncDelegate`. A small read-only
  `feature/ondc` (or a tab in `feature/ecom`) shows onboarding state + live ONDC order feed.
- **Rationale**: Protocol callbacks demand always-online, sub-second, signature-verified handling — the
  opposite of the offline Room model. But the *outcome* (an order to pack) is a normal order, so the
  merchant's offline fulfilment workflow is unchanged. Forcing ONDC entities onto `/sync` would be both
  impossible (live protocol) and pointless (config rarely changes).
- **Alternatives considered**: Model `OndcSubscription`/serviceability as `SyncDelegate` entities (rejected —
  they're singletons per workspace, server-authoritative, and changed online; a sync round-trip adds
  nothing). Run any Beckn handling on-device (rejected — keys, latency, NAT, single source of truth).

## R12. Staging / pre-prod onboarding & compliance gating

- **Decision**: Environment-scoped registry URLs (`staging`, `pre-prod`, `prod`) in config; an
  `OndcEnvironment` flag on the subscription. A workspace cannot go live on `prod` until it has passed the
  ONDC **log-verification / pre-prod test cases**; the backend gates `confirm` acceptance on
  `subscription.status == SUBSCRIBED && environment matches`. Beckn payload schema validation (against the
  ONDC RET JSON schemas) runs on every inbound/outbound message and is logged.
- **Rationale**: ONDC mandates passing pre-prod test scenarios before prod access; schema validation is how
  the network certifies a participant. Gating prevents a misconfigured workspace from transacting.
- **Alternatives considered**: Single environment (rejected — can't onboard/test without affecting prod).
  Skip schema validation (rejected — ONDC log verification requires schema-valid messages).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Module placement | New `ondc` bounded context, reuses `ecom`/`order` via services + events (R1) |
| Endpoint auth model | Network-facing `/ondc/v1/beckn/*`, signature-authed, excluded from `SessionUserFilter` (R2) |
| Sync vs async protocol | Synchronous `ACK`, asynchronous signed `on_*` callback on a worker pool (R3) |
| Registry & subscription | `OndcSubscription` entity + site-verification + registry lookup, env-scoped (R4) |
| Signing | Ed25519 / BLAKE-512 digest, Beckn `Authorization` header, BouncyCastle, registry-verified (R5) |
| Catalog mapping | `EcomListedProduct` → provider/item, RET taxonomy via `OndcCategoryMapping`, GST/HSN tags (R6) |
| Order ingestion | `confirm` → reuse `EcomOrderIngestionService` → management `Order` (`orderType="ONDC"`) (R7) |
| Serviceability/inventory/price | Per-provider config + live stock/tax re-check at select/init (R8) |
| Settlement | Persist `settlement_details` + `OndcSettlement` ledger now; full RSF in Phase 3 (R9) |
| Idempotency | `(transaction_id, message_id, action)` unique; order idempotent on `ecomOrderRef` (R10) |
| Offline boundary | Beckn path is online-only; outcome is a normal offline-synced `Order`; config via plain REST (R11) |
| Staging/compliance | Env-scoped registries, prod gated on pre-prod pass + schema validation (R12) |
