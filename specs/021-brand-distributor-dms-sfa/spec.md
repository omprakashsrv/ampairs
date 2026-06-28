# Feature Specification: Brand → Distributor DMS + Sales Force Automation

**Feature Branch**: `021-brand-distributor-dms-sfa`  
**Created**: 2026-06-28  
**Status**: Draft  
**Input**: User description: "specs/021-brand-distributor-dms-sfa"

## Overview

Ampairs today serves a **single business per workspace**. This feature takes Ampairs up-market into the
**multi-tier trade chain** that FMCG/CPG companies run on: a **brand** sells to **distributors**
(primary sales), each distributor sells to **retailers** (secondary sales), and **field sales reps**
working for a distributor walk a daily route of retail outlets to take orders at the counter.

It delivers two connected capabilities:

1. **Sales Force Automation (SFA)** — a field rep app that lets a rep run their daily beat, log outlet
   visits with location and time, take orders at the retailer counter, and mark attendance — all working
   **fully offline** in areas with no connectivity, syncing when a signal returns.
2. **Distribution Management System (DMS)** — a brand's view of what is happening below it: how much each
   distributor is selling on to retailers (secondary sales), how much stock distributors are holding,
   how distributors and reps are tracking against targets, and (later) trade schemes and the claims a
   distributor raises to be reimbursed for scheme spend.

The hard constraints that shape the whole feature: each tier (brand, distributor) remains an
**independent, isolated business** — a brand must **never** see another business's data except what that
business has **explicitly consented** to share; and the rep app must keep working when the phone is offline.

Reference systems in this space: BeatRoute, Bizom, FieldAssist.

## Clarifications

### Session 2026-06-28

- Q: When a distributor accepts a brand's link, what retailer-level visibility should the brand get by default? → A: Coded/aggregated outlets by default; identified-retailer sharing (name/area) is an explicit opt-in on the link scope, and full contact PII is never shared.
- Q: How current must the brand's secondary-sales / stock figures be? → A: Hybrid — each qualifying distributor sale/stock change triggers a snapshot rebuild, coalesced/debounced to at most once every ~5 minutes per distributor; brand figures are therefore at most ~5 minutes stale.
- Q: Should outlet check-in enforce proximity to the outlet's location? → A: Capture + flag — compute distance from the captured location to the outlet and flag/score visits outside a configurable radius, but never block the check-in.
- Q: May a rep visit outlets not on today's plan or register a new retailer in the field? → A: Yes — ad-hoc visits to any of the distributor's outlets are allowed, and a rep may register a new retailer in the field (offline); all stays scoped to the rep's own distributor, and adherence is still measured against the plan with ad-hoc stops reported separately.
- Q: How is a primary order (brand → distributor) placed across the link? → A: Handshake — the brand records the order in its own tenant addressed to the linked distributor; it surfaces to the distributor as an inbound order over the link, which the distributor confirms, becoming a normal order in the distributor's tenant (no direct cross-tenant write).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Field rep runs the daily beat offline (Priority: P1)

A distributor employs field sales reps. Each rep is assigned a recurring weekly route ("beat") of retail
outlets to visit on specific days. In the morning the rep opens the app, sees today's planned outlets in
visit order, and starts working the route — often in a market with no mobile signal. At each outlet the
rep checks in (capturing location and time), records the visit outcome (productive / no-order), takes an
order at the counter, and moves to the next stop. At the end of the day the rep checks out. Everything the
rep captures is saved on the device immediately and uploads automatically whenever connectivity returns.

**Why this priority**: This is the standalone, shippable core that delivers value to a distributor on day
one — even with no brand attached. Without reliable offline capture, the entire SFA proposition collapses,
because the target market (field sales in rural and semi-urban India) routinely has no connectivity at the
point of sale.

**Independent Test**: With the device in airplane mode, a rep can open today's beat, check in at an outlet,
take a counter order, mark attendance, and check out; re-enabling connectivity uploads every record with no
loss and no duplication, and the orders appear in the distributor's normal order list.

**Acceptance Scenarios**:

1. **Given** a rep assigned to a beat scheduled for today, **When** they open the app, **Then** they see the
   day's planned outlets in visit sequence with each outlet's name and address.
2. **Given** the device has no network, **When** the rep checks in at an outlet, **Then** the visit is saved
   on the device with the captured location and timestamp and the rep can continue immediately.
3. **Given** the device has no network, **When** the rep takes an order at the counter, **Then** the order
   is saved locally and the rep sees a confirmation, with no spinner or blocking on the network.
