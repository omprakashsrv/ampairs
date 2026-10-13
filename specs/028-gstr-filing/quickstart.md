# Quickstart — GST Return Filing & Reconciliation (GSTR), Phase 1

**Feature**: `028-gstr-filing` · **Phase**: 1 (export-first) · **Audience**: backend dev / QA

This is a runnable-in-spirit, end-to-end walkthrough that proves the **Phase-1 value**: auto-prepare a
month's **GSTR-1** and **GSTR-3B** from finalized invoices, catch filing-blocking data errors, export a
**GST-portal-compatible** file, run multiple GSTINs independently, route a late invoice into the next
open period, and surface read-only status on mobile.

> **Out of scope for this quickstart (Phase 2):** electronic **FILING** via the GSP/GSTN network
> (EVC/OTP, ARN minting), and **2A/2B** supplier reconciliation + the 3B ITC table. Where this doc shows
> an ARN it is read-only display of a value set elsewhere. The **electronic-file action and credential
> setup are owner/admin-only**; preparation/readiness/export shown here may be available to a broader
> role.
>
> **Money & rounding:** internal math is exact (`BigDecimal` scale 4); **portal return totals are
> rounded to the whole rupee** (`HALF_UP`), rounding applied **once at the section-total boundary** — so
> section sums foot to the rounded header.

---

## Conventions used below

- Base URL: `http://localhost:8080` (adjust for your environment).
- Every request carries the workspace header and an auth token:
  - `X-Workspace-ID: ws_demo_01`
  - `Authorization: Bearer <jwt>`
- All request/response bodies are **snake_case** and wrapped in the standard `ApiResponse<T>` envelope:
  ```json
  { "success": true, "data": { ... }, "error": null,
    "timestamp": "2026-07-01T09:00:00Z", "path": "/gstr/v1/...", "trace_id": "…" }
  ```
- `{gstin}` = a registered 15-char GSTIN · `{period}` = `MMYYYY` (monthly) or `Q{n}YYYY` (quarterly).
- Example tax period throughout: **June 2026 → `062026`**.

---

## Prerequisites

1. **A workspace** (`ws_demo_01`) with an **owner/admin user** signed in (JWT obtained via the auth flow).
2. The **`gstr_enabled`** setting is **ON** for the workspace (registered by `GstrSettingDefinitions`;
   toggle via the `setting` module). The `b2cl_threshold` setting defaults to ₹1,00,000.
3. A representative mix of **finalized, non-cancelled** invoices in June 2026, all billed from the
   Maharashtra GSTIN (state code `27`), covering every classification path:

   | # | Invoice | Buyer | PoS | Rate | Notes |
   |---|---|---|---|---|---|
   | INV-101 | B2B registered | GSTIN `27AABCU…` | 27 (intra) | 18% | CGST+SGST; lands in **B2B** |
   | INV-102 | B2B registered | GSTIN `29AAACK…` | 29 (inter) | 12% | IGST; lands in **B2B** |
   | INV-103 | B2C unregistered, **inter-state, ₹1,40,000** | no GSTIN | 29 (inter) | 18% | > threshold → **B2CL** |
   | INV-104 | B2C unregistered, intra-state, ₹4,500 | no GSTIN | 27 (intra) | 5% | → **B2CS** (state+rate summary) |
   | INV-105 | B2C unregistered, inter-state, ₹2,000 | no GSTIN | 29 (inter) | 18% | below threshold → **B2CS** |
   | CDN-106 | **Credit note** against INV-101 | GSTIN `27AABCU…` | 27 | 18% | → **CDNR**, reduces totals |

   Each line carries a valid **HSN/SAC** and the seller GSTIN/place-of-supply. (These are the fields the
   `Gstr1Aggregator` reads from `invoice` + the immutable tax audit snapshot — it never re-computes tax.)

> If June has **no** activity for a GSTIN, prepare still works and yields a **NIL** return — exercise
> that separately if you like (`FR-008`).

---

## Step 1 — Register a GSTIN

Create the per-state GSTIN branch the period is keyed to. State code = first 2 digits of the GSTIN.

```bash
curl -X POST http://localhost:8080/gstr/v1/gstins \
  -H "X-Workspace-ID: ws_demo_01" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
        "gstin": "27AABCU9603R1ZN",
        "state_code": "27",
        "legal_name": "Demo Traders Pvt Ltd",
        "trade_name": "Demo Traders",
        "registration_type": "REGULAR",
        "filing_frequency": "MONTHLY"
      }'
```

**Expected** (`ApiResponse<GstinRegistrationResponse>`):

