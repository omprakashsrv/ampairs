# Phase 1 Data Model — GST Return Filing & Reconciliation (`gstr`)

**Feature**: `028-gstr-filing` | **Date**: 2026-06-28 | **Plan**: [plan.md](./plan.md) | **Research**: [research.md](./research.md)

This document is the Phase 1 design artifact for the `gstr` bounded context's persistence model: the
entities, their fields, relationships, validation/uniqueness rules, enums, and the state machines that
encode the filing lifecycle. It is a *design* doc — short illustrative Kotlin snippets are given for
shape, not full source.

## Intro — what the model encodes

The model is shaped by two research decisions above all others:

- **R2 — return-period & multi-GSTIN.** A single legal business may hold **several GSTINs (one per state
  of operation — the "branch" model)**, and GST returns are filed **per GSTIN, per return type, per
  period**. So the model is rooted on a first-class **`GstinRegistration`** (NOT the single nullable
  `TaxConfiguration.gstin`), and the filing aggregate **`GstReturnPeriod`** is keyed by
  `(owner_id, gstin, return_type, financial_year, period)`. Each GSTIN files on its own schedule
  (monthly vs quarterly/QRMP) with independent status, totals and ARN. The 2-digit state code embedded
  in the 15-char GSTIN is also the place-of-supply key the GSTR-1 B2CS/HSN summaries aggregate on.

- **R8 — idempotency, period-locking & immutability.** A filed GST return is a legal submission, so the
  model makes three guarantees structural: **(1)** a *unique* `(owner_id, gstin, return_type,
  financial_year, period)` constraint → exactly one return per period (idempotent re-prepare);
  **(2)** an immutable **`GstReturnSnapshot`** frozen the moment a period reaches `FILED`, plus a
  *source-period lock* surfaced via `PeriodLockService.isPeriodLocked(gstin, date)`; **(3)** a
  **`GstFilingAttempt`** carrying a stable `clientRequestId` so a retried file call after a lost
  acknowledgement never double-files. Re-preparation regenerates the snapshot **only** while
  `status ∈ {NOT_STARTED, PREPARED, RECONCILED}`.

All entities follow the project conventions: tenant entities extend `OwnableBaseDomain` (`@TenantId
ownerId`, `uid`, `createdAt`/`updatedAt` as `Instant`); timestamps are `Instant` → `TIMESTAMPTZ`
(Postgres) / `TIMESTAMP` (MySQL), never `LocalDateTime`; money is `BigDecimal` → `DECIMAL(19,4)`;
enums are persisted `@Enumerated(EnumType.STRING)`; the large computed/section payloads are
`@JdbcTypeCode(SqlTypes.JSON)` / `TEXT`/`LONGTEXT`.

**Phase tags:** P1 = Phase 1 (export-first GSTR-1/3B prep + self-check + multi-GSTIN). P2 = Phase 2
(GSP API filing + 2A/2B pull + ITC reconciliation). P3 = Phase 3 (annual returns).

---

## Entities

### `GstinRegistration` — per-state GSTIN branch model · **P1**

**Purpose.** Models each GST registration a business holds (one row per state GSTIN). It is the
honest replacement for the single nullable `TaxConfiguration.gstin` that cannot represent a multi-state
filer (R2). Source documents are attributed to a registration by **seller GSTIN state**.

**Base class.** `OwnableBaseDomain` (tenant-scoped — a workspace owns its GSTINs).

| Field | Type | Null? | Notes |
|---|---|---|---|
| `gstin` | `String` (len 15) | no | The 15-char GSTIN; the natural per-state identity. Validated 15-char GSTIN pattern. |
| `stateCode` | `String` (len 2) | no | First 2 digits of `gstin`; the place-of-supply / B2CS aggregation key. Derived on save and validated to match `gstin.take(2)`. |
| `legalName` | `String` (len 255) | no | Legal name as registered with GST. |
| `tradeName` | `String` (len 255) | yes | Trade name; optional. |
| `registrationType` | `RegistrationType` enum | no | `REGULAR` / `COMPOSITION` / `SEZ` / `CASUAL`. Drives GSTR-1 vs CMP-08 eligibility. |
| `filingFrequency` | `FilingFrequency` enum | no | `MONTHLY` / `QUARTERLY` (QRMP). Determines whether periods are months or quarters. |
| `authorizedSignatory` | `String` (len 255) | yes | Name of the EVC/OTP signatory (P2 filing reference; OTP goes to *their* registered phone, never the app). |
| `isActive` | `Boolean` | no | Soft activation flag; default `true`. |

