# Feature Specification: SFA Field Operations — Beat Plan, Attendance, Store Visits

**Parent feature**: `021-brand-distributor-dms-sfa` (sub-spec; develop on branch
`claude/brand-distributor-dms-sfa-12692h`, PR #170)
**Created**: 2026-06-29
**Status**: Draft
**Input**: User description: "specify the beat_plan, attendance, and store visits modules"

## Overview

This sub-spec details the three field-operations modules that make up the distributor's **Sales Force
Automation (SFA)** day, expanding the parent 021 spec's US1 / FR-008–FR-017a:

1. **Beat Plan** — the route-to-market structure: named routes (beats) of retail outlets, a rep's recurring
   weekly journey plan (PJP), the day's planned stops, and adherence reporting.
2. **Attendance** — the rep's working-time record: check-in/check-out with location and time, working hours,
   and presence reporting.
3. **Store Visits** — the actual stop at a retail outlet: check-in/out, outcome, order, optional survey,
   geo-fence flag, ad-hoc and new-outlet handling.

The three are independently implementable but compose into one daily workflow: the rep **checks in**
(Attendance) → works **today's beat** (Beat Plan) → logs a **visit** at each outlet (Store Visits) → **checks
out** (Attendance).

### Common foundations (inherited from the parent — apply to all three modules)

- **Tenant**: all entities are owned by the **distributor** workspace; the field rep is a `FIELD_REP`
  member, scoped to their assigned beats. (Up-chain brand visibility is governed by the parent's `TradeLink`,
  out of scope here.)
- **Offline-first**: every rep-authored record (planned-visit consumption, visit, attendance) is created on
  the device and rides the canonical offline `/sync` engine (client-generated uid, `synced=false`,
  idempotent upsert). Capture never blocks on the network.
- **Geo & time**: location (lat/lng) and timestamps are captured **on-device at author time**; the server's
  `updatedAt` remains the sync authority. Location is **captured and flagged, never used to block** an action
  (geo-fence is informational — parent clarification).
- **Timezone**: day/period bucketing uses the **business timezone** (not the device's), per the parent's
  locale model.
- **Point-in-time**: historical records keep the facts captured at author time; later edits don't rewrite
  history.

## Clarifications

### Session 2026-06-29

- Q: Can a rep have more than one open (un-checked-out) attendance at a time? → A: No — at most one open
  check-in per rep; a new check-in while one is open is rejected (or auto-closes the prior with a flag).
- Q: Is a store visit valid without an order? → A: Yes — an **unproductive / no-order** visit is a valid,
  recorded outcome (with an optional reason); a visit is never required to produce an order.
- Q: How is "today's beat" determined? → A: From the rep's **journey plan (PJP)** for the current weekday in
  the business timezone, materialised as that day's `PlannedVisit`s; ad-hoc visits are additional, not part
  of the plan.

---

## Module 1 — Beat Plan

**Goal**: Let a distributor structure its retail coverage into routes and assign reps to work them on a
repeating weekly schedule, and measure how well reps adhere to the plan.

### Actors
- **Distributor manager** (OWNER/ADMIN/MANAGER) — creates beats, adds outlets, assigns reps (PJP).
- **Field rep** (FIELD_REP) — consumes today's planned outlets; cannot edit beats.

### User Scenarios

#### US-BP1 — Manager defines a beat with ordered outlets (Priority: P1)
A manager creates a named beat (e.g. "MG Road – AM") and adds the distributor's retail customers to it in
visit order, with the day(s) of week the beat runs.

**Independent Test**: Create a beat, add 5 outlets with sequence 1–5 and visit days {Mon, Thu}; the beat
returns the outlets in order for those days.

**Acceptance Scenarios**:
1. **Given** a distributor's customers, **When** the manager creates a beat and adds outlets with a sequence
   and visit days, **Then** the beat lists them in sequence with their scheduled days.
2. **Given** a beat, **When** the manager reorders or removes an outlet, **Then** the sequence updates and a
   removed outlet no longer appears on future runs (past visit history is unaffected).

#### US-BP2 — Manager assigns a rep on a weekly journey plan; rep sees today's stops (Priority: P1)
The manager assigns a rep to one or more beats per weekday (the PJP). Each working day the rep opens the app
and sees the outlets planned for **today** in visit order.

**Independent Test**: Assign rep R to beat B on Mondays; on a Monday, R's "today's beat" shows B's outlets in
order; on a Tuesday it does not.

**Acceptance Scenarios**:
1. **Given** a PJP assigning rep R to beat B on Mondays, **When** R opens the app on a Monday (business tz),
   **Then** today's planned visits are B's outlets in sequence.
2. **Given** a rep with no assignment today, **When** they open the app, **Then** today's beat is empty (they
   may still make ad-hoc visits — see Store Visits).
3. **Given** a rep, **When** they view beats, **Then** they see only beats assigned to them, not other reps'.

#### US-BP3 — Beat adherence reporting (Priority: P2)
A manager sees, per rep and period, how the actual visits compare to the plan: visit completion %, on-time %,
and ad-hoc visits counted separately.

**Independent Test**: For a rep with 40 planned and 34 actual visits in a period, adherence shows 85% visit
completion; ad-hoc visits are reported separately.

**Acceptance Scenarios**:
1. **Given** planned visits and authored visits for a period, **When** the manager opens adherence, **Then**
   it shows planned vs visited vs missed and visit/on-time %.
2. **Given** ad-hoc visits, **When** adherence is computed, **Then** they are counted separately and excluded
   from planned-visit %.

### Functional Requirements — Beat Plan
- **FR-BP1**: The system MUST let a manager create/edit/deactivate a **beat** (named route) within the
  distributor workspace.
- **FR-BP2**: The system MUST let a manager add the distributor's customers to a beat as **beat outlets**,
  each with a visit **sequence** and scheduled **visit day(s)** of the week.
- **FR-BP3**: The system MUST let a manager assign a rep to one or more beats on a recurring weekly
  **journey plan (PJP)**, with an effective date range.
- **FR-BP4**: The system MUST derive each working day's **planned visits** for a rep from the PJP for that
  weekday (business timezone), in the beat's outlet sequence.
- **FR-BP5**: A rep MUST see only the beats/outlets assigned to them; the planned-visit view MUST present
  today's outlets in visit order.
- **FR-BP6**: The system MUST compute **adherence** per rep and period — planned vs actual (visited/missed),
  visit completion %, on-time %, with ad-hoc visits counted separately.
- **FR-BP7**: Removing/reordering an outlet or changing a PJP MUST affect only future planned visits; past
  visit history MUST be unaffected (point-in-time).
- **FR-BP8**: Beats, beat outlets, and journey plans MUST be available on the rep's device offline (pulled
  via `/sync`); manager edits propagate on next sync.

