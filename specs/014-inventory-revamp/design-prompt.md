# Design Prompt — Ampairs Inventory (Mobile + Desktop, full flow)

> Paste this into your design tool (Claude, etc.). It is self-contained: product context,
> data model, every screen, every state/use-case, and adaptive mobile↔desktop behavior.

---

## Role & goal

You are a senior product designer. Design the **complete UI/UX and end-to-end flows for the
Inventory feature** of **Ampairs**, a multi-tenant business-management app. Cover **both form
factors from one design system**: **mobile** (Android/iOS, single-pane) and **desktop** (JVM
windowed, two-pane). Deliver flows, wireframes for every screen, component specs, and **all states
and edge cases** listed below — nothing hand-waved.

## Platform & design-system constraints (must follow)

- **Compose Multiplatform + Material 3.** Use M3 components, tokens, type scale, and elevation.
  Dynamic color (Material Kolor) with full **light & dark** themes. Never hardcode colors — use
  `colorScheme` roles.
- **Adaptive layout** via Material 3 adaptive `ListDetailPaneScaffold`:
  - **Mobile (compact width):** single pane. List → tap → Detail pushes. Primary action is a **FAB**.
    Navigation is a **side drawer** (iOS has **no hardware back** — every screen needs an explicit
    back/close affordance).
  - **Desktop / expanded width:** **two-pane** list + detail side by side; denser **data tables**,
    hover states, right-click/context menus, keyboard shortcuts, multi-select.
- **Localization & formatting:** all copy is localized. Money renders via the **workspace business
  currency** (e.g. `₹9,20,710.50` / `$1,234.00`) and dates via the **business timezone/format** —
  show this in mockups, don't hardcode `$`.
- **Offline-first:** the app works fully offline; everything syncs in the background. The UI must
  express sync/pending/error state without blocking the user (see States below).
- **Accessibility:** content descriptions on all icon buttons, 44dp+ touch targets, color is never
  the *only* signal (pair stock colors with labels/icons), readable contrast in both themes.

## Domain model (what the screens render)

**Inventory Item**
- `name`, `sku`, linked `product`/`variant`, `unit`
- `currentStock` (on hand), `reservedStock`, `availableStock` (= on hand − reserved)
- `reorderLevel`, `costPrice`, `sellingPrice`, `mrp`, `active`
- Derived status: **In stock**, **Low stock** (`0 < currentStock ≤ reorderLevel`), **Out of stock**
  (`≤ 0`); **stock value** = `currentStock × costPrice`

**Stock Movement (append-only ledger entry)**
- `transactionNumber`, `type` (STOCK_IN / STOCK_OUT / ADJUSTMENT / COUNT),
  `reason` (PURCHASE / SALE / RETURN / DAMAGE / LOSS / OPENING / CORRECTION / COUNT_ADJUSTMENT),
  `quantity`, `balanceAfter`, `unitCost`, `source` (ORDER / INVOICE / RETURN / MANUAL / COUNT) +
  reference number, `date`, `performedBy`, `notes`
- Movements are **immutable history** — never editable/deletable in the UI.

**Workspace settings (Inventory)** — surfaced in a settings screen, all toggles:
- `auto_deduct_on_order` (deduct stock when a sale is confirmed/finalized)
- `block_orders_when_out_of_stock`
- `allow_negative_stock`
- `allow_manual_override`
- `enable_low_stock_alerts`

**Key behavior to reflect in UX:** stock changes are **server-authoritative**. The user records
intent (adjust, count, confirm an order); on-hand updates and the server reconciles. Sales
(order confirm / invoice finalize) **auto-deduct** stock when the inventory module is installed and
`auto_deduct_on_order` is on — so stock can change *without* a manual action, and the item/history
must reflect that with a clear source label (e.g. "Sale · INV-1042").

## Screens & flows to design (full coverage)

1. **Inventory Dashboard / home**
   - KPI cards: total items, **total stock value**, **low-stock count**, **out-of-stock count**.
   - Quick actions: Add item, Adjust stock, Physical count, View low stock.
   - Recent movements preview. Sync status chip.
   - Desktop: KPIs as a top row + recent-activity table; mobile: stacked cards + list.

2. **Item list**
   - Searchable (name/SKU), **filters** (All / Low / Out of stock / Inactive), **sort** (name, stock,
     value, updated). Each row: name, SKU, **stock badge (color + label + icon)**, selling price,
     value. Pull-to-refresh (mobile) / refresh + manual "Sync now".
   - Desktop: sortable **table** with columns, multi-select for bulk actions, density toggle.
   - Mobile: list rows + FAB (Add). Tap → detail.

