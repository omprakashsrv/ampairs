# Feature Specification: Dynamic Pricing & Replenishment

**Feature Branch**: `027-dynamic-pricing-replenishment`
**Created**: 2026-06-28
**Status**: Draft
**Input**: User description: "specs/027-dynamic-pricing-replenishment"

## Overview *(informative)*

Today every product carries a single flat selling price, a customer group can only carry one
blanket discount percentage, and inventory has a reorder field that nothing ever calculates. This
feature gives a business two related capabilities:

1. **Dynamic pricing** — owners define *pricing rules* (special prices and discounts that depend on
   who the customer is, how much they buy, and what time of year it is), and the right price is then
   applied automatically the moment a line is added to an order or invoice — including when the
   device is offline.
2. **Replenishment** — the system watches how fast each item sells and how long it takes to
   restock, and produces *reorder suggestions* (how low to let stock fall before reordering, and how
   much to reorder) that the user can turn into a draft purchase.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automatic, rule-based pricing at order time (Priority: P1)

A business owner sets up pricing rules: a 10% standing discount for the "Wholesale" customer group,
a quantity break that drops the unit price when 50+ units are bought, and a "Diwali" promotional
price on a category that is only valid 1–31 October. A salesperson later builds an order for a
Wholesale customer buying 60 units during the promo window. As each line is added, the correct unit
price appears automatically, the salesperson sees which rule produced it, and the same price is
produced whether the salesperson is online or completely offline in the field. When the order is
saved, the price that was shown is the price that is recorded — a later change to the rules does not
silently re-price the saved order.

**Why this priority**: This is the core value of the feature and the minimum viable product. Pricing
correctness directly affects revenue and customer trust, and the offline guarantee is what makes the
mobile sales workflow usable in the field. It can ship and deliver value entirely on its own.

**Independent Test**: Create several overlapping rules, capture an order both online and with the
device offline, and confirm (a) the applied price matches the most-specific/highest-priority rule,
(b) the online and offline prices are identical, (c) the applied rule is visible on the line, and
(d) editing the rules afterward does not change the already-saved order's price.

**Acceptance Scenarios**:

1. **Given** a Wholesale-group rule of 10% off and no other matching rule, **When** a salesperson
   adds a line for a Wholesale customer, **Then** the line's unit price is 10% below the catalog
   price and the line shows that the Wholesale rule was applied.
2. **Given** a quantity-break rule for 50+ units and a customer-group rule that both match, **When** a
   line of 60 units is added, **Then** exactly one rule wins by the documented precedence (one price,
   not both discounts compounded by default) and the result is shown to the user.
3. **Given** a promotional rule valid only 1–31 October in the business's timezone, **When** a line is
   priced on 30 September, **Then** the promo is **not** applied; **When** priced on 5 October,
   **Then** the promo **is** applied.
4. **Given** the device is offline, **When** lines are priced and the order is later synced, **Then**
   the price computed offline equals the price the server computes for the same inputs and rule set.
5. **Given** a saved order priced under a rule, **When** that rule is later edited or deactivated,
   **Then** the saved order keeps its original recorded price.
6. **Given** a salesperson manually overrides a line price, **When** the line is saved, **Then** the
   manual price wins over any rule and the line records that the price was set manually.

---

### User Story 2 - Reorder suggestions and draft purchases (Priority: P2)

An inventory manager opens a "needs reordering" view. For each item the system shows a suggested
reorder point (the stock level at which to reorder), a suggested order quantity, and the demand and
lead-time assumptions behind them. The manager selects several at-or-below-reorder-point items and
generates a single draft purchase, which carries a generated purchase number and can be reviewed and
adjusted before being committed. The suggested reorder level can be accepted onto the item, but the
inventory record remains the system of record — the suggestion never overwrites it silently.

**Why this priority**: Prevents stockouts and over-ordering, which is high business value, but it
depends on demand history accumulating and is independent of the pricing engine, so it is the second
deliverable.

**Independent Test**: Seed an item with a known sales history and lead time, run the suggestion
generation, and confirm the reorder point, safety stock, and order quantity match the expected
values for that demand and lead time; then create a draft purchase from selected suggestions and
confirm it is numbered and editable.

**Acceptance Scenarios**:

1. **Given** an item with a stable average daily demand and a known lead time, **When** suggestions
   are generated, **Then** the reorder point reflects demand-over-lead-time plus a safety buffer, and
   the buffer grows when demand variability or lead time grows.
2. **Given** several items at or below their suggested reorder point, **When** the manager generates a
   draft purchase, **Then** one draft is created with a generated number containing a line per
   selected item at its suggested quantity.
3. **Given** a generated suggestion, **When** the manager accepts the suggested reorder level onto the
   item, **Then** the item's reorder level is updated and the change is attributable to the
   suggestion; **When** the manager ignores it, **Then** the item's existing reorder level is
   unchanged.
