# gstr-prepare — Prepare, readiness gate, GSTR-1/3B/CMP-08 retrieval (Phase 1)

The export-first core: auto-prepare a period's return from finalized invoices, run the filing-readiness
gate, and retrieve the section-structured snapshot. No GSTN round-trip — all server-computed (R3/R4/R10).

**Controller:** `GstrController`. **Services:** `GstReturnService`, `Gstr1Aggregator`,
`Gstr3bAggregator`, `Cmp08Service`, `ReturnReadinessService`.

**RBAC:** prepare / readiness / retrieve are available to any workspace member (broader than the
owner/admin-only file action — FR-031).

Common path params: `{gstin}` (15-char), `{type}` (`gstr1`|`gstr3b`|`cmp08`), `{period}`
(`MMYYYY`|`Q{n}YYYY`|`YYYY`). See README for period conventions; QRMP filers file one GSTR-1/quarter
(no monthly IFF).

---

## 1. Prepare (or re-prepare) a return

```
POST /gstr/v1/returns/{gstin}/{type}/{period}/prepare
```

Reads every finalized, non-cancelled invoice and credit/debit note for `(gstin, period)` from the
`invoice` module (via the immutable tax audit snapshot, falling back to live `taxInfos` for legacy
rows — R3), classifies each into exactly one GSTR-1 section, and writes/overwrites the
`GstReturnSnapshot`. Tax is **never re-computed** — rates are taken as of issue (FR-005). Then runs the
readiness check and advances the lifecycle.

Request body — optional `PrepareRequest`:

| Field | Type | Default | Notes |
|---|---|---|---|
| `financial_year` | string? | derived from `period` | e.g. `2026-27` |
| `nil_return` | bool | `false` | force a NIL return for an empty period (FR-008) |
| `force` | bool | `false` | re-prepare an already-`PREPARED`/`RECONCILED` period |

### Response — `ApiResponse<GstReturnPeriodResponse>`

| Field (snake_case) | Type | Notes |
|---|---|---|
| `uid`, `gstin`, `return_type`, `financial_year`, `period` | | identity |
| `status` | string | `PREPARED` if readiness clean, else stays `NOT_STARTED` with blocking errors |
| `total_taxable_value`, `total_tax`, `total_igst`/`cgst`/`sgst`/`cess` | string (rupee) | headline totals, rupee-rounded at section boundary (R12) |
| `invoice_count` | int | |
| `section_summary` | object | per-section counts/totals: `b2b`, `b2cl`, `b2cs`, `cdnr`, `cdnur`, `exp`, `nil`, `hsn`, `docs` |
| `readiness` | object | embedded `ReturnReadinessReport` summary (`blocking_count`, `warning_count`, `is_ready`) |
| `prepared_at` | string (ISO-8601) | |

### Lifecycle / errors

| Case | Result |
|---|---|
| Period `FILED`/`ACKNOWLEDGED` | **409 — `PERIOD_LOCKED`** (re-prepare refused; immutable snapshot, R8) |
| Already `PREPARED`/`RECONCILED`, `force=false` | 409 — `ALREADY_PREPARED` (use `force=true` to regenerate) |
| `gstin` not registered | 404 — `GSTIN_NOT_FOUND` |
| Blocking readiness errors found | **200**, but `status` stays `NOT_STARTED` and `readiness.is_ready=false`; the return does **not** advance (FR-010) |
| Empty period, `nil_return=false` | 200 — prepares an empty snapshot; readiness clean; client may file as NIL |

> Re-preparing regenerates totals from current invoices **only while not filed** (FR-024). The readiness
> gate blocks `PREPARED → RECONCILED` while any blocking error exists (R10) — see endpoint 2.

---

## 2. Filing-readiness report (the gate)

```
GET /gstr/v1/returns/{gstin}/{type}/{period}/readiness
```

Returns the `ReturnReadinessReport` — the precise blocking-vs-warning list the GSTN portal would reject
on (R10/FR-009). **Errors block** the return from advancing to `PREPARED`/`RECONCILED` (and from being
filed); **warnings do not block**.

### Response — `ApiResponse<ReturnReadinessReportResponse>`

| Field | Type | Notes |
|---|---|---|
| `is_ready` | bool | `true` ⇔ `blocking_count == 0` |
| `blocking_count` / `warning_count` | int | |
| `blocking_errors` | `[ReadinessIssue]` | |
| `warnings` | `[ReadinessIssue]` | |
| `late_fee_estimate` | string (rupee)? | **informational only** — never charged/managed (FR-016) |
| `interest_estimate` | string (rupee)? | informational only |

`ReadinessIssue`:

| Field | Type | Notes |
|---|---|---|
| `code` | string | `MISSING_CUSTOMER_GSTIN`, `INVALID_CUSTOMER_GSTIN`, `MISSING_HSN`, `MISSING_PLACE_OF_SUPPLY`, `INVALID_PLACE_OF_SUPPLY`, `MISSING_SELLER_GSTIN`, `TAX_BREAKUP_MISMATCH`, `DOC_SERIES_GAP` |
| `severity` | string | `BLOCKING` \| `WARNING` |
| `invoice_uid` | string? | the offending source document |
| `invoice_number` | string? | |
| `line_ref` | string? | line/HSN at fault (for `MISSING_HSN`/`TAX_BREAKUP_MISMATCH`) |
| `field` | string? | the field flagged |
| `message` | string | human-readable |
| `series`, `missing_sequence` | string?, string? | for `DOC_SERIES_GAP` |

The system **never auto-edits a finalized invoice** to clear an issue (FR-011) — the user corrects the
source document in the `invoice` module and re-prepares.

