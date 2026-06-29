# sfa module

Distributor field-sales automation (SFA) — the offline-first rep app backend (feature 021, US1 + Phase 8b reporting). Standalone MVP: no cross-tenant concern. All entities ride the canonical offline `/sync` contract (UID-keyed bulk upsert, soft-delete-inclusive incremental feed). Tenant-scoped via `@TenantId` (`OwnableBaseDomain`).

## REST Endpoints

### Offline `/sync` (`/sfa/v1`)
| Method | Path | Description |
|--------|------|-------------|
| GET/POST | `/sfa/v1/beats/sync` | Beats (named routes) |
| GET/POST | `/sfa/v1/beat-outlets/sync` | Beat → outlet membership |
| GET/POST | `/sfa/v1/journey-plans/sync` | Recurring PJP assignments |
| GET/POST | `/sfa/v1/planned-visits/sync` | Expected daily stops |
| GET/POST | `/sfa/v1/visits/sync` | Actual visits (geo-flagged, ad-hoc) |
| GET/POST | `/sfa/v1/attendance/sync` | Check-in/out |
| GET/POST | `/sfa/v1/field-orders/sync` | Counter-order pointers |
| GET/POST | `/sfa/v1/leaves/sync` | Manager-marked leave |
| GET/POST | `/sfa/v1/visit-surveys/sync` | Store-visit survey responses |

### Reporting & leave CRUD (`/sfa/v1`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/sfa/v1/adherence?rep_member_uid&period_from&period_to` | Beat adherence (planned vs actual; ad-hoc separate) |
| GET | `/sfa/v1/attendance/summary?rep_member_uid&from&to` | Days present, hours, open days, leave |
| GET | `/sfa/v1/visits/productivity?rep_member_uid&from&to` | Productive-call %, unique-outlet coverage, ad-hoc |
| POST/GET/DELETE | `/sfa/v1/leaves` | Manager leave CRUD (soft-delete) |

## Key entities
`Beat`, `BeatOutlet`, `JourneyPlan` (PJP), `PlannedVisit`, `Visit` (geoFenceStatus/adHoc/plannedVisitUid), `Attendance` (OPEN/CLOSED/AUTO_CLOSED, single-open enforced), `FieldOrder` (references an `order` uid), `Leave`, `VisitSurveyResponse` (JSON answers).

## Key patterns
- Pure, unit-tested calculators: `GeoFenceCalculator` (informational flag, never blocks check-in — FR-016a), `AdherenceCalculator` (FR-017), `AttendanceSummaryCalculator` / `VisitProductivityCalculator` (FR-AS/VP, AgingService-style read-model).
- Ad-hoc rule: `ad_hoc=false` requires a `planned_visit_uid`; `ad_hoc=true` forbids it.
- A fulfilled planned visit is reconciled to VISITED on visit upsert.
- `SfaSyncCheckpointContributor` contributes per-entity `max(updatedAt)` for the mobile bootstrap.

## Migrations
`V1.0.117` (SFA tables), `V1.0.121` (leaves + visit_survey_responses). Both PostgreSQL + MySQL.

## Deferred follow-ups
The `form`-module `EntityType.VISIT_SURVEY` template provider (survey answers store as JSON for now).
