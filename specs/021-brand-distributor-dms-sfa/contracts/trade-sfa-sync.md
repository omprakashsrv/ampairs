# Contract — SFA offline `/sync` endpoints

Canonical offline-sync contract (`docs/guides/offline-sync-contract.md`). Each SFA resource exposes the
same GET (pull) + POST (push) shape. Tenant = the **distributor** workspace; author = a FIELD_REP (or
MANAGER) member. Records are UID-keyed (client-generated uids), in-band soft-delete, pull feed includes
soft-deleted rows.

Resources: `visits`, `field-orders`, `attendance`, `beats`, `journey-plans`.
(`beats`/`journey-plans` are typically authored by a MANAGER and pulled by reps; `visits`/`field-orders`/
`attendance` are authored offline by reps.)

## PULL

```
GET /trade/v1/{resource}/sync
    ?last_sync={ISO-8601}&page={int}&size={int}&sort_by=updatedAt&sort_dir=ASC
→ ApiResponse<PageResponse<{Resource}Response>>     # content includes soft-deleted rows; hasNext guards paging
```

## PUSH

```
POST /trade/v1/{resource}/sync          # same URL as pull
body: List<{Resource}Request>           # active upserts AND soft-deleted rows; client UID-keyed
→ ApiResponse<List<{Resource}Response>>  # server upserts in batches of 100, echoes canonical rows
```

## Resource payloads (response shape; request mirrors it minus server audit fields)

### VisitResponse
```json
{
  "uid": "VST...", "rep_member_uid": "WMB...", "customer_uid": "CUS...",
  "planned_visit_uid": "PVS...|null", "ad_hoc": false,
  "outcome": "PRODUCTIVE|UNPRODUCTIVE|NO_ORDER",
  "check_in_at": "2026-06-28T04:11:00Z", "check_out_at": "2026-06-28T04:19:00Z",
  "lat": 12.97, "lng": 77.59, "geo_fence_status": "IN_RADIUS|OUT_OF_RADIUS|NO_LOCATION",
  "distance_meters": 23.4, "field_order_uid": "FOR...|null", "notes": "...",
  "active": true, "updated_at": "2026-06-28T04:19:01Z"
}
```
Rules: `ad_hoc=false` ⇒ `planned_visit_uid` required; `ad_hoc=true` ⇒ it must be null. `geo_fence_status`
is computed on capture and NEVER blocks the push (clarification R13.3). A rep registering a new outlet does
so via the existing customer `/sync` first, then references its `customer_uid` here (R13.4).

### FieldOrderResponse
```json
{ "uid": "FOR...", "rep_member_uid": "WMB...", "customer_uid": "CUS...",
  "visit_uid": "VST...|null", "order_uid": "ORD...", "total": "1240.00",
  "active": true, "updated_at": "..." }
```
`order_uid` references the real distributor order (created via the `order` module); the field order is the
thin trade-side reference, tagged SECONDARY for rollup.

### AttendanceResponse
```json
{ "uid": "ATT...", "rep_member_uid": "WMB...", "type": "CHECK_IN|CHECK_OUT",
  "at": "2026-06-28T03:30:00Z", "lat": 12.97, "lng": 77.59, "active": true, "updated_at": "..." }
```

### BeatResponse / BeatOutlet (nested) and JourneyPlanResponse
```json
{ "uid": "BET...", "name": "MG Road AM", "active": true,
  "outlets": [ { "uid": "BTO...", "customer_uid": "CUS...", "sequence": 1, "visit_days": ["MON","THU"] } ],
  "updated_at": "..." }
```
```json
{ "uid": "PJP...", "rep_member_uid": "WMB...", "beat_uid": "BET...", "weekday": "MON",
  "effective_from": "2026-06-01T00:00:00Z", "effective_to": null, "active": true, "updated_at": "..." }
```

## Adherence read (not a /sync — FR-017 / SC-010)
```
GET /trade/v1/adherence?rep_member_uid={WMB...}&period_from=...&period_to=...
→ ApiResponse<AdherenceSummary>
  { "planned": 40, "visited": 34, "missed": 6, "visit_pct": 85.0, "on_time_pct": 78.0, "ad_hoc_count": 5 }
```
Adherence is planned-vs-actual (PlannedVisit VISITED/MISSED reconciled from authored Visits); ad-hoc
(unplanned) visits are counted separately and excluded from `visit_pct`.

## Acceptance
- Pushing the same client uid twice upserts once (idempotent) — SC-002.
- A visit/order/attendance authored with `active=false` is hard-deleted on the next pull cycle per contract.
- Out-of-radius or no-location visits sync successfully and are flagged, not rejected — SC-001/SC-003.
- Adherence can be reported for any rep and period; ad-hoc visits are counted separately — SC-010.
