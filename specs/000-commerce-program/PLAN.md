# Commerce Program Plan — Retail + Wholesale + Brands + Ecom Storefront

**Created**: 2026-06-05
**Status**: Draft — program roadmap (not a single feature)
**Scope decision**: **India-first, global-ready schema** — ship India-first (GST, ₹, Razorpay,
Shiprocket/Delhivery) but bake money-as-`Money`, an explicit sales-channel model, and the existing
tax-strategy seam into the schema now so going global is feature-flagged, not a rewrite.
**Repos**: `ampairs` (Spring Boot backend), `ampairs-app` (KMP/Compose), `ampairs-web` (Angular).

> This document is the orchestrator output: a grounded gap analysis + dependency-ordered roadmap.
> It deliberately does **not** implement anything. Each roadmap item becomes its own speckit feature
> (`/speckit.specify → clarify → plan → tasks → analyze → implement`). Feature **009 (Pricing)** is
> already drafted alongside this plan at `specs/009-commerce-pricing/spec.md`.

---

## 1. Where we actually are (grounded in the code, June 2026)

The original "40% done / half-built ecom" estimate was too pessimistic. A full read of both repos
shows the storefront platform is **production-grade end-to-end** — it shipped via
`specs/008-ecommerce-order-platform`. The real gaps are narrower and more specific than "build an
ecom platform."

### Already built — do NOT rebuild

| Capability | Backend (`ampairs`) | App (`ampairs-app`) |
|---|---|---|
| **Storefront** | `com.ampairs.ecom` — `Storefront` (slug, status, `accessMode` PUBLIC/RESTRICTED), publish/unpublish | `feature/ecom` — `StorefrontGateViewModel`, `EcomStorefrontScreen` |
| **Public catalog** | `EcomListedProduct`, `StorefrontPublicController` at `/v1/store/{slug}/**`; synced from product via Kafka (`CatalogSyncService`) | Cursor-based incremental pull (`CatalogRepository`, `EcomCatalogSyncDelegate`), `ListedProductEntity` |
| **Cart** | `EcomCart` + `EcomCartItem`, `CartService`, `CheckoutService` | Local optimistic mirror (`CartEntity`/`CartItemEntity`, `CartRepository`) reconciled at checkout |
| **Orders (storefront)** | `EcomOrder` + `EcomOrderLineItem` (carries `workspaceId`, `managementOrderRef`, `shipmentGroup`) | `OrdersScreen`, `OrderTrackingScreen`, `EcomOrderSyncDelegate` (pull-only) |
| **Addresses** | `CustomerAddress`, `CustomerAddressService` | Full CRUD push/pull (`AddressRepository`, `EcomAddressSyncDelegate`) |
| **Public tenant resolution** | **`StorefrontTenantInterceptor`** resolves tenant from slug, runs before `SessionUserFilter`, sets `TenantContextHolder` to `storefront.ownerId` — the hard multi-tenant-vs-public problem is **already solved and security-reviewed** | n/a |
| **Tax engine + strategy seam** | `TaxConfiguration.taxStrategy` already a string (`INDIA_GST` / `USA_SALES_TAX` / `UK_VAT`); two-layer master+workspace model; `componentComposition` JSON | `feature/tax` full CRUD |
| **Wholesale primitives** | `CustomerType` (`defaultCreditLimit`, `defaultCreditDays`), `CustomerGroup` (`defaultDiscountPercentage`, `priorityLevel`) | `feature/customer` group/type CRUD + import-from-master |
| **B2B order + invoice** | `com.ampairs.order` (`Order`/`OrderItem` with price snapshot, `ecomOrderRef` link), invoice module | `feature/order`, `feature/invoice` (sync **stubbed** — see gaps) |
| **Brands (as taxonomy)** | `brandId` on `Product`; `EcomTaxonomyImage` (BRAND/CATEGORY/SUBCATEGORY) | `BrandEntity` in `feature/product` |
| **Postgres migrations** | `db/migration/postgresql/` **already exists** (ecom is Postgres-only, V1.0.62–74); product has both `mysql/` + `postgresql/` | n/a |

