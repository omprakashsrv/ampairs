# Feature Specification: Commerce Promotions & Offers (Retail + Wholesale + Distribution + Brands)

**Feature Branch**: `015-commerce-promotions` (developed on `claude/wonderful-dirac-zl47z7` per session policy)
**Created**: 2026-06-23
**Status**: Draft
**Input**: Add a channel- and segment-aware **offers/promotions engine** on top of the pricing engine
(feature 009) so a merchant can run line/cart discounts, coupon codes, BOGO / free-goods schemes, and
brand-funded volume/value schemes (QPS/TPR) for retail, wholesale, and distribution buyers — applied
identically for in-store order/invoice entry **and** the online storefront, with the resolved offer
snapshotted onto the order so the price the customer saw is the price they pay.
**Program context**: Commerce program (`specs/000-commerce-program/PLAN.md`), roadmap item
"Promotions". Builds on **009 Pricing** (price resolution, `SalesChannel`, `Money`, ecom Kafka
projection) and **010 Store-Ops** (order/invoice create flow). Does **not** rebuild pricing, tax,
catalog, storefront, cart, or checkout.

## Clarifications

### Session 2026-06-23 (decisions confirmed with product owner)

- Q: One combined pricing+offers module, or two? → A: **Two bounded contexts.** Pricing
  (`com.ampairs.pricing`, feature 009) resolves the **base unit price**; promotions
  (`com.ampairs.promotion`, this feature) **adjusts** the order on top of resolved prices. Different
  lifecycles, different stacking semantics.
- Q: MVP offer types? → A: **(O1/O2)** line + cart % / flat discount, **(O3)** coupon codes,
  **(O4)** BOGO / free-goods ("buy X get Y free", same or different SKU), **(O7)** brand volume/value
  scheme (QPS/TPR — slab discount on total qty/value of a brand). Bundle/combo (O6) and cash-discount
  (O8) are explicitly **deferred** to a follow-up.
- Q: Where does an offer apply in the calculation pipeline? → A: After price resolution (009) and
  **before** tax (`TaxCalculationEngine` / `DocumentTotalsCalculator`). Offers never recompute the
  base price; they produce discount lines / free-goods lines / order-level reductions that feed tax.
- Q: In-store first or online first? → A: **In-store first** (order + invoice create flow on the KMP
  app and the monolith `order`/`invoice` services), **then** project the same offers to the ecom read
  model so the storefront and online customer ordering apply identical offers.
- Q: Money? → A: Inherits 009 / program decision D1 — `{ amount_minor: Long, currency: ISO-4217 }` on
  the wire, `BigDecimal(19,4)` + `currency CHAR(3)` in DB, `Money(minorUnits, currency)` in the app.
  Default `INR`. No raw `Double` for money in new code.
- Q: Snapshot? → A: Yes. The applied offer (id + type + value + free-goods lines) is **snapshotted**
  onto the order/invoice line / cart / ecom order line. A later edit to the promotion never alters an
  existing snapshot.

### Session 2026-06-23 (clarify — annotation/field-value targeting & combo offers)

- Q: How is offer eligibility by any product/customer field value modeled? → A: **Hybrid**, identical
  to pricing (009): structured eligibility dimensions (channel, customer group, **customer-type**,
  customer, brand, category, **product-group**, product/variant, **geo-zone**, min-qty, min-cart) plus
  an optional list of **attribute predicates** `{ field, operator, value }` evaluated **last** at
  lowest precedence. Reuses the shared `GeoZone` master (FR-016/009) and the same `AttributePredicate`
  shape — no second targeting model.
- Q: Geography for offers? → A: Same **named geo-zones** as pricing (pincode → zone mapping); an
  offer may be eligible for a geo-zone. Reuses the shared `GeoZone` entity.