```json
{ "success": true,
  "data": {
    "gstin": "27AABCU9603R1ZN",
    "state_code": "27",
    "legal_name": "Demo Traders Pvt Ltd",
    "trade_name": "Demo Traders",
    "registration_type": "REGULAR",
    "filing_frequency": "MONTHLY",
    "active": true,
    "created_at": "2026-07-01T09:00:00Z"
  },
  "error": null }
```

Because `filing_frequency = MONTHLY`, periods are presented as **months** (`MMYYYY`). A `QUARTERLY`
GSTIN would present quarters (`Q1…Q4`) and prepare **one GSTR-1 per quarter** (no monthly IFF — Phase 3).

---

## Step 2 — Prepare GSTR-1 for (gstin, GSTR1, period)

```bash
curl -X POST \
  "http://localhost:8080/gstr/v1/returns/27AABCU9603R1ZN/GSTR1/062026/prepare" \
  -H "X-Workspace-ID: ws_demo_01" \
  -H "Authorization: Bearer $JWT"
```

This reads every finalized, non-cancelled invoice/credit-note for that GSTIN's state in June 2026 and
classifies each into **exactly one** section, foots rate-wise, and writes an immutable
`GstReturnSnapshot`. Status advances `NOT_STARTED → PREPARED` (only if readiness passes — see Step 3).

**Expected** (`ApiResponse<Gstr1Response>` — abridged; totals are illustrative):

```json
{ "success": true,
  "data": {
    "gstin": "27AABCU9603R1ZN",
    "return_type": "GSTR1",
    "period": "062026",
    "financial_year": "2026-27",
    "status": "PREPARED",
    "sections": {
      "b2b":  { "invoice_count": 2, "taxable_value": 250000, "igst": 12000, "cgst": 22500, "sgst": 22500 },
      "b2cl": { "invoice_count": 1, "taxable_value": 140000, "igst": 25200, "cgst": 0, "sgst": 0 },
      "b2cs": [
        { "pos": "27", "rate": 5,  "taxable_value": 4500, "cgst": 112.5, "sgst": 112.5, "igst": 0 },
        { "pos": "29", "rate": 18, "taxable_value": 2000, "cgst": 0, "sgst": 0, "igst": 360 }
      ],
      "cdnr": { "note_count": 1, "taxable_value": -50000, "cgst": -4500, "sgst": -4500, "igst": 0 },
      "exp":  { "invoice_count": 0 },
      "nil":  { "nil_rated": 0, "exempt": 0, "non_gst": 0 },
      "hsn":  [
        { "hsn": "8471", "uqc": "NOS", "quantity": 12, "taxable_value": 250000, "total_tax": 57000 },
        { "hsn": "8523", "uqc": "NOS", "quantity": 60, "taxable_value": 146500, "total_tax": 25672.5 }
      ],
      "docs": [
        { "doc_type": "INVOICE",     "series": "INV", "from": "101", "to": "105", "total": 5, "cancelled": 0, "net_issued": 5 },
        { "doc_type": "CREDIT_NOTE", "series": "CDN", "from": "106", "to": "106", "total": 1, "cancelled": 0, "net_issued": 1 }
      ]
    },
    "headline": { "total_taxable_value": 346500, "total_tax": 78672, "rounded": true },
    "prepared_at": "2026-07-01T09:01:00Z"
  },
  "error": null }
```

**What to verify (SC-002):**
- Each source document appears in **exactly one** section (INV-101/102 → B2B; INV-103 → B2CL because it
  is unregistered + inter-state + above ₹1,00,000; INV-104/105 → B2CS by `(pos, rate)`; CDN-106 → CDNR).
- The B2CL/B2CS **split happens at the threshold** — INV-105 (₹2,000, inter-state) is still B2CS.
- Rate-wise section totals **foot to the source invoices**; the headline is rupee-rounded once.
- A later product rate change does **not** alter these figures (rates come from the issue-time snapshot — `FR-005`).

Retrieve later without re-preparing: `GET /gstr/v1/returns/{gstin}/GSTR1/062026/gstr1`.

---

## Step 3 — Readiness check (blocking errors gate "ready")

The readiness report separates **blocking errors** from **warnings**. Errors prevent the period from
advancing to `PREPARED`/ready (and, in Phase 2, from being filed). The system **never** auto-edits a
finalized invoice — you fix the source and re-prepare.

### 3a. Introduce two defects in the source data
- Edit **INV-102** so its **customer GSTIN is removed** (now a B2B invoice with no buyer GSTIN).
- Edit a line on **INV-101** so its **HSN/SAC is blank**.

### 3b. Re-run readiness

