# Handoff: Store Ops — Order & Invoice with GST

## Overview

A store-operations feature for **store staff** to create **sales orders** and **GST tax
invoices** for customers in India — fast order-desk entry, not consumer checkout. Orders and
invoices share one line-item pattern and one totals engine; either can be created independently,
or an invoice can be generated from an order. The flow is **offline-first** and **adaptive**
(single-pane on phone, list-detail two-pane on tablet/desktop, keyboard-first on desktop).

This bundle is the **design layer** (low-fi wireframes + a precise implementation spec) for the
work already specified in `spec/spec.md` (feature `010-store-ops-order-invoice`).

---

## About the design files

The files in `wireframes/` are **design references created in HTML** — low-fidelity wireframes
that communicate **structure, flow, adaptive behavior, and the states each screen must handle**.
They are **not production code to copy**. The HTML uses a hand-drawn "sketchy" style purely to
signal that visuals are not final; colour in the wireframes is used only to mark *roles*
(violet = primary/focus, amber = sticky/attention, green = synced, red = error).

Your task is to **recreate these designs in the existing `ampairs-app` codebase** using its
established patterns — **not** to ship the HTML.

### Target environment (from `spec/spec.md` + the Ampairs design system)

- **App:** `ampairs-app` — Kotlin Multiplatform (Android, iOS, **Desktop JVM**, WASM).
- **UI:** **Compose Multiplatform** + **Material 3** themed via **Material Kolor** (dynamic color
  from a seed). All UI components map to M3 (see the swap notes below).
- **Architecture:** MVI, offline-first sync, Metro DI. See `spec/plan.md` and the project's
  `/offline-sync`, `/metro-di`, `/cmp-practices` rules. Use `collectAsStateWithLifecycle`,
  `stringResource` only (no hardcoded UI strings).
- **Backend:** `ampairs` (Spring Boot, multi-tenant). New bulk/paginated sync endpoints are
  specified in `spec/sync-api.md` and the FR-B0x requirements.

> The backend treats the **app as the tax-calculation authority** — it stores `taxInfos`/`totalTax`
> as supplied and does not recompute. Getting the client-side math right (below) is therefore the
> single most important part of this feature.

---

## Fidelity

**Low-fidelity (wireframes).** Use them as the source of truth for **layout, hierarchy, flow,
adaptive behavior, sticky-vs-scroll regions, primary-vs-secondary actions, and required states.**
Apply the **Ampairs Material 3 design system** (tokens in `tokens/ampairs-colors-and-type.css`,
summarized below) for all actual styling — colors, type ramp, spacing, shape, elevation. Do not
reproduce the sketchy look.

---

## Screens / Views

Eleven surfaces. The same document model renders three ways: **single-pane** (phone),
**list-detail two-pane** (tablet/desktop), with desktop adding **keyboard-first** line entry.
"Compact" = phone; "Expanded" = tablet & desktop. Open `wireframes/Order & Invoice Wireframes.html`
and use the **Screen inventory** and **Wireframes** tabs alongside this list.

### 1 · Document list
- **Purpose:** browse orders + invoices, search, see status & sync at a glance, start a new doc.
- **Layout — compact:** full-width list rows; tap → navigate to detail. FAB (`+`) bottom-right.
- **Layout — expanded:** `NavigableListDetailPaneScaffold` — left list rail (~34%) + right detail
  preview; selecting a row highlights it in place (2px primary ring) and loads the detail pane (no
  navigation).
- **Components:**
  - **Sticky** search field (`OutlinedTextField`, leading search icon) — searches document number
    and customer name/phone.
  - **Filter chips** row (`FilterChip`): All / Orders / Invoices + status (Draft).
  - **List row:** type icon (`shopping_cart` order / `receipt_long` invoice), document number
    (mono), customer name, date (`dd MMM yyyy`), amount (mono ₹, right-aligned), and a **sync chip**
    (see States S10–S13).
  - **Primary action:** "**+ New ▾**" — a split/menu button choosing Order or Invoice (FAB on phone,
    button in the top app bar on desktop).

