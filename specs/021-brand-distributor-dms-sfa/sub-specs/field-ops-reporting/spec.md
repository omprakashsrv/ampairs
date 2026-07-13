# Feature Specification: SFA Reporting & Capture Extensions — Attendance Summary/Leave + Visit Productivity/Survey

**Parent feature**: `021-brand-distributor-dms-sfa` (sub-spec; develop on branch
`claude/brand-distributor-dms-sfa-12692h`, PR #170)
**Created**: 2026-06-29
**Status**: Draft
**Input**: User description: "Attendance summary/leave (FR-AT2/4/5/6/8); Visit productivity / survey (FR-SV6/SV7)"

## Overview

This sub-spec details the **reporting and survey layer** on top of the SFA field-operations capture modules,
expanding the `sfa-field-operations` sub-spec's **FR-AT2/4/5/6/8** (attendance) and **FR-SV6/SV7** (visit
survey/productivity), and resolving `/speckit.analyze` finding **C5** (those behaviors were specified but not
yet covered by the parent plan/tasks).

Raw capture — offline check-in/out and visit logging — is already specified and task-covered. What's added
here is the **derived management view** of that data plus two genuinely new concepts:

1. **Attendance Summary & Leave** — turn raw check-in/out events into per-rep working-hours / presence /
   late-absent summaries, enforce the single-open-attendance rule, auto-close forgotten check-outs, and
   support marking **leave** (excused absence) so summaries and beat adherence exclude excused days.
2. **Visit Productivity & Survey** — turn raw visits into per-rep productivity views (productive-call %,
   lines/value per call, duration, coverage), and let a rep capture structured **survey/audit** responses at
   an outlet (shelf check, competitor presence) from configurable templates.

All entities are owned by the **distributor** workspace. **Summaries/productivity are server-computed online
reads**; **raw capture (attendance, visits, survey responses) is offline-first** and rides the canonical
`/sync` engine — inheriting the parent's offline, point-in-time, and business-timezone conventions.

## Clarifications

### Session 2026-06-29

- Q: Who marks leave / excused absence? → A: **Manager-marked** (a rep × day excused-absence record); a rep
  self-service request→approve flow is a noted later enhancement, not in this scope.
- Q: How are store-visit surveys/audits modelled? → A: **Reuse the existing dynamic form capability** — a
  configurable survey template (sections/fields/validation) — with **structured per-visit responses stored
  queryably** so survey answers can be rolled up into reports (not free-form JSON only).
- Q: What happens to working hours when a rep forgets to check out? → A: The open attendance is **auto-closed
  at a configurable cutoff** (default end of the business day) and **flagged**, so working hours never run
  unbounded.

---

## Module A — Attendance Summary & Leave

**Goal**: Give a manager a trustworthy view of each rep's field presence and working time, and a way to
excuse legitimate absences.

### Actors
- **Distributor manager** (OWNER/ADMIN/MANAGER) — views attendance summaries, marks leave.
- **Field rep** (FIELD_REP) — marks attendance (covered by sfa-field-operations); subject of the summary.

### User Scenarios

#### US-AS1 — Manager sees a per-rep attendance summary (Priority: P1)
A manager opens an attendance summary for a rep (or team) over a period and sees days present, total/average
working hours, and late/absent indicators — computed from the rep's check-in/out events in the business
timezone.

**Independent Test**: For a rep with check-ins on 5 of 6 working days, the summary shows 5 present, the
correct total hours, 1 absent day, and any late check-ins.

**Acceptance Scenarios**:
1. **Given** a rep's check-in/out events over a period, **When** the manager opens the summary, **Then** it
   shows days present, total and average working hours, and late/absent indicators, bucketed by business day.
2. **Given** a working day with no check-in (and no leave), **When** the summary is computed, **Then** that
   day is counted **absent**.

#### US-AS2 — Single open attendance + auto-close of a forgotten check-out (Priority: P1)
The system keeps at most one open attendance per rep and closes a forgotten one at a cutoff so working hours
stay sane.

**Independent Test**: A rep checks in and never checks out; after the configured cutoff the attendance is
auto-closed and flagged; a second check-in while one is open is rejected (or auto-closes the prior, flagged).

**Acceptance Scenarios**:
1. **Given** an open attendance, **When** the rep checks in again, **Then** it is rejected — or the prior is
   auto-closed and flagged — so only one open attendance exists.
2. **Given** an open attendance with no check-out by the cutoff, **When** the cutoff passes, **Then** the
   attendance is auto-closed at the cutoff and flagged `AUTO_CLOSED`, and working hours are bounded.

