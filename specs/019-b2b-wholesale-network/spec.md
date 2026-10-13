# Feature Specification: B2B Wholesale Network (workspace-to-workspace ordering)

**Feature Branch**: `019-b2b-wholesale-network`
**Created**: 2026-06-28
**Status**: Draft
**Input**: User description: "specs/019-b2b-wholesale-network"

## Overview

Ampairs already runs the business of individual workspaces (retailers, distributors, wholesalers). Today
each of those businesses operates as an island: when a retailer wants to restock from a distributor who is
*also* an Ampairs customer, that ordering happens off-platform (phone, WhatsApp, paper) and is re-keyed by
hand on both sides.

This feature creates a **private B2B ordering network between Ampairs workspaces**, modelled on the
Udaan / Jumbotail buyer↔seller relationship: a retailer workspace connects to a distributor/wholesaler
workspace, browses that seller's negotiated price-list, and places a purchase order. When the seller
accepts, the order flows straight into the seller's normal fulfilment, invoicing, and credit/ledger
processes, and the buyer can track it end to end. The whole relationship is **opt-in, mutually consented,
and revocable** — a seller's prices and a buyer's profile are only ever shared with parties both sides have
agreed to connect with.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Connect a buyer and seller workspace (Priority: P1)

A retailer business and a distributor business, both already on Ampairs, want to trade on the platform.
One side initiates a connection request (the retailer enters the distributor's connect code, or the
distributor invites the retailer); the other side reviews and approves it. Only after mutual approval can
either side see anything the other has chosen to share.

**Why this priority**: The consented connection is the foundation and the security gate for everything
else. No catalog can be browsed and no order can be placed until an active connection exists. Without this,
there is no network — and crossing one business's data into another's without consent would be a data leak.

**Independent Test**: Create two workspaces; from one, request a connection to the other; from the second,
approve it. Verify the connection becomes active for both and that, before approval, neither side can reach
the other's catalog, profile, or orders. Verify a rejected or revoked connection blocks all access.

**Acceptance Scenarios**:

1. **Given** two workspaces with no relationship, **When** the buyer submits a connection request to the
   seller's connect code, **Then** the connection is created in a "pending approval" state and the seller is
   notified.
2. **Given** a pending connection, **When** the seller approves it, **Then** the connection becomes
   "active", both sides are notified, and the buyer now appears in the seller's customer records with the
   agreed credit terms.
3. **Given** a pending connection, **When** the seller rejects it, **Then** the connection is closed and no
   data is shared in either direction.
4. **Given** an active connection, **When** either side revokes or suspends it, **Then** all further
   cross-workspace access (catalog, new orders, profile) is immediately denied, while orders already in
   flight remain owned by the parties that hold them.

---

### User Story 2 - Browse a seller's negotiated price-list (Priority: P1)

Once connected, the buyer browses the seller's shared catalog and sees the **prices the seller has
negotiated specifically for that buyer** — not the seller's cost, MRP, or any other buyer's pricing. The
seller curates which products are visible and at what price (with optional minimum order quantities, pack
sizes, and tier breaks).

**Why this priority**: Wholesale is price-list-driven and confidential. A buyer needs to see the right
prices to order, and the seller must be able to give different buyers different terms without leaking
margins or one buyer's deal to another. This is the immediate value of being connected.

**Independent Test**: As a seller, build a price-list and assign it to a connection. As the connected
buyer, browse the catalog and confirm the displayed prices match the assigned price-list, that products not
on the list are not visible, and that cost/MRP are never shown. Assign a different price-list to a second
buyer and confirm each sees only their own prices.

**Acceptance Scenarios**:

1. **Given** an active connection with an assigned price-list, **When** the buyer opens the seller's
   catalog, **Then** each product shows the buyer-specific negotiated price and any minimum order quantity
   or pack-size rule, and never shows the seller's cost or MRP.
2. **Given** a product the seller has not added to the buyer's price-list, **When** the buyer browses,
   **Then** that product does not appear.
3. **Given** two buyers connected to the same seller with different price-lists, **When** each browses,
   **Then** each sees only their own negotiated prices and cannot see the other's.
4. **Given** a connection that is not active, **When** a buyer attempts to browse the seller's catalog,
   **Then** access is denied.

---

### User Story 3 - Place and submit a purchase order (Priority: P1)

The buyer builds a purchase order against the seller's price-list — selecting items and quantities,
optionally while offline (e.g. a field salesperson with no signal) — and then submits it to the seller for
acceptance. Submission validates the order against the live connection and the buyer's credit standing.

**Why this priority**: Placing the order is the core transaction the network exists to enable. Offline
drafting is the differentiator versus web-only wholesale marketplaces, but submission must reconcile with
the seller's live state (consent still active, credit available, prices current).

