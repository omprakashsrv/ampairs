# gstr-sync — Pull-only status feed + GstinRegistration CRUD (Phase 1)

The mobile app is a **read-only status/summary surface** (R11): it mirrors per-GSTIN/period return
status and headline totals, and triggers prepare/file only as online commands (see `gstr-prepare.md` /
`gstr-filing.md`). This file covers the **pull-only `/sync` feed** the app mirrors, plus the
`GstinRegistration` CRUD the web/owner uses to register each state's GSTIN.

**Controllers:** `GstrController` (sync feed), `GstinRegistrationController`.

---

## Why this feed is PULL-ONLY (no client push)

A GST return is a **server-authored period aggregate** over the whole period's finalized invoices and
their immutable tax snapshots — it cannot be authored or filed offline on a single device that may hold
only a subset of invoices, and filing needs a live GSTN round-trip + the signatory's EVC (R11).
Therefore `GstReturnPeriod` is **off the canonical read/write `/sync` push** — it exposes only the
`GET /sync` half. There is **no `POST /sync`** for return periods; the client never writes return state.
This is the same off-`/sync` posture as `einvoice` and `tax` (documented as a deliberate deviation in
plan.md · Complexity Tracking).

> Mobile `SyncDelegate`: `GstReturnPeriodSyncDelegate` is registered **pull-only** — it implements the
> pull half of the contract and no-ops push.

---

## 1. Pull the return-period status feed

```
GET /gstr/v1/returns/sync
```

The incremental status feed the mobile app mirrors into its Room `gstr` DB. Returns one row per
`(gstin, returnType, financialYear, period)` with the lifecycle status, headline GSTR-1/3B totals and
(once filed) the ARN. **Includes soft-deleted / superseded rows** so deletions propagate (canonical
contract rule 3).

### Query params (snake_case)

| Param | Type | Default | Notes |
|---|---|---|---|
| `last_sync` | string (ISO-8601 `Instant`) | — | URL-decoded then `Instant.parse`. Absent/blank → full feed. |
| `page` | int | `0` | |
| `size` | int | `100` | |
| `sort_by` | string | `updatedAt` | |
| `sort_dir` | string | `ASC` | `ASC` \| `DESC` |

Optional filter (additive, snake_case): `gstin` — restrict the feed to a single registration.

### Response — `ApiResponse<PageResponse<GstReturnPeriodStatusResponse>>`

`GstReturnPeriodStatusResponse` (display-only; heavy snapshot JSON omitted — fetched on demand via
`gstr-prepare.md`):

| Field (snake_case) | Type | Notes |
|---|---|---|
| `uid` | string | period uid |
| `gstin` | string | 15-char GSTIN |
| `return_type` | string | `GSTR1` \| `GSTR3B` \| `CMP08` |
| `financial_year` | string | e.g. `2026-27` |
| `period` | string | `MMYYYY` \| `Q{n}YYYY` \| `YYYY` |
| `filing_frequency` | string | `MONTHLY` \| `QUARTERLY` |
| `status` | string | `NOT_STARTED` \| `PREPARED` \| `RECONCILED` \| `FILED` \| `ACKNOWLEDGED` |
| `total_taxable_value` | string (rupee) | headline GSTR-1 outward taxable, rupee-rounded |
| `total_tax` | string (rupee) | headline tax (IGST+CGST+SGST+CESS) |
| `total_igst` / `total_cgst` / `total_sgst` / `total_cess` | string (rupee) | headline split |
| `invoice_count` | int | source documents aggregated |
| `arn` | string? | acknowledgement reference number, once filed (P2) |
| `filed_at` | string (ISO-8601)? | filed timestamp |
| `prepared_at` | string (ISO-8601)? | last prepared timestamp |
| `readiness_blocking_count` | int | blocking errors at last prepare (0 ⇒ ready) |
| `is_deleted` | bool | soft-delete flag — client hard-deletes on pull |
| `updated_at` | string (ISO-8601) | sync cursor |