#### US-AS3 — Leave / excused absence (Priority: P2)
A manager marks a rep × day as leave/excused so it is not counted as absent and so that day's planned visits
are excused (not "missed") in beat adherence.

**Independent Test**: A rep on marked leave for a day shows that day as excused (not absent), and the day's
planned visits are excused rather than missed.

**Acceptance Scenarios**:
1. **Given** a rep × day marked as leave, **When** the attendance summary is computed, **Then** the day is
   excused — not absent.
2. **Given** leave on a day with planned visits, **When** adherence is computed, **Then** those planned
   visits are excused, not counted as missed.

### Functional Requirements — Attendance Summary & Leave
- **FR-AS1**: The system MUST derive, per rep and period (business timezone): **days present**, **total and
  average working hours** (check-out − check-in), from the rep's attendance events.
- **FR-AS2**: The system MUST define **late** (first check-in later than the rep's earliest planned beat
  start that day) and **absent** (a working day with neither a check-in nor leave) and surface both in the
  summary.
- **FR-AS3**: The system MUST enforce **at most one open** attendance per rep — a second check-in while one
  is open is rejected, or auto-closes the prior with a flag.
- **FR-AS4**: The system MUST **auto-close** an attendance left open past a **configurable cutoff** (default
  end of the business day), mark it `AUTO_CLOSED`, and bound the resulting working hours.
- **FR-AS5**: A manager MUST be able to mark **leave / excused absence** for a rep × day (with a reason);
  excused days MUST be excluded from the absent count and from beat-adherence "missed" calculations.
- **FR-AS6**: The system MUST expose an **attendance summary** read per rep (and per team) over a period.
- **FR-AS7**: A check-out with no matching open check-in MUST be handled gracefully (flagged, not lost) and
  excluded from working-hours totals.

### Key Entities — Attendance Summary & Leave
- **Leave**: a rep × day excused-absence record (reason, marked-by, status) owned by the distributor.
- **Attendance Summary**: a derived view (not stored authoritative) — days present, working hours, late/
  absent/excused counts per rep × period.

### Success Criteria — Attendance Summary & Leave
- **SC-AS1**: A per-rep attendance summary for any period equals the totals derivable from that rep's
  check-in/out events and leave records, in the business timezone.
- **SC-AS2**: No attendance contributes more than the cutoff-bounded hours to a day (auto-close prevents
  unbounded working hours).
- **SC-AS3**: A day marked as leave is never counted absent, and its planned visits are excused (not missed)
  in adherence.

### Edge Cases — Attendance Summary & Leave
- **Forgot to check out** — auto-closed at the cutoff, flagged (FR-AS4).
- **Check-out crossing midnight** — attributed to the check-in's business day.
- **Leave on a planned-visit day** — planned visits excused, not missed.
- **No-GPS check-in** — recorded (parent rule); does not affect the summary's hours/presence.
- **Check-out with no check-in** — flagged, excluded (FR-AS7).

---

## Module B — Visit Productivity & Survey

**Goal**: Turn captured visits into a productivity view for managers, and let reps capture structured outlet
surveys/audits that roll up into reports.

### Actors
- **Field rep** — captures survey/audit responses on a visit (offline).
- **Distributor manager** — views visit productivity and survey rollups; (optionally) configures templates.

### User Scenarios

#### US-VP1 — Rep captures a survey/audit on a visit (offline) (Priority: P1)
At an outlet, the rep fills a configurable survey (e.g. shelf availability, competitor presence, planogram
compliance) as part of the visit, fully offline; responses sync with the visit.

**Independent Test**: Offline, the rep completes a survey on a visit; on reconnect the structured responses
upload exactly once and are attached to that visit.

**Acceptance Scenarios**:
1. **Given** a configured survey template, **When** the rep fills it on a visit offline, **Then** the
   responses are saved with the visit and sync on reconnect.
2. **Given** a survey with required questions, **When** the rep submits it incomplete, **Then** the missing
   required answers are flagged (capture still not blocked by the network).

#### US-VP2 — Manager sees per-rep visit productivity (Priority: P1)
A manager opens visit productivity for a rep/period and sees productive-call %, average lines/value per
productive call, average visit duration, and unique outlets covered.

**Independent Test**: For a rep with 30 visits of which 24 are productive, the report shows 80% productive
calls plus average order value/lines per call and unique-outlet coverage.

**Acceptance Scenarios**:
1. **Given** authored visits over a period, **When** the manager opens productivity, **Then** it shows
   productive-call %, average lines/value per productive call, average duration, and unique outlets covered.
2. **Given** a rep who revisits the same outlet twice in the period, **When** coverage is computed, **Then**
   that outlet is counted once.