4. **Given** locally captured visits and orders, **When** connectivity returns, **Then** all records upload
   automatically and appear in the distributor's data, with each record appearing exactly once even if the
   upload is retried.
5. **Given** a rep starts and ends the day, **When** they check in and check out, **Then** attendance is
   recorded with location and time for both events.
6. **Given** a rep is assigned only to certain beats, **When** they use the app, **Then** they can only see
   and act on the outlets on their assigned beats.

---

### User Story 2 - Brand and distributor establish a consented trade link (Priority: P1)

A brand wants to onboard a distributor onto its network so it can later see that distributor's secondary
sales and stock. The brand sends a link invitation to the distributor; the distributor reviews exactly
what would be shared and accepts (or declines). Only after the distributor accepts does any data flow
between the two businesses, and the distributor can revoke the link at any time. Each business remains its
own separate tenant throughout — joining a network never merges their accounts.

**Why this priority**: The consented link is the single trust edge that makes every brand-facing capability
possible and lawful. It must exist before any cross-business visibility (Stories 3–6) can be built, and it
is the mechanism that keeps tenant isolation intact.

**Independent Test**: A brand invites a distributor; before acceptance the brand sees nothing of the
distributor's data; after acceptance the brand can see only what the agreed sharing scope allows; after the
distributor revokes, the brand again sees nothing new.

**Acceptance Scenarios**:

1. **Given** a brand and a distributor that both use Ampairs, **When** the brand sends a link invitation,
   **Then** the distributor receives a pending request describing the data that would be shared.
2. **Given** a pending invitation, **When** the distributor accepts, **Then** an active link is created with
   the agreed sharing scope and both parties can see the link's status.
3. **Given** a pending invitation, **When** the distributor declines, **Then** no link is created and no data
   is shared.
4. **Given** an active link, **When** the distributor revokes it, **Then** the brand can no longer access any
   of that distributor's data going forward.
5. **Given** no active link between a brand and a distributor, **When** the brand attempts to view that
   distributor's data, **Then** access is denied.
6. **Given** a distributor, **When** it accepts invitations from multiple brands, **Then** each brand sees
   only its own link's data and never another brand's.

---

### User Story 3 - Brand sees secondary sales rolled up across distributors (Priority: P2)

A brand manager wants to know how much product is actually moving from distributors to retailers — the
real demand signal, not just what the brand shipped to distributors. The brand opens a dashboard that
aggregates secondary sales across all linked distributors, broken down by product, time period, and
geography or outlet, so the brand can spot fast- and slow-moving lines and underperforming areas. The
numbers come from the distributors' own sales records, shared only through active links, and update as
distributors record more sales — without the brand ever reaching into a distributor's private records
directly.

**Why this priority**: Secondary-sales visibility is the primary reason a brand buys a DMS. It depends on
Stories 1–2 producing data and a consented link, so it lands after the foundation.

**Independent Test**: With two linked distributors each recording retailer sales, the brand's secondary-sales
view shows the combined totals by product and period; sales from an unlinked distributor never appear; and
a backdated correction by a distributor is reflected correctly without double-counting.

**Acceptance Scenarios**:

1. **Given** linked distributors with recorded retailer sales, **When** the brand opens the secondary-sales
   view, **Then** it sees aggregated quantity and value by product, period, and area/outlet across those
   distributors.
2. **Given** a distributor that is not linked to the brand, **When** the brand views secondary sales, **Then**
   that distributor's sales are excluded.
3. **Given** a sharing scope that shares only coded/aggregated outlets, **When** the brand views the data,
   **Then** it sees outlets by code or aggregated area, not raw retailer contact details.
4. **Given** a distributor records or backdates a sale after the brand last looked, **When** the brand
   refreshes, **Then** the totals reflect the corrected figures with no duplicated or lost amounts.

---

### User Story 4 - Brand sees distributor stock and replenishment signals (Priority: P2)

A brand wants to prevent stockouts and over-stocking across its network. It opens a stock view that shows,
per linked distributor, how much of each product the distributor is holding, derived from the distributor's
own inventory. Combined with secondary sales, this surfaces days-of-stock and out-of-stock signals so the
brand (or the distributor) can act before a line runs dry.

**Why this priority**: Stock visibility is the second-most-valuable DMS capability after secondary sales and
reuses the same consented-sharing mechanism, so it pairs naturally into P2.

**Independent Test**: A linked distributor's on-hand quantity for a product is reflected in the brand's stock
view; an unlinked distributor's stock is never shown; reducing the on-hand quantity is reflected on refresh.