**Relationships.** One `GstinRegistration` → many `GstReturnPeriod` (by the `gstin` string value, under
the tenant). Not a hard FK to keep the period aggregate keyed on the GSTIN string (matches the unique
constraint in R2/R8).

**Validation / uniqueness.**
- Unique `(owner_id, gstin)` — a workspace registers each GSTIN once.
- `gstin` matches the standard 15-char GSTIN regex; `stateCode == gstin.take(2)`.
- `registrationType` and `filingFrequency` required (no nullable enum).

```kotlin
@Entity(name = "gstr_gstin_registration")
@Table(uniqueConstraints = [UniqueConstraint(name = "uk_gstin_registration",
    columnNames = ["owner_id", "gstin"])],
    indexes = [Index(name = "idx_gstin_reg_owner", columnList = "owner_id")])
class GstinRegistration : OwnableBaseDomain() {
    @Column(name = "gstin", nullable = false, length = 15) var gstin: String = ""
    @Column(name = "state_code", nullable = false, length = 2) var stateCode: String = ""
    @Column(name = "legal_name", nullable = false, length = 255) var legalName: String = ""
    @Column(name = "trade_name", length = 255) var tradeName: String? = null
    @Enumerated(EnumType.STRING) @Column(name = "registration_type", nullable = false, length = 20)
    var registrationType: RegistrationType = RegistrationType.REGULAR
    @Enumerated(EnumType.STRING) @Column(name = "filing_frequency", nullable = false, length = 20)
    var filingFrequency: FilingFrequency = FilingFrequency.MONTHLY
    // ...
}
```

---

### `GstReturnPeriod` — the filing aggregate root · **P1**

**Purpose.** The filing unit and aggregate root: one return for one GSTIN, return type, financial year
and tax period. Carries the status lifecycle and — once filed — the ARN and filed date. Links to its
immutable `GstReturnSnapshot`. The unique key makes idempotency and period-locking a single DB
constraint (R8).

**Base class.** `OwnableBaseDomain`.

| Field | Type | Null? | Notes |
|---|---|---|---|
| `gstin` | `String` (len 15) | no | The owning registration's GSTIN (part of the unique key). |
| `returnType` | `ReturnType` enum | no | `GSTR1` / `GSTR3B` / `CMP08` / `GSTR9` / `GSTR9C`. |
| `financialYear` | `String` (len 9) | no | e.g. `2026-2027`. Part of the unique key; needed for QRMP/annual logic. |
| `period` | `String` (len 10) | no | Tax period token: `MMYYYY` (monthly), `Q{n}YYYY` (quarterly), or the FY (annual). |
| `status` | `ReturnStatus` enum | no | `NOT_STARTED → PREPARED → RECONCILED → FILED → ACKNOWLEDGED`. Default `NOT_STARTED`. |
| `snapshotUid` | `String` (len 200) | yes | Reference to the latest `GstReturnSnapshot.uid` (the computed return). Null until first prepare. |
| `arn` | `String` (len 30) | yes | Acknowledgement Reference Number; set only on successful filing (P2). |
| `filedAt` | `Instant` | yes | Timestamp of successful filing → `TIMESTAMPTZ`/`TIMESTAMP`. Null until filed. |
| `dueDate` | `Instant` | yes | Statutory due date; drives the informational late-fee/interest estimate (R10). |
| `headlineTaxableValue` | `BigDecimal(19,4)` | yes | Cached headline total for fast list/mobile rendering (also lives in the snapshot). |
| `headlineTotalTax` | `BigDecimal(19,4)` | yes | Cached headline tax total. |
| `isNil` | `Boolean` | no | True when prepared as a NIL return (no qualifying activity). Default `false`. |
| `lockedAt` | `Instant` | yes | Set when the period transitions to `FILED`; the source-period lock marker (R8). |