```bash
curl "http://localhost:8080/gstr/v1/returns/27AABCU9603R1ZN/GSTR1/062026/readiness" \
  -H "X-Workspace-ID: ws_demo_01" \
  -H "Authorization: Bearer $JWT"
```

**Expected** (`ApiResponse<ReturnReadinessReport>`) — status held back, two blocking errors:

```json
{ "success": true,
  "data": {
    "gstin": "27AABCU9603R1ZN", "return_type": "GSTR1", "period": "062026",
    "ready": false,
    "blocking_errors": [
      { "code": "MISSING_CUSTOMER_GSTIN", "invoice_uid": "INV-102",
        "field": "customer_gst", "message": "B2B invoice has no valid customer GSTIN" },
      { "code": "MISSING_HSN", "invoice_uid": "INV-101",
        "line": 1, "field": "hsn_sac", "message": "Line is missing a mandatory HSN/SAC code" }
    ],
    "warnings": [
      { "code": "LATE_FILING_ESTIMATE",
        "message": "Estimated late fee/interest ₹0 (informational only — never charged)" }
    ]
  },
  "error": null }
```

Re-running `prepare` while blocking errors exist keeps the status **out of** `PREPARED` (`FR-010`).
Late-fee/interest is **informational only** — never charged, never a managed liability (`FR-016`).

### 3c. Fix the source and re-prepare
Restore INV-102's customer GSTIN and INV-101's line HSN, then `POST …/prepare` again. Readiness now
clears (`ready: true`, empty `blocking_errors`) and the period advances to **PREPARED**, then may be
moved to **RECONCILED** by the invoice⟷GSTR-1 self-check.

**What to verify (SC-004):** missing GSTIN, missing HSN, and invalid place-of-supply are all caught
**before** export — and resolving them lets status advance.

---

## Step 4 — Prepare GSTR-3B (ties to GSTR-1 to the rupee)

3B's **outward liability is derived from the GSTR-1 totals** already computed (plus reverse-charge), so
the two reconcile by construction. The **ITC section is shown as pending** (purchase/2B data is Phase 2)
— not silently zero.

```bash
curl -X POST \
  "http://localhost:8080/gstr/v1/returns/27AABCU9603R1ZN/GSTR3B/062026/prepare" \
  -H "X-Workspace-ID: ws_demo_01" \
  -H "Authorization: Bearer $JWT"
```

**Expected** (`ApiResponse<Gstr3bResponse>` — abridged):

```json
{ "success": true,
  "data": {
    "gstin": "27AABCU9603R1ZN", "return_type": "GSTR3B", "period": "062026", "status": "PREPARED",
    "table_3_1_outward": {
      "taxable_value": 346500, "igst": 25560, "cgst": 18112, "sgst": 18112, "cess": 0,
      "derived_from": "GSTR1"
    },
    "table_3_1_d_reverse_charge": { "taxable_value": 0, "igst": 0, "cgst": 0, "sgst": 0 },
    "table_4_itc": { "status": "PENDING",
      "message": "Input-tax-credit pending until purchase/2B data is available (Phase 2)" },
    "tie_out": { "matches_gstr1_outward": true, "drift": 0 }
  },
  "error": null }
```

**What to verify (SC-003):** `tie_out.matches_gstr1_outward = true` and `drift = 0` — the 3B outward tax
equals the GSTR-1 outward totals **to the rupee**; reverse-charge entries are present; ITC reads
**PENDING**, never `0`.

---

## Step 5 — Export a portal-compatible file

Export the prepared GSTR-1 in the **GSTN offline-utility JSON** shape (PascalCase/mixed GSTN field names
— a deliberately non-standard external contract built by isolated `*PortalBuilder` DTOs), or as **Excel**.

### 5a. JSON (portal-compatible)

```bash
curl "http://localhost:8080/gstr/v1/returns/27AABCU9603R1ZN/GSTR1/062026/export?format=json" \
  -H "X-Workspace-ID: ws_demo_01" \
  -H "Authorization: Bearer $JWT" \
  -o gstr1_062026.json
```

**Expected** — a GSTN-portal-compatible document (note GSTN's own field names: `gstin`, `fp`, `b2b`,
`inv`, `itms`, `txval`, `iamt`/`camt`/`samt`/`csamt`, `pos`, `rt`, `hsn_sc`):