**Independent Test**: As a connected buyer, draft a purchase order while offline, reconnect, and submit it.
Confirm the order reaches the seller for review, that quantities below a minimum order quantity are
rejected, and that submitting on a revoked connection is refused.

**Acceptance Scenarios**:

1. **Given** an active connection and an assigned price-list, **When** the buyer drafts a purchase order
   offline and later regains connectivity, **Then** the draft is preserved and can be submitted.
2. **Given** a valid draft purchase order, **When** the buyer submits it, **Then** the order is recorded
   against the connection, the seller is notified of an incoming order, and the buyer can track its status.
3. **Given** a line whose quantity is below the product's minimum order quantity or not a multiple of its
   pack size, **When** the buyer attempts to submit, **Then** submission is rejected with a clear reason.
4. **Given** a connection that was revoked after the draft was created, **When** the buyer submits,
   **Then** submission is refused and the buyer is told the connection is no longer active.

---

### User Story 4 - Seller accepts an order; status flows back to the buyer (Priority: P1)

The seller reviews incoming purchase orders and accepts (or rejects) them. An accepted order enters the
seller's existing order pipeline as a normal sales order against the buyer (who appears as a customer in the
seller's records), and proceeds through fulfilment and invoicing. As the seller advances the order, the
status is reflected back to the buyer.

**Why this priority**: Acceptance is what turns a request into a committed transaction and connects the
network to the seller's real operations (fulfilment, invoice, inventory, ledger). Status round-trip is what
makes the buyer trust the platform over phone/WhatsApp.

**Independent Test**: As a seller, accept a submitted purchase order and advance it through fulfilment.
Confirm the buyer sees each status change, that the order appears once (not duplicated) even if submission
is retried, and that rejecting an order notifies the buyer with a reason.

**Acceptance Scenarios**:

1. **Given** a submitted purchase order, **When** the seller accepts it, **Then** a corresponding sales
   order is created in the seller's pipeline against the buyer, and the buyer is notified that the order was
   accepted.
2. **Given** the same purchase order submitted twice (e.g. a retried network call), **When** the seller's
   side processes it, **Then** only one sales order exists for that purchase order.
3. **Given** an accepted order, **When** the seller advances its status (e.g. packed, dispatched,
   delivered), **Then** the buyer sees the updated status.
4. **Given** a submitted purchase order, **When** the seller rejects it, **Then** the buyer is notified with
   the rejection reason and no sales order is created.

---

### User Story 5 - Credit terms, invoicing, and receivables (Priority: P2)

The connection carries agreed credit terms (credit days, credit limit, payment terms). When the buyer
submits an order, acceptance can be gated on the buyer's outstanding balance against their limit. Once the
seller finalises an invoice for a fulfilled order, the receivable is posted against the buyer in the
seller's ledger, and aging/collections apply as they do for any customer.

**Why this priority**: Wholesale runs on credit. Reusing the seller's existing invoicing, ledger, aging, and
collections is what makes the receivable side work without parallel bookkeeping. It builds on the accepted-
order flow (P1) but is not required for the first demonstrable slice.

**Independent Test**: Set a credit limit on a connection, place orders until the limit is reached, and
confirm a further order's acceptance is blocked on credit. Finalise an invoice for a fulfilled order and
confirm the receivable appears against the buyer and ages over time.

**Acceptance Scenarios**:

1. **Given** a connection with a credit limit and a buyer whose outstanding balance already meets that
   limit, **When** a new order is submitted, **Then** acceptance is blocked (or flagged) on credit.
2. **Given** a fulfilled order, **When** the seller finalises its invoice, **Then** the receivable is posted
   against the buyer in the seller's ledger and the buyer can see the invoice.
3. **Given** an outstanding receivable, **When** it passes its due date, **Then** it ages and is eligible
   for the seller's existing collections/reminders.

---

### Edge Cases

- **Duplicate connection**: A buyer requests a connection to a seller they are already connected to (or have
  a pending request with) — the system must not create a second active relationship.
- **Self-connection**: A workspace attempts to connect to itself — must be refused.
- **Permission within a workspace**: A workspace member without the right role attempts to approve a
  connection, share a price-list, or place an order — must be refused even if the connection is active.
- **Price change mid-order**: The seller changes the price-list after the buyer drafted an order — submission
  must use a defined, agreed price (last accepted/current price) and surface any change to the buyer rather
  than silently re-pricing.
- **Revocation with orders in flight**: A connection is revoked while accepted orders are mid-fulfilment —
  those orders stay owned by their respective parties and complete; only new cross-workspace access stops.
- **Buyer profile edits**: The buyer's mirrored customer record in the seller's workspace is edited locally
  by the seller's staff — these edits must not corrupt the connection's agreed terms or desync identity.