### 2 · Document editor — header
- **Purpose:** customer, date, and document settings.
- **Sticky** header band above the line area (collapsible card on phone, pinned band on desktop).
- **Components:** customer selector (`AssistChip`/field with M3 avatar showing initials, opens
  customer picker / walk-in capture); date field; **price-mode segmented toggle** (Tax-exclusive /
  Tax-inclusive — `SegmentedButton`, **first-class**, see math); invoice-number field (dashed/ghost,
  shows the number that will be assigned on save).

### 3 · Line-items area + add-line
- **Purpose:** the list of lines and the add-line affordance.
- **Layout — compact:** stacked **line cards**; tapping a card opens the per-line bottom sheet.
- **Layout — expanded:** an editable **grid/table** (columns: Product · Unit · Qty · Unit price ·
  Disc · Taxable · GST · Line total). **Keyboard:** `Tab` moves cell→cell; **`Enter` commits the
  line and starts a fresh one focused on Product**; this is the desktop optimization for fast
  repeated entry. The editing row is highlighted.
- **Primary action:** "**+ Add line**".

### 4 · Per-line editor
- **Purpose:** product, variant, unit, qty, overridable unit price, line discount.
- **Compact:** **bottom sheet** (`ModalBottomSheet`) — one line at a time, large touch targets,
  "Done · next" advances to a new line.
- **Expanded:** the same fields **inline in the grid row** (no modal).
- **Components:** product (opens picker, screen 5), variant chip (if product has variants), **unit
  selector + live conversion hint** (screen 7), Qty field (decimal-constrained), **overridable** Unit
  price field (✎), line discount (% / flat switch + value), and a live mini-breakdown (Taxable / GST /
  Line total).

### 5 · Product picker + inline create entry
- **Purpose:** search catalog, choose a product, or create one inline.
- **Compact:** full-screen search sheet. **Expanded:** type-ahead popover/dropdown under the Product
  cell (no takeover).
- **Components:** **sticky** search field; result rows (name, price, HSN chip, GST% chip, unit);
  **the last row is always "+ Create '<typed text>'"** when there's no exact match → opens screen 6.

### 6 · Inline create-product
- **Purpose:** minimal new product so entry isn't blocked.
- **Compact:** nested bottom sheet over the picker. **Expanded:** compact dialog.
- **Components (only these four):** Name; Selling price ₹; Base unit; **HSN / tax code** (resolves
  the GST rate, shown as a chip). Saves to catalog offline (`PENDING_PUSH`) then drops in as a line.
  A blank tax code is allowed but the line is flagged 0%/exempt (never silently untaxed).

### 7 · Unit selector with conversion hint
- **Purpose:** transact in base or any derived unit.
- **Compact:** small bottom sheet/menu. **Expanded:** `DropdownMenu` on the unit cell.
- **Components:** list of units (base shown as "(base) ×1", derived units show their multiplier);
  selected unit highlighted; a **live hint**: e.g. `1 BOX = 24 PCS · price ₹90 → ₹2,160 / BOX ·
  qty allows 0 decimals`. Products with no conversions show only the base unit (×1) — no error.

### 8 · Sticky totals
- **Purpose:** subtotal, grouped CGST/SGST/IGST, discounts, grand total — always reconciling.
- **Compact:** collapsed **bar** pinned at the bottom (grand total always visible); tap ▴ expands the
  full breakdown as a sheet.
- **Expanded:** pinned right column (desktop) / pinned bottom band (tablet).
- **Components:** Subtotal (taxable), Overall discount line, each tax component (CGST / SGST / IGST,
  grouped by rate), and a prominent Grand total. The **overall discount control** (% / ₹ switch +
  value, with the active discount *mode* labelled) lives **with the totals** (the numbers it changes).

### 9 · Save bar
- **Compact:** sticky bottom action bar. **Expanded:** Save in the top app bar + **`⌘S`/`Ctrl+S`**.
- Shows live **sync state** ("saved offline" → "syncing" → "synced"). Primary action: **Save**.