**Acceptance Scenarios**:

1. **Given** a linked distributor with recorded inventory, **When** the brand opens the stock view, **Then**
   it sees on-hand quantity per product for that distributor as of a stated point in time.
2. **Given** secondary-sales and stock data for a distributor, **When** the brand views replenishment signals,
   **Then** it sees days-of-stock and out-of-stock indicators per product.
3. **Given** an unlinked distributor, **When** the brand views stock, **Then** that distributor's stock is
   excluded.

---

### User Story 5 - Targets vs achievement for distributors and reps (Priority: P2)

A brand sets sales targets for its distributors (primary), and a distributor sets targets for its reps and
beats (secondary), over a period and product/area grain. Each party can see actual achievement measured
against target, computed from the same sales records used elsewhere. Reps see a personal scorecard; the
brand sees distributor-level achievement on its dashboard.

**Why this priority**: Targets-vs-achievement is the universal sales-management KPI and turns the captured
sales data into management value, but it depends on the sales rollups from Stories 1–4.

**Independent Test**: A target set for a distributor and period shows the correct achievement percentage as
sales accrue; a rep's scorecard shows their own achievement against their assigned target.

**Acceptance Scenarios**:

1. **Given** a target set for a distributor over a period and product/area, **When** sales accrue, **Then**
   achievement is shown as actual versus target with a percentage.
2. **Given** a rep with an assigned target, **When** they open their scorecard, **Then** they see their own
   achievement against target, not other reps'.
3. **Given** a target period that has not started or has ended, **When** a party views it, **Then**
   achievement reflects only sales within the target period.

---

### User Story 6 - Trade schemes and claims settlement (Priority: P3)

A brand runs trade promotions (e.g. buy-a-slab-get-a-discount, free goods, display incentives) that a
distributor funds upfront and later claims back. The brand authors a scheme with its eligibility rules and
period and publishes it down to linked distributors. Qualifying secondary sales accrue a claim amount; the
distributor reviews and submits the claim; the brand approves, rejects, or settles it. Both sides see a
consistent claim figure because it is computed from the same shared sales data, giving an auditable
reimbursement trail instead of spreadsheets.

**Why this priority**: Claims are the financial backbone of distribution but carry the highest correctness
bar and depend on everything above (links, secondary sales, schemes). They land last so the riskier money
flow rides on proven foundations.

**Independent Test**: A published scheme accrues the correct claim from qualifying sales; the distributor can
submit it; the brand can approve and settle it; rejected claims do not settle; and the claim amount matches
on both sides.

**Acceptance Scenarios**:

1. **Given** a brand authors a scheme with eligibility and period, **When** it publishes the scheme, **Then**
   linked distributors within scope can see it.
2. **Given** secondary sales that qualify under a scheme, **When** the claim is computed, **Then** the claim
   amount reflects the scheme rules applied to those sales, identically for brand and distributor.
3. **Given** an accrued claim, **When** the distributor submits it, **Then** it moves to a submitted state
   awaiting brand action.
4. **Given** a submitted claim, **When** the brand approves and settles it, **Then** it reaches a settled
   state with a reference the distributor can reconcile; **When** the brand rejects it, **Then** it does not
   settle and the reason is recorded.

---

### Edge Cases

- **Offline duplication**: A rep's upload is retried after a flaky connection — each captured visit, order,
  and attendance record must appear exactly once (idempotent on retry).
- **Multi-device / multi-rep merge**: Two reps (or the same rep on two devices) capture data offline that
  syncs out of order — records must merge without loss, with the latest authoritative version winning.
- **Backdated / out-of-order distributor sale**: A distributor records or corrects a sale for a past date —
  brand-facing secondary-sales totals must self-correct without double-counting or stale totals.
- **Link revoked mid-stream**: A distributor revokes a link while a brand is viewing its data — subsequent
  brand access stops; previously captured rep data stays owned by the distributor.
- **Sharing scope excludes PII**: When the scope shares only coded/aggregated outlets, no raw retailer
  contact details may reach the brand.
- **Rep removed from a beat**: A rep loses access to a beat — they can no longer see or act on those outlets,
  but data they already captured remains valid in the distributor's records.
- **Distributor offline when brand looks**: The brand should still see the last shared figures rather than
  failing, because brand-facing data is published, not read live from the distributor.
- **Outlet on a beat that no longer exists as a customer**: A retailer is deactivated by the distributor —
  it should drop off future beats without breaking already-captured history.