- **Stale offline draft**: A draft references a product the seller has since removed from the price-list —
  submission must reject or flag the affected lines clearly.
- **Out-of-stock / partial fulfilment**: The seller can only fulfil part of an order — the buyer is informed
  of the fulfilled vs. short quantities.
- **Notification delivery failure**: A counterpart's notification (new order, acceptance) fails to send —
  the order state must still be correct and visible in-app; notification is best-effort, not the source of
  truth.

## Requirements *(mandatory)*

### Functional Requirements

#### Connections & consent

- **FR-001**: The system MUST let a workspace initiate a connection request to another Ampairs workspace,
  either by the buyer requesting via the seller's connect code or by the seller inviting a buyer.
- **FR-002**: The system MUST require the receiving workspace to explicitly approve a connection request
  before any data is shared in either direction.
- **FR-003**: The system MUST support connection states of requested, pending approval, active, suspended,
  and revoked, and MUST treat **only an active connection** as authorising any cross-workspace access.
- **FR-004**: The system MUST allow either party to suspend or revoke an active connection at any time, and
  revocation/suspension MUST immediately deny all further cross-workspace access (catalog browse, new order
  submission, profile visibility).
- **FR-005**: The system MUST prevent a workspace from connecting to itself and MUST prevent more than one
  active (or pending) connection between the same buyer and seller pair.
- **FR-006**: The system MUST record, per connection, the agreed credit terms (credit days, credit limit,
  payment terms) and the assigned price-list.

#### Identity & visibility

- **FR-007**: On connection activation, the system MUST represent the buyer as a customer within the
  seller's workspace (carrying the agreed name, tax identity, address, and credit terms) so the seller's
  existing order/invoice/credit processes can reference it.
- **FR-008**: The system MUST limit what each side can see to consented data only: a buyer may see the
  seller's shared catalog/price-list, their own orders, and order status; a seller may see the buyer's
  disclosed profile (tax identity, address, credit terms) and the orders addressed to them — and nothing
  else.
- **FR-009**: The system MUST never expose a seller's cost price, MRP, internal margins, unlisted products,
  or any other buyer's pricing to a connected buyer.
- **FR-010**: The system MUST require, in addition to an active connection, that the acting member holds an
  appropriate role in their own workspace to perform connection, price-list, or ordering actions.
- **FR-011**: The system MUST record an audit trail of every cross-workspace access, capturing both
  workspaces, the connection, and the acting user.

#### Price-lists & catalog

- **FR-012**: The system MUST let a seller create and maintain named price-lists as a set of buyer-specific
  price overrides on their catalog, and assign a price-list to a connection.
- **FR-013**: The system MUST let a price-list line carry an optional minimum order quantity, pack size, and
  tier/quantity-break pricing.
- **FR-014**: The system MUST present the buyer a catalog view filtered to the products on their assigned
  price-list, with the negotiated price applied, and MUST exclude products not on that price-list.

#### Purchase orders

- **FR-015**: The system MUST let a buyer draft and edit a purchase order against an assigned price-list,
  including while offline, and preserve the draft until submitted.
- **FR-016**: The system MUST let a buyer submit a drafted purchase order to the seller as an online action
  that re-validates the connection is active, the prices are current/agreed, and (where applicable) the
  buyer's credit standing.
- **FR-017**: The system MUST enforce per-line minimum order quantity and pack-size rules at submission and
  reject or flag non-conforming lines with a clear reason.
- **FR-018**: The system MUST let the seller review incoming purchase orders and accept or reject each, with
  a reason captured on rejection.
- **FR-019**: On acceptance, the system MUST create a corresponding sales order in the seller's existing
  order pipeline against the buyer's customer record, and MUST ensure a re-submitted (retried) purchase
  order does not create a duplicate sales order.
- **FR-020**: The system MUST reflect the seller's order status changes (e.g. accepted, packed, dispatched,
  delivered, cancelled) back to the buyer's purchase order, and MUST maintain a correlation between the
  buyer's purchase order, the seller's sales order, and any resulting invoice.

#### Credit, invoicing & ledger

- **FR-021**: The system MUST be able to gate (block or flag) acceptance of an order on the buyer's
  outstanding balance versus the connection's credit limit.
- **FR-022**: When the seller finalises an invoice for a fulfilled B2B order, the system MUST post the
  receivable against the buyer in the seller's ledger and make the invoice visible to the buyer, so that
  existing aging and collections apply.

#### Notifications

- **FR-023**: The system MUST notify the relevant counterpart workspace of connection requests/approvals/
  rejections, purchase-order submission/acceptance/rejection, fulfilment status changes, and invoice events.
- **FR-024**: The system MUST keep in-app order and connection state correct and authoritative independent
  of whether a notification was successfully delivered.

### Key Entities *(include if feature involves data)*