**Relationships.**
- `@ManyToOne`-style logical link to `GstinRegistration` by `gstin` (under tenant).
- One `GstReturnPeriod` → one current `GstReturnSnapshot` (via `snapshotUid`); history snapshots may
  exist but only the period-referenced one is authoritative.
- One `GstReturnPeriod` → many `GstFilingAttempt` (P2).
- `@NamedEntityGraph` `GstReturnPeriod.full` for period + snapshot + attempts (Principle VII).

**Validation / uniqueness.**
- **Unique `(owner_id, gstin, return_type, financial_year, period)`** — exactly one return per period
  (FR-024, R8). This is the idempotency backbone.
- `arn`/`filedAt` are mutually consistent: both null before `FILED`, both set after.
- Re-prepare is permitted only while `status ∈ {NOT_STARTED, PREPARED, RECONCILED}` (enforced in
  service + see State Transitions).

```kotlin
@Table(uniqueConstraints = [UniqueConstraint(name = "uk_gstr_return_period",
    columnNames = ["owner_id", "gstin", "return_type", "financial_year", "period"])],
    indexes = [
        Index(name = "idx_gstr_period_owner_gstin", columnList = "owner_id, gstin"),
        Index(name = "idx_gstr_period_status", columnList = "status")])
class GstReturnPeriod : OwnableBaseDomain() { /* fields above */ }
```

---

### `GstReturnSnapshot` — immutable computed return · **P1**

**Purpose.** The computed, section-structured contents of a prepared return (GSTR-1 sections, or the
GSTR-3B summary, or CMP-08). It is the input to both export and (P2) filing. Frozen the moment its
period reaches `FILED` so filed history can never drift (R8). Deriving from the immutable invoice tax
snapshot (spec 026 R8) guarantees rates **as of issue** (FR-005).

**Base class.** `OwnableBaseDomain`.

| Field | Type | Null? | Notes |
|---|---|---|---|
| `periodUid` | `String` (len 200) | no | Back-reference to the owning `GstReturnPeriod.uid`. |
| `returnType` | `ReturnType` enum | no | Mirrors the period's type for standalone querying. |
| `sectionData` | JSON (`@JdbcTypeCode(SqlTypes.JSON)`) → `TEXT`/`LONGTEXT` | no | The full section-structured payload: GSTR-1 sections keyed by `Gstr1Section` (B2B/B2CL/B2CS/CDNR/CDNUR/EXP/NIL/HSN/DOCS) or the 3B summary tables. The single source for export/portal builders. |
| `totalTaxableValue` | `BigDecimal(19,4)` | no | Headline taxable value (internal scale-4 precision). |
| `totalIgst` | `BigDecimal(19,4)` | no | Headline IGST. |
| `totalCgst` | `BigDecimal(19,4)` | no | Headline CGST. |
| `totalSgst` | `BigDecimal(19,4)` | no | Headline SGST. |
| `totalCess` | `BigDecimal(19,4)` | no | Headline CESS. |
| `documentCount` | `Int` | no | Number of source documents aggregated. |
| `preparedAt` | `Instant` | no | When this snapshot was computed → `TIMESTAMPTZ`/`TIMESTAMP`. |
| `frozen` | `Boolean` | no | `true` once the period is `FILED`; a frozen snapshot is never regenerated. Default `false`. |
| `sourceSnapshotMode` | `String` (len 20) | no | `AUDIT_SNAPSHOT` (spec 026 immutable tax snapshot) or `LIVE_FALLBACK` (legacy invoices without a snapshot) — records the provenance (R3). |

**Relationships.** One `GstReturnSnapshot` belongs to one `GstReturnPeriod` (via `periodUid`); the
period's `snapshotUid` points back to the authoritative current snapshot.