### The actual gaps (this is what the program builds)

| Gap | Why it's a gap today | Roadmap feature |
|---|---|---|
| **Pricing engine** | One flat `price`/`mrp` per `EcomListedProduct`; wholesale is only a single `defaultDiscountPercentage` on the customer group. No price lists, no per-channel/per-group pricing, no MOQ, no slab/tier pricing, no resolution service. | **009 Pricing** ← drafted |
| **Sales channel model** | RETAIL vs WHOLESALE is nowhere on cart/order/storefront. Only `accessMode` (PUBLIC/RESTRICTED) exists, which is access control, not a price/tax channel. | 009 (introduces `SalesChannel`) + threaded through 011/012 |
| **Money consistency + currency** | `ecom` = `BigDecimal(19,4)`, `order`/`product` = `Double`, `ProductVariant` = `BigDecimal(15,2)`. **No currency field anywhere.** App is `Double` throughout. | **Cross-cutting decision D1** (below); enforced from 009 on |
| **Payments** | Only subscription billing exists. `EcomOrder` has no payment/transaction entity, no gateway. | 011 Payments |
| **Shipping / fulfillment** | `EcomOrderLineItem.shipmentGroup` is an orphan field — no zones, rates, courier, AWB, tracking. | 012 Shipping |
| **Promotions** | None — no coupons, no cart-level discounts. | 013 Promotions |
| **B2B offline order/invoice sync** | `OrderSyncDelegate`/`InvoiceSyncDelegate` return `Success(0)` no-ops; not registered with `@SyncEntityKey`. Wholesale order entry on the app can't sync. | 010 B2B order/invoice sync |
| **Reviews / ratings** | None. | 014 Reviews (optional, post-MVP) |
| **Search at scale** | Postgres `ILIKE`/FTS today; fine for MVP. | Deferred — revisit at scale |
| **Multi-currency / i18n catalog / VAT+sales-tax impls** | Schema gets the seams now (D1, tax strategy); concrete global impls deferred. | 015 Go-Global (post India-MVP) |

---

## 2. Cross-cutting decisions to lock BEFORE feature 009

These are expensive to change later. Decisions D3 and D4 are **already resolved by existing code** —
documented here so features don't re-litigate them.

### D1 — Money representation (the #1 thing to get right) — **DECISION**

Reality is inconsistent (Double vs two different BigDecimal scales, no currency). Standardize:

- **API / wire contract**: every monetary value is `{ "amount_minor": <Long>, "currency": "<ISO-4217>" }`
  (minor units as integer — paise/cents). Integers on the wire are unambiguous and safe for both the
  KMP and Angular clients; no float drift, no locale parsing.
- **Backend persistence**: `BigDecimal(19,4)` + a sibling `currency CHAR(3)` column. This **aligns to
  the scale `ecom` already uses**, so no lossy migration of live ecom data. New `pricing`/`payment`
  tables use it from the start. Legacy `order`/`product` `Double` columns get a **documented,
  non-blocking** migration to `BigDecimal(19,4)` + currency (tracked per feature, not a prerequisite).
- **KMP app**: introduce a `Money(minorUnits: Long, currency: String)` value class used at **all new
  commerce boundaries** (pricing, cart price snapshots, payment). Do **not** rewrite every legacy
  `Double` field at once — wrap at the boundary, migrate inward. This kills `Double` where new money
  math happens (the real risk zone) without a big-bang refactor.
- **Rule going forward**: no raw `Double`/un-currencied `BigDecimal` for money in any new code.

### D2 — Sales channel model — **DECISION**