- **Connection**: The consented relationship between a buyer workspace and a seller workspace. Holds the two
  parties, its state (requested → pending → active → suspended/revoked), who approved it, the agreed credit
  terms, and the assigned price-list. The authorising record every cross-workspace action checks first.
- **Price-list**: A seller-owned, named set of buyer-specific price overrides on the seller's catalog,
  assignable to a connection. Each line references a product and carries the negotiated price plus optional
  minimum order quantity, pack size, and tier breaks.
- **Catalog (shared view)**: The buyer-facing projection of the seller's products filtered to the assigned
  price-list, showing negotiated prices only — never cost, MRP, or unlisted items.
- **Purchase Order**: The buyer-authored order document (header + lines referencing the seller's products and
  the agreed prices). Drafted/edited offline; submitted online. Tracks its own status mirrored from the
  seller's sales order.
- **Buyer customer record (mirror)**: The representation of the buyer as a customer inside the seller's
  workspace, created on connection activation, carrying the disclosed profile and credit terms, and linked
  back to the connection so the seller's order/invoice/credit/ledger processes work unchanged.
- **Order correlation / link**: The mapping between a buyer's purchase order, the seller's resulting sales
  order, and any resulting invoice — the single point that keeps status and invoicing reconciled in both
  directions and makes acceptance idempotent.
- **Credit terms**: Credit days, credit limit, and payment terms agreed on the connection and reflected onto
  the buyer's mirror customer record for credit gating, aging, and collections.
- **Audit record**: A log of each cross-workspace access (both workspaces, connection, acting user, action)
  for security review.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Two previously unconnected workspaces can establish a mutually approved connection in under 2
  minutes of active interaction across both sides.
- **SC-002**: A connected buyer can browse a seller's price-list and place a complete purchase order in
  under 3 minutes.
- **SC-003**: 100% of cross-workspace data access is gated by an active connection — no catalog, profile, or
  order data is ever readable across workspaces without one, verified by isolation tests covering
  non-connected, pending, suspended, and revoked states.
- **SC-004**: A seller's cost price, MRP, margins, unlisted products, and other buyers' prices are never
  visible to a connected buyer in any view (0 leakage).
- **SC-005**: An accepted purchase order results in exactly one sales order in the seller's pipeline, even
  when submission is retried (0 duplicates).
- **SC-006**: Seller order-status changes are reflected to the buyer within one fulfilment step, so the buyer
  never has to call/message the seller to learn order status.
- **SC-007**: A revoked or suspended connection blocks all new cross-workspace access immediately (no
  successful access after revocation).
- **SC-008**: A buyer can draft a purchase order with no network connectivity and successfully submit it once
  reconnected, with no loss of drafted data.
- **SC-009**: B2B receivables created from accepted orders appear in the seller's existing aging/collections
  views with no separate bookkeeping, so the seller manages B2B and other customer credit in one place.

## Assumptions

- Both the buyer and the seller are existing Ampairs workspaces; this feature does not onboard businesses
  that are not already on the platform (external/non-Ampairs sellers are out of scope for this spec — that
  is the ONDC adapter, spec 018).
- The seller's catalog already exists (the products the seller lists for B2B reuse the same curated set the
  seller would expose to a storefront); this feature shares and re-prices that catalog rather than defining a
  new catalog model.
- The seller's existing order, invoice, inventory, and credit/ledger processes are the system of record for
  fulfilment, invoicing, and receivables; this feature feeds into them rather than replacing them.
- Purchase-order **drafting/editing** is offline-capable on the buyer side; **submission, acceptance, credit
  checks, and catalog browse** are online actions because they depend on the other workspace's live state.
- Credit gating, invoice/ledger posting, and collections build on the existing payment/collections
  capabilities (specs 013/017) and are sequenced into a later phase (P2) after the core connect→order→accept
  flow (P1).
- Notifications reuse the platform's existing tenant-aware notification channels (in-app, plus SMS/WhatsApp/
  push) and are best-effort signals, not the source of truth for state.
- A seller may offer different prices to different buyers; pricing is inherently per-connection, not a single
  global B2B price.

## Out of Scope

- A public, discoverable seller directory and self-serve network discovery (a later phase; Phase 1 uses a
  connect code / explicit invite).
- A buyer ordering simultaneously across many distributors with cross-seller cart/checkout (later phase).
- B2B returns, credit notes, and partial-fulfilment adjustments beyond surfacing fulfilled-vs-short
  quantities (later phase, via the seller's existing adjustment paths).
- Embedded credit / BNPL financing of B2B orders (separate spec 020).
- A web (Angular) B2B portal — this spec covers the platform capability and the mobile experience; the web
  portal is deferred.
- Connecting to non-Ampairs sellers/marketplaces (covered by the ONDC seller adapter, spec 018).
