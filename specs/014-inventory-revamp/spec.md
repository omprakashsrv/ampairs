# Feature Specification: Inventory Module Revamp (Pragmatic Core)

**Feature Branch**: `claude/zealous-meitner-q8hovy`
**Created**: 2026-06-22
**Status**: Draft
**Input**: User description: "As a Staff Engineer and Product Manager, review the inventory module. We need a complete revamp of that module including UI, Flow, design and overall architecture." Scoped (via clarification) to: **deliverable = full speckit spec + plan**, **ambition = pragmatic core** (single-warehouse items + movements + adjustments + low-stock alerts + auto-deduct on order; batch/serial/multi-warehouse deferred behind config flags).

## Problem Statement *(context)*

Today the inventory capability is incoherent across the two clients:

- The **backend** models a rich inventory domain but exposes essentially one read endpoint, has **no offline-sync contract**, and its order-to-stock listener only logs — so **a sale never changes stock**.
- The **mobile app** ships a trivial flat "stock + price editor" that ignores the backend model, is **online-only with a local cache** (it bypasses the app's offline-first sync architecture), and offers no movement history, adjustments, or alerts.

The result: business owners cannot trust their stock numbers, get no low-stock warnings, and lose data reliability when offline. This feature replaces the fragmented implementation with a single, coherent, offline-first inventory product for a single stocking location.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Stock moves automatically when I sell (Priority: P1)

A business owner records a sale (order/invoice). The quantity of each item sold is automatically removed from on-hand stock, and a movement record is created describing what happened. If the sale is cancelled or returned, the stock is automatically restored. Auto-deduction respects the workspace's inventory policy settings.

**Why this priority**: This is the single highest-value outcome. An inventory system whose numbers don't change when goods move is not an inventory system. Without it, every other feature reports numbers the owner cannot trust.

**Independent Test**: Enable auto-deduction, create an item with known stock, place an order/invoice for a quantity, and verify on-hand stock decreases by exactly that quantity and a corresponding movement is recorded; cancel/return the document and verify stock is restored.

**Acceptance Scenarios**:

1. **Given** auto-deduction is enabled and an item has 10 units on hand, **When** a sale for 3 units is confirmed, **Then** on-hand stock becomes 7 and an outbound movement of 3 (reason: sale) is recorded with the document reference.
2. **Given** a confirmed sale that deducted stock, **When** the sale is cancelled or fully returned, **Then** the stock is restored and a compensating inbound movement is recorded.
3. **Given** the policy "block sales when out of stock" is enabled and an item has 2 units, **When** a sale for 5 units is attempted, **Then** the sale is rejected with a clear out-of-stock reason and stock is unchanged.
4. **Given** the policy "allow negative stock" is enabled and an item has 2 units, **When** a sale for 5 units is confirmed, **Then** the sale succeeds and on-hand stock becomes -3.
5. **Given** auto-deduction is disabled, **When** a sale is confirmed, **Then** stock is not changed automatically (manual adjustment remains available).

---

### User Story 2 - Adjust stock with a reason and see a trustworthy history (Priority: P1)

A user corrects an item's on-hand quantity (e.g., received new stock, found damage, wrote off loss, or recorded an opening balance) by entering the change and selecting a reason. Every change — manual or automatic — appears in a chronological movement history for the item, showing what changed, why, by how much, the resulting balance, and the source.

**Why this priority**: Trust is the product. Owners must be able to explain every discrepancy. Manual adjustments are also the only way to set up initial stock and correct reality.

**Independent Test**: Open an item, record a stock-in of +5 (reason: purchase) and a stock-out of -2 (reason: damage), and verify the item's on-hand reflects the net change and the history lists both entries with reason, quantity, running balance, and timestamp in order.

**Acceptance Scenarios**:

1. **Given** an item with 10 units, **When** the user records a stock-in of 5 with reason "purchase", **Then** on-hand becomes 15 and the history shows an inbound entry of +5 with running balance 15.
2. **Given** an item with 15 units, **When** the user records a stock-out of 2 with reason "damage", **Then** on-hand becomes 13 and the history shows an outbound entry of -2 with running balance 13.
3. **Given** an item with prior movements, **When** the user opens its movement history, **Then** all movements (manual and sale-driven) appear newest-first with reason, signed quantity, resulting balance, source reference, and date/time in the workspace's locale.
4. **Given** a movement has been recorded, **When** the user views it, **Then** it cannot be silently edited or deleted — corrections are made by recording a new compensating movement (auditable).

---

### User Story 3 - Know what's running low (Priority: P1)

A user sets a reorder level per item. The app surfaces a dashboard summarizing items that are low on stock, out of stock, and the total stock value on hand. The user receives alerts when items cross into low/out-of-stock so they can reorder before stocking out.

**Why this priority**: Avoiding stockouts is the #1 reason small businesses adopt inventory software. This turns inventory from a passive ledger into a proactive tool.

**Independent Test**: Set an item's reorder level to 5, reduce its stock to 4, and verify it appears in the low-stock summary and an alert is generated; reduce to 0 and verify it moves to out-of-stock.

**Acceptance Scenarios**:

1. **Given** an item with reorder level 5 and 10 on hand, **When** stock drops to 5 or below (but above 0), **Then** the item is reported as low-stock on the dashboard and a low-stock alert is generated.
2. **Given** an item, **When** its on-hand reaches 0 or below, **Then** it is reported as out-of-stock on the dashboard and an out-of-stock alert is generated.
3. **Given** items with on-hand quantities and unit costs, **When** the user opens the dashboard, **Then** the total stock value (sum of on-hand × unit cost) is displayed.
4. **Given** the same low-stock condition persists across days, **When** alerts are generated, **Then** the user is not spammed with duplicate alerts for an unchanged condition.

---

### User Story 4 - Inventory works fully offline and reconciles automatically (Priority: P1)

A user creates items, records adjustments, and runs a stock take while offline (no connectivity). All changes are saved locally and the app remains fully usable. When connectivity returns, local changes are pushed to the server and server changes are pulled, with no data loss and predictable conflict handling. Changes made on one device appear on the user's other devices.

**Why this priority**: The app is offline-first by architecture; an inventory module that silently fails or loses data offline violates the product's core promise and the platform's architecture rules.

**Independent Test**: Disable connectivity, create/edit items and record adjustments, confirm everything is usable and persisted; re-enable connectivity and verify all local changes reach the server and any server-side changes appear locally.

**Acceptance Scenarios**:

1. **Given** no connectivity, **When** the user creates an item and records an adjustment, **Then** both succeed locally and the item/movement are visible immediately.
2. **Given** queued offline changes, **When** connectivity returns, **Then** the changes are pushed automatically without user action and reflected on the server.
3. **Given** an item edited offline on this device and also changed on the server, **When** sync runs, **Then** the local unsynced change is preserved (local-edit-wins) until it is pushed.
4. **Given** an item deleted on the server, **When** sync runs, **Then** the item is removed locally unless it has local unsynced edits.
5. **Given** a transient push failure, **When** connectivity is available again, **Then** the change is retried automatically and not lost.

---

### User Story 5 - Perform a physical stock take (Priority: P2)

A user counts physical stock and enters the counted quantity per item. The system reconciles each item's on-hand to the counted value, recording the difference as an auditable count-adjustment movement.

**Why this priority**: Periodic counts are how owners re-anchor system stock to reality. It builds on the movement/adjustment foundation (P1) and is the natural follow-on, but the business can operate on manual adjustments before it lands.

**Independent Test**: For an item with system on-hand 10, enter a counted quantity of 8, and verify on-hand becomes 8 with a count-adjustment movement of -2 recorded.

**Acceptance Scenarios**:

1. **Given** an item with system on-hand 10, **When** the user submits a physical count of 8, **Then** on-hand becomes 8 and a count-adjustment movement of -2 is recorded.
2. **Given** a count equal to system on-hand, **When** submitted, **Then** no movement is recorded (no spurious zero-quantity entries).
3. **Given** a count session over many items, **When** the user submits, **Then** only items whose counted value differs from system on-hand produce movements.

---

### User Story 6 - Configure inventory behavior per workspace (Priority: P2)

An admin configures inventory policy for the workspace: whether to auto-deduct on sale, whether to block sales when out of stock, whether to allow negative stock, whether manual override is allowed, and alert preferences (low-stock alerts on/off). These settings govern the behavior in Stories 1 and 3. They are managed through the **existing central workspace settings** experience alongside other modules' toggles — not a separate inventory-only settings area.

**Why this priority**: The policies in Story 1/3 must be controllable, but sensible defaults let the business operate without touching settings first.

**Independent Test**: Toggle "auto-deduct on sale" off in the workspace settings and confirm sales no longer change stock; toggle it on and confirm they do.

**Acceptance Scenarios**:

1. **Given** the workspace settings, **When** the admin changes an inventory policy, **Then** subsequent inventory behavior follows the new policy.
2. **Given** a brand-new workspace, **When** inventory is first used, **Then** safe defaults apply without requiring configuration (auto-deduct on, block-on-out-of-stock off, negative stock off, low-stock alerts on).
3. **Given** an inventory policy changed on one device, **When** another device syncs, **Then** the updated policy applies there too.
4. **Given** the inventory module is not installed for a workspace, **When** the admin opens workspace settings, **Then** inventory policies do not appear.

### Edge Cases

- **Sale references an item with no inventory record**: the sale is not blocked by inventory; no movement is created for untracked items (inventory tracking is opt-in per item).
- **Duplicate auto-deduction**: confirming the same order twice, or a retried sync, must not double-deduct (deduction is idempotent per source document + line).
- **Partial returns / partial fulfillment**: stock is restored/deducted only for the quantity actually returned/fulfilled.
- **Concurrent movements on the same item** (e.g., a sale and a manual adjustment at once): the resulting on-hand and running balances remain consistent and correctly ordered.
- **Reorder level of 0 or unset**: the item is never considered "low" (only "out" at ≤ 0).
- **Negative or zero adjustment quantity**: rejected with a clear validation message.
- **Very large movement history**: history loads incrementally and remains responsive.
- **Item deleted while it still has on-hand stock**: deletion is a soft action; on-hand and history are preserved for audit and the item disappears from active lists.
- **Sync conflict where the server reports an item deleted but the user edited it offline**: the local edit is preserved until pushed (local-edit-wins).
- **Clock/timezone differences**: movement timestamps are stored in a single canonical time and displayed in the workspace's business locale.

## Requirements *(mandatory)*

### Functional Requirements

**Items & stock**

- **FR-001**: System MUST let users create, view, edit, search, and soft-delete inventory items for a single stocking location.
- **FR-002**: Each inventory item MUST track on-hand quantity, reserved quantity, available quantity (on-hand minus reserved), and an optional reorder level.
- **FR-003**: Each inventory item MUST hold cost price, selling price, and MRP, and MAY be linked to a product/variant and a unit of measure.
- **FR-004**: System MUST treat inventory tracking as opt-in per item; items without an inventory record are not affected by stock logic.

**Movements & history**

- **FR-005**: System MUST record every stock change — inbound (stock-in), outbound (stock-out), adjustment (with reason), and physical-count reconciliation — as an immutable movement.
- **FR-006**: Each movement MUST capture the signed quantity, the reason, the resulting on-hand balance, the source (manual vs. document reference such as order/invoice), the actor, and the timestamp.
- **FR-007**: System MUST present an item's movement history newest-first, loaded incrementally for responsiveness.
- **FR-008**: Movements MUST NOT be editable or hard-deletable by users; corrections are made by recording a new compensating movement.
- **FR-009**: Manual adjustments MUST require a positive quantity and a reason; invalid input MUST be rejected with a clear message.

**Automatic stock from sales**

- **FR-010**: When the workspace policy enables auto-deduction, confirming a sale (order/invoice) MUST reduce on-hand stock for each tracked line item by the sold quantity and record an outbound movement referencing the source document.
- **FR-011**: Cancelling or returning a sale MUST restore stock for the affected quantity and record a compensating inbound movement.
- **FR-012**: Auto-deduction and restoration MUST be idempotent per source document and line so retries or duplicate events never double-count.
- **FR-013**: When "block sales when out of stock" is enabled, a sale that would drive a tracked item below zero MUST be rejected with a clear out-of-stock reason and leave stock unchanged.
- **FR-014**: When "allow negative stock" is enabled, sales MUST be permitted to drive on-hand below zero.

**Alerts & dashboard**

- **FR-015**: System MUST identify items as low-stock (0 < on-hand ≤ reorder level, reorder level > 0) and out-of-stock (on-hand ≤ 0).
- **FR-016**: System MUST present a dashboard summarizing low-stock items, out-of-stock items, and total stock value (sum of on-hand × cost).
- **FR-017**: System MUST generate low-stock and out-of-stock alerts when an item crosses the threshold, without generating duplicate alerts for an unchanged condition.

**Configuration**

- **FR-018**: System MUST expose per-workspace inventory policy — auto-deduct on sale, block sales when out of stock, allow negative stock, allow manual override, low-stock alerts on/off — through the **central workspace settings registry** (the same mechanism other modules use for cross-cutting toggles), surfaced in the existing workspace settings experience. Inventory MUST NOT introduce a separate, parallel configuration store.
- **FR-019**: System MUST apply safe defaults for a new workspace without requiring configuration (auto-deduct on, block-on-out-of-stock off, negative stock off, low-stock alerts on); defaults apply until an explicit override is set.
- **FR-019a**: Inventory policy settings MUST only be visible/applicable when the inventory module is installed for the workspace.

**Physical count**

- **FR-020**: System MUST let users submit a counted quantity per item and reconcile on-hand to that value, recording the difference as a count-adjustment movement; equal counts MUST produce no movement.

**Offline-first & sync**

- **FR-021**: All inventory reads and writes MUST work offline; the app MUST remain fully usable without connectivity.
- **FR-022**: Local changes MUST be persisted immediately and queued for automatic synchronization when connectivity returns, with no user action required.
- **FR-023**: Synchronization MUST reconcile items, movements, and settings bidirectionally, propagate deletions, and survive app restarts (queued changes are not lost).
- **FR-024**: Conflict resolution MUST preserve local unsynced edits over server data until they are pushed (local-edit-wins).
- **FR-025**: Failed pushes MUST be retried automatically and surfaced (not silently reported as success).
- **FR-026**: Changes made on one device MUST become visible on the user's other devices after sync.

**Multi-tenancy & integrity**

- **FR-027**: All inventory data MUST be isolated per workspace; no cross-workspace leakage.
- **FR-028**: System MUST display monetary and date/time values in the active workspace's business locale.

**Scope guards (deferred capabilities)**

- **FR-029**: The system MUST be designed so that multi-warehouse/transfers, batch/lot + expiry tracking, serial-number tracking, and ledger/valuation snapshots can be added later WITHOUT reworking the item/movement foundation; however, NONE of these are exposed in this feature.

### Key Entities *(include if feature involves data)*

- **Inventory Item**: A stock-tracked good at the single stocking location. Key attributes: identifier, name/SKU, link to product/variant and unit, on-hand quantity, reserved quantity, available quantity (derived), reorder level, cost/selling/MRP prices, active flag. Relationships: optionally references a Product/variant and a Unit; has many Movements.
- **Inventory Movement**: An immutable record of a single stock change. Key attributes: identifier, item reference, movement type (in/out/adjustment/count), reason, signed quantity, resulting balance, source (manual or document reference like order/invoice + line), actor, timestamp. Relationships: belongs to one Inventory Item; may reference a source document.
- **Inventory Policy (settings)**: Per-workspace inventory policy values — auto-deduct on sale, block sales when out of stock, allow negative stock, allow manual override, low-stock alerts enabled. These are **not a dedicated inventory entity**; they are stored and synced as entries in the shared workspace settings registry under the `inventory` namespace, with defaults declared by the inventory module. Relationships: keyed per workspace + setting key.
- **Stock Alert** (conceptual): A low-stock or out-of-stock condition surfaced to the user; deduplicated per item+condition.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After a sale is confirmed with auto-deduction enabled, on-hand stock reflects the deduction 100% of the time, and the change is visible to the user within a few seconds.
- **SC-002**: Duplicate or retried sale events never cause double-deduction (0 double-counts across a stress test of repeated/retried confirmations).
- **SC-003**: Every stock change (manual or automatic) is represented by exactly one movement in the item's history (no missing and no spurious entries).
- **SC-004**: An item that crosses its reorder threshold appears in the low-stock summary and generates exactly one alert per crossing (no duplicates for an unchanged condition).
- **SC-005**: With connectivity disabled, users can complete all core inventory tasks (create item, adjust stock, run a count) with a 0% failure rate attributable to the lack of connectivity.
- **SC-006**: After connectivity is restored, 100% of queued local changes reach the server and 100% of server changes are reflected locally, with no data loss across an app restart.
- **SC-007**: A user can determine why an item's stock is at its current value by reading its movement history, without contacting support (validated via task-completion testing).
- **SC-008**: The mobile inventory module passes the project's offline-sync and dependency-injection architecture conformance review (repository is local-only; a single sync delegate owns all server traffic; state is modeled with the standard UI-state pattern).
- **SC-009**: Total stock value shown on the dashboard equals the sum of on-hand × unit cost across active items at all times (reconciles exactly).

## Assumptions

- **Single stocking location** for this feature; multi-warehouse is explicitly deferred. Where the existing data model has a location concept, a single default location is used.
- The existing **order** and **invoice** lifecycles emit (or can be made to emit) the events needed to trigger deduction/restoration; this feature defines the inventory side of that contract and the minimal hook required on the sales side.
- "Sale confirmed" is the trigger point for deduction; the precise lifecycle state(s) (e.g., confirmed/fulfilled/invoiced) will be finalized during planning with the order/invoice owners, but deduction is idempotent regardless of which event fires.
- **Reserved stock** is supported in the model but reservation flows (holding stock for unconfirmed orders) are minimal in this feature; the primary path is on-hand deduction at confirmation.
- Inventory policy reuses the **existing central workspace settings module** (definitions, sync, and the generic settings UI) rather than a dedicated inventory configuration store; the inventory module only declares its setting definitions and reads their effective values. Any legacy dedicated inventory-config data is migrated into the central settings registry during planning/implementation.
- Alerts are surfaced in-app on the dashboard; push/notification delivery reuses the platform's existing notification mechanism where available and degrades gracefully to in-app only.
- Inventory tracking is **opt-in per item**; existing products without inventory records are unaffected until an item is created for them.
- Monetary/date formatting follows the workspace business locale already provided by the platform.
- Existing legacy inventory data (the old flat model and the legacy backend entity) will be migrated or retired during planning; no user-facing legacy behavior is preserved if it conflicts with this spec.

## Out of Scope (deferred to a later spec)

- Multiple warehouses and inter-warehouse transfers.
- Batch/lot tracking and expiry, including FIFO/FEFO/LIFO consumption strategies.
- Serial-number tracking and per-unit lifecycle.
- Daily ledger/valuation snapshots and advanced valuation methods (e.g., weighted-average ledger).
- Purchase-order management and supplier records (beyond a reason/reference on a stock-in movement).
- Barcode scanning hardware integration (the UI should not preclude it, but it is not delivered here).