```json
{
  "gstin": "27AABCU9603R1ZN",
  "fp": "062026",
  "b2b": [ { "ctin": "29AAACK…", "inv": [ { "inum": "INV-102", "val": 112000,
    "itms": [ { "rt": 12, "txval": 100000, "iamt": 12000 } ] } ] } ],
  "b2cl": [ { "pos": "29", "inv": [ { "inum": "INV-103", "val": 165200,
    "itms": [ { "rt": 18, "txval": 140000, "iamt": 25200 } ] } ] } ],
  "b2cs": [ { "pos": "27", "rt": 5, "txval": 4500, "camt": 112.5, "samt": 112.5 } ],
  "cdnr": [ … ],
  "hsn": { "data": [ { "hsn_sc": "8471", "uqc": "NOS-NUMBERS", "qty": 12,
    "txval": 250000, "iamt": 0, "camt": 22500, "samt": 22500 } ] },
  "doc_issue": { "doc_det": [ … ] }
}
```

This is the file the user uploads to the GST portal / offline tool (SC-005) — no re-keying.

### 5b. Excel

```bash
curl "http://localhost:8080/gstr/v1/returns/27AABCU9603R1ZN/GSTR1/062026/export?format=xlsx" \
  -H "X-Workspace-ID: ws_demo_01" \
  -H "Authorization: Bearer $JWT" \
  -o gstr1_062026.xlsx
```

**Expected:** a binary `.xlsx` (`Content-Type: application/vnd.openxmlformats-…sheet`), one worksheet per
section, totals **rounded to whole rupees** at the section boundary (the only rounding point).

> Same `?format=json|xlsx` pattern works for GSTR-3B: `…/GSTR3B/062026/export?format=json`.

---

## Step 6 — Multi-GSTIN, independent per state

Register a second-state GSTIN and confirm each GSTIN's GSTR-1 contains **only its own state's**
invoices and carries **independent** status.

```bash
# Register Karnataka (state 29) GSTIN
curl -X POST http://localhost:8080/gstr/v1/gstins \
  -H "X-Workspace-ID: ws_demo_01" -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{ "gstin": "29AABCU9603R1Z5", "state_code": "29",
        "legal_name": "Demo Traders Pvt Ltd", "trade_name": "Demo Traders – KA",
        "registration_type": "REGULAR", "filing_frequency": "MONTHLY" }'
```

Finalize **INV-201**, billed **from the Karnataka GSTIN** (seller GSTIN `29…`), then prepare its GSTR-1:

```bash
curl -X POST \
  "http://localhost:8080/gstr/v1/returns/29AABCU9603R1Z5/GSTR1/062026/prepare" \
  -H "X-Workspace-ID: ws_demo_01" -H "Authorization: Bearer $JWT"
```

**What to verify (SC-007):**
- The Karnataka GSTR-1 contains **only INV-201** (zero cross-state leakage); the Maharashtra GSTR-1 is
  unchanged and still excludes INV-201.
- Each GSTIN's period has its **own status** — preparing/advancing one does not affect the other.
- A sale is attributed to the GSTIN of the **state it was billed from** (seller GSTIN state), not the
  workspace.

---

## Step 7 — Late invoice into a FILED period routes to the NEXT open period

This validates the clarification: a back-dated invoice dated inside an **already-FILED** period must
still **finalize (keeping its real date)**, be reported in the **next open** period's GSTR-1, and **never
alter** the filed return.

> Phase 1 has no live FILING, so simulate the lock: mark the June 2026 period **FILED** for
> `27AABCU9603R1ZN` (test seam / direct status set), so `PeriodLockService.isPeriodLocked(gstin, date)`
> returns true for any June 2026 date.

1. **Finalize INV-150 dated `2026-06-20`** (inside the now-FILED June period). Confirm in the `invoice`
   module that finalize **succeeds** and the invoice **keeps `2026-06-20`** — it is never blocked or
   rejected (`FR-025`).
2. **Re-prepare the filed June return** — confirm it is **refused** (a FILED period is immutable):
   ```bash
   curl -X POST "http://localhost:8080/gstr/v1/returns/27AABCU9603R1ZN/GSTR1/062026/prepare" \
     -H "X-Workspace-ID: ws_demo_01" -H "Authorization: Bearer $JWT"
   ```
   **Expected** (error envelope):
   ```json
   { "success": false, "data": null,
     "error": { "code": "PERIOD_LOCKED",
       "message": "GSTR1 for 27AABCU9603R1ZN 062026 is FILED and immutable" } }
   ```
3. **Prepare the next open period (July 2026 → `072026`)** and confirm **INV-150 is reported there**
   (as a later-period document), while the filed June snapshot is **unchanged**:
   ```bash
   curl -X POST "http://localhost:8080/gstr/v1/returns/27AABCU9603R1ZN/GSTR1/072026/prepare" \
     -H "X-Workspace-ID: ws_demo_01" -H "Authorization: Bearer $JWT"
   ```