### 10 · Document detail / print view
- **Purpose:** read-only; an **invoice must look like a GST tax invoice** and be printable.
- **Layout:** document on a "paper" surface; actions in the app bar / rail.
- **Components:** "TAX INVOICE" title, seller GSTIN + state, **prominent invoice number** (mono),
  date; Bill-to block (name, GSTIN, state); line table (Item/HSN, Qty+unit, Rate, Taxable, CGST,
  SGST [or IGST], Amount); **tax summary grouped by rate**; Taxable / Total GST / Grand total; amount
  in words. Actions: **Print**, Share, Edit (and "Create invoice" on an order detail).

### 11 · Order → invoice confirmation
- **Purpose:** confirm conversion and surface what copies over.
- **Layout:** centered `AlertDialog` (all sizes).
- **Components:** title; a checklist of what's copied (lines/units/prices, tax breakdown & discounts,
  cross-link order⇄invoice, invoice number on save); a note that a re-conversion **opens the existing
  invoice** (idempotent — no duplicate). Primary: **Create invoice**.

---

## Interactions & Behavior

- **Live recalculation:** every line add / remove / qty / price / discount / unit / customer change
  re-runs that line's GST and re-aggregates the document totals immediately (no "calculate" button).
  Show a brief shimmer on the totals while recomputing (State S8), not a blocking spinner.
- **Price-mode toggle** (Tax-exclusive ↔ Tax-inclusive): flips the basis for **every number** on the
  document — recompute all lines and totals.
- **Customer change** sets place-of-supply, which flips the whole document between CGST+SGST (intra)
  and IGST (inter).
- **Desktop keyboard flow:** `Tab` across cells; `Enter` commits + opens a new line on Product;
  `⌘S`/`Ctrl+S` saves. Focus order follows the line-item anatomy (Product → Variant → Unit → Qty →
  Price → Discount).
- **Walk-in capture:** invoice flow lets you skip a customer record and capture name + phone (+
  optional GSTIN/state); defaults to intra-state against the seller state.
- **Idempotent conversion:** if `order.invoiceRefId` is set, "Create invoice" opens the existing
  invoice instead of creating a new one.
- **Offline-first save:** save persists locally (`synced = false`, `PENDING_PUSH`) and the row
  appears in the list immediately; sync pushes later and reconciles the server number.

---

## The calculation rules (the crux — implement exactly)

These come from `spec/spec.md` (FR-003, FR-004, FR-014–FR-017, C1–C5) and `spec/data-model.md`.
Build them as **pure, unit-tested calc-core functions** (`feature/tax`), independent of UI.

**Ordering (always):** line discount → overall-discount apportionment → **then** tax. Never tax then
discount.

1. **Place of supply → scenario.** Derive buyer & seller state from the first 2 digits of the GSTIN
   (fallback: address state → seller state). **Same state ⇒ INTRA (CGST + SGST). Different state ⇒
   INTER (IGST).** (NB: the legacy `TaxSpec` had this inverted — fix it.)
2. **Unit scaling (FR-014).** `baseQuantity = quantity × multiplier` (via `UnitConversionEngine`);
   per-unit price = per-base-unit price × multiplier. Constrain qty input to the unit's
   `decimalPlaces`. No conversion defined ⇒ base unit, multiplier 1.
3. **Line discount (FR-015, pre-tax):** percent → `value = base × pct`; flat → as entered. Reduces
   that line's taxable base only. Floor taxable at 0 (warn, no negative tax — State S14).
4. **Overall discount (FR-015, C2 — support both modes):**
   - `PRE_TAX_APPORTIONED` (GST-compliant): distribute across lines **proportional to each line's
     pre-discount taxable**; assign any rounding remainder to the largest line so the parts sum
     exactly to the entered discount; recompute each line's GST on its net taxable.
   - `POST_TAX_REDUCTION`: compute full GST per line, then subtract the discount from the grand total
     (per-HSN tax unaffected).