- **Empty network**: A brand with no accepted links sees empty dashboards, not errors.
- **Location unavailable or out-of-radius at check-in**: GPS is off, denied, or imprecise, or the rep is
  beyond the outlet's radius — the visit is still recorded and marked (no location / out-of-radius), never
  blocked.
- **Scheme with no qualifying sales**: Produces a zero claim, not an error or a negative figure.

## Requirements *(mandatory)*

### Functional Requirements

#### Trade network & consent (foundation)

- **FR-001**: The system MUST allow a brand business to invite a distributor business to join its trade
  network as a pending request.
- **FR-002**: The system MUST allow a distributor to accept or decline a pending invitation, and MUST share
  no data between the two businesses until the invitation is accepted.
- **FR-003**: Each link MUST record an agreed sharing scope that governs what categories of data (e.g.
  secondary sales, stock, whether outlets are identified or coded) the brand may see. By default a newly
  accepted link shares outlets only as coded/aggregated; sharing identified retailers (name/area) is an
  explicit opt-in the distributor enables on the link scope, and full retailer contact PII is never shared.
- **FR-004**: The system MUST allow a distributor to revoke an active link at any time, after which the brand
  can access no further data from that distributor.
- **FR-005**: The system MUST keep each business an independent tenant — joining a network MUST NOT merge
  accounts, and one business MUST NOT see another's data except through an active link's agreed scope.
- **FR-006**: A distributor MUST be able to participate in multiple brands' networks simultaneously, with each
  brand seeing only its own link's data.
- **FR-007**: The system MUST deny any brand attempt to view a distributor's data when no active link with a
  sufficient scope exists.

#### Field sales rep app — SFA (offline-first)

- **FR-008**: The system MUST let a distributor define beats (named routes) as an ordered list of retail
  outlets, where outlets are the distributor's existing customer records, with a visit sequence and
  scheduled visit day(s).
- **FR-009**: The system MUST let a distributor assign reps to beats on a recurring weekly journey plan, and
  MUST show each rep the planned outlets for the current day in visit order.
- **FR-010**: A rep MUST be able to log a visit to an outlet capturing the outlet, the visit outcome
  (e.g. productive / no-order), location, and time, and optional notes.
- **FR-011**: A rep MUST be able to take an order at the retailer counter that flows into the distributor's
  normal order processing.
- **FR-012**: A rep MUST be able to record attendance via check-in and check-out, each capturing location and
  time.
- **FR-013**: The rep app MUST allow visit logging, order capture, and attendance to be completed fully
  offline, persisting locally and confirming to the rep without requiring a network connection.
- **FR-014**: The system MUST automatically upload locally captured rep data when connectivity returns, with
  each record appearing exactly once even across retries, and without overwriting newer data.
- **FR-015**: A rep's planned day is driven by their assigned beats, but a rep MAY make ad-hoc (unplanned)
  visits to any outlet belonging to their own distributor. A rep MUST remain scoped to their own
  distributor's data and MUST NOT see or act on another distributor's or another business's outlets.
- **FR-015a**: A rep MUST be able to register a new retailer outlet in the field (offline) into the
  distributor's customer records and immediately visit/take an order at it; the new outlet syncs like any
  other offline-authored record.
- **FR-016**: The system MUST capture the location and time of visits and attendance at the moment of
  authoring on the device.
- **FR-016a**: The system MUST compare a visit's captured location to the outlet's known location and flag
  (or score) visits that fall outside a configurable radius, for reporting. It MUST NOT block check-in on
  proximity — an out-of-radius or location-unavailable check-in is still recorded, marked accordingly.
- **FR-017**: The system MUST compute beat adherence (planned versus actual visits, including visit
  completion and on-time measures) for reporting, counting ad-hoc (unplanned) visits separately from
  planned-visit adherence.

#### Brand DMS visibility (secondary sales, stock, targets)

- **FR-018**: The system MUST derive secondary sales from the distributor's own sales documents (distributor
  → retailer), without requiring a separate parallel data-entry flow.
- **FR-019**: The system MUST present a brand with aggregated secondary sales across its linked distributors,
  broken down by product, time period, and geography or outlet, scoped to active links only.
- **FR-020**: The system MUST present a brand with each linked distributor's on-hand stock per product, as of
  a stated point in time, scoped to active links only.
- **FR-021**: The system MUST surface replenishment signals (e.g. days-of-stock, out-of-stock) derived from
  secondary sales and stock.