3. **Item detail**
   - Header: name, SKU, big **on-hand** with status, available vs reserved, stock value.
   - Pricing block (cost / selling / MRP), reorder level, unit, linked product.
   - **Movement history timeline** (most recent first): signed qty, type/reason, source + reference,
     balance after, date, who. Infinite scroll / paged.
   - Actions: **Adjust stock**, Edit, Deactivate/Delete (soft), and on desktop these live in a side
     panel/toolbar.

4. **Add / Edit item form**
   - Fields: name (req), SKU, unit (dropdown from catalog), cost/selling/MRP, reorder level,
     **opening stock** (create only — explain it becomes an OPENING movement). Inline validation,
     IME-next/done, focus management. Save → returns to detail/list with a success cue.

5. **Adjust stock** (single item)
   - Pick **Stock in / Stock out** (segmented), quantity, **reason** (dropdown scoped to direction),
     optional notes, optional unit cost (for stock-in valuation). Show **resulting on-hand preview**.
   - Guardrails: if result < 0 and negative stock not allowed → block with a clear message; if
     `allow_manual_override` is off, respect it. Confirm → creates a movement.

6. **Physical count / stock-take**
   - Select a set of items (or all / by filter). For each: show **system qty** vs an editable
     **counted qty**; compute **variance** live. Review screen summarizing only the items with
     variance → Confirm → posts COUNT adjustments. Support save-as-draft and resume.
   - Desktop: spreadsheet-like editable table; mobile: stepper list, one item at a time + progress.

7. **Movement history / ledger** (global)
   - All movements across items, filter by type/reason/source/date-range, search by reference.
   - Export affordance (desktop).

8. **Low-stock view**
   - Items at/below reorder level, sorted by urgency; quick "Stock in" / "Create order" actions.
   - Tie to `enable_low_stock_alerts` (in-app notification surface + badge on the dashboard).

9. **Inventory settings**
   - The 5 toggles with plain-language descriptions and consequences (e.g. what "block orders when
     out of stock" does to the sales flow). Saved per workspace.

10. **Cross-feature touchpoints (design the inventory-facing bits)**
    - In the **order/invoice line-item editor**: show **availability** per product (in/low/out
      badge), and when `block_orders_when_out_of_stock` is on, show the **blocked** state + message.
      Clarify that confirming a sale **auto-deducts** stock.
    - These screens are owned by order/invoice — design only the inventory indicators/affordances.

## States & use-cases to cover for EVERY screen

- **Loading** (skeletons, not spinners where possible), **empty** (first-run: "Add your first item",
  filtered-empty: "No low-stock items"), **error** (with retry), **success/confirmation** cues.
- **Offline & sync:** offline banner; per-row/per-item **pending-sync** indicator; global **syncing**
  state (dashboard chip + list refresh spinner driven by sync, not by a manual coroutine); "changes
  will sync when online".
- **Module not installed / needs update:** if the inventory module isn't enabled for the workspace,
  show the gated/"Update app" state instead of the feature.
- **Permissions / read-only** roles (view but not edit).
- **Edge data:** out-of-stock, negative stock (when allowed), zero/blank prices, very large numbers
  & grouping, long names/SKUs (truncation), many movements (paging), no default warehouse yet.
- **Concurrency:** stock changed on the server (another device or an auto-deduct from a sale) while
  the user is viewing/editing — show a non-destructive "updated" refresh, never silent data loss.
- **Destructive actions:** confirm dialogs for delete/deactivate; movements are never editable.

## Deliverables

1. **End-to-end user flows** (flowcharts) for: add item, adjust stock, physical count, react to
   low stock, and the auto-deduct-on-sale path (how it shows up in inventory).
2. **Wireframes/mockups for every screen above**, in **both** mobile (compact) and desktop
   (expanded two-pane) variants, in **light and dark**.
3. A **component/state inventory**: list cards, stock badge, KPI card, movement timeline row,
   editable count table, adjust sheet/dialog, filters/sort, empty/error/loading/offline states.
4. **Adaptive notes** explaining how each screen reflows compact↔expanded (what becomes a pane,
   table, sheet vs dialog, FAB vs toolbar).
5. **Microcopy** for empty/error/guardrail/confirmation messages.
6. A short **design rationale** tying choices to the offline-first, server-authoritative, multi-
   tenant constraints.

## Tone & style

Clean, dense-but-calm business tool; M3 look; fast for power users (desktop tables, keyboard) yet
simple on mobile (one primary action per screen). Prioritize **clarity of stock truth** (what's on
hand, what's reserved/available, what changed and why) over decoration.