- Q: Combo offers in MVP? → A: **Yes** — add a `BUNDLE` promotion type with
  `effectMode = FIXED_PRICE | DISCOUNT`: a bundle defines a set of products with required quantities;
  the effect is either a **fixed combo price** ("A+B+C for ₹499") or a **combo discount** ("any N from
  this set → % / flat off those lines"). Runs in the same resolve → apply → tax → snapshot pipeline.
  Bundle is therefore **removed from Out of Scope**.
- Q: Management surface for promotions/coupons? → A: **KMP app admin UI only** (no Angular web admin
  this feature). Offline-first: app `feature/promotion` is full CRUD (Room write `synced=false` +
  `markPendingPush`, `PromotionSyncDelegate` push **and** pull). Backend exposes `/promotion/v1` CRUD
  + `/sync`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Merchant runs line & cart discounts in store ordering/invoicing (Priority: P1)

A store creates a workspace-level offer "Diwali 10% off" applying a 10% cart discount to any
**RETAIL** order above ₹2,000. Staff taking an order/invoice on the KMP app (offline) see the offer
auto-apply once the cart crosses ₹2,000, reducing the total; the applied offer is recorded on the
order. Line-level % / flat discounts staff already enter by hand continue to work and are now
attributed to a manual-discount source for reporting.

**Why this priority**: This is the smallest end-to-end slice that proves the engine: an offer is
defined once, auto-applied during offline order/invoice entry, snapshotted, and synced — with zero
regression to today's manual discount entry. Everything else (coupons, BOGO, schemes, online) layers
on this spine.

**Independent Test**: Define a cart-discount offer (RETAIL, min-cart ₹2,000, 10%); on the app create
an order at ₹1,800 (offer not applied) then add a line to reach ₹2,400 (offer applies, total reduced
by ₹240); save offline; confirm the order line/order carries the snapshotted offer and syncs intact.

**Acceptance Scenarios**:

1. **Given** an active cart-discount offer (RETAIL, min-cart ₹2,000, 10%), **When** staff build an
   order whose subtotal is ₹1,800, **Then** the offer does **not** apply and the total is unchanged.
2. **Given** the same offer, **When** the subtotal reaches ₹2,400, **Then** a ₹240 order-level
   discount is applied **before** tax and reflected in the grand total.
3. **Given** an applied offer, **When** the order is saved offline and later synced, **Then** the
   snapshotted offer (id, type, value) round-trips unchanged.
4. **Given** a manually entered line discount, **When** the order is saved, **Then** the discount is
   retained and tagged `source = MANUAL` (no behavioral change vs today).
5. **Given** no active offer matches, **When** an order is built, **Then** totals are identical to
   today's behavior (zero regression).

---

### User Story 2 — Coupon codes with eligibility (Priority: P1)

A merchant creates coupon `WELCOME50` = ₹50 flat off, eligible for **RETAIL** channel, min-cart ₹500,
limited to the "New" customer group, valid 1–31 Dec, max 1 use per customer, 1,000 total uses. Staff
(in store) and customers (online) enter the code; the engine validates eligibility + limits and
applies it, or returns a clear reason for rejection.

**Why this priority**: Coupons are the most-requested promo lever and exercise the full eligibility +
usage-limit machinery that BOGO and schemes reuse. P1 because it is independently valuable and proves
the eligibility/limit subsystem.

**Independent Test**: Create `WELCOME50` as above; apply it to a ₹400 RETAIL cart (rejected: below
min-cart), a ₹600 cart for a non-"New" customer (rejected: group), and a ₹600 cart for a "New"
customer within the window (accepted: ₹50 off); apply it twice for the same customer (second rejected:
per-customer limit).

**Acceptance Scenarios**:

1. **Given** coupon `WELCOME50` (₹50, RETAIL, min-cart ₹500, group "New", 1/customer), **When** a
   "New" customer applies it to a ₹600 RETAIL cart in the window, **Then** ₹50 is deducted before tax
   and a usage is recorded against that customer.
2. **Given** the same coupon, **When** applied to a ₹400 cart, **Then** it is rejected with reason
   `BELOW_MIN_CART`.
3. **Given** the same coupon, **When** applied by a customer not in group "New", **Then** it is
   rejected with reason `INELIGIBLE_GROUP`.
4. **Given** the customer already redeemed it once, **When** they apply it again, **Then** it is
   rejected with reason `USAGE_LIMIT_REACHED`.
5. **Given** the coupon's total redemptions hit 1,000, **When** anyone applies it, **Then** it is
   rejected with reason `GLOBAL_LIMIT_REACHED`.
6. **Given** the coupon window has passed, **When** it is applied, **Then** it is rejected with reason
   `EXPIRED`.

---

### User Story 3 — BOGO / free-goods scheme for wholesale & distribution (Priority: P1)

A distributor offer: "Buy 10 cases of Brand-X Soap, get 1 case free." When a wholesale order contains
≥10 qualifying cases, the engine adds a **free-goods line** (1 case at ₹0, flagged as a scheme
freebie) rather than a percentage discount. The free unit is tax-handled per the configured policy and
the scheme is snapshotted for audit/settlement (brand-funding attribution).

**Why this priority**: Free-goods ("X free with Y") is the defining FMCG distribution scheme and the
one the legacy `Discount(percent, value)` model **cannot represent**. It is the core wholesale value
of this feature.

**Independent Test**: Define a BOGO scheme (buy 10 → 1 free, SKU = Brand-X Soap case, WHOLESALE);
build a wholesale order with 9 cases (no freebie), then 10 cases (1 free-goods line added at ₹0), then
21 cases (2 free-goods lines); confirm free lines are flagged and snapshotted.

**Acceptance Scenarios**:

1. **Given** a "buy 10 get 1 free" scheme on Brand-X Soap case for WHOLESALE, **When** a wholesale
   order has 9 cases, **Then** no free-goods line is added.
2. **Given** the same scheme, **When** the order has 10 cases, **Then** exactly one free-goods line
   (qty 1, unit price ₹0, `isFreeGood = true`, `sourcePromotionUid` set) is added.
3. **Given** the same scheme, **When** the order has 21 cases, **Then** two free-goods lines are added
   (floor(21/10) = 2), and any configured per-order free-goods cap is honored.
4. **Given** a "buy X get **different** SKU Y free" scheme, **When** the trigger qty is met, **Then**
   the free line references SKU Y, not the purchased SKU.
5. **Given** a free-goods line, **When** tax is computed, **Then** it follows the configured
   free-goods tax policy (e.g., taxable at MRP vs zero-rated) deterministically.

---

### User Story 4 — Brand volume/value scheme (QPS/TPR) (Priority: P2)

A brand-funded scheme: across all Brand-X SKUs in a single wholesale order, total qty 1–49 → 0% off,
50–99 → 3% off, 100+ → 5% off (a quantity slab on the **brand aggregate**, not per line). A value
variant uses total ₹ instead of qty. The achieved slab discount is apportioned back across the
brand's lines before tax and attributed to the brand for settlement.

**Why this priority**: Volume/value schemes (QPS — Quarterly/Quantity Purchase Scheme, TPR — Temporary
Price Reduction) are how brands fund distributor/dealer incentives. P2 because it depends on the
eligibility + apportionment machinery proven by P1 stories and is most valuable once BOGO ships.

**Independent Test**: Define a brand qty-slab scheme (Brand-X: 50–99 → 3%, 100+ → 5%, WHOLESALE);
order 40 mixed Brand-X units (no discount), 60 units (3% across Brand-X lines), 120 units (5%);
confirm the discount is apportioned per line and attributed to Brand-X.

**Acceptance Scenarios**:

1. **Given** a Brand-X qty-slab scheme (50–99 → 3%, 100+ → 5%, WHOLESALE), **When** a wholesale order
   totals 40 Brand-X units across SKUs, **Then** no scheme discount applies.
2. **Given** the same scheme, **When** the brand total is 60 units, **Then** a 3% discount on the
   Brand-X line subtotal is apportioned across those lines before tax.
3. **Given** a **value**-based variant, **When** the Brand-X line value crosses a ₹ slab, **Then** the
   corresponding % applies on value, not qty.
4. **Given** a non-Brand-X line in the same order, **When** the scheme resolves, **Then** that line is
   unaffected.

---

### User Story 5 — Same offers reflected for online customer ordering (Priority: P2)

Everything configured for in-store ordering reflects on the online storefront via the existing
denormalized read model. An anonymous RETAIL shopper sees retail coupons/cart offers; a logged-in B2B
customer on a RESTRICTED wholesale storefront gets their group's BOGO/scheme offers. The offer applied
at "add to cart" is snapshotted and honored at checkout even if the merchant edits the promotion after.

**Why this priority**: The user's explicit requirement — store-defined offers must apply online
without a second source of truth. P2 because it depends on the in-store engine (P1) and reuses the
catalog/price Kafka projection pattern already in place.

**Independent Test**: Define a RETAIL coupon and a WHOLESALE BOGO in store; on the public storefront
confirm an anonymous shopper can apply the retail coupon (and cannot see/apply the wholesale BOGO);
as a logged-in Distributor customer confirm the BOGO auto-applies; change the promotion server-side
and confirm an already-checked-out order keeps the snapshot.

**Acceptance Scenarios**:

1. **Given** an active RETAIL coupon and a WHOLESALE BOGO, **When** an anonymous shopper browses a
   RETAIL storefront, **Then** they can apply the coupon and never see or obtain the wholesale BOGO.
2. **Given** a logged-in Distributor on a RESTRICTED wholesale storefront, **When** their cart meets a
   BOGO trigger, **Then** the free-goods line is added at checkout (snapshotted).
3. **Given** the promotion is edited by the merchant after add-to-cart, **When** the customer
   completes checkout, **Then** the order charges/credits the **snapshotted** offer, not the new one.
4. **Given** the public coupon-apply endpoint, **When** called for a storefront slug, **Then** it runs
   inside the existing `StorefrontTenantInterceptor` tenant context — no `X-Workspace-ID` required —
   and resolves offers from the **projected read model** (no synchronous call into the promotion
   module).

---

### Edge Cases

- **Stacking**: when multiple offers match (e.g., a cart discount + a coupon), apply per a documented,
  deterministic policy: each offer declares `stackable: Boolean` and a `priority`; non-stackable
  offers are mutually exclusive (highest priority / best-value-for-customer wins per a configured
  `conflictPolicy`). The resolution MUST return the ordered list of applied offers for auditability.
- **Offer vs MOQ/tier (009)**: offers operate on prices already resolved by 009 (including
  tier/slab). An offer never re-opens price resolution; it only adds discount/free-goods lines.
- **Free-goods out of stock**: if a free-goods SKU is unavailable, the scheme is flagged (warn for B2B
  rep, block/substitute per storefront policy) — never silently dropped.
- **Free-goods tax treatment**: the policy (zero-rated vs taxable at MRP) is explicit and consistent
  across in-store and online; GST on free goods is a known compliance edge.
- **Rounding**: apportioned scheme discounts must reconcile to the offer total to the minor unit (no
  ±1 paise drift); the line that absorbs rounding is deterministic.
- **Negative/zero totals**: stacked offers can never drive a line or order total below zero; clamp and
  flag.
- **Coupon case/whitespace**: codes are normalized (uppercase, trimmed) before lookup.
- **Concurrent global-limit redemption**: usage counting must be atomic so a limited coupon cannot
  over-redeem under concurrency (online + in-store at once).
- **Snapshot vs re-resolution**: offers are resolved live while building the cart/order; once an order
  is placed/snapshotted, the server MUST NOT re-run offer resolution over the snapshot.
- **Deleted product/brand**: an offer referencing a deleted product/brand is skipped and reported, not
  fatal.
- **Anonymous eligibility**: anonymous shoppers have no customer group; group-restricted offers never
  apply to them.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST let a merchant create, edit, activate, deactivate, schedule
  (`startsAt`/`endsAt`), and soft-delete **promotions** scoped to one or more `SalesChannel`
  (RETAIL | WHOLESALE) and optional eligibility across structured dimensions (customer group,
  **customer-type**, customer, brand, category, **product-group**, product/variant, **geo-zone**,
  min-qty, min-cart-value) **plus** optional **attribute predicates** `{ field, operator, value }`
  evaluated last at lowest precedence. Geo-zone reuses the shared `GeoZone` master (FR-016/009);
  the attribute-predicate shape is shared with pricing.
- **FR-002**: The system MUST support these promotion **types** for MVP:
  `CART_DISCOUNT` (% / flat, line- or order-level), `COUPON` (code-gated % / flat),
  `BOGO` / free-goods (buy X get Y free; Y may be a different SKU),
  `VOLUME_SCHEME` (qty- or value-slab discount on a brand/category aggregate, i.e. QPS/TPR), and
  `BUNDLE` / combo with `effectMode = FIXED_PRICE | DISCOUNT` (a set of products with required
  quantities priced at a fixed combo amount, or "any N from the set → % / flat off those lines").
  (Manual line discounts are retained and tagged `source = MANUAL`.)
- **FR-003**: The system MUST provide a **promotion engine** that, given resolved-price lines + cart
  context (channel, customer-or-anonymous, optional coupon code, workspace), returns the **ordered
  list of applied offers**, the resulting **discount adjustments** (line/order), any **free-goods
  lines**, and a **rejection reason** for any requested-but-inapplicable offer (e.g. coupon).
- **FR-004**: The engine MUST operate **after** price resolution (009) and **before** tax; it MUST NOT
  recompute base prices. Outputs feed the existing `TaxCalculationEngine`/`DocumentTotalsCalculator`.
- **FR-005**: Resolution MUST be **deterministic** under multiple matches via `stackable`, `priority`,
  and a documented `conflictPolicy`; the chosen set + order MUST be returned for audit.
- **FR-006**: When no offer matches (and no coupon is entered), totals MUST be **identical to today's**
  (zero regression for merchants who configure no promotions).
- **FR-007**: Coupons MUST validate eligibility (channel, group/customer, min-cart, brand/category,
  validity window) and **usage limits** (per-customer and global), returning a specific reason on
  rejection. Usage counting MUST be **atomic** (no over-redemption under concurrency).
- **FR-008**: BOGO/free-goods MUST add explicit **free-goods lines** (`isFreeGood = true`, unit price
  ₹0, `sourcePromotionUid`), honoring trigger ratios (floor(qty/triggerQty)) and any per-order cap,
  with a configurable, deterministic **free-goods tax policy**.
- **FR-009**: Volume/value schemes MUST aggregate qty or value over the offer's scope (brand/category)
  within one order, select the achieved slab, and **apportion** the discount across the scope's lines
  before tax, reconciling to the offer total to the minor unit.
- **FR-010**: Cart add and checkout (online) and order/invoice save (in-store) MUST **snapshot** the
  applied offers (id, type, value, free-goods lines, currency); later promotion edits MUST NOT alter
  existing snapshots.
- **FR-011**: RETAIL storefront resolution for **anonymous** shoppers MUST use the storefront's
  `defaultChannel` and MUST NOT expose or apply WHOLESALE-only offers.
- **FR-012**: RESTRICTED storefront resolution for an **authenticated B2B customer** MUST use that
  customer's group + the storefront channel.
- **FR-013**: The public coupon/offer endpoints MUST operate within the existing
  `StorefrontTenantInterceptor` tenant context and MUST NOT require `X-Workspace-ID`.
- **FR-014**: Promotion master data MUST be **projected to the ecom read model** (same Kafka pattern
  as `CatalogSyncService` / the 009 price-list projection) so storefront and app resolution need no
  synchronous call into the promotion module.
- **FR-015**: The promotion module MUST expose a **public service interface** for `order`/`invoice`
  (and the app's local engine mirror) to apply offers in-process — no direct repository access across
  modules (module-boundary rule).
- **FR-016**: Promotions, coupons, and usage records MUST be **tenant-scoped** (`OwnableBaseDomain`)
  and MUST never leak across workspaces.
- **FR-017**: Every monetary value stored/returned MUST carry an explicit **ISO-4217 currency**
  (`Money`); default `INR`.
- **FR-018**: The KMP app MUST resolve and apply offers **offline** from a locally projected read
  model (offers + coupons pulled via the `/sync` contract), so in-store order/invoice entry works
  without connectivity; snapshots sync per the offline-first rules.
- **FR-019**: Stacked offers MUST never drive any line or order total below zero (clamp + flag).
- **FR-020**: Brand-funded schemes (BOGO/volume) MUST record **funding/attribution** metadata
  (e.g. `fundingBrandId`) on the snapshot for downstream settlement reporting.
- **FR-021**: Promotion/coupon **management** (create/edit/activate/deactivate/soft-delete across all
  types incl. BUNDLE, eligibility, geo-zones, predicates) MUST be performed in the **KMP app admin
  UI** and work offline-first (local write `synced=false` → `markPendingPush` → `PromotionSyncDelegate`
  push). No Angular web admin is in scope for this feature.

### Non-Functional / Constraints

- **NFR-001**: Public storefront offer resolution P95 < 60 ms (served from the projected read model).
- **NFR-002**: Backend money columns `BigDecimal(19,4)` + `currency CHAR(3)`; API money is
  `{ amount_minor: Long, currency }`. **No raw `Double` for money in new code.**
- **NFR-003**: KMP app uses `Money(minorUnits, currency)` at the promotion boundary; no `Double` in
  the new promotion read model.
- **NFR-004**: Flyway migrations in **both** `promotion/src/main/resources/db/migration/{mysql,postgresql}/`
  (Postgres primary; check `./gradlew :ampairs_service:flywayInfo` for the next version); `Instant`
  timestamps, `TIMESTAMPTZ`/`TIMESTAMP`; add `promotion` to `migrationModules`.
- **NFR-005**: Coupon usage counting MUST be transactionally safe (DB-level uniqueness/atomic
  increment) to prevent over-redemption.
- **NFR-006**: Compile all three app targets (Android, iOS, Desktop) before the feature is "done".
- **NFR-007**: Offers + tax interaction MUST be covered by a calculation parity test: identical inputs
  yield identical totals in the in-store calculator, the app engine, and the monolith order service.

### Key Entities

- **Promotion** (`OwnableBaseDomain`) — `uid`, `name`, `type` (CART_DISCOUNT | COUPON | BOGO |
  VOLUME_SCHEME | BUNDLE), `channels: Set<SalesChannel>`, `status` (DRAFT/ACTIVE/INACTIVE),
  `priority: Int`, `stackable: Boolean`, `conflictPolicy`, optional `startsAt`/`endsAt`, `currency`,
  `active`, `fundingBrandId: String?`.
- **PromotionEligibility** (embedded/JSON) — `customerGroupId?`, `customerType?`, `customerId?`,
  `brandId?`, `categoryId?`, `productGroupId?`, `productId?`/`variantSku?`, `geoZoneId?`, `minQty?`,
  `minCartValue?`, `attributePredicates: List<AttributePredicate>?` (lowest-precedence match).
- **GeoZone** (shared master, reused from feature 009) — referenced by `geoZoneId`; resolver maps the
  customer's/delivery pincode to a zone.
- **AttributePredicate** (value/JSON, shared with 009) — `field`, `operator`, `value`; evaluated last.
- **PromotionEffect** (embedded/JSON, type-specific) — for CART_DISCOUNT: `{ percent? , flatAmount? ,
  scope: LINE|ORDER }`; for COUPON: `code`, effect like CART_DISCOUNT + `freeShipping?`; for BOGO:
  `{ triggerProductId/variantSku, triggerQty, freeProductId/variantSku, freeQty, perOrderCap?,
  freeGoodsTaxPolicy }`; for VOLUME_SCHEME: `{ aggregateBy: BRAND|CATEGORY, basis: QTY|VALUE,
  slabs: List<{ minThreshold, percent }> }`; for BUNDLE:
  `{ effectMode: FIXED_PRICE|DISCOUNT, items: List<{ productId/variantSku, qty }>,
  fixedPriceMinor?, percent?, flatAmount?, minItemsFromSet? }`.
- **Coupon** (may be modeled as `Promotion` of type COUPON, or a child) — `code` (normalized,
  unique-active per workspace), `perCustomerLimit?`, `globalLimit?`.
- **CouponRedemption** (`OwnableBaseDomain`) — `uid`, `couponUid`, `customerId?`, `orderRef`,
  `redeemedAt` — drives atomic usage counting (unique `(couponUid, customerId, orderRef)`).
- **PromotionApplication** (DTO + snapshot fields, not a free-standing master) — `appliedPromotionUid`,
  `type`, `discountAmount {amount_minor,currency}`, `freeGoodsLines: List<FreeGoodsLine>`,
  `fundingBrandId?`, `appliedSlabThreshold?` — snapshotted onto order/invoice/cart.
- **FreeGoodsLine** (value/JSON) — `productId`/`variantSku`, `qty`, `unitPriceMinor = 0`,
  `sourcePromotionUid`, `taxPolicy`.
- **SalesChannel** (enum, from 009) — `RETAIL`, `WHOLESALE` (extensible).
- **Existing entities — additive snapshot fields** (back-compat retained):
  - `OrderItem` / `InvoiceItem` / `EcomCartItem` / `EcomOrderLineItem` — add `appliedPromotionUids`,
    `promotionDiscountMinor`, `isFreeGood`, `sourcePromotionUid`, `currency`.
  - `Order` / `Invoice` / `EcomOrder` — add `appliedPromotions` (snapshot list),
    `promotionDiscountTotalMinor`.
  - `Storefront` (ecom) — reuse `defaultChannel` added by 009.
- **Ecom read-model `PromotionProjection`** (new, in `ecom` + app `feature/promotion`) —
  denormalized, Kafka-fed copy of active promotions/coupons used for storefront/app resolution.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A merchant can configure each MVP offer type (cart discount, coupon, BOGO, volume
  scheme) once and have it apply identically in in-store order/invoice entry and at online checkout.
- **SC-002**: Merchants who configure **no** promotions see **identical** totals to today — verified
  by a regression suite over existing order/invoice/storefront flows.
- **SC-003**: Anonymous retail shoppers can never see or obtain a WHOLESALE-only offer (negative test
  on the public endpoint).
- **SC-004**: A limited coupon cannot be over-redeemed under concurrent in-store + online redemption
  (atomic-usage test).
- **SC-005**: BOGO free-goods and volume-scheme apportionment reconcile to the offer total to the
  minor unit (no rounding drift) — property test.
- **SC-006**: The same offer set produces the same adjusted totals on the storefront, in B2B app order
  entry, and in the monolith `order` service for identical inputs (parity test).
- **SC-007**: Every monetary value returned by any promotion API carries an explicit currency; a
  contract test rejects any bare-number money field.
- **SC-008**: Public storefront offer resolution P95 < 60 ms under the projected read model.
- **SC-009**: A `BUNDLE` offer applies correctly in both modes — a fixed-price combo charges the set
  at the configured amount, and a combo-discount applies only when ≥ `minItemsFromSet` qualifying
  items are present — verified in store and at online checkout.
- **SC-010**: Geo-zone targeting resolves the correct rule for a given customer/delivery pincode, and
  attribute-predicate matches never override a structured-dimension match (precedence test).

## Out of Scope (this feature)

- **Pricing engine itself** (price lists, tiers/slabs, MOQ, base-price resolution) — that is feature
  **009**; this feature consumes its resolved prices.
- **Payment-term cash discount (O8)** — deferred follow-up. (Bundle/combo O6 is now **in scope** — see
  FR-002 `BUNDLE`.)
- **Free shipping mechanics** beyond a coupon flag — real shipping zones/rates live in feature 012;
  this feature only carries a `freeShipping` coupon effect that shipping later honors.
- **Loyalty points / store credit / gift cards** — separate future feature.
- **Payments/refunds** (feature 011) — offer-driven credits affect totals only, not settlement.
- **Multi-currency activation / FX** (feature 015 Go-Global) — only the currency field is carried.
- **Brand-settlement reporting UI** — this feature records funding/attribution metadata; the
  reconciliation report is a downstream feature.

## Dependencies & Assumptions

- **Depends on feature 009 (Pricing)**: `SalesChannel`, `Money`/currency, `PricingResolutionService`
  output (resolved unit price + source), `Storefront.defaultChannel`, and the ecom Kafka
  projection infrastructure (`CatalogSyncService` / price-list projection) — all reused here.
- **Depends on feature 010 (Store-Ops)**: the order/invoice create flow and the price-resolution seam
  it leaves open; offers plug in immediately after that seam, before tax.
- Assumes `com.ampairs.product` (`Product`/`ProductVariant`, `brandId`, category) and
  `com.ampairs.customer` (`Customer.customerGroup`/`customerType`, `CustomerGroup`) as they exist.
- Assumes `StorefrontTenantInterceptor` continues to resolve tenant from slug for public endpoints.
- Assumes the offline-sync `/sync` contract (`docs/guides/offline-sync-contract.md`) for projecting
  promotions/coupons to the app, and the existing `TaxCalculationEngine`/`DocumentTotalsCalculator`
  for tax after offers.

---

*Next steps*: run `/speckit.clarify` (confirm stacking/`conflictPolicy` defaults + free-goods tax
policy), then `/speckit.plan`, `/speckit.tasks`, `/speckit.analyze`, and stop for review before
`/speckit.implement`. Build order: **009 Pricing → 015 Promotions** (offers need resolved prices).
Develop on `claude/wonderful-dirac-zl47z7`; no PR unless requested.