4. **Given** a demand forecast is not yet available for an item, **When** suggestions are generated,
   **Then** the system falls back to the item's own recent movement history rather than producing no
   suggestion.

---

### User Story 3 - Natural-language pricing & replenishment questions (Priority: P3)

A user asks the in-app assistant questions such as "which items need reordering this week?" or
"what's the Wholesale price for SKU A-100?" and gets a correct answer drawn from the current rules
and suggestions, working on-device.

**Why this priority**: A convenience layer over the data the first two stories already produce. It
adds reach but is not required for the feature to deliver its core value.

**Independent Test**: With rules and suggestions present, ask each sample question and confirm the
answer matches what the rule/suggestion views show.

**Acceptance Scenarios**:

1. **Given** active reorder suggestions, **When** the user asks which items need reordering, **Then**
   the assistant lists the items at or below their reorder point.
2. **Given** a customer-group pricing rule, **When** the user asks for that group's price on a
   specific item, **Then** the assistant returns the price that the pricing engine would apply.

---

### Edge Cases

- **No rule matches a line** → the catalog/base price is used and the line records that no rule
  applied (catalog fallback), never an empty or zero price.
- **Two rules tie on every precedence factor** → the tie is broken deterministically (e.g. most
  recently effective, then a stable identifier) so the same winner is always chosen on every device
  and on the server.
- **A future-dated rule** must not affect today's pricing; an expired rule must stop applying the
  moment its window closes in the business timezone.
- **Back-dated document** (entered with a past business date) prices using the rules that were
  effective on that date, not the rules effective "now".
- **Device holds a stale rule set** → the offline price is still recorded; on sync the server
  re-validates against the current rules and the authoritative result is reflected back, without
  blocking the sale.
- **Currency rounding** → percentage and amount adjustments resolve to a clean rounded unit price
  with no fractional-currency drift, identical on device and server.
- **Item with no sales history and no forecast** → no misleading suggestion is fabricated; the item
  is surfaced as "insufficient data" rather than given a spurious reorder point.
- **Workspace switch** → rules and suggestions shown always belong to the active workspace only.

## Requirements *(mandatory)*

### Functional Requirements

**Pricing rules (P1)**

- **FR-001**: Owners MUST be able to create, edit, deactivate, and delete pricing rules scoped to a
  specific product, a product category, or all products.
- **FR-002**: A pricing rule MUST optionally target a customer group, an optional quantity band
  (minimum and/or maximum quantity), and an optional effective window (start and/or end date), any of
  which may be left open.
- **FR-003**: A pricing rule MUST express its adjustment as one of: a percentage off, a flat amount
  off, or a fixed override price.
- **FR-004**: The system MUST evaluate the applicable price for a line from the line's product,
  quantity, customer group, and the document's business date, choosing exactly one winning rule by a
  documented, deterministic precedence (priority, then specificity, then recency) — rules MUST NOT
  stack by default.
- **FR-005**: The system MUST provide a configurable, off-by-default option to allow one additional
  non-conflicting promotional rule to apply on top of the base rule, applied in a fixed order so the
  result remains deterministic.
- **FR-006**: The system MUST produce an identical resolved price for the same inputs and rule set
  whether evaluated on the device (offline) or on the server, including identical currency rounding.
- **FR-007**: The system MUST evaluate effective windows and seasonal/promotional dates against the
  workspace's business timezone, not the device or server local time.
- **FR-008**: When a line is saved, the system MUST record (snapshot) the resolved unit price, the
  winning rule (if any), and the price source (rule / catalog fallback / manual) so that later rule
  changes never re-price a saved document.
- **FR-009**: A manual price override on a line MUST take precedence over any rule and MUST be
  recorded as a manual price source for audit.
- **FR-010**: When no rule matches, the system MUST fall back to the product's catalog price and
  record that fallback.
- **FR-011**: The existing single per-customer-group discount MUST be preserved as an equivalent
  lowest-priority pricing rule, so no current pricing behavior is lost.
- **FR-012**: Pricing rules MUST be available on the device offline and stay current via the standard
  synchronization mechanism, and the server MUST re-validate the device-resolved price on sync and be
  authoritative on the rule set without blocking an offline sale.

**Replenishment (P2)**

- **FR-013**: The system MUST estimate, per inventory item, an average daily demand and a measure of
  demand variability from available demand history, preferring a published demand forecast and
  falling back to the item's own recent movement history when no forecast exists.
- **FR-014**: The system MUST compute, per item, a safety-stock buffer, a reorder point, and a
  suggested order quantity from demand, demand variability, lead time, and configurable cost/service
  settings.
- **FR-015**: Lead time, target service level, and ordering/holding cost assumptions MUST be
  configurable per workspace, with sensible defaults.