5. **Tax basis (FR-003, C1):**
   - `TAX_EXCLUSIVE`: taxable = price; GST added on top.
   - `TAX_INCLUSIVE`: `taxable = price / (1 + Σrate)`; extract GST out of the price. Round the
     extracted taxable and each component so components + taxable sum back to the inclusive amount
     exactly.
6. **Rounding:** each component to 2 decimals, half-up; **line total = taxable + Σ components** so
   displayed totals always reconcile.
7. **Invoice numbering (FR-017, C4/C5):** assign at **save**, on the **client**, from a **per-series**
   sequence (series = device/counter, optionally branch + financial year, with a distinct prefix),
   strictly consecutive. Offline invoices are fully numbered & printable. Backend enforces
   `UNIQUE(owner, series, sequence_number)` and **rejects** collisions (never silently renumbers).

**Worked examples to assert against** (seller in Maharashtra, 27):
- 2 BAG cement @ ₹385, 28%, intra → taxable ₹770, CGST 14% ₹107.80 + SGST 14% ₹107.80, line ₹985.60.
- Same line, buyer in Karnataka (29) → IGST 28% ₹215.60, line ₹985.60.
- Two lines ₹500 @ 18% with a **flat ₹200 overall discount (apportioned)** → ₹400 taxable/line,
  GST ₹72/line, **grand total ₹944.00**.
- Inclusive: price ₹385 incl. @ 28% → taxable ₹300.78, GST ₹84.22 (sum back to ₹385).

---

## State Management

MVI per `/cmp-practices`. Per document, the UI state needs at minimum:
- `priceMode` (`TAX_EXCLUSIVE | TAX_INCLUSIVE`), `overallDiscountMode`
  (`PRE_TAX_APPORTIONED | POST_TAX_REDUCTION`), defaulted from a business setting (FR-B10).
- `scenario` (INTRA | INTER), derived from customer vs seller state.
- `customer` / walk-in details; `date`; assigned/`pending` invoice number.
- `lines: List<LineState>` — each: product, variantSku?, `unitId`, `quantity`, `baseQuantity`,
  `unitPrice` (overridable), `lineDiscount` (kind + amount), and the **computed** taxable / taxInfos /
  totalTax / lineTotal.
- `overallDiscount` (kind + amount); computed document `taxInfos` / `totalTax` / `totalAmount`.
- `syncState` (saved-offline / syncing / synced / failed); `validation` flags (e.g. missing tax code).

Transitions: any line/header/discount/customer/price-mode edit → re-run calc-core → new state. Save →
persist local (`synced=false`, `markPendingPush(ORDER|INVOICE)`), assign invoice number for invoices.
Sync delegates own all network traffic (`OrderSyncDelegate` `dependsOn=[CUSTOMER, PRODUCT]`,
`InvoiceSyncDelegate` `dependsOn=[CUSTOMER, PRODUCT, ORDER]`). See `spec/data-model.md` §6 and
`spec/sync-api.md`. Data fetching: catalog/customer/unit/tax-rule data is synced locally; read from
Room, never call APIs from repositories.

### Required states to design for (Wireframes → States tab)
empty list · calculating · CGST/SGST line vs IGST line · inclusive vs exclusive · line + overall
discount · discount-floors-at-zero warning · validation error (missing tax code) · saved-offline ·
syncing · sync-failed (inline retry) · fully-synced numbered invoice.

---

## Design Tokens (Ampairs Material 3)

Full token file: `tokens/ampairs-colors-and-type.css`. In the app these are the
**`MaterialTheme.colorScheme` / `Typography` / `Shapes`** slots (Material Kolor generates the scheme
from the seed). Key values:

**Color (light) — seed from the logo:**
- Primary (violet) `#7026B5`, on-primary `#FFFFFF`, primary-container `#EEDBFF`.
- Secondary (amber-orange) `#8B5300`, secondary-container `#FFDDB4`.
- Tertiary (indigo, used for "default/selected") `#5156A9`, tertiary-container `#E0E0FF`.
- Error `#BA1A1A`, error-container `#FFDAD6`.
- Background/surface `#FEFBFF`; surface-container `#F0EDF1`; surface-container-low `#F6F2F7`;
  on-surface `#1C1B1B`; on-surface-variant `#5F5E5F`; outline-variant for dividers.
