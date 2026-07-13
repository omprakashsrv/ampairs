# Contract — Field-Ops Reporting, Survey & Leave (sub-spec field-ops-reporting)

Distributor-tenant management/reporting layer over the offline-captured SFA data. Reports are **online
server-computed reads** (read-only services, `ApiResponse<T>`); leave is **manager CRUD**; survey responses
are **offline** and ride the canonical `/sync` (see `trade-sfa-sync.md`). All endpoints `ApiResponse<T>`,
snake_case, `X-Workspace-ID` tenant.

## Attendance summary (FR-AS1/2/6)
```
GET /trade/v1/attendance/summary?rep_member_uid={WMB...}&from=...&to=...   # tenant: distributor (manager)
→ ApiResponse<AttendanceSummaryResponse>
  { "rep_member_uid": "WMB...", "from": "...", "to": "...",
    "days_present": 22, "days_absent": 2, "days_excused": 1,
    "total_hours": 176.5, "avg_hours": 8.02, "late_days": 3 }
```
Business-timezone bucketing; `days_excused` from `Leave`; `late_days` = first check-in later than the rep's
earliest planned beat start. Pattern mirrors `payment` `GET /payment/v1/aging` (`AgingService`).

## Leave — manager CRUD (FR-AS5)
```
POST   /trade/v1/leaves   body: { "rep_member_uid": "WMB...", "date": "2026-06-29", "reason": "..." }  → ApiResponse<LeaveResponse>
GET    /trade/v1/leaves?rep_member_uid=&from=&to=                                                       → ApiResponse<PageResponse<LeaveResponse>>
DELETE /trade/v1/leaves/{uid}                                                                            → ApiResponse<Unit>
```
```json
// LeaveResponse
{ "uid": "LVE...", "rep_member_uid": "WMB...", "date": "2026-06-29", "reason": "...",
  "marked_by": "WMB...", "status": "EXCUSED" }
```
An excused day is not counted absent and its planned visits are excused (not missed) in adherence.

## Visit productivity (FR-VP3)
```
GET /trade/v1/visits/productivity?rep_member_uid={WMB...}&from=...&to=...&area={...}?   # tenant: distributor
→ ApiResponse<VisitProductivityResponse>
  { "rep_member_uid": "WMB...", "visits": 30, "productive": 24, "productive_pct": 80.0,
    "avg_lines_per_call": 4.2, "avg_value_per_call": "1240.00", "avg_duration_min": 6.5,
    "unique_outlets": 27 }
```
`unique_outlets` dedupes revisits; productivity derives from authored Visits + their FieldOrders.

## Survey rollup (FR-VP4)
```
GET /trade/v1/visits/survey-rollup?from=...&to=...&area={...}?   # tenant: distributor (manager)
→ ApiResponse<SurveyRollupResponse>
  { "from": "...", "to": "...",
    "questions": [ { "key": "shelf_availability", "responses": 120, "yes_pct": 86.0 },
                   { "key": "competitor_present", "responses": 120, "yes_pct": 41.0 } ] }
```
Aggregates **structured** `VisitSurveyResponse` answers (counts/percentages per question) by period/area.
Point-in-time: a later survey-template change does not rewrite past responses (FR-VP5). The survey
**template** is configured via the `form` module (`EntityType.VISIT_SURVEY`, `GET/POST /form/v1/config/
schema/sync`).