#### US-VP3 — Survey rollup reporting (Priority: P2)
A manager sees survey answers aggregated across visits (e.g. shelf-availability %, % outlets with competitor
presence) over a period and area/beat.

**Independent Test**: With survey responses across many visits, the rollup shows the % of outlets reporting
shelf availability and competitor presence for the period.

**Acceptance Scenarios**:
1. **Given** structured survey responses over a period, **When** the manager opens the survey rollup,
   **Then** it aggregates the answers (counts/percentages) by question, period, and area/beat.

### Functional Requirements — Visit Productivity & Survey
- **FR-VP1**: The system MUST support **configurable survey/audit templates** (ordered questions with types
  and validation) that a rep fills on a visit; a workspace MAY tailor the template.
- **FR-VP2**: A rep MUST be able to capture survey responses **on a visit fully offline**; responses are
  **structured** (per-question) and ride the canonical `/sync` engine (idempotent, multi-device-safe).
- **FR-VP3**: The system MUST derive **visit productivity** per rep and period: productive-call %, average
  lines/value per productive call, average visit duration, and **unique-outlet coverage** (revisits deduped).
- **FR-VP4**: The system MUST expose a **survey rollup** read — aggregating structured responses (counts/
  percentages per question) by period and area/beat.
- **FR-VP5**: Survey capture MUST be **point-in-time**: a later template change MUST NOT rewrite previously
  captured responses.
- **FR-VP6**: Required survey questions left blank MUST be flagged on submission, but MUST NOT block the
  visit/capture from being saved offline.
- **FR-VP7**: Productivity and survey reads MUST be scoped to the distributor and filterable by rep, period,
  and area/beat.

### Key Entities — Visit Productivity & Survey
- **Visit Survey (template)**: a configurable questionnaire (ordered, typed, validated questions) per
  workspace, reusing the platform's dynamic form capability.
- **Visit Survey Response**: a rep's structured answers captured on a specific visit (per-question values),
  offline-authored.
- **Visit Productivity**: a derived view — productive-call %, lines/value per call, duration, coverage per
  rep × period.

### Success Criteria — Visit Productivity & Survey
- **SC-VP1**: Per-rep productivity figures for any period equal the values derivable from that rep's visits
  (productive-call %, avg lines/value per call, avg duration, unique-outlet coverage with revisits deduped).
- **SC-VP2**: 100% of offline-captured survey responses upload exactly once on reconnect and attach to the
  correct visit, including after a retry.
- **SC-VP3**: Survey rollups aggregate structured responses by question, period, and area/beat.
- **SC-VP4**: A template change never alters previously captured responses (point-in-time).

### Edge Cases — Visit Productivity & Survey
- **Revisit same outlet** — counted once for coverage; both visits count for call totals.
- **Partial survey** — saved with required-blank flags; not lost, not blocked.
- **Template changed mid-period** — past responses keep their original questions/answers (point-in-time).
- **Offline multi-device survey** — merged without loss (client-uid idempotent upsert).
- **Visit with no survey** — valid; survey is optional per FR-SV6.

## Assumptions

- **Leave is manager-marked** (rep × day); a rep self-service request→approve workflow is a later
  enhancement, out of scope here.
- **Surveys reuse the platform's existing dynamic form/template capability** (configurable sections/fields/
  validation), with **structured, queryable per-visit responses** so they can be rolled up (FR-VP4) — not
  stored as opaque free-form text.
- **Auto-close cutoff** is a configurable workspace setting; default is the end of the business day.
- **Reports (attendance summary, visit productivity, survey rollup) are server-computed online reads**; the
  **raw capture** (attendance events, visits, survey responses) is offline-first via `/sync`.
- All time bucketing uses the **business timezone**, consistent with the parent.

## Dependencies

- Parent **021** — the `Attendance`, `Visit`, and `FieldOrder` entities, the offline `/sync` engine, the
  `FIELD_REP` role + beat scoping, and the business-timezone locale model.
- **`sfa-field-operations` sub-spec** — the capture layer this reporting/survey layer reads from (beats/PJP
  for "late vs planned start" and adherence excusal; visits for productivity; attendance for summaries).
- The platform's **dynamic form/template capability** (the same engine used for entity form configuration) —
  reused for survey templates rather than building a new questionnaire system.

## Out of Scope

- **Payroll / incentive computation** from attendance or productivity (reporting only).
- **Route optimization** / automatic beat sequencing.
- **Live GPS tracking / breadcrumbing** between stops.
- **Cross-tenant / brand visibility** of these reports (governed by the parent `TradeLink` / snapshots).
- **Rep self-service leave request/approval** workflow (later enhancement).