Introduce `enum SalesChannel { RETAIL, WHOLESALE }` (extensible: `DISTRIBUTOR`, `B2B_MARKETPLACE`
later). Thread it through **from feature 009**, even though India-MVP may launch retail-first:

- A `Storefront` gains a `defaultChannel` (most storefronts = RETAIL; a wholesale/distributor
  storefront = WHOLESALE, complementing the existing RESTRICTED `accessMode`).
- A `PriceList` is scoped to a channel (+ optional customer group).
- `EcomCart` / `EcomOrder` / B2B `Order` carry the resolved `channel` + a **price snapshot** so the
  price the customer saw is the price they're charged.
- Retrofitting wholesale MOQ/tier pricing into a retail-only schema later is the expensive path —
  baking the seam in now is cheap.

### D3 — Public catalog vs tenant filter — **ALREADY SOLVED (document, don't rebuild)**

`StorefrontTenantInterceptor` already resolves the tenant from the storefront slug and sets
`TenantContextHolder` before `SessionUserFilter`, and `/v1/store/{slug}/**` is on the skip-list.
Pricing resolution on the storefront simply runs **inside that already-established tenant context**.
New work: the public price-resolution endpoint must (a) default to the storefront's `defaultChannel`,
and (b) optionally honor an authenticated customer's group for B2B/restricted storefronts. No new
tenant-security design required — reuse the interceptor.

### D4 — Postgres migration path — **ALREADY EXISTS (document)**