- **FR-016**: The system MUST surface reorder suggestions, indicating which items are at or below
  their reorder point, with the assumptions behind each suggestion visible to the user.
- **FR-017**: Users MUST be able to generate a draft purchase from one or more reorder suggestions;
  the draft MUST carry a generated purchase number and be reviewable and editable before commit.
- **FR-018**: The system MUST treat reorder levels as *suggestions* — accepting a suggestion updates
  the item's reorder level, but the inventory record remains the system of record and is never
  overwritten without an explicit accept.
- **FR-019**: Suggestions MUST refresh when demand or stock changes materially and on a regular
  scheduled basis, and MUST be available to review on the device.
- **FR-020**: When an item has insufficient demand data, the system MUST mark it as such rather than
  fabricate a reorder point.

**Cross-cutting**

- **FR-021**: All pricing rules and replenishment suggestions MUST be isolated per workspace; a user
  only ever sees and acts on data for the active workspace.
- **FR-022**: Pricing and replenishment data MUST be answerable through the in-app natural-language
  assistant's on-device query path (P3).

### Key Entities *(include if feature involves data)*

- **Pricing Rule**: A workspace-scoped, effective-dated, prioritized rule. Attributes: scope
  (product / category / all) and its reference, optional customer group, optional quantity band,
  optional effective window, adjustment type (percentage off / amount off / fixed price) and value,
  priority, and active flag.
- **Price Resolution (result)**: The outcome of evaluating rules for a line — resolved unit price,
  currency, the winning rule (if any), and the price source (rule / catalog fallback / manual). It is
  snapshotted onto the order/invoice line.
- **Reorder Suggestion**: A per-item computed recommendation. Attributes: average daily demand, demand
  variability, lead time, safety stock, reorder point, suggested order quantity, status, and the time
  it was generated.
- **Draft Purchase**: A reviewable, numbered draft built from selected reorder suggestions, with one
  line per item at its suggested quantity, edited before being committed.
- **Pricing/Replenishment Settings**: Per-workspace policy — stacking on/off, target service level,
  default lead time, ordering cost, holding cost.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For any line, the price shown on the device and the price computed by the server for the
  same inputs and rule set are identical in 100% of cases, including at currency-rounding boundaries.
- **SC-002**: A salesperson with no network connectivity can price and save a complete multi-line
  order with rule-based prices, with no step blocked on connectivity.
- **SC-003**: A saved order's recorded line prices remain unchanged after any subsequent edit,
  deactivation, or deletion of the pricing rules that originally applied — verified across 100% of
  re-opened/reprinted documents.
- **SC-004**: An owner can configure a customer-group discount, a quantity break, and a seasonal promo
  and see each correctly applied (and correctly *not* applied outside its window) within a single
  setup session, without needing support.
- **SC-005**: For an item with a known steady demand and lead time, the generated reorder point and
  safety stock match the expected values for that demand and lead time, and increase when demand
  variability or lead time increases.
- **SC-006**: A reorder suggestion can be turned into a numbered, editable draft purchase in under a
  minute for a batch of items.
- **SC-007**: Accepting or ignoring a reorder suggestion never changes an item's reorder level except
  through an explicit accept action (no silent overwrites), verified in 100% of cases.
- **SC-008**: The in-app assistant answers "which items need reordering" and "what is the {group}
  price for {item}" with answers matching the rule/suggestion views.

## Assumptions

- This feature targets the existing order/invoice capture flow on mobile (Android/iOS/Desktop) and the
  backend service; a web admin screen for authoring rules is a tracked follow-up, not part of the
  initial scope.
- Money is handled with exact, non-floating arithmetic and a single rounding step to the workspace
  currency's minor unit, so cross-platform results agree exactly.
- Demand input for replenishment comes from the forecasting capability (feature 022) when available,
  with a movement-history fallback; replenishment never reads forecasting's internal data directly.
- Default target service level corresponds to roughly 95% (a standard retail default) and is
  adjustable; default lead time and cost assumptions are workspace-configurable.
- "Most specific" precedence means product-scoped beats category-scoped beats all-products, a
  group-targeted rule beats an untargeted one, and a quantity-banded or dated rule beats an open one,
  with priority taking precedence over specificity and recency as the final tie-break.
- Inventory remains the owner of the committed reorder level; this feature only *suggests*.

## Dependencies

- **Inventory (spec 014)** — owns stock levels, movement history, and the committed reorder level.
- **Forecasting (feature 022)** — publishes the demand signal consumed by replenishment.
- **Customer / Product** — supply customer groups and catalog prices/categories that rules target.
- **Order / Invoice** — host the line where the resolved price is applied and snapshotted.
- **Workspace settings, sequence numbering, and offline synchronization** — reused infrastructure for
  policy, purchase numbering, and on-device rule/suggestion availability.