**What to verify (SC-006):** the filed June return is never altered in place; the late invoice surfaces
in the next open period; finalize was never blocked.

---

## Step 8 — Mobile read-only status surface (pull-only `/sync`)

The mobile app **pulls** period status + headline totals + ARN read-only — it never computes or edits a
return. The pull-only feed is on the canonical `/sync` contract.

```bash
curl "http://localhost:8080/gstr/v1/returns/sync?last_sync=&page=0&size=100&sort_by=updatedAt&sort_dir=ASC" \
  -H "X-Workspace-ID: ws_demo_01" -H "Authorization: Bearer $JWT"
```

**Expected** (`ApiResponse<PageResponse<GstReturnPeriodSyncDto>>` — abridged):

```json
{ "success": true,
  "data": { "content": [
      { "gstin": "27AABCU9603R1ZN", "return_type": "GSTR1", "period": "062026",
        "status": "FILED", "arn": "AA2706260000000",
        "total_taxable_value": 346500, "total_tax": 78672, "updated_at": "2026-07-01T09:10:00Z" },
      { "gstin": "27AABCU9603R1ZN", "return_type": "GSTR3B", "period": "062026",
        "status": "PREPARED", "arn": null,
        "total_taxable_value": 346500, "total_tax": 61784, "updated_at": "2026-07-01T09:05:00Z" },
      { "gstin": "29AABCU9603R1Z5", "return_type": "GSTR1", "period": "062026",
        "status": "PREPARED", "arn": null, "updated_at": "2026-07-01T09:08:00Z" }
    ],
    "has_next": false, "page": 0, "size": 100 },
  "error": null }
```

**What to verify (SC-009):** a field user sees the correct **status, headline totals and ARN** per
GSTIN/period; the device performs **no return computation**. (`ARN` here is read-only — it is set by
Phase-2 filing.) "prepare"/"file" from mobile are **online commands** to the server only.

---

## What this validates — Success Criteria map

| Step | Validates | Spec criteria |
|---|---|---|
| 2 | GSTR-1 prepared in one action from finalized invoices, headline totals reviewed, no manual entry | **SC-001** |
| 2 | Every invoice in exactly one section; section totals foot to source (B2B/B2CL/B2CS/CDNR/HSN/DOCS) | **SC-002** |
| 4 | 3B outward liability equals GSTR-1 outward totals to the rupee (`drift = 0`); ITC shown pending | **SC-003** |
| 3 | Missing GSTIN / missing HSN / invalid place-of-supply caught as blocking errors before export | **SC-004** |
| 5 | Portal-compatible JSON (+ Excel) exported in the GSTN offline-utility shape | **SC-005** |
| 7 | Filed period never altered; late invoice routed to next open period; no in-place edit | **SC-006** |
| 6 | Each GSTIN files independently; only its own state's invoices; zero cross-state leakage | **SC-007** |
| (P2) | Books⟷2A/2B mismatch buckets + ±₹1 rounding tolerance | **SC-008** *(Phase 2 — not in this quickstart)* |
| 8 | Mobile shows status / headline totals / ARN read-only; no on-device computation | **SC-009** |

> **SC-008** (purchase ⟷ supplier-reported reconciliation, mismatch taxonomy, ±₹1 tolerance) depends on
> the **2A/2B pull + ITC engine**, which is **Phase 2** and intentionally out of this Phase-1 quickstart.

---

## Test commands

Run from the backend repo with the feature context set:

```bash
cd /home/user/ampairs
export SPECIFY_FEATURE=028-gstr-filing      # speckit feature context for /speckit.* tooling

# Backend module tests (once implemented): GSTR-1 aggregation golden tests,
# 3B⟷GSTR-1 tie-out, readiness gate, portal-JSON conformance, period-lock/immutability.
./gradlew :gstr:test

# Wider gates (need Docker for Testcontainers)
./gradlew :ampairs_service:flywayInfo     # confirm the V1.0.110 band is free before merge
./gradlew testAll
```

> Phase-1 endpoints exercised here: `POST /gstr/v1/gstins`,
> `POST /gstr/v1/returns/{gstin}/{type}/{period}/prepare`, `GET …/readiness`, `GET …/gstr1`,
> `GET …/gstr3b`, `GET …/export?format=json|xlsx`, `GET /gstr/v1/returns/sync`.
> Electronic **filing** (EVC/OTP, ARN minting) and **2A/2B** reconciliation are **Phase 2**; the
> **file action and credential setup are owner/admin-only**.
