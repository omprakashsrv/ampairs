# Feature Specification: Product-Brand Attribution (Hop A — ProductBrand ↔ Brand Workspace)

**Parent feature**: `021-brand-distributor-dms-sfa` (this is a sub-spec; develop on branch
`claude/brand-distributor-dms-sfa-12692h`, PR #170)
**Created**: 2026-06-29
**Status**: Draft
**Input**: User description: "product-brand-attribution (Hop A, ProductBrand↔workspace)"

## Overview

In the Brand → Distributor DMS (feature 021), a brand and a distributor are **separate businesses
(separate workspaces) with separate product catalogs**. A single distributor typically carries **many
brands**' products. Before a brand can see "how much of *my* product is selling through this distributor",
the system must know **which of the distributor's products belong to that brand** — and exclude every other
brand's products.

This sub-spec defines the **primary, cheap attribution mechanism ("Hop A")**: linking the distributor's
**existing in-catalog brand label** (the `ProductBrand` a distributor already tags products with, e.g.
"Nestlé") to the **brand's workspace** over the consented trade link. Once a distributor designates that
"my 'Nestlé' product-brand label corresponds to the linked Nestlé workspace", every product under that
label — and its secondary sales and stock — is attributed to that brand.

Hop A answers the question *"is this the brand's product?"* (attribution + competitor exclusion). It is
deliberately **independent of, and a prerequisite to**, the optional SKU-level identity mechanism ("Hop B",
`NetworkProduct`, covered separately) that reconciles a distributor's specific product to the brand's
specific SKU for SKU-grain rollups. A product attributed by Hop A but not yet SKU-matched by Hop B still
counts toward the brand's totals — it is never dropped.

**Why this matters / what it fixes**: the parent spec currently makes SKU-level mapping carry the whole
load (a distributor product with no confirmed SKU mapping is *excluded* from the brand view). That makes a
brand **undercount its own sales** until every SKU is individually mapped, and ignores the brand label the
distributor has *already* assigned. Hop A makes brand attribution complete and cheap, reusing existing data.

## Clarifications

### Session 2026-06-29

- Q: Can more than one of a distributor's product-brand labels map to a single linked brand? → A: Yes — a
  distributor may designate one or more of its labels (handling aliases/duplicates like "Nestlé" and
  "Nestle India") as corresponding to the same linked brand workspace; the brand sees the union.
- Q: How is a distributor product attributed when it has no brand label at all? → A: It is **not** attributed
  to any brand — attribution requires the product to be under a label the distributor has designated for an
  active link. Untagged products never surface to any brand.
- Q: When a product is re-tagged to a different brand, does its sales history move? → A: No —
  **point-in-time** attribution: each sale's brand attribution is fixed at sale time; re-tagging affects only
  subsequent sales, and a snapshot recompute never rewrites a brand's historical totals.
- Q: How are attributed-but-unmapped (no SKU match) sales shown to the brand? → A: As a **single aggregated
  "unmapped" total** per period/grain (qty/value only) — the brand never sees the distributor's individual
  product names/codes for unmapped items (the distributor does the SKU mapping and sees its own catalog).
- Q: Can the brand see which distributor has designated a label for it? → A: Yes — **distributor-controlled,
  brand-visible read-only**: the brand sees that an active designation exists per linked distributor and the
  resulting attributed totals, but cannot alter it (no two-sided approval handshake).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Distributor designates its brand label, brand sees only its own sales (Priority: P1)

A distributor already organizes its catalog by brand (it tags each product it stocks with an in-catalog
brand label). When it accepts a link from a brand, it designates which of its brand labels corresponds to
that brand's business. From then on, the brand's DMS view shows the secondary sales and stock of **all the
distributor's products under that label** — and nothing else. Products the distributor carries for other
brands never appear in this brand's view.

**Why this priority**: This is the core attribution edge — without it the brand either sees nothing,
everything (including competitors), or only the slice that has been SKU-mapped. It is a prerequisite for
every brand-facing product figure in feature 021 (secondary sales, stock, targets, schemes).

**Independent Test**: A distributor carrying two brands' products links to Brand A, designates its "Brand A"
label, and records sales of both brands' products → Brand A's secondary-sales view shows exactly the Brand A
products' sales and none of Brand B's; Brand B (if linked) sees only its own.

**Acceptance Scenarios**:

1. **Given** a distributor with products tagged to its in-catalog brand labels and an active link to Brand A,
   **When** the distributor designates its "Brand A" label as corresponding to Brand A's workspace, **Then**
   all products under that label are attributed to Brand A.
2. **Given** that designation, **When** the distributor records retailer sales of products under the "Brand A"
   label, **Then** those sales appear in Brand A's secondary-sales view.
3. **Given** the distributor also sells products under a different (undesignated) label, **When** Brand A
   views its secondary sales, **Then** those other-brand sales do **not** appear.
4. **Given** the distributor designates two of its labels (aliases) for Brand A, **When** Brand A views its
   sales, **Then** it sees the combined sales under both labels.
5. **Given** no designation exists for an active link, **When** the brand views its data, **Then** it sees
   no attributed products (empty), not all of the distributor's catalog.

---

### User Story 2 - Brand-tagged sales count even before SKU mapping (Priority: P2)

A brand wants its total "product moving through this distributor" figure to be **complete** from day one —
not to grow only as each individual SKU gets reconciled. A product attributed to the brand by its label,
but not yet matched to a specific brand SKU, must still be counted in the brand's totals (shown under an
"unmapped SKU" bucket), rather than disappearing until someone maps it.