- **FR-022**: Brand-facing figures MUST self-correct when a distributor records or backdates a sale or stock
  change, with no double-counting and no stale totals, and MUST NOT require live access into the
  distributor's private records during normal viewing. Each qualifying distributor sale or stock change MUST
  trigger a snapshot rebuild, coalesced to at most once every ~5 minutes per distributor, so brand-facing
  figures are at most ~5 minutes behind the distributor's recorded data.
- **FR-023**: When the agreed scope shares only coded or aggregated outlets, the brand MUST NOT receive raw
  retailer contact details.
- **FR-024**: The system MUST allow targets to be set per tier and grain — brand→distributor (primary) and
  distributor/rep→beat (secondary) — over a period and product/area, and MUST compute achievement against
  target from the same sales records used elsewhere.
- **FR-024a**: The system MUST let a brand place a primary order against a linked distributor by recording the
  order in the brand's own tenant; the order MUST surface to the distributor as an inbound order over the
  active link for the distributor to confirm, upon which it becomes a normal order in the distributor's
  tenant. The system MUST NOT write the order directly into the distributor's tenant without that
  confirmation, and primary-order placement MUST require an active link.
- **FR-025**: A rep MUST be able to see a personal scorecard of their own achievement against their target.

#### Trade schemes & claims (financial)

- **FR-026**: A brand MUST be able to author a trade scheme with a type (e.g. slab, value, quantity, free
  goods, display), eligibility (product/category × geography × period), and funding source, and publish it
  down to linked distributors within scope.
- **FR-027**: The system MUST accrue a claim for qualifying secondary sales under a published scheme, computed
  from the same shared sales data so brand and distributor see an identical figure.
- **FR-028**: The system MUST support a claim lifecycle of draft → submitted → approved or rejected → settled,
  with the distributor submitting and the brand approving, rejecting, or settling.
- **FR-029**: A settled claim MUST record a reference the distributor can reconcile against its own books; a
  rejected claim MUST record a reason and MUST NOT settle.

#### Roles & cross-cutting

- **FR-030**: The system MUST provide a field-rep role within the distributor business, scoped to assigned
  beats, without making the rep a member of any other business.
- **FR-031**: Network administration actions (approving links, publishing schemes) MUST require brand
  administrator-level authority; claim submission MUST be distributor-side and claim approval/settlement
  brand-side.
- **FR-032**: All money amounts MUST be stored and computed exactly (no rounding drift) and presented in the
  relevant business's currency.
- **FR-033**: All timestamps MUST be stored in a timezone-unambiguous form so location/time captured in the
  field reconciles correctly regardless of device or server timezone.

### Key Entities *(include if feature involves data)*

- **Trade Network**: A brand's network of linked distributors; the container for everything the brand sees
  below it.
- **Trade Link**: The explicit, consented connection between one brand and one distributor, carrying status
  (pending / active / revoked) and the agreed sharing scope. The sole edge across which any data flows
  between the two businesses.
- **Network Retailer**: A distributor's retail outlet (one of its existing customers) surfaced to a linked
  brand by code by default, or with identified details (name/area, never full contact PII) when the link
  scope explicitly opts in.
- **Beat**: A named route owning an ordered list of outlets with visit sequence and scheduled day(s).
- **Beat Outlet**: The membership of a retailer outlet in a beat, with its sequence and visit day.
- **Journey Plan (PJP)**: A rep's recurring weekly assignment of beats — the planned calendar of routes.
- **Planned Visit**: An expected stop for a given day derived from the journey plan.
- **Visit**: An actual rep stop at an outlet, with location, time, outcome, notes, any order taken, and a
  proximity flag/score (distance from the outlet's known location; out-of-radius visits are flagged, not
  blocked) — captured offline.
- **Attendance**: A rep's check-in / check-out events with location and time.
- **Field Order**: An order taken at the retailer counter, flowing into the distributor's order processing.
- **Primary Order**: A brand → distributor order recorded in the brand's tenant and confirmed by the
  distributor over the active link, becoming a normal order in the distributor's tenant on confirmation.
- **Sales Target**: A target by tier and grain (period × product/area × distributor or rep/beat) against which
  achievement is measured.
- **Secondary Sales (aggregate)**: A published, point-in-time rollup of distributor→retailer sales by product
  × period × outlet-or-area, shared up the link.
- **Distributor Stock (aggregate)**: A published, point-in-time rollup of a distributor's on-hand stock by
  product, shared up the link.
