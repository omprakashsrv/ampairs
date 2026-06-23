# Feature Specification: Commerce Pricing Engine (Retail + Wholesale)

**Feature Branch**: `009-commerce-pricing`
**Created**: 2026-06-05
**Status**: Draft
**Input**: Add a channel-aware pricing engine (price lists, per-customer-group pricing, MOQ, slab
tiers) on top of the existing product catalog and ecom storefront, so the same product can sell at a
retail price to walk-in/B2C shoppers and at tiered wholesale prices to B2B customers — India-first,
global-ready (money carries a currency; tax strategy seam already exists).
**Program context**: First feature of the commerce program — see `specs/000-commerce-program/PLAN.md`.
Builds on `008-ecommerce-order-platform` (storefront, cart, checkout, orders) and the
`com.ampairs.product` / `com.ampairs.customer` modules. Does **not** rebuild any of those.

## Clarifications

### Session 2026-06-05 (pre-filled defaults — confirm in `/speckit.clarify`)

- Q: Where does pricing master data live? → A: New management-monolith bounded context
  `com.ampairs.pricing`, exposed to `order`/`invoice` via a public service interface and **projected
  into the ecom read model via Kafka** (same pattern as `CatalogSyncService`). Storefront resolves
  prices from the projection — no cross-service call on the hot path.