### Key Entities — Beat Plan
- **Beat**: a named route (distributor-owned); active flag.
- **Beat Outlet**: membership of a customer (retail outlet) in a beat — sequence, visit day(s).
- **Journey Plan (PJP)**: a rep's recurring weekly beat assignment (rep × beat × weekday, effective range).
- **Planned Visit**: the expected stop for a given day, derived from the PJP — status PENDING / VISITED /
  MISSED.

### Success Criteria — Beat Plan
- **SC-BP1**: A rep opening the app on a scheduled day sees 100% of that beat's outlets, in sequence, within
  2 seconds, fully offline.
- **SC-BP2**: Adherence (planned vs actual, ad-hoc separate) can be reported for any rep and period.
- **SC-BP3**: A reorder/removal or PJP change never alters a brand's or manager's historical adherence
  figures.

### Edge Cases — Beat Plan
- An outlet on **multiple beats** (legitimate) — appears on each beat's planned days; a visit reconciles the
  relevant planned visit.
- A rep **unassigned mid-week** — future planned visits stop; already-authored visits remain.
- A scheduled day that is a **holiday / rep on leave** — planned visits may be marked missed or excused (ties
  to Attendance leave).
- **Overlapping beats** assigned to a rep on the same day — both sets of planned visits appear, deduped by
  outlet.
- An outlet **deactivated** by the distributor — drops off future planned visits; history intact.

---

## Module 2 — Attendance

**Goal**: Record when a rep starts and ends their field day, with location and time, to derive presence and
working hours.

### Actors
- **Field rep** — marks check-in / check-out.
- **Distributor manager** — views attendance summaries.

### User Scenarios

#### US-AT1 — Rep checks in and out (offline) (Priority: P1)
At the start of the day the rep checks in; at the end, checks out. Each event captures location and time on
the device and syncs later. Works with no network and with no/with poor GPS (recorded, never blocked).

**Independent Test**: With the device offline, the rep checks in (location+time saved), works, checks out;
both events upload on reconnect, exactly once.

**Acceptance Scenarios**:
1. **Given** no open attendance, **When** the rep checks in, **Then** an attendance is opened with captured
   location and time.
2. **Given** an open check-in, **When** the rep checks out, **Then** the attendance is closed and working
   hours are derivable.
3. **Given** GPS is off/denied, **When** the rep checks in, **Then** the check-in is still recorded, marked
   `NO_LOCATION` (never blocked).
4. **Given** an open check-in, **When** the rep tries to check in again, **Then** it is rejected (or the prior
   is auto-closed and flagged) — at most one open attendance per rep.

