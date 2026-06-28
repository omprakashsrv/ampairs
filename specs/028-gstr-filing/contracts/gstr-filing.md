# gstr-filing — Electronic filing, ARN, 2A/2B pull, reconciliation (Phase 2)

> **PHASE 2.** These endpoints require GSP/GSTN onboarding (the `GstnFilingProvider` port + per-workspace
> resolver, R6/R7). They are **online-only** GSTN round-trips driven by a backend queue + retry worker
> (`GstnFilingWorker`, `@Scheduled` exponential backoff) — the mobile app surfaces status/ARN read-only
> and triggers file as an online command only (R11).

**Controllers:** `GstrFilingController` (file/status/2A-2B/recon), `GstrController`
(purchase-register import). **Services:** `GstReturnService`, `GstnProviderResolver`,
`ReconciliationEngine`, `PurchaseRegisterImportService`.

## RBAC — owner/admin gate (FR-031, spec clarification)

| Endpoint | Roles |
|---|---|
| `/file`, `/file/confirm` (the legal EVC/ARN submission) | **OWNER / ADMIN only** |
| credential setup (see note) | **OWNER / ADMIN only** |
| `/filing-status`, `/2b/pull`, `/reconciliation`, purchase import | any workspace member |

A non-owner/admin file/credential attempt → **403 `FORBIDDEN`** (they may still prepare/review/export —
US5 #5). EVC OTP goes to the **authorized signatory's** own registered mobile/email via GSTN — it is
**never shown to or stored by the app** (FR-022/FR-027). GSP/GSTN credentials stay server-side,
env-provided + encrypted per-GSTIN row (R6/Principle XI), never in source or on the device.

Path params: `{gstin}` (15-char), `{type}` (`gstr1`|`gstr3b`|`cmp08`), `{period}`.

---

## 1. Initiate filing — request EVC (step 1 of 2)

```
POST /gstr/v1/returns/{gstin}/{type}/{period}/file          (OWNER/ADMIN)
```

Submits the prepared+reconciled return to GSTN via the provider and **requests an EVC OTP**, which GSTN
sends to the signatory. Creates/advances a `GstFilingAttempt`
(`INITIATED → SUBMITTED → EVC_REQUESTED`).

### Idempotency (R7/R8/FR-026)

The request carries a stable **`client_request_id`**; a retry after a lost ack does **not** double-file.
Before submitting, the worker/service calls the provider's `getReturnStatus` — if the period is already
`FILED` at GSTN, it stores the existing ARN and returns success instead of re-filing (never double-files
a period — SC-006).

Request — `FileReturnRequest`:

| Field | Type | Notes |
|---|---|---|
| `client_request_id` | string | `@field:NotBlank` — idempotency key; same id = same attempt |
| `signatory_pan` | string? | signatory whose registered mobile receives the EVC (else default) |
| `verification_method` | string | `EVC` (only); `DSC` is out of scope (server-side DSC custody non-starter) |

### Response — `ApiResponse<GstFilingAttemptResponse>`

| Field | Type | Notes |
|---|---|---|
| `attempt_uid` | string | |
| `status` | string | `INITIATED` \| `SUBMITTED` \| `EVC_REQUESTED` \| `FILED` \| `ACKNOWLEDGED` \| `FAILED` |
| `client_request_id` | string | echoed |
| `evc_requested` | bool | OTP sent to signatory |
| `gstn_reference` | string? | provider transaction ref |
| `message` | string | e.g. "OTP sent to authorized signatory" |

### Errors

| Case | Result |
|---|---|
| Non-owner/admin | 403 — `FORBIDDEN` |
| Period not `RECONCILED` / has blocking readiness errors | 409 — `RETURN_NOT_READY` (R10 gate; cannot file with blocking errors) |
| Period already `FILED`/`ACKNOWLEDGED` | 200 — idempotent success, returns existing ARN (never double-files) |
| No GSP credential configured for GSTIN | 412 — `FILING_NOT_CONFIGURED` |
| GSTN unavailable / maintenance | 200 — attempt `INITIATED`, **queued** for retry (online-only, R7) |

---

## 2. Confirm filing — submit OTP (step 2 of 2)

```
POST /gstr/v1/returns/{gstin}/{type}/{period}/file/confirm   (OWNER/ADMIN)
```

The signatory's EVC OTP is relayed (online command) to complete filing. On success GSTN returns the
**ARN**, which is persisted on the `GstReturnPeriod`; the period advances `FILED → ACKNOWLEDGED` and
becomes **immutable + source-locked** (R8/FR-023/FR-025).

Request — `ConfirmFilingRequest`:

| Field | Type | Notes |
|---|---|---|
| `attempt_uid` | string | `@field:NotBlank` — the `EVC_REQUESTED` attempt |
| `client_request_id` | string | same idempotency key as step 1 |
| `otp` | string | `@field:NotBlank` — the signatory's EVC; relayed to GSTN, **never persisted** |

### Response — `ApiResponse<GstReturnPeriodResponse>`

Includes `status` (`FILED`/`ACKNOWLEDGED`), `arn`, `filed_at`. After this the period is locked:
re-prepare → `PERIOD_LOCKED`; a back-dated invoice into this period finalizes and is routed to the
**next open** period's GSTR-1 (never alters filed history — FR-025; handled on the invoice finalize
path, not here).