```jsonc
{ "success": true, "data": {
    "is_ready": false, "blocking_count": 2, "warning_count": 1,
    "blocking_errors": [
      { "code": "MISSING_CUSTOMER_GSTIN", "severity": "BLOCKING",
        "invoice_uid": "INV-...","invoice_number": "INV/2026/0042",
        "field": "to_customer_gst", "message": "B2B invoice missing customer GSTIN" },
      { "code": "MISSING_HSN", "severity": "BLOCKING",
        "invoice_uid": "INV-...","invoice_number": "INV/2026/0051",
        "line_ref": "line#3", "field": "hsn", "message": "Line has no HSN/SAC code" }
    ],
    "warnings": [
      { "code": "DOC_SERIES_GAP", "severity": "WARNING", "series": "INV/2026",
        "missing_sequence": "0049", "message": "Sequence 0049 missing in series INV/2026" }
    ],
    "late_fee_estimate": null, "interest_estimate": null
  }, "error": null, "timestamp": "...", "path": "...", "traceId": "..." }
```

---

## 3. Retrieve the prepared GSTR-1 snapshot

```
GET /gstr/v1/returns/{gstin}/gstr1/{period}
```

The section-structured GSTR-1 in **internal snake_case** (the GSTN-field-named portal shape is the
export, see `gstr-export.md`). Returns the computed `GstReturnSnapshot`.

### Response — `ApiResponse<Gstr1SnapshotResponse>`

| Section (snake_case) | Shape | Notes |
|---|---|---|
| `b2b` | `[B2bInvoice]` | invoice-wise; registered buyer (R3) |
| `b2cl` | `[B2clInvoice]` | unregistered, inter-state, > threshold (FR-004 configurable) |
| `b2cs` | `[B2csSummaryRow]` | other B2C, summarized by `(place_of_supply, rate)` |
| `cdnr` / `cdnur` | `[CreditDebitNote]` | credit/debit notes, registered/unregistered |
| `exp` | `[ExportInvoice]` | exports / zero-rated, with/without IGST |
| `nil` | `NilSummary` | nil-rated / exempt / non-GST aggregated |
| `hsn` | `[HsnSummaryRow]` | rolled up by `(hsn, uqc, rate)` → qty, taxable, tax |
| `docs` | `[DocSeriesRow]` | per series: issued / cancelled / net |
| `totals` | object | rupee-rounded headline totals |

Each line carries internal fields like `taxable_value`, `igst`, `cgst`, `sgst`, `cess`, `rate`,
`place_of_supply`, `invoice_number`, `invoice_date`. 404 → `RETURN_NOT_PREPARED` if no snapshot exists
yet (call prepare first).

---

## 4. Retrieve the prepared GSTR-3B summary

```
GET /gstr/v1/returns/{gstin}/gstr3b/{period}
```

The GSTR-3B summary **derived from the GSTR-1 totals** (+ RCM) so 3B⟷GSTR-1 tie to the rupee by
construction (R4/FR-006/SC-003). The **ITC section is marked pending** until purchase data exists
(FR-007) — never silently zero.

### Response — `ApiResponse<Gstr3bSnapshotResponse>`

| Field | Shape | Notes |
|---|---|---|
| `outward_supplies` | object (table 3.1) | taxable/IGST/CGST/SGST/CESS — equals GSTR-1 outward totals |
| `inter_state_unreg` | object (table 3.2) | inter-state to unregistered/composition/UIN |
| `reverse_charge` | object | RCM inward liability (from `rcmApplicable`) |
| `itc` | object | `status: "PENDING"` in P1 (no purchase data); fields nullable |
| `itc_pending` | bool | `true` in P1 — surfaces "pending" not "zero" |
| `net_tax_payable` | object (rupee) | output + RCM − eligible ITC (ITC zero/pending in P1) |
| `late_fee_estimate`, `interest_estimate` | string?(rupee) | informational only (FR-016) |
| `ties_to_gstr1` | bool | self-check: 3B outward == GSTR-1 outward to the rupee |

404 → `RETURN_NOT_PREPARED` if GSTR-1 has not been prepared for the period (3B is derived from it).

---

## 5. Retrieve the prepared CMP-08 (composition dealers)

```
GET /gstr/v1/returns/{gstin}/cmp08/{period}
```

For `registration_type = COMPOSITION` GSTINs — the quarterly composition summary (`Cmp08Service`,
ties to spec 026 composition mode). `{period}` is a quarter (`Q{n}YYYY`).

### Response — `ApiResponse<Cmp08SnapshotResponse>`

| Field | Type | Notes |
|---|---|---|
| `outward_supply_value` | string (rupee) | quarter turnover |
| `tax_payable` | object (rupee) | composition tax split |
| `inward_rcm_value` / `inward_rcm_tax` | string (rupee) | reverse-charge inward |
| `period`, `financial_year` | string | |

| Error | Result |
|---|---|
| GSTIN not `COMPOSITION` | 400 — `NOT_A_COMPOSITION_DEALER` |
| Not prepared | 404 — `RETURN_NOT_PREPARED` |

---

## 6. List periods for a GSTIN (period picker)

```
GET /gstr/v1/returns/{gstin}?financial_year=2026-27&type=gstr1&page=0&size=50
→ ApiResponse<PageResponse<GstReturnPeriodResponse>>
```

Periods presented per the GSTIN's `filing_frequency` — months for monthly, quarters for quarterly
(FR-020). Drives the web period picker; each row carries its independent status, totals and (if filed)
ARN — multi-GSTIN independence (US4, SC-007).

---

## Endpoint count: 6
(prepare, readiness, GSTR-1 retrieve, GSTR-3B retrieve, CMP-08 retrieve, period list.)