**Validation / uniqueness.**
- Once `frozen = true`, the row is immutable — re-prepare must create a *new* snapshot only when the
  period is not yet filed; a frozen snapshot is never overwritten.
- The headline totals are rupee-rounded **once at the section boundary** for portal export (R12) — see
  the Money/rounding note; the stored `DECIMAL(19,4)` keeps full internal precision.

```kotlin
@Table(indexes = [
    Index(name = "idx_gstr_snapshot_period", columnList = "period_uid"),
    Index(name = "idx_gstr_snapshot_owner", columnList = "owner_id")])
class GstReturnSnapshot : OwnableBaseDomain() {
    @Column(name = "period_uid", nullable = false, length = 200) var periodUid: String = ""
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "section_data", nullable = false)
    var sectionData: String = "{}"
    @Column(name = "total_taxable_value", nullable = false, precision = 19, scale = 4)
    var totalTaxableValue: BigDecimal = BigDecimal.ZERO
    @Column(name = "prepared_at", nullable = false) var preparedAt: Instant = Instant.now()
    @Column(name = "frozen", nullable = false) var frozen: Boolean = false
    // ...
}
```

---

### `PurchaseRegisterEntry` — import-fed ITC source · **P1** (schema) / **P2** (matching)

**Purpose.** The minimal *books* side of input-tax-credit reconciliation. Ampairs has no first-class
purchase/vendor module today (R5), so this is the documented future source, populated in Phase 1 only by
manual CSV/Excel import and consumed by the ITC matching engine in Phase 2. Carries the rate-wise tax,
supplier GSTIN and invoice number that `AdjustmentVoucher`s lack.

**Base class.** `OwnableBaseDomain`.

| Field | Type | Null? | Notes |
|---|---|---|---|
| `gstin` | `String` (len 15) | no | The buying registration's GSTIN (which GSTIN claims the ITC). |
| `supplierGstin` | `String` (len 15) | yes | Supplier's GSTIN; null for unregistered-supplier purchases. |
| `supplierInvoiceNo` | `String` (len 50) | no | Supplier's invoice number — the reconciliation match key. |
| `supplierInvoiceDate` | `Instant` | no | Supplier's invoice date → `TIMESTAMPTZ`/`TIMESTAMP`. |
| `period` | `String` (len 10) | no | Tax period the purchase is claimed in. |
| `taxableValue` | `BigDecimal(19,4)` | no | Taxable value. |
| `cgst` | `BigDecimal(19,4)` | no | CGST input. |
| `sgst` | `BigDecimal(19,4)` | no | SGST input. |
| `igst` | `BigDecimal(19,4)` | no | IGST input. |
| `cess` | `BigDecimal(19,4)` | no | CESS input. |
| `itcEligibility` | `String` (len 20) | no | `ELIGIBLE` / `INELIGIBLE` / `PARTIAL` / `REVERSAL` — the claimable status. |
| `sourceRef` | `String` (len 255) | yes | Reference to the import batch / source file. |

**Relationships.** Logically scoped to a `GstinRegistration` by `gstin`; matched against `Gstn2bRecord`
in Phase 2, producing `ReconciliationResult` rows.

**Validation / uniqueness.**
- Unique `(owner_id, gstin, supplier_gstin, supplier_invoice_no, supplier_invoice_date)` to dedupe
  re-imports of the same purchase line.
- Money fields default to zero, never null, to keep aggregation total-safe.

---

### `Gstn2bRecord` — supplier-reported inward line (2A/2B) · **P2**

**Purpose.** An inward-supply line as reported to the GST network *by the supplier*, pulled from GSTN
(2A live feed / 2B static monthly statement) via the GSP. The counter-party side of ITC reconciliation
(R5/R9).

**Base class.** `OwnableBaseDomain`.