- Q: Money representation? → A: API contract `{ amount_minor: Long, currency: ISO-4217 }`; persistence
  `BigDecimal(19,4)` + `currency CHAR(3)` (aligns to ecom's existing scale). See PLAN.md D1.
- Q: Default behavior when no price list matches? → A: Fall back to the product's existing
  `sellingPrice` (today's behavior) → **zero regression** for merchants who never configure a list.
- Q: Channel set? → A: `SalesChannel { RETAIL, WHOLESALE }` (extensible). India-MVP may launch
  retail-only; the wholesale path must exist in the schema and resolution from day one.

### Session 2026-06-23 (program review — pricing/offers split confirmed)

- Q: Are pricing and offers one feature or two? → A: **Two.** This feature (009) owns **base-price
  resolution** (channel + group/customer/brand/category price lists, slab tiers, MOQ, variant,
  validity, catalog fallback). A separate feature (**015 Commerce Promotions**) owns **offers** that
  modify the order on top of resolved prices (cart/coupon discounts, BOGO/free-goods, brand
  volume/value schemes). 009 leaves a clean handoff: resolution returns the effective unit price +
  source; the promotion engine consumes that output, then tax runs.
- Q: Brands and distribution explicitly in scope? → A: Yes. Price lists may be scoped to a **brand**
  or **category** (not only product/variant), and the **DISTRIBUTOR** segment is served via
  customer-group-scoped wholesale lists (the `SalesChannel` enum stays extensible:
  `DISTRIBUTOR`/`B2B_MARKETPLACE` later). Per-customer special prices are the highest-priority match.
- Q: In-store first or online first? → A: **In-store first.** Wire `PricingResolutionService` into the
  KMP app order/invoice line-entry path (replacing `sellingPrice × multiplier`) and the monolith
  `order`/`invoice` services at the 010 seam; **then** project price lists to the ecom read model so
  the storefront/online ordering resolve identically.
- Q: Resolution precedence (overlap)? → A: per-customer special > customer-group/channel list >
  brand/category list > catalog fallback; ties broken by list `priority`, then most-recently-activated
  (FR-004). Variant match wins over base product within a list.

### Session 2026-06-23 (clarify — annotation/field-value targeting)

- Q: How is "custom pricing by any product/customer field value" modeled? → A: **Hybrid.**
  Structured, first-class **targeting dimensions** for the hot cases (sales channel, customer group,
  **customer-type**, specific customer, brand, category, product/variant, **product-group**,
  **geo-zone**) **plus** an optional list of **attribute predicates** `{ field, operator, value }`
  over customer/product attributes (incl. custom JSON attributes) for rare cases. Predicates are
  evaluated **last** and rank **below** any structured-dimension match (lowest precedence) so
  resolution stays deterministic, indexable, offline-resolvable, and projectable to the ecom read
  model. The dimension set is extensible (new structured dimensions added case-by-case).
- Q: Geography granularity for pincode-based pricing? → A: **Named geo-zones.** A reusable `GeoZone`
  (zone = set of pincodes, pincode-ranges, and/or states) is referenced by price lists; the resolver
  maps the customer's (or delivery) pincode → zone. Exact-pincode pricing is a single-pincode zone.
  `GeoZone` is shared master data (referenced by uid; reused by promotions feature 015).
- Q: Where do merchants create/manage price lists (management surface)? → A: **KMP app admin UI
  only** — no Angular web admin in this feature. Management is offline-first: the app `feature/pricing`
  is a full CRUD module (Room write `synced=false` + `markPendingPush`, `PricingSyncDelegate` owns
  push **and** pull), not just a read-model. Backend exposes the same `/pricing/v1` CRUD + `/sync`.
- Q: Updated overlap precedence with the new dimensions? → A: per-customer special >
  customer/group + channel list (most specific) > product-group / brand / category list > geo-zone /
  customer-type list > **attribute-predicate match** > catalog fallback; ties → list `priority`, then
  most-recently-activated; variant match wins over base within a list.
- Q: Where is price resolution executed — merchant orders vs online orders? → A: **Two trust models.**
  (1) **Merchant app** order/invoice resolves price **client-side** (offline, over the synced
  read-model), snapshots it, and pushes; the backend `order`/`invoice` `/sync` endpoints **persist the
  client snapshot as-is and do NOT re-resolve** (everything the app needs is already synced to it).
  (2) **Online customer** orders (ecom) are resolved/validated **server-side** at cart/checkout (the
  shopper device is untrusted and has no merchant data) over the ecom projection. The resolution
  *algorithm* is single-sourced and must produce identical results in both (parity, SC-006).

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Merchant sets a wholesale price list with tier breaks (Priority: P1)

A merchant who sells both to walk-in retail customers and to bulk buyers creates a **WHOLESALE** price
list for their "Distributor" customer group. For "Rice 5kg" they set: ₹240 for 1–9 units, ₹225 for
10–49 units, ₹210 for 50+ units, with a minimum order quantity (MOQ) of 10. Retail customers continue
to pay the catalog selling price of ₹260.

**Why this priority**: This is the core capability the whole feature exists for. Without channel- and
quantity-aware price lists, "wholesale" is just a single flat discount percentage — which the system
already has and which cannot express tier breaks or MOQ.

**Independent Test**: Create a price list, attach it to a customer group + WHOLESALE channel, add a
tiered item, then resolve the price for that product at quantities 5, 10, and 60 — getting ₹240, ₹225,
and ₹210 respectively, with an MOQ violation flagged at quantity 5.

**Acceptance Scenarios**:

1. **Given** a merchant in a workspace, **When** they create a price list named "Distributor 2026"
   with `channel = WHOLESALE` and customer group "Distributor", **Then** the list is created in Draft
   and is not yet used in resolution until activated.
2. **Given** an active wholesale list with tiers (1–9 → ₹240, 10–49 → ₹225, 50+ → ₹210) and MOQ 10 for
   "Rice 5kg", **When** the price is resolved for a Distributor customer at quantity 60, **Then** the
   effective unit price is ₹210 and the line is valid.
3. **Given** the same list, **When** the price is resolved at quantity 5, **Then** the effective unit
   price is ₹240 **and** the result flags `belowMoq = true` (MOQ 10 not met).
4. **Given** a product with **no** matching price list for the customer/channel, **When** its price is
   resolved, **Then** the engine returns the product's existing `sellingPrice` (catalog fallback) with
   `source = CATALOG_FALLBACK`.

---

### User Story 2 — Storefront shows the right price to the right shopper (Priority: P1)

A public RETAIL storefront shows catalog/retail prices to anonymous shoppers. A RESTRICTED wholesale
storefront (existing `accessMode = RESTRICTED`) shows the logged-in B2B customer their group's
wholesale tier prices. The price a shopper sees when adding to cart is the price they are charged at
checkout (snapshot), even if the merchant changes the list afterward.

**Why this priority**: Pricing that isn't reflected on the storefront and honored at checkout is
invisible to customers and creates billing disputes. Snapshotting prevents "the price changed between
cart and pay" bugs.

**Independent Test**: Browse a wholesale storefront as a Distributor customer, see tier price ₹225 for
qty 10, add to cart; have the merchant change the list to ₹230; complete checkout — the order line
charges the snapshotted ₹225.

**Acceptance Scenarios**:

1. **Given** an anonymous shopper on a RETAIL storefront, **When** they view "Rice 5kg", **Then** they
   see the retail/catalog price (₹260), never a wholesale tier.
2. **Given** a logged-in Distributor customer on a RESTRICTED wholesale storefront, **When** they view
   "Rice 5kg" at qty 10, **Then** they see ₹225 (their group's tier).
3. **Given** a wholesale customer adds an item at the resolved tier price, **When** the merchant later
   edits the price list, **Then** the customer's cart and resulting order retain the **snapshotted**
   price they saw at add-time.
4. **Given** the public price-resolution endpoint, **When** it is called for a storefront slug,
   **Then** it resolves within the storefront's existing tenant context
   (`StorefrontTenantInterceptor`) and defaults to the storefront's `defaultChannel` — no
   `X-Workspace-ID` header required.

---

### User Story 3 — B2B order entry on the app uses the same prices (Priority: P2)

A sales rep creating a wholesale order in the KMP app for a Distributor customer sees each line
auto-price from the active wholesale list (with tier breaks as quantity changes), works offline, and
the order syncs with the price snapshot intact.

**Why this priority**: The same resolution must serve B2B order entry, not just the storefront —
otherwise reps quote different prices than the storefront. Lower than P1 because it depends on the
order-sync work (feature 010) to fully round-trip, but the read-model/resolution must be ready.

**Independent Test**: Open the app's order screen offline, select a Distributor customer and a product
at qty 50, and confirm the line auto-fills the ₹210 tier from the locally projected price list.

**Acceptance Scenarios**:

1. **Given** the app has pulled the price-list projection, **When** a rep adds a product line for a
   Distributor customer at qty 50, **Then** the unit price auto-fills to the ₹210 tier from the local
   read model (no network needed).
2. **Given** the rep changes the line quantity from 50 to 5, **When** the line re-resolves, **Then**
   the unit price updates to ₹240 and an MOQ-not-met warning shows.
3. **Given** an offline-created order with snapshotted prices, **When** connectivity returns, **Then**
   the order pushes with the snapshot unchanged (resolution is not re-run server-side over the
   snapshot).

---

### User Story 4 — Currency travels with every price (global-ready) (Priority: P3)

Every price the engine stores, resolves, and returns carries an explicit ISO-4217 currency. For the
India launch this is always `INR`, but no code assumes it — so multi-currency in the Go-Global feature is a data
change, not a refactor.

**Why this priority**: Cheap insurance now; very expensive to retrofit later. Not a launch blocker for
India, hence P3.

**Acceptance Scenarios**:

1. **Given** any price-list item, **When** it is stored, **Then** it persists `amount` +
   `currency` (defaulting to the workspace base currency, `INR`).
2. **Given** any resolution response, **When** it is returned over the API, **Then** the money fields
   are `{ amount_minor, currency }` — never a bare number.
3. **Given** a price list, **When** it is created, **Then** all its items must share the list's
   currency (no mixed-currency list).

---

### Edge Cases

- **Overlapping lists**: a customer matches two active lists for the same channel → resolve by
  explicit `priority` (higher wins); ties broken by most-recently-activated. Resolution must be
  deterministic and the chosen list id must be returned for auditability.
- **Tier gaps/overlaps**: tier ranges within an item must be contiguous and non-overlapping —
  validated at save; quantity above the top tier uses the top tier.
- **MOQ vs availability**: MOQ is a *warning/blocker* flag from resolution, not silently enforced;
  the cart/order layer decides whether to hard-block (storefront) or warn (B2B rep).
- **Variant pricing**: a price-list item may target a `ProductVariant` (by variant `sku`) or the base
  product; variant match wins over base-product match.
- **Inactive/expired list**: lists may have optional `startsAt`/`endsAt`; outside the window the list
  is ignored and resolution falls back.
- **Catalog fallback currency**: when falling back to `product.sellingPrice` (a legacy `Double` with
  no currency), tag it with the workspace base currency.
- **Deleted product**: a price-list item whose product was deleted is skipped and reported, not fatal.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST let a merchant create, edit, activate, deactivate, and soft-delete
  **price lists** scoped to a `SalesChannel` (RETAIL | WHOLESALE) and any combination of structured
  **targeting dimensions**: customer group, customer-type, specific customer, brand, category,
  product-group, product/variant, and **geo-zone** (see FR-016/FR-017).
- **FR-016**: The system MUST support **named geo-zones** as reusable master data (a `GeoZone` whose
  membership is a set of pincodes, pincode-ranges, and/or states). A price list MAY be scoped to a
  geo-zone; resolution MUST map the customer's (or delivery) pincode to its zone. An exact-pincode
  price is a single-pincode zone. `GeoZone` is referenced by uid and shared with feature 015.
- **FR-017**: A price list MAY additionally carry an optional ordered list of **attribute predicates**
  `{ field, operator, value }` over customer/product attributes (incl. custom JSON attributes).
  Predicate-only matches MUST rank **below** every structured-dimension match (lowest precedence
  before catalog fallback) and MUST be evaluated deterministically. Resolution remains offline-capable
  and projectable.
- **FR-018**: Price-list **management** (create/edit/activate/deactivate/soft-delete, incl. items,
  tiers, MOQ, geo-zones, predicates) MUST be performed in the **KMP app admin UI** and work
  offline-first (local write `synced=false` → `markPendingPush` → `PricingSyncDelegate` push). No
  Angular web admin is in scope for this feature.
- **FR-002**: A price list MUST contain **price-list items** targeting a product (or specific variant),
  each with a unit price and optional **MOQ** and optional ordered **quantity tiers** (`minQty`,
  `unitPrice`).
- **FR-003**: The system MUST provide a **resolution service** that, given (customerId-or-anonymous,
  channel, productId/variant, quantity, workspace), returns the **effective unit price**, the
  **source** (`PRICE_LIST` | `CATALOG_FALLBACK`), the **matched list id** (if any), and a **belowMoq**
  flag.
- **FR-004**: Resolution MUST be **deterministic** for overlapping matches via list `priority`, then
  most-recently-activated.
- **FR-005**: When no active list matches, resolution MUST fall back to the product/variant catalog
  `sellingPrice` with `source = CATALOG_FALLBACK` (zero regression for unconfigured merchants).
- **FR-006**: RETAIL storefront resolution for **anonymous** shoppers MUST use the storefront's
  `defaultChannel` and MUST NOT expose WHOLESALE prices.
- **FR-007**: RESTRICTED storefront resolution for an **authenticated B2B customer** MUST use that
  customer's group + the storefront channel.
- **FR-008**: The public resolution endpoint MUST operate within the existing
  `StorefrontTenantInterceptor` tenant context and MUST NOT require `X-Workspace-ID`.
- **FR-009**: Cart add and checkout MUST **snapshot** the resolved unit price + currency onto the
  cart item / order line; subsequent price-list edits MUST NOT alter existing snapshots.
- **FR-010**: Every stored and returned price MUST carry an explicit **ISO-4217 currency**; all items
  in one list share one currency.
- **FR-011**: Pricing master data MUST be **projected to the ecom read model** so storefront and app
  resolution need no synchronous call into the pricing module.
- **FR-012**: Price resolution is executed in **two places only**: (a) the **merchant app**
  client-side resolver (offline), and (b) the **ecom server-side** path (cart/checkout) over the
  projection. The backend `order`/`invoice` `/sync` endpoints MUST persist the client-provided price
  snapshot **without re-resolving** (offline-first trust model). The pricing module exposes its
  resolution as a **single-sourced algorithm/service** (used for the ecom projection-based resolver,
  admin preview, and to keep the app resolver in parity) — no synchronous cross-module resolution call
  on the merchant order path.
- **FR-013**: Price lists and items are **tenant-scoped** (`OwnableBaseDomain`) and MUST never leak
  across workspaces.
- **FR-014**: Tier ranges within an item MUST be validated contiguous and non-overlapping at save.
- **FR-015**: A `Storefront` MUST gain a `defaultChannel` (default RETAIL) used by FR-006.

### Non-Functional / Constraints

- **NFR-001**: Public storefront price resolution P95 < 50 ms (served from the projected read model,
  not a cross-service call).
- **NFR-002**: Backend money columns `BigDecimal(19,4)` + `currency CHAR(3)`; API money is
  `{ amount_minor: Long, currency }`. **No raw `Double` for money in new code.**
- **NFR-003**: KMP app uses a `Money(minorUnits, currency)` value class at the pricing boundary; no
  `Double` in the new pricing read model.
- **NFR-004**: Flyway migrations in **both** `pricing/src/main/resources/db/migration/{mysql,postgresql}/`
  (check `./gradlew :ampairs_service:flywayInfo` for the next version), `Instant` timestamps,
  `TIMESTAMPTZ`/`TIMESTAMP`.
- **NFR-005**: Compile all three app targets (Android, iOS, Desktop) before the feature is "done".

### Key Entities

- **PriceList** (`OwnableBaseDomain`) — `uid`, `name`, `channel: SalesChannel`, and optional
  structured targeting: `customerGroupId?`, `customerType?`, `customerId?`, `brandId?`, `categoryId?`,
  `productGroupId?`, `geoZoneId?` (null on all = applies to all in channel); plus
  `attributePredicates: List<AttributePredicate>?` (lowest-precedence match); `currency`,
  `priority: Int`, `status` (DRAFT/ACTIVE/INACTIVE), optional `startsAt`/`endsAt`, `active`.
- **GeoZone** (shared master, `OwnableBaseDomain`) — `uid`, `name`, `members` (pincodes,
  pincode-ranges, states); referenced by `PriceList.geoZoneId` and by promotions (feature 015).
- **AttributePredicate** (value/JSON) — `field` (e.g. `customer.attributes.tier`, `product.attributes.x`),
  `operator` (EQ/NEQ/IN/GT/LT/...), `value`. Evaluated last, lowest precedence.
- **PriceListItem** (`OwnableBaseDomain`) — `uid`, `priceListId`, `productId`, `variantSku: String?`,
  `unitPrice` (BigDecimal) + inherits list `currency`, `moq: BigDecimal?`, `tiers: List<PriceTier>`
  (JSON; each `minQty`, `unitPrice`).
- **PriceTier** (value/JSON) — `minQty: BigDecimal`, `unitPrice: BigDecimal`.
- **SalesChannel** (enum) — `RETAIL`, `WHOLESALE` (extensible).
- **PriceResolution** (DTO, not persisted) — `effectiveUnitPrice {amount_minor,currency}`, `source`,
  `matchedPriceListUid: String?`, `appliedTierMinQty: BigDecimal?`, `belowMoq: Boolean`.
- **Storefront** (existing, `com.ampairs.ecom`) — **add** `defaultChannel: SalesChannel = RETAIL`.
- **EcomCartItem / EcomOrderLineItem / OrderItem** (existing) — **add** snapshotted
  `resolvedUnitPriceAmountMinor` + `currency` + `priceSource` + `matchedPriceListUid` (snapshot fields;
  existing price fields retained for back-compat).
- **Ecom read-model `PriceListProjection`** (new, in `ecom` + app `feature/pricing`) — denormalized,
  Kafka-fed copy used for storefront/app resolution.

## Success Criteria *(mandatory)*

- **SC-001**: A merchant can configure a wholesale tiered price list end-to-end and a B2B customer is
  charged the correct tier price at storefront checkout, with the snapshot honored after a later edit.
- **SC-002**: Merchants who configure **no** price list see **identical** pricing behavior to today
  (catalog `sellingPrice`) — verified by a regression suite over existing storefront/order flows.
- **SC-003**: Anonymous retail shoppers can never see or obtain a wholesale price (verified by a
  negative test on the public endpoint).
- **SC-004**: Public storefront resolution P95 < 50 ms under the projected read model.
- **SC-005**: Every price returned by any pricing API carries an explicit currency; a contract test
  rejects any bare-number money field.
- **SC-006**: The single-sourced resolution algorithm produces the **same effective price** in the
  merchant app (offline, over Room) and on the ecom server-side path (over the projection) for
  identical inputs (parity test). The monolith `order`/`invoice` `/sync` stores the app's snapshot
  verbatim and does not re-resolve (verified: pushed snapshot == persisted snapshot).

## Out of Scope (this feature)

- Payments/refunds (the Payments feature, `013-payment-collection`), shipping rates (the Shipping
  feature, future), coupons/promotions (the Promotions feature, `015-commerce-promotions`) — promotions
  stack **on top of** resolved prices, they do not belong in the pricing engine.
- Implementing the stubbed Order/Invoice **sync delegates** (the Store-Ops feature,
  `010-store-ops-order-invoice`) — 009 ships the read model + resolution; full B2B offline round-trip
  lands with Store-Ops.
- Multi-currency **activation** / FX / VAT+sales-tax strategy impls (the Go-Global feature, future) —
  009 only carries the currency field and keeps `INR` as the workspace default.
- Search/faceting changes.

## Dependencies & Assumptions

- Assumes `com.ampairs.product` (`Product`/`ProductVariant`, `sellingPrice`, `brandId`,
  category/subcategory) and `com.ampairs.customer` (`CustomerGroup`, `CustomerType`) as they exist
  today.
- Assumes the `ecom` Kafka catalog-sync infrastructure (`CatalogSyncService`,
  `EcomCatalogKafkaConsumer`) is reusable for the price-list projection.
- Assumes `StorefrontTenantInterceptor` continues to resolve tenant from slug for public endpoints.
- Confirm PLAN.md open questions Q1 (currency column timing — assumed: add now, default INR) and Q2
  (pricing home — assumed: monolith + Kafka projection) during `/speckit.clarify`.

---

*Next steps*: run `/speckit.clarify` (confirm the pre-filled clarifications + PLAN.md Q1/Q2), then
`/speckit.plan`, `/speckit.tasks`, `/speckit.analyze`, and stop for review before
`/speckit.implement`. Develop on `claude/blissful-goldberg-iuzI6`; no PR unless requested.