| Error | Result |
|---|---|
| Wrong/expired OTP | 400 — `INVALID_EVC` (attempt stays `EVC_REQUESTED`; re-request) |
| Attempt not in `EVC_REQUESTED` | 409 — `INVALID_FILING_STATE` |
| Non-owner/admin | 403 — `FORBIDDEN` |

---

## 3. Filing status / ARN

```
GET /gstr/v1/returns/{gstin}/{type}/{period}/filing-status
→ ApiResponse<FilingStatusResponse>
```

The authoritative filing status (reconciled against GSTN via `getReturnStatus`), surfaced read-only on
mobile (US7). Available to any member.

| Field | Type | Notes |
|---|---|---|
| `status` | string | period lifecycle status |
| `arn` | string? | once filed |
| `filed_at` | string (ISO-8601)? | |
| `latest_attempt` | `GstFilingAttemptResponse`? | current attempt status (queue/retry visibility) |
| `gstn_status` | string? | last reconciled GSTN portal status |

---

## 4. Pull supplier-reported 2A/2B

```
POST /gstr/v1/returns/{gstin}/2b/pull          (online-only; queued)
body: { "period": "062026", "statement": "2B" }   // "2A" (live) | "2B" (static monthly)
→ ApiResponse<Gstn2bPullResponse>
```

Pulls supplier-reported inward invoices from GSTN via the provider into `Gstn2bRecord` rows (the
counter-party side of ITC reconciliation, R5/R9). Online-only — queues + retries on GSTN
unavailability (FR not failing outright). 2A/2B pull is **backend-only**, never on the device.

| Field | Type | Notes |
|---|---|---|
| `pull_uid` | string | |
| `statement` | string | `2A` \| `2B` |
| `record_count` | int | rows pulled |
| `pulled_at` | string (ISO-8601)? | null while queued |
| `status` | string | `QUEUED` \| `PULLED` \| `FAILED` |

---

## 5. Reconciliation result (mismatch buckets)

```
GET /gstr/v1/returns/{gstin}/{type}/{period}/reconciliation?bucket={optional}
→ ApiResponse<ReconciliationResultResponse>
```

The books⟷2B ITC reconciliation (and the invoice⟷GSTR-1 self-check) — **flag-only**, never mutating
source data (R9/FR-013/FR-014). Optional `bucket` filter (snake_case enum value).

| Field | Type | Notes |
|---|---|---|
| `summary` | object | counts per bucket + `eligible_itc` / `at_risk_itc` (rupee) |
| `lines` | `[ReconLine]` | per purchase/2B line |

`ReconLine`:

| Field | Type | Notes |
|---|---|---|
| `bucket` | string | `MATCHED`, `MISMATCH_VALUE`, `MISMATCH_GSTIN`, `MISSING_IN_2B`, `MISSING_IN_BOOKS`, `PROBABLE_MATCH` |
| `supplier_gstin` | string? | |
| `supplier_invoice_no` / `supplier_invoice_date` | string? | |
| `books_taxable` / `b2_taxable` | string (rupee)? | |
| `books_tax` / `b2_tax` | string (rupee)? | |
| `delta` | string (rupee)? | books − 2B; within ±₹1 tolerance ⇒ not a mismatch (R12/SC-008) |
| `itc_at_risk` | bool | `MISSING_IN_2B` ⇒ credit at risk (chase supplier) |

`MISMATCH_VALUE` = matched key, amount differs beyond ±₹1; `MISSING_IN_2B` = in books, supplier
hasn't filed (ITC at risk); `MISSING_IN_BOOKS` = in 2B, not booked; `PROBABLE_MATCH` = fuzzy
invoice-no. Every books line and every 2B line lands in exactly one bucket (SC-008). Feeds the GSTR-3B
ITC table (R4).

---

## 6. Import the purchase register (books side)

```
POST /gstr/v1/purchase-register/import          (multipart/form-data)
parts:  file=<CSV|XLSX>   period=<MMYYYY|Q{n}YYYY>   gstin=<15-char>
→ ApiResponse<PurchaseRegisterImportResponse>
```

Seeds `PurchaseRegisterEntry` rows (supplier GSTIN, supplier invoice no/date, taxable, CGST/SGST/
IGST/CESS, ITC eligibility) from a CSV/Excel the accountant already maintains — the books side of ITC
reconciliation until a first-class purchase module exists (R5/FR-015). Not a `/sync` resource; UI-invoked
multipart, like `file`.

| Field | Type | Notes |
|---|---|---|
| `import_uid` | string | |
| `imported_count` | int | rows accepted |
| `rejected_count` | int | rows with errors |
| `errors` | `[ImportRowError]` | `{ row, field, message }` |

| Error | Result |
|---|---|
| Unsupported file type | 400 — `UNSUPPORTED_IMPORT_FORMAT` |
| `gstin` not registered | 404 — `GSTIN_NOT_FOUND` |
| Period `FILED` | 200 — imported, but recon attributes ITC to the next open period (filed history untouched) |

---

## Notes

- **Credential setup** (the encrypted per-GSTIN `GstnCredential` row + GSP selection) is owner/admin-only
  (FR-031). The credential management endpoints are deferred to the Phase-2 implementation detail; they
  are listed here only to record the RBAC gate — they are **not** counted among the six endpoints below.
- **Online-only / queue+retry:** `/file`, `/file/confirm` and `/2b/pull` enqueue work the
  `GstnFilingWorker` drains with exponential backoff; a GSTN outage queues rather than fails (R7).

## Endpoint count: 6
(file, file/confirm, filing-status, 2b/pull, reconciliation, purchase-register/import.)