| Field | Type | Null? | Notes |
|---|---|---|---|
| `gstin` | `String` (len 15) | no | The recipient registration's GSTIN (whose 2B this is). |
| `supplierGstin` | `String` (len 15) | no | Supplier's GSTIN as reported to GSTN. |
| `supplierInvoiceNo` | `String` (len 50) | no | Supplier's invoice number (match key). |
| `supplierInvoiceDate` | `Instant` | no | Supplier's invoice date. |
| `returnPeriod` | `String` (len 10) | no | The 2B return period the row belongs to. |
| `taxableValue` | `BigDecimal(19,4)` | no | Reported taxable value. |
| `cgst` | `BigDecimal(19,4)` | no | Reported CGST. |
| `sgst` | `BigDecimal(19,4)` | no | Reported SGST. |
| `igst` | `BigDecimal(19,4)` | no | Reported IGST. |
| `cess` | `BigDecimal(19,4)` | no | Reported CESS. |
| `itcAvailable` | `Boolean` | no | GSTN's ITC-available flag for the line. |
| `rawPayload` | JSON (`@JdbcTypeCode(SqlTypes.JSON)`) → `TEXT` | yes | The raw GSTN 2B line as received (audit/diagnostics; ACL'd, omitted from list DTOs). |
| `pulledAt` | `Instant` | no | When this row was pulled from GSTN → `TIMESTAMPTZ`/`TIMESTAMP`. |

**Relationships.** Scoped to a `GstinRegistration` by `gstin`; the supplier side matched against
`PurchaseRegisterEntry` → `ReconciliationResult`.

**Validation / uniqueness.**
- Unique `(owner_id, gstin, return_period, supplier_gstin, supplier_invoice_no)` so a re-pull upserts
  rather than duplicates.

---

### `ReconciliationResult` — mismatch bucket per line · **P1** (invoice⟷GSTR-1) / **P2** (books⟷2B)

**Purpose.** The classification of a single reconciliation pairing into exactly one mismatch bucket,
with the eligible-vs-at-risk ITC figure. Two reconciliation jobs write here: the Phase-1 intra-platform
invoice⟷GSTR-1 self-check, and the Phase-2 books⟷2B ITC matching (R9). The engine is **flag-only** — it
never mutates source records.

**Base class.** `OwnableBaseDomain`.

| Field | Type | Null? | Notes |
|---|---|---|---|
| `gstin` | `String` (len 15) | no | The GSTIN whose reconciliation this is. |
| `period` | `String` (len 10) | no | The reconciliation period. |
| `reconKind` | `String` (len 20) | no | `INVOICE_VS_GSTR1` (P1) or `BOOKS_VS_2B` (P2) — which job produced the row. |
| `mismatchType` | `MismatchType` enum | no | `MATCHED` / `MISMATCH_VALUE` / `MISMATCH_GSTIN` / `MISSING_IN_2B` / `MISSING_IN_BOOKS` / `PROBABLE_MATCH`. |
| `booksRef` | `String` (len 200) | yes | UID of the books-side line (`PurchaseRegisterEntry` or the finalized invoice) — null when missing-in-books. |
| `counterpartyRef` | `String` (len 200) | yes | UID of the 2B record (or the GSTR-1 section row) — null when missing-in-2B. |
| `eligibleItc` | `BigDecimal(19,4)` | no | ITC safe to claim for this line. Default zero. |
| `atRiskItc` | `BigDecimal(19,4)` | no | ITC flagged at risk (e.g. missing-in-2B). Default zero. |
| `valueDelta` | `BigDecimal(19,4)` | yes | The amount difference for `MISMATCH_VALUE` (within/over the ±₹1 tolerance — R12). |
| `detail` | `String` (len 1000) | yes | Human-readable explanation (which field/series gap), used by the readiness/recon UI. |

**Relationships.** Scoped to a `GstinRegistration` + period; references books and counter-party lines by
their UIDs (loose coupling, never a hard FK across the books/2B sources).

**Validation / uniqueness.**
- At least one of `booksRef` / `counterpartyRef` is non-null (a `MISSING_*` row has exactly one side).
- Money comparisons use `BigDecimal` scale 4 with the ±₹1 reconciliation tolerance so rupee rounding
  alone does not create false mismatches (R12).

---

### `GstFilingAttempt` — electronic-filing transaction · **P2**

**Purpose.** One electronic-filing transaction with its own status machine, used to drive the queue,
retries and idempotency (R7/R8). Filing is online-only; the OTP/EVC goes to the authorized signatory,
never to the app.

**Base class.** `OwnableBaseDomain`.

| Field | Type | Null? | Notes |
|---|---|---|---|
| `periodUid` | `String` (len 200) | no | The `GstReturnPeriod` being filed. |
| `gstin` | `String` (len 15) | no | The filing GSTIN. |
| `status` | `FilingStatus` enum | no | `INITIATED → SUBMITTED → EVC_REQUESTED → FILED → ACKNOWLEDGED` \| `FAILED`. Default `INITIATED`. |
| `clientRequestId` | `String` (len 64) | no | **Idempotency key** — stable per logical filing so a retry after a lost ack never double-files (R8). |
| `arn` | `String` (len 30) | yes | ARN returned by GSTN on success (also persisted on the period). |
| `gspReference` | `String` (len 100) | yes | The GSP/provider transaction reference. |
| `evcRequestedAt` | `Instant` | yes | When the EVC/OTP was requested from GSTN → `TIMESTAMPTZ`/`TIMESTAMP`. |
| `submittedAt` | `Instant` | yes | When the return was submitted (pre-EVC). |
| `completedAt` | `Instant` | yes | When the attempt reached a terminal state. |
| `attemptCount` | `Int` | no | Retry counter for the backoff worker. Default `0`. |
| `errorCode` | `String` (len 100) | yes | GSTN/GSP error code on `FAILED`. |
| `errorMessage` | `String` (len 1000) | yes | Human-readable failure reason. |

**Relationships.** Many `GstFilingAttempt` → one `GstReturnPeriod` (via `periodUid`).

**Validation / uniqueness.**
- Unique `(owner_id, gstin, period_uid, client_request_id)` — the idempotency guarantee at the DB level.
- Before re-filing, the worker calls `getReturnStatus` and treats an already-`FILED` period as success
  (storing the existing ARN), never re-submitting (R8).

---

### `GstnCredential` — encrypted per-GSTIN GSP/GSTN creds · **P2**

**Purpose.** The per-GSTIN GSP/GSTN credentials needed for API filing and 2A/2B pull. Owner/admin-gated
(FR-031), kept server-side only, encrypted at rest; never in source control, never sent to the mobile
app (FR-027, rule 10-security).

**Base class.** `OwnableBaseDomain`.

| Field | Type | Null? | Notes |
|---|---|---|---|
| `gstin` | `String` (len 15) | no | The GSTIN these credentials authenticate. |
| `gspProvider` | `String` (len 50) | no | Which GSP impl (e.g. `CLEARTAX`, `MASTERINDIA`, `SANDBOX`) resolves these. |
| `gstnUsername` | `String` (len 255) | no | The GSTN API username for the GSTIN. |
| `encryptedSecret` | `String` (`columnDefinition = "TEXT"`) | no | Encrypted GSP client-secret / API key blob (envelope-encrypted; key from env). |
| `encryptedSessionToken` | `String` (`columnDefinition = "TEXT"`) | yes | Encrypted cached GSTN session token (short-lived; managed by `GstnSessionTokenCache`). |
| `sessionExpiresAt` | `Instant` | yes | Session token expiry → `TIMESTAMPTZ`/`TIMESTAMP`. |
| `isActive` | `Boolean` | no | Whether these creds are usable. Default `true`. |

**Relationships.** One active `GstnCredential` per `(owner_id, gstin)`.

**Validation / uniqueness.**
- Unique `(owner_id, gstin)` for the active credential.
- Secrets are stored encrypted only; raw secrets are ACL'd out of every DTO and never logged.

---

## Enums

All persisted `@Enumerated(EnumType.STRING)`. Values are exactly as listed in plan.md.

| Enum | Values | Notes |
|---|---|---|
| `ReturnType` | `GSTR1`, `GSTR3B`, `CMP08`, `GSTR9`, `GSTR9C` | The return being filed. GSTR9/9C are P3. |
| `ReturnStatus` | `NOT_STARTED`, `PREPARED`, `RECONCILED`, `FILED`, `ACKNOWLEDGED` | The `GstReturnPeriod` lifecycle (forward-only; see State Transitions). |
| `FilingStatus` | `INITIATED`, `SUBMITTED`, `EVC_REQUESTED`, `FILED`, `ACKNOWLEDGED`, `FAILED` | The `GstFilingAttempt` lifecycle. `FAILED` is terminal-with-retry. |
| `RegistrationType` | `REGULAR`, `COMPOSITION`, `SEZ`, `CASUAL` | On `GstinRegistration`; drives GSTR-1 vs CMP-08. |
| `FilingFrequency` | `MONTHLY`, `QUARTERLY` | On `GstinRegistration`; months vs quarters (QRMP). |
| `Gstr1Section` | `B2B`, `B2CL`, `B2CS`, `CDNR`, `CDNUR`, `EXP`, `NIL`, `HSN`, `DOCS` | The GSTR-1 section a source document is classified into (keys in `sectionData`). |
| `MismatchType` | `MATCHED`, `MISMATCH_VALUE`, `MISMATCH_GSTIN`, `MISSING_IN_2B`, `MISSING_IN_BOOKS`, `PROBABLE_MATCH` | The reconciliation bucket per line. |

```kotlin
enum class ReturnStatus { NOT_STARTED, PREPARED, RECONCILED, FILED, ACKNOWLEDGED }
enum class FilingStatus { INITIATED, SUBMITTED, EVC_REQUESTED, FILED, ACKNOWLEDGED, FAILED }
enum class Gstr1Section { B2B, B2CL, B2CS, CDNR, CDNUR, EXP, NIL, HSN, DOCS }
enum class MismatchType { MATCHED, MISMATCH_VALUE, MISMATCH_GSTIN, MISSING_IN_2B, MISSING_IN_BOOKS, PROBABLE_MATCH }
```

---

## State transitions

### `GstReturnPeriod.status` (R8)

```
                 prepare                reconcile/2B check            file (P2)            GSTN ack
  NOT_STARTED ───────────▶ PREPARED ──────────────────▶ RECONCILED ───────────▶ FILED ───────────▶ ACKNOWLEDGED
       │                      │  ▲                          │  ▲                   │ (frozen+locked)
       │                      │  └──────────────────────────┘  │                   │
       └──── re-prepare ──────┴── re-prepare ─────────────────┘                   └─ immutable; no further
            (regenerates GstReturnSnapshot)                                          edits — corrections are an
                                                                                     amendment in a LATER period
```

**Rules (the R8 guarantees):**
- **Re-prepare is allowed only while `status ∈ {NOT_STARTED, PREPARED, RECONCILED}`** — it regenerates a
  fresh `GstReturnSnapshot` from current invoices. Attempting to re-prepare a `FILED`/`ACKNOWLEDGED`
  period is **refused** (`PeriodLockedException`).
- Advancing to `PREPARED`/`RECONCILED` is **blocked while any blocking readiness error exists** (R10) —
  the readiness gate.
- At **`FILED`** the period's snapshot is **frozen** (`GstReturnSnapshot.frozen = true`), `lockedAt`,
  `arn` and `filedAt` are set, and the **source period is locked** — no in-place edits ever again.
  Corrections become an amendment reported in a *later* period's GSTR-1, never an edit of the filed
  return.
- `ACKNOWLEDGED` is the terminal happy state (GSTN confirmed the ARN).

### `GstFilingAttempt.status` (R7/R8)

```
  INITIATED ──submit──▶ SUBMITTED ──request EVC──▶ EVC_REQUESTED ──OTP confirmed──▶ FILED ──ack──▶ ACKNOWLEDGED
      │                     │                           │                             │
      └─────────────────────┴───────────────────────────┴──────────── any GSTN/GSP error ───────▶ FAILED
                                                                                                     │
                                                                                  worker retry (backoff,
                                                                                  getReturnStatus pre-check)
                                                                                  ──▶ new attempt / resume
```

**Rules:**
- `clientRequestId` makes the whole chain idempotent — a retry after a lost ack does **not** double-file;
  the worker first calls `getReturnStatus` and, if GSTN already shows the period `FILED`, records the
  existing ARN and short-circuits to success.
- `FAILED` is retried by the `@Scheduled` backoff worker (`attemptCount` drives backoff); a terminal
  `ACKNOWLEDGED`/`FILED` is never retried.
- Reaching `FILED`/`ACKNOWLEDGED` on the attempt drives the owning `GstReturnPeriod` to the matching
  status (and triggers the freeze+lock above).

---

## Period locking & late-invoice routing

This subsection encodes the spec clarification (Session 2026-06-28) and FR-025: **a finalized invoice
dated inside an already-FILED period is NEVER blocked.**

- The invoice finalize path (in the `invoice` module) consults the `gstr` public service
  **`PeriodLockService.isPeriodLocked(gstin, invoiceDate)`** — an *additive* call. The query returns the
  lock state of the return period that `invoiceDate` would fall into (`true` iff a `GstReturnPeriod` for
  that `(gstin, type, period)` is `FILED`/`ACKNOWLEDGED`, i.e. `lockedAt != null`).
- A locked hit **does not reject or block the finalize.** The invoice finalizes normally and **keeps its
  real invoice date** — filed history is preserved.
- Instead, the finalize seam **tags the invoice's GSTR-1 reporting period to the next *open* period** —
  filing-period attribution. The late document is then aggregated into the next open period's GSTR-1
  (reported as a later-period document/amendment, per FR-025/R8), never altering the filed return in
  place.
- Equivalently for cancellation: a cancel touching a filed period flows into the next open period's
  return, not an in-place edit.
- Until any period is `FILED` (i.e. all of Phase 1, which is export-first), `isPeriodLocked` returns
  `false` for every date — the seam ships in Phase 1 but only becomes effective once electronic filing
  (Phase 2) drives periods to `FILED`. This keeps the integration additive and inert until needed.

> **Why a query, not a guard:** the lock is *informational routing input* for the invoice flow, not a
> veto. Modelling it as `isPeriodLocked(...) → Boolean` (rather than a `throw`) is exactly what lets the
> finalize succeed and re-route, satisfying "finalizing such an invoice is never blocked or rejected."

---

## Money & rounding note (R12)

- **Internal computation is `BigDecimal` scale 4** everywhere (`DECIMAL(19,4)`), consistent with the
  `payment`/spec-026 ledger. Invoice legacy `Double` totals are converted to exact `BigDecimal`
  **once** at aggregation time — never accumulated as `Double`.
- **Portal-facing return totals are rupee-rounded `HALF_UP` (scale 0) exactly once, at the
  section-total boundary** — not per invoice. Rounding once at the section total is what makes the
  section sums foot to the rupee-rounded header GSTN expects; rounding per invoice then summing would
  not tie.
- The stored snapshot/headline `DECIMAL(19,4)` columns retain full precision; the rupee rounding is
  applied by the `*PortalBuilder` DTOs at export, not in storage.
- **Reconciliation tolerance is ±₹1** (`BigDecimal` scale 4 compare with a 1-rupee band) so rupee
  rounding between books and supplier-reported 2B does not generate thousands of false
  `MISMATCH_VALUE` rows.

---

## Phase tag summary

| Entity | Phase | Migration |
|---|---|---|
| `GstinRegistration` | **P1** | `V1.0.110` |
| `GstReturnPeriod` | **P1** | `V1.0.110` |
| `GstReturnSnapshot` | **P1** | `V1.0.110` |
| `PurchaseRegisterEntry` | **P1** schema / **P2** matching | `V1.0.110` |
| `ReconciliationResult` | **P1** (invoice⟷GSTR-1) / **P2** (books⟷2B) | `V1.0.111` |
| `Gstn2bRecord` | **P2** | `V1.0.111` |
| `GstFilingAttempt` | **P2** | `V1.0.112` |
| `GstnCredential` | **P2** | `V1.0.112` |