`db/migration/postgresql/` exists; `ecom` is Postgres-only, `product` ships both. New commerce
modules: **Postgres is primary**; add a `mysql/` variant only if the module must also run inside the
MySQL-backed monolith. (CLAUDE.md's "mysql only" note is stale — update it as part of 009.)

### D5 — Where pricing lives — **DECISION**

Pricing is needed by **both** B2B order entry (monolith `order`/`invoice`) **and** the storefront
(`ecom` service). Put the master data + resolution service in a **new management-monolith bounded
context `com.ampairs.pricing`** (product-adjacent), exposed to `order`/`invoice` via a public service
interface (per module-boundary rules), and **projected into the ecom read model via Kafka** — the
exact pattern `CatalogSyncService` already uses for catalog. This avoids a distributed pricing call on
the hot storefront path and keeps the resolution logic single-sourced.

### D6 — Payment gateway abstraction — **DECISION (for feature 011)**

One `PaymentGateway` port with `Razorpay` (India) + `Stripe` (global) adapters. Flow:
create-gateway-order → client SDK collects payment → **webhook confirms** (idempotent, signature-
verified) → order state transition. **Store only gateway tokens/refs — never PAN/CVV/card data**
(PCI: tokenization at the gateway only). Region-routed gateway selection.

---

## 3. Dependency-ordered roadmap (speckit features)

Each feature is scoped to **compile and ship independently** and ride the existing speckit pipeline.
Numbering continues from the real next number **009** (008 = ecom platform is already done).

```
009 Pricing engine        ── PREREQUISITE for real wholesale + accurate cart/order totals
        │                     (PriceList, PriceListItem, MOQ, slab tiers, SalesChannel,
        │                      resolution service, Kafka projection to ecom)  ← spec drafted
        ▼
010 B2B order/invoice sync ── implement the stubbed Order/Invoice SyncDelegates so wholesale
        │                     order entry works offline on the app (consumes 009 pricing)
        ▼
011 Payments              ── PaymentGateway port + Razorpay adapter (India-first; Stripe seam),
        │                     webhooks, refunds, idempotency; wires into EcomOrder + checkout
        ▼
012 Shipping / fulfillment ── zones, rate rules, courier port (Shiprocket/Delhivery), AWB +
        │                     tracking webhook → order/shipment status → FCM push
        ▼
013 Promotions            ── coupons (%/flat/BOGO/free-ship), eligibility (channel, group,
        │                     min-cart, brand/category), stacking; applied at cart re-resolution
        ▼
014 Reviews / ratings      ── (optional, post-MVP) product reviews on the storefront
        ▼
015 Go-Global             ── multi-currency activation, VAT + US-sales-tax TaxStrategy impls,
                              i18n catalog, Stripe SCA/3DS, region gateway routing, GDPR/DPDP.
                              Behind feature flags so India stays unaffected.
```

**India-first MVP = 009 → 013.** **Truly global = + 015.** 010 can run in parallel with 011 once 009
lands (different surfaces: app-sync vs storefront-checkout).

### Per-feature scope sketch

| # | Module(s) | New backend BC | App work | Ships independently because… |
|---|---|---|---|---|
| 009 | `pricing` (new), projects to `ecom`/`product` | `com.ampairs.pricing` | `feature/pricing` read-model + price display in ecom/order | Resolution falls back to today's flat `price` when no list matches → zero regression |
| 010 | `order`, `invoice` | — (implement stubs) | `OrderSyncDelegate`/`InvoiceSyncDelegate` real impls + `SyncEntity` already present | Pure sync impl; no storefront dependency |
| 011 | `payment` (new) | `com.ampairs.payment` | checkout payment screen + status | Behind a per-storefront "payments enabled" flag; COD still works without it |
| 012 | `shipping` (new) | `com.ampairs.shipping` | tracking screen already exists | Flat-rate fallback works without courier integration |
| 013 | `promotion` (new) | `com.ampairs.promotion` | coupon entry at cart | No coupon = no change to totals |
| 015 | cross-cutting | extends `tax`, all | i18n + currency display | Flags default off → India behavior unchanged |

---

## 4. Workflow & guardrails (apply to every feature)

- **Backend** (`ampairs`): `OwnableBaseDomain` (tenant-scoped) + `Instant` timestamps; DTO isolation
  (`domain/dto/`, extension-fn mapping); `ApiResponse<T>`; no try/catch in controllers; tenant
  context set at controller level only; Flyway in **both** `db/migration/mysql/` and
  `db/migration/postgresql/` (Postgres primary; check `flywayInfo` for next version); `@EntityGraph`
  for relations.
- **App** (`ampairs-app`): offline-first per `/offline-sync` — repository local-only (Room
  `synced=false` + `markPendingPush`), a `{Module}SyncDelegate` owns all API traffic,
  `@ContributesIntoMap(WorkspaceScope::class)` + `@SyncEntityKey`; Metro DI per `/metro-di`
  (`@SingleIn(WorkspaceScope::class)` DBs, `@Inject` repos, `@ContributesIntoMap` ViewModels); Compose
  per `/cmp-practices` (MVI, `collectAsStateWithLifecycle`, `stringResource` only). Add the new
  `SyncEntity` enum entry. Compile all 3 targets before "done".
- **Money**: enforce D1 — `Money(minorUnits, currency)` at the boundary; no raw `Double` money.
- **Git**: develop on `claude/blissful-goldberg-iuzI6` in each repo; commit with clear messages; push
  to that branch; **no PR unless explicitly asked.**

---

## 5. Open questions to confirm before 010+

1. **Currency activation timing** — keep `currency` columns populated with `INR` from 009, or defer
   the column until 015? (Recommendation: add the column in 009, default `INR` — cheap insurance.)
2. **Pricing home** — confirm D5 (pricing in the monolith, Kafka-projected to ecom) vs putting it in
   the ecom service. Affects whether B2B `order` reads pricing in-process or over a service call.
3. **Payments gateway priority** — Razorpay-only for MVP, or Razorpay + Stripe from 011?
4. **Wholesale launch** — does the India-MVP launch wholesale storefronts, or retail-only with the
   channel seam dormant until later?

> Confirm 1–4 (or accept the recommendations) and the program is ready to run `/speckit.clarify` on
> feature **009**, whose spec is already drafted at `specs/009-commerce-pricing/spec.md`.