- **Trade Scheme**: A brand-funded promotion with type, eligibility, period, and funding, published down links.
- **Scheme Claim**: A reimbursement accrued from qualifying secondary sales under a scheme.
- **Claim Settlement**: The lifecycle and outcome (submitted → approved/rejected → settled) of a scheme claim,
  with a reconciliation reference.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A field rep can capture a complete outlet visit (check-in, outcome, counter order) in under
  60 seconds with the device fully offline.
- **SC-002**: 100% of records captured offline are uploaded with no loss and no duplication once connectivity
  returns, including after a failed upload is retried.
- **SC-003**: A rep can complete an end-to-end beat day — open today's route, visit every planned outlet,
  take orders, and check in/out — without the app ever blocking on the network.
- **SC-004**: No brand can see any data from a distributor with which it has no active, sufficiently-scoped
  link — verified for every brand-facing view.
- **SC-005**: A brand's secondary-sales totals match the sum of its linked distributors' shared sales for the
  same product and period, and remain correct after a distributor backdates or corrects a sale.
- **SC-006**: For a brand linked to at least 200 distributors, the secondary-sales and stock dashboards
  return their first page of results within 2 seconds (p95) under normal load.
- **SC-007**: Targets-vs-achievement figures shown to a brand and to a distributor for the same scope agree
  with each other.
- **SC-008**: A trade-scheme claim amount computed for the brand equals the amount the distributor sees for
  the same scheme and qualifying sales, end-to-end through settlement.
- **SC-009**: Revoking a link stops all further brand access to that distributor's data immediately, with no
  residual visibility.
- **SC-010**: Beat adherence (planned vs actual visits) can be reported for any rep and period from captured
  data.
- **SC-011**: A qualifying distributor sale or stock change is reflected in the brand's secondary-sales /
  stock view within ~5 minutes, and a backdated or corrected distributor sale is reflected with correct
  (non-double-counted) totals within the same window.

## Assumptions

- **Both tiers run Ampairs**: A distributor uses Ampairs as a normal single-tier business today; a brand also
  runs Ampairs. The feature links existing tenants rather than importing external businesses.
- **Retailers are the distributor's customers**: Outlets on a beat are the distributor's customer records; the
  feature does not introduce a separate retailer master. Reps may add new retailers in the field (offline)
  into the distributor's customer records — these are still ordinary customers, not a parallel master.
- **Secondary sales reuse existing sales documents**: Distributor→retailer sales are the distributor's normal
  orders/invoices, tagged and rolled up — not re-entered.
- **Sharing is published, not live**: The brand reads shared aggregates the distributor publishes under the
  link; the brand does not query the distributor's live records directly during normal viewing. This keeps
  the brand's view resilient to the distributor being offline and keeps the consent boundary auditable.
- **Default outlet sharing is coded/aggregated**: A newly accepted link shares outlets only as coded/
  aggregated by default. Sharing identified retailers (name/area) is an explicit opt-in the distributor
  enables on the link scope; full retailer contact PII is never shared regardless of scope
  (data-minimisation default).
- **Tertiary sales (retailer→consumer)** are out of scope for the initial phases and are estimated only later
  where data allows.
- **Phasing**: Delivery is phased — P1 = the distributor SFA rep app + link/consent foundation; P2 = brand
  DMS visibility (secondary sales, stock, targets); P3 = trade schemes, claims/settlement, advanced analytics,
  tertiary-sales estimation, and web parity. Each phase is independently shippable.
- **Field devices are primarily Android**, though the app targets the platforms Ampairs already supports.
- **Location and time are captured on-device at author time** and reconciled server-side; the server's record
  remains authoritative for sync ordering.

## Dependencies

- Existing **workspace/membership/roles** capability, extended with a field-rep role and beat scoping.
- Existing **order** and **invoice** capabilities (distributor counter orders and the secondary-sales source).
- Existing **product / inventory** capability (the SKU master and distributor on-hand stock).
- Existing **customer** capability (retail outlets are customers).
- Existing **offline sync** capability on mobile (the rail the SFA app rides for offline capture).
- Optional later dependency on the **ledger/collections** capability for posting claim settlements the
  distributor can reconcile.

## Out of Scope (initial phases)

- A brand-owned retailer master separate from the distributor's customers.
- Live cross-business queries from a brand into a distributor's private records.
- Tertiary-sales (retailer→consumer) capture; estimation only, and only in a later phase.
- Full Angular web parity for the brand dashboard (a later phase).
- Automated money movement for claim settlement beyond recording a reconcilable reference (a later phase may
  post a ledger adjustment).