```jsonc
{
  "success": true,
  "data": {
    "content": [
      {
        "uid": "GSTRP-20260628-AB12",
        "gstin": "27ABCDE1234F1Z5",
        "return_type": "GSTR1",
        "financial_year": "2026-27",
        "period": "062026",
        "filing_frequency": "MONTHLY",
        "status": "FILED",
        "total_taxable_value": "1842500",
        "total_tax": "331650",
        "total_igst": "120000",
        "total_cgst": "105825",
        "total_sgst": "105825",
        "total_cess": "0",
        "invoice_count": 412,
        "arn": "AA2706260012345X",
        "filed_at": "2026-07-09T06:21:00Z",
        "prepared_at": "2026-07-08T18:02:11Z",
        "readiness_blocking_count": 0,
        "is_deleted": false,
        "updated_at": "2026-07-09T06:21:00Z"
      }
    ],
    "page": 0, "size": 100, "total_elements": 1, "total_pages": 1, "has_next": false
  },
  "error": null, "timestamp": "2026-07-09T07:00:00Z",
  "path": "/gstr/v1/returns/sync", "traceId": "..."
}
```

### Errors

| Case | Result |
|---|---|
| Missing `X-Workspace-ID` | 400 — `error.code = MISSING_WORKSPACE` |
| Unparseable `last_sync` | falls back to full feed (no error) |
| Invalid `sort_dir` | 400 — `error.code = BAD_REQUEST` |

> **No `POST /gstr/v1/returns/sync`.** Attempting a push is `405 METHOD_NOT_ALLOWED` — return periods
> are server-authored (see "Why pull-only" above).

---

## GstinRegistration CRUD

A workspace registers each GSTIN it holds (one per state — the branch model, R2/FR-017). **CRUD is
OWNER/ADMIN-only** (a registration is a legal filing identity). All entities are tenant-scoped.

`GstinRegistrationRequest` (snake_case):

| Field | Type | Validation |
|---|---|---|
| `gstin` | string | `@field:Pattern` 15-char GSTIN; state code = first 2 digits |
| `legal_name` | string | `@field:NotBlank` |
| `trade_name` | string? | |
| `registration_type` | string | `REGULAR` \| `COMPOSITION` \| `SEZ` \| `CASUAL` |
| `filing_frequency` | string | `MONTHLY` \| `QUARTERLY` (QRMP) |
| `state_code` | string? | derived from GSTIN if omitted |

`GstinRegistrationResponse` adds: `uid`, derived `state_code`, `state_name`, `active`, `created_at`,
`updated_at`.

### 2. Create

```
POST /gstr/v1/registrations          (OWNER/ADMIN)
body: GstinRegistrationRequest
→ ApiResponse<GstinRegistrationResponse>
```

| Error | Result |
|---|---|
| Duplicate `gstin` in workspace | 409 — `error.code = DUPLICATE_GSTIN` |
| Malformed GSTIN | 400 — validation error |
| Non-owner/admin | 403 — `FORBIDDEN` |

### 3. List

```
GET /gstr/v1/registrations?page=0&size=50&active=true
→ ApiResponse<PageResponse<GstinRegistrationResponse>>
```

### 4. Get one

```
GET /gstr/v1/registrations/{gstin}
→ ApiResponse<GstinRegistrationResponse>
```
404 → `GSTIN_NOT_FOUND` if not registered in this workspace.

### 5. Update

```
PUT /gstr/v1/registrations/{gstin}   (OWNER/ADMIN)
body: GstinRegistrationRequest
→ ApiResponse<GstinRegistrationResponse>
```
`gstin` itself is immutable; only legal/trade name, registration type and filing frequency change.
Changing `filing_frequency` does not retroactively re-bucket already-prepared periods.

### 6. Deactivate (soft-delete)

```
DELETE /gstr/v1/registrations/{gstin}   (OWNER/ADMIN)
→ ApiResponse<Unit>
```
Soft-deletes (`active = false`). Refused (409 → `REGISTRATION_HAS_FILED_RETURNS`) if any period for
this GSTIN is `FILED`/`ACKNOWLEDGED` — filed history must be retained.

---

## Endpoint count: 7
(1 pull-only sync feed + 6 `GstinRegistration` CRUD: create, list, get, update, delete, and the feed.)