**Why this priority**: Completeness of the headline number is what makes the DMS trustworthy; if totals
silently undercount until mapping is finished, the brand can't rely on the dashboard. This decouples
attribution (Hop A) from SKU reconciliation (Hop B).

**Independent Test**: With a designated label and zero SKU-level mappings, the brand's total secondary-sales
value equals the sum of all sales of products under that label; as SKU mappings are added, the same total is
progressively itemized by brand SKU without the total changing.

**Acceptance Scenarios**:

1. **Given** a designated label and no SKU-level mappings, **When** the brand views its secondary-sales
   total, **Then** it equals the sum of all designated-label product sales.
2. **Given** some products under the label are later SKU-matched, **When** the brand views the breakdown,
   **Then** matched sales appear under their brand SKU and the remainder under an "unmapped" bucket, and the
   grand total is unchanged.

---

### User Story 3 - Designation and tag changes stay correct over time (Priority: P3)

Catalogs change: a distributor re-tags a product to a different brand, removes a tag, adds a new label, or
the link is revoked. The brand's attributed view must follow these changes — promptly and without leaking or
losing data.

**Why this priority**: Correctness over time prevents silent drift (a product that switches brands must move
in the brand's view), but it builds on US1/US2.

**Independent Test**: Re-tagging a product from Brand A's label to Brand B's label moves its future sales out
of Brand A's view and into Brand B's; revoking the link stops attribution entirely.

**Acceptance Scenarios**:

1. **Given** a product under Brand A's designated label, **When** the distributor re-tags it to Brand B's
   label, **Then** its subsequent sales attribute to Brand B (and stop attributing to Brand A).
2. **Given** an active designation, **When** the distributor removes the designation (or the link is
   revoked), **Then** the brand no longer sees any attributed products.
3. **Given** a product whose brand label is removed entirely (untagged), **When** any brand views its data,
   **Then** that product is not attributed to any brand.

---

### Edge Cases

- **Untagged product**: A distributor product with no brand label is never attributed to any brand.
- **Label maps to a brand with no active link**: No attribution occurs until a link to that brand is active.
- **Alias labels**: Two distributor labels designated for the same brand are unioned, with no double-counting
  of a product that somehow sits under both.
- **Re-tag mid-period**: Attribution is point-in-time — each sale keeps the attribution it had at sale time;
  re-tagging changes only future sales, and a snapshot recompute does **not** move historical attribution.
- **Designation removed / link revoked**: Attribution stops going forward; prior published figures are not
  retroactively altered beyond ceasing further sharing.
- **Distributor designates a label that matches multiple linked brands** (shouldn't happen): a label may be
  designated for at most one brand per active link; conflicting designations are rejected.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-A01**: The system MUST let a distributor designate one or more of its in-catalog brand labels as
  corresponding to a linked brand's workspace, over an **active** trade link.
- **FR-A02**: The system MUST attribute every distributor product under a designated label — and that
  product's secondary sales and stock — to the corresponding brand.
- **FR-A03**: The system MUST exclude from a brand's view any distributor product that is not under a label
  designated for that brand (other-brand and untagged products never appear to the brand).
- **FR-A04**: A distributor product attributed to a brand by its label but **not** reconciled to a specific
  brand SKU MUST still be counted in that brand's totals, presented as a **single aggregated "unmapped"
  total** per period/grain (qty/value only), never dropped and never itemized by the distributor's individual
  product name/code. (Attribution is independent of, and a prerequisite to, SKU-level identity.)
- **FR-A05**: The system MUST support designating multiple distributor labels (aliases) for one brand,
  presenting their union without double-counting a product.
- **FR-A06**: Attribution MUST follow catalog changes: re-tagging a product to a different brand's label
  moves its subsequent sales/stock to that brand; removing the designation or revoking the link stops
  attribution going forward.
- **FR-A07**: A distributor label MUST be designatable for **at most one** brand per active link; a
  conflicting designation MUST be rejected with a clear reason.
- **FR-A08**: Designating, changing, or removing an attribution MUST require an active trade link and the
  distributor's authority; the brand cannot designate on the distributor's behalf.
- **FR-A09**: A change to a designation or to a product's brand label MUST be reflected in the brand's
  attributed view within the parent feature's data-freshness window (see Success Criteria).
- **FR-A10**: Attribution MUST be **point-in-time**: each sale's brand attribution is determined at sale
  time and fixed thereafter. Re-tagging a product or changing a designation affects only subsequent sales; a
  snapshot recompute MUST NOT retroactively move a brand's historical attributed totals. (Implies the
  attribution in effect at sale time is captured, not derived live from the product's current label.)
- **FR-A11**: A brand MUST be able to see, read-only, that an active designation exists for each linked
  distributor and the resulting attributed totals; the brand MUST NOT be able to create or alter a
  designation (no two-sided approval is required for attribution to take effect).

### Key Entities *(include if feature involves data)*

- **Distributor Brand Label**: The distributor's existing in-catalog brand grouping (one of many the
  distributor maintains) that products are tagged with. Pre-exists this feature; reused, not recreated.
- **Brand Attribution (designation)**: The consented mapping, per trade link, from one of the distributor's
  brand labels to the linked brand's workspace. The Hop-A edge. Carries which label, which link/brand, and
  status (active/removed).
- **Attributed Product (view concept)**: A distributor product surfaced to a brand because it sits under a
  label designated for that brand; carries the brand attribution and, when present, its SKU-level brand
  match (Hop B) — otherwise bucketed "unmapped SKU".

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-A01**: After a distributor designates a label for Brand A, **100%** of the distributor's sales of
  products under that label appear in Brand A's secondary-sales view, and **0%** of products under
  undesignated labels appear.
- **SC-A02**: Brand A's total attributed secondary-sales value **equals** the sum of all designated-label
  product sales, **regardless of how many products are SKU-mapped** (completeness independent of Hop B).
- **SC-A03**: A designation change or a product re-tag is reflected in the brand's attributed view within the
  parent feature's freshness window (≈5 minutes).
- **SC-A04**: Revoking the link or removing the designation stops all further attribution to that brand
  immediately, with no residual products surfacing.
- **SC-A05**: For a distributor carrying multiple brands, no product under one brand's label is ever visible
  to a different linked brand (zero cross-brand leakage), verified per linked brand.

## Assumptions

- **Distributors already tag products by brand**: The in-catalog brand label is an existing capability;
  distributors routinely organize their catalog this way. Where a distributor has not tagged a product, it
  is simply unattributed (FR-A03) until tagged.
- **Attribution is brand-level, identity is SKU-level**: This sub-spec covers attribution only (Hop A). The
  optional SKU-level reconciliation (Hop B) that lets the brand roll up by its *specific* SKU is a separate
  concern; the two compose — Hop A decides *whose* product, Hop B decides *which* SKU.
- **One label → at most one brand per link**: A given distributor label is not split across competing brands
  on the same link (FR-A07); aliases for the *same* brand are allowed (FR-A05).
- **Storage/sync remain workspace-isolated**: Attribution is a consented overlay; it never merges the two
  workspaces' catalogs.

## Dependencies

- Parent feature **021** — the consented trade link (`TradeLink`) is the edge attribution rides on; the
  secondary-sales/stock snapshots are where attributed figures surface; SKU-level identity (Hop B,
  `NetworkProduct`) is the complementary refinement.
- Existing **product catalog** capability — the distributor's in-catalog brand labels and product→label tags
  (reused, not recreated).

## Out of Scope

- **SKU-level product identity (Hop B)**: reconciling a distributor's specific product to the brand's
  specific SKU (barcode/GTIN matching) — specified separately; this sub-spec only requires that unmatched-
  but-attributed products still count (FR-A04).
- Brand-side catalog publishing mechanics and SKU auto-match — part of Hop B.
- Any change to how a distributor creates or manages its in-catalog brand labels (pre-existing capability).