- Brand mark colors (logo/splash only): orange `#F38A1C`, amber `#FFB400`, violet `#4F1B7E`.
- Semantic for sync chips: success/green (synced), info/blue (syncing), amber (saved-offline),
  error/red (failed). Use M3 container/on-container pairs, not the raw wireframe hexes.

**Type:** **Roboto** (display/headline/title/body/label), **Roboto Mono** for **currency & GST
numerics / document numbers** (tabular), Noto Sans Devanagari for `lang=hi`. Preserve the 15 M3
roles; app runs at **density -2**. Money is `Double` for this feature (no `Money` type yet); format
₹ with Indian digit grouping; dates `dd MMM yyyy`.

**Shape:** M3 corner scale 4 / 8 / 12 / 16 / 28 / full. Buttons ~28 (pill), cards 16, chips full,
dialogs 28. **Spacing:** 4 / 8 / 12 / 16 / 24 / 32 / 48 (steps down one notch on mobile).
**Elevation:** cards shadow-1 (hover → shadow-2, translateY -2px on web), dialogs shadow-3, sticky
toolbars shadow-1. **Motion:** standard easing `cubic-bezier(0.4,0,0.2,1)`, ~300ms; no spring/bounce.

**Wireframe colour → real role mapping:** violet = primary action / focus → `primary`; amber =
sticky/attention → use elevation + a pinned surface, not a colour; green/blue/amber/red sync chips →
M3 semantic container colours; the sketchy borders are **not** part of the design.

### M3 component swap notes
List rows → `ListItem` in `NavigableListDetailPaneScaffold`; filter chips → `FilterChip`; price-mode
& %/₹ → `SegmentedButton`; line editor (phone) → `ModalBottomSheet`; product picker (phone) →
full-screen `ModalBottomSheet`, (desktop) → `ExposedDropdownMenuBox`/popover; unit selector →
`DropdownMenu`; totals → pinned `Surface`/`BottomAppBar`; confirm → `AlertDialog`; new-doc → split
button / `FloatingActionButton` (phone); fields → `OutlinedTextField`.

---

## Assets

- `wireframes/ampairs-mark.png` — Ampairs brand mark (transparent). Use the **real** launcher/brand
  assets already in `ampairs-app` (`androidApp/.../res/`); don't redraw the mark.
- No in-product illustrations — empty/loading states are **icons + copy only** (matches the app).
- Icons: Material Icons / Material Symbols names — e.g. `shopping_cart` (orders), `receipt_long`
  (invoices), `inventory_2` (products), `percent` (tax), `search`, `add`, `sync`. In Compose use
  `Icons.Default.*` equivalents.

---

## Files in this bundle

| Path | What it is |
| --- | --- |
| `README.md` | This document — self-sufficient implementation brief. |
| `wireframes/Order & Invoice Wireframes.html` | The low-fi wireframes (open in a browser). Tabs: Overview · Flows · Screen inventory · Wireframes · States. |
| `wireframes/wireframe.css`, `wireframes/ampairs-mark.png` | Support files for the wireframes. |
| `spec/spec.md` | **Authoritative feature spec** — scope, clarifications C1–C5, FR-001…FR-B10, success criteria, edge cases. |
| `spec/data-model.md` | Entities + new fields, calc-core models, migrations, sync wiring. |
| `spec/plan.md`, `spec/tasks.md`, `spec/research.md`, `spec/quickstart.md` | Engineering plan, task breakdown, research, quickstart. |
| `spec/sync-api.md` | The bulk-upsert + paginated sync API contract. |
| `tokens/ampairs-colors-and-type.css` | Full M3 token set (colors, type, spacing, shape, motion). |

**Suggested reading order:** this README → `spec/spec.md` → `spec/data-model.md` →
`spec/sync-api.md`, with the wireframes HTML open for layout/flow reference.