#### US-AT2 — Manager sees attendance summary (Priority: P2)
A manager views, per rep and period, days present, working hours, and late/early flags (e.g. first check-in
vs the assigned beat's start).

**Independent Test**: For a rep with check-ins on 5 of 6 working days, the summary shows 5 present, total
hours, and any late check-ins.

**Acceptance Scenarios**:
1. **Given** attendance events over a period, **When** the manager opens the summary, **Then** it shows days
   present, total/average working hours, and late/absent indicators.

### Functional Requirements — Attendance
- **FR-AT1**: A rep MUST be able to **check in** and **check out**, each capturing location (lat/lng) and time
  on-device, fully offline.
- **FR-AT2**: The system MUST allow **at most one open** (un-checked-out) attendance per rep; a new check-in
  while one is open is rejected, or auto-closes the prior and flags it.
- **FR-AT3**: Missing/denied location MUST NOT block check-in/out; the event is recorded and marked
  `NO_LOCATION`.
- **FR-AT4**: The system MUST derive **working hours** (check-out − check-in) and **days present** per rep
  over a period, bucketed in the business timezone.
- **FR-AT5**: The system MUST surface **late/absent** indicators (e.g. first check-in vs the rep's earliest
  planned beat start; a working day with no check-in = absent).
- **FR-AT6**: A check-out with no matching open check-in MUST be handled gracefully (flagged, not lost).
- **FR-AT7**: Attendance records MUST ride the offline `/sync` engine (client uid, idempotent upsert).
- **FR-AT8**: The system SHOULD support marking **leave/absence** for a rep/day so adherence and attendance
  reports can exclude excused days.

### Key Entities — Attendance
- **Attendance**: a rep's check-in / check-out pair (or events) — type, time, location, optional `NO_LOCATION`
  flag, optional auto-closed flag.
- **Leave (optional)**: a rep × day excused-absence marker.

### Success Criteria — Attendance
- **SC-AT1**: A rep can check in/out in under 10 seconds offline, including when GPS is unavailable.
- **SC-AT2**: 100% of attendance events upload exactly once after reconnect (idempotent).
- **SC-AT3**: Per-rep working hours and days-present can be reported for any period, in the business timezone.

### Edge Cases — Attendance
- **Forgot to check out** — auto-close at a cutoff (e.g. end of day) and flag, so working hours aren't
  unbounded.
- **Double check-in** — rejected or prior auto-closed (FR-AT2).
- **No GPS / denied** — recorded `NO_LOCATION`.
- **Crossing midnight** (late check-out) — attributed to the check-in's business-day.
- **Device clock skew** — captured on-device; server `updatedAt` is the sync authority; gross skew flagged.

---

## Module 3 — Store Visits

**Goal**: Capture the rep's actual stop at a retail outlet — proof of visit (geo/time), outcome, the order
taken, and optional survey — including unplanned stops and brand-new outlets, all offline.

### Actors
- **Field rep** — logs visits.
- **Distributor manager** — reviews visits and productivity.

### User Scenarios

#### US-SV1 — Rep logs a visit with outcome and order (offline) (Priority: P1)
At an outlet the rep checks in (capturing location and time), records the outcome, optionally takes a counter
order and notes/survey, and checks out — all offline. Out-of-radius or no-GPS visits are still recorded and
flagged.

**Independent Test**: Offline, the rep checks in at outlet O, records PRODUCTIVE + takes an order, checks
out; on reconnect the visit (with its order reference and geo-fence flag) uploads exactly once and reconciles
the planned visit.

**Acceptance Scenarios**:
1. **Given** an outlet on today's beat, **When** the rep checks in, **Then** the visit captures the outlet,
   location, time, and (if computable) distance to the outlet with a `geoFenceStatus`.
2. **Given** a visit, **When** the rep records an outcome (PRODUCTIVE / UNPRODUCTIVE / NO_ORDER) and optional
   order + notes, **Then** they are saved with the visit; a counter order flows into the distributor's order
   processing.
3. **Given** the rep is outside the outlet's radius or has no GPS, **When** they check in, **Then** the visit
   is still recorded, flagged `OUT_OF_RADIUS` / `NO_LOCATION`, never blocked.
4. **Given** an authored visit referencing a planned visit, **When** it syncs, **Then** the planned visit is
   marked VISITED.

#### US-SV2 — Ad-hoc visit and new outlet in the field (Priority: P1)
The rep visits an outlet not on today's plan, or discovers a new shop and registers it on the spot (offline),
then visits it.

**Independent Test**: Offline, the rep adds a new retailer and logs an ad-hoc visit to it; on reconnect the
new outlet (via the customer module) and the `ad_hoc` visit both upload and link correctly.

**Acceptance Scenarios**:
1. **Given** an outlet not on today's plan but belonging to the distributor, **When** the rep visits it,
   **Then** the visit is recorded with `ad_hoc = true` (no planned-visit link).
2. **Given** a new shop, **When** the rep registers it offline, **Then** a distributor customer is created and
   is immediately visitable; the visit references it.

#### US-SV3 — Visit productivity reporting (Priority: P2)
A manager sees per-rep/period productivity: productive-call %, lines/value per call, visit duration, and
coverage (unique outlets visited).

**Independent Test**: For a rep with 30 visits of which 24 are productive, the report shows 80% productive
calls plus average order value/lines per call.

**Acceptance Scenarios**:
1. **Given** authored visits over a period, **When** the manager opens productivity, **Then** it shows
   productive-call %, average lines/value per productive call, average duration, and unique outlets covered.

### Functional Requirements — Store Visits
- **FR-SV1**: A rep MUST be able to log a **visit** to an outlet capturing the outlet, check-in/out time,
  location, outcome (PRODUCTIVE / UNPRODUCTIVE / NO_ORDER), and optional notes — fully offline.
- **FR-SV2**: The system MUST compute the **distance** from the captured location to the outlet's known
  location and set `geoFenceStatus` (IN_RADIUS / OUT_OF_RADIUS / NO_LOCATION); this is informational and MUST
  NOT block the visit.
- **FR-SV3**: A counter **order** taken on a visit MUST flow into the distributor's normal order processing,
  with the visit holding a reference to it; an `UNPRODUCTIVE`/`NO_ORDER` visit (optionally with a reason) is
  valid and requires no order.
- **FR-SV4**: A rep MUST be able to make **ad-hoc** (unplanned) visits to any of the distributor's outlets
  (`ad_hoc = true`, no planned-visit link) and to **register a new outlet** in the field (offline) and visit
  it immediately.
- **FR-SV5**: An authored visit referencing a planned visit MUST reconcile it to **VISITED**; ad-hoc visits
  MUST be counted separately from planned-visit adherence.
- **FR-SV6**: The system SHOULD support optional **survey / audit** responses on a visit (e.g. shelf check,
  competitor presence) captured offline.
- **FR-SV7**: The system MUST derive visit **productivity** per rep/period — productive-call %, lines/value
  per call, average duration, unique outlets covered.
- **FR-SV8**: Visits MUST ride the offline `/sync` engine (client uid, idempotent upsert; multi-device/out-of-
  order merge without loss).

### Key Entities — Store Visits
- **Visit**: a rep's actual stop — outlet, check-in/out time, location, outcome, `geoFenceStatus` +
  `distanceMeters`, `ad_hoc`, optional planned-visit link, optional order reference, notes, optional survey
  responses.
- **Field Order**: the thin reference to the counter order created in the distributor's `order` module
  (tagged SECONDARY for the parent's brand rollup).
- **Visit Survey (optional)**: structured responses captured on a visit.

### Success Criteria — Store Visits
- **SC-SV1**: A rep can complete a visit (check-in, outcome, counter order, check-out) in under 60 seconds,
  fully offline.
- **SC-SV2**: 100% of offline-authored visits/orders/new-outlets upload exactly once on reconnect, including
  after a retry, with multi-device merges losing nothing.
- **SC-SV3**: Out-of-radius / no-GPS visits are recorded and flagged, never blocked.
- **SC-SV4**: Per-rep productivity (productive-call %, lines/value per call, coverage) can be reported for any
  period.

### Edge Cases — Store Visits
- **Visit with no order** — valid (UNPRODUCTIVE/NO_ORDER), optional reason.
- **Out-of-radius / no GPS** — recorded and flagged (FR-SV2).
- **Revisit same outlet same day** — allowed; both visits recorded (productivity dedupes coverage by outlet).
- **New outlet offline then synced** — customer created via the customer module first; the visit references
  its uid.
- **Multi-device / out-of-order sync** — merged without loss (client-uid idempotent upsert; latest wins).
- **Order later cancelled** — the visit stays PRODUCTIVE at author time (point-in-time); secondary-sales
  rollup self-corrects in the parent.

## Dependencies

- Parent **021** — the `FIELD_REP` role + beat scoping, the offline `/sync` engine, the geo/permissions
  platform actuals, and the `customer` (outlets) / `order` (counter orders) modules.
- These three modules map to the parent entities: Beat / BeatOutlet / JourneyPlan / PlannedVisit (Beat Plan),
  Attendance (Attendance), Visit / FieldOrder (Store Visits) — this sub-spec deepens their behavior.

## Out of Scope

- Up-chain brand visibility / attribution of these visits' orders (parent `TradeLink` / snapshots).
- Beat **auto-optimization** / route planning (sequencing is manager-defined here).
- Rep **payroll / incentive** computation from attendance/productivity (reporting only here).
- Live GPS **tracking/breadcrumbing** between stops (only author-time geo-stamps are captured).
