# Phase 0 Research — GST Return Filing & Reconciliation (GSTR)

All Technical Context unknowns are resolved below. Each item: **Decision · Rationale · Alternatives
considered**. These supersede the inline assumptions in `spec.md` with concrete technical choices.

This feature is the **filing-and-reconciliation tier** that sits downstream of the data the platform
already produces. It does **not** re-plan tax calculation (owned by `tax` + spec 026), IRN/e-invoice
generation (spec 015 `einvoice`), or the party ledger (spec 013 `payment`). It **consumes** finalized
invoices from the `invoice` module (`series`/`sequenceNumber`, `customerGst`/`sellerGst`,
`placeOfSupply`/`sellerPlaceOfSupply`, `taxInfos`, `totalCost`/`totalTax`), the immutable
per-transaction tax audit snapshot proposed in spec 026 R8, and (where present) e-invoice `irn`/`ackNo`
from spec 015. Its job is to **aggregate** those into GSTR-1/3B/CMP-08 return shapes, **reconcile**
books against GSTN-reported data (2A/2B), and **file** — either by exporting a GST-portal-compatible
artifact (MVP) or via a GSP/ASP API (Phase 2). Filing and 2A/2B pull are, by construction, **online-only
backend operations** (a GSTN round-trip), surfaced read-only on mobile.

---

## R1. Module boundary — new `gstr` module vs extend `tax`

- **Decision**: A **new backend bounded context `gstr`** owns the return-period model, the GSTR-1/3B/
  CMP-08 aggregation, the reconciliation engine, the GSTN-portal export builders, and (Phase 2) the
  GSP filing client + ARN tracking. It **reads** finalized invoices via the `invoice` module's public
  `InvoiceService`, reacts to `InvoiceFinalizedEvent` / `InvoiceCancelledEvent` (the same seam
  `payment` and `einvoice` already consume), reads the `tax` audit snapshot, and reads
  `EInvoiceDocument` IRN data through the `einvoice` service. The `tax` module is touched only
  additively (it continues to own the GST *calculation*); `invoice`/`payment` are not modified.
- **Rationale**: Return filing is a distinct compliance concern with its own period lifecycle, external
  GSTN/GSP integration, secrets, retry queue, immutability rules and large persisted return JSON —
  exactly the "new bounded context gets its own module" rule (Principle IX), and the precise sibling of
  the `einvoice` module (spec 015). It is *not* the same context as `tax`: `tax` computes per-line GST
  on a live transaction (a pure, offline, deterministic function); `gstr` aggregates many finalized
  transactions across a period and talks to GSTN. Jamming periodic filing into `tax` would couple a
  regulated external integration to the offline calculator and bloat the tax domain. The split mirrors
  how spec 026 explicitly kept *calculation* in `tax` but pushed *effects* into `payment`/`invoice`.
- **Alternatives considered**: Extend `tax` with filing (rejected — different aggregate grain, different
  lifecycle, external GSTN secrets in the calc module). Extend `einvoice` (rejected — e-invoicing is
  per-document IRN minting; returns are period aggregates over many documents; they share only the
  GSP-provider *style*, not the data model). A shared `compliance` mega-module spanning einvoice + gstr
  (rejected — premature; they integrate cleanly via services/events without merging).

## R2. Return-period & multi-GSTIN model — the filing aggregate

- **Decision**: The filing aggregate root is a **`GstReturnPeriod`** keyed by
  **`(gstin, returnType, financialYear, period)`**, where `gstin` is a **`GstinRegistration`** (a new
  per-state GSTIN under the workspace, NOT the single `TaxConfiguration.gstin`), `returnType ∈
  {GSTR1, GSTR3B, CMP08, GSTR9, GSTR9C}`, and `period` is a tax period (`MMYYYY` for monthly,
  `Q{n}YYYY` for quarterly/QRMP, year for annual). Each `GstReturnPeriod` carries a
  `status` lifecycle and links to a generated, immutable `GstReturnSnapshot` (the computed return JSON).
  A **`GstinRegistration`** entity (`OwnableBaseDomain`) models each GSTIN a business holds —
  state code (first 2 digits of the 15-char GSTIN), legal/trade name, registration type
  (`REGULAR`/`COMPOSITION`/`SEZ`/`CASUAL`), and the QRMP/monthly filing frequency. Source documents are
  bucketed to a registration by **seller GSTIN state**, not by workspace.
- **Rationale**: GST returns are filed **per GSTIN per period**, and a single legal business can hold
  multiple GSTINs (one per state of operation — the "branch" model). The existing
  `TaxConfiguration.gstin` is a *single nullable string* on the workspace tax config — sufficient for a
  one-state seller but structurally unable to represent the multi-state filer the brief requires. A
  first-class `GstinRegistration` is the honest model: it lets a workspace file GSTR-1 for `27…`
  (Maharashtra) and `29…` (Karnataka) independently, with independent period status and ARNs. Keying the
  period by `(gstin, returnType, fy, period)` makes idempotency and period-locking (R8) a simple unique
  constraint. The state code embedded in the GSTIN is also the place-of-supply key the GSTR-1
  B2CS/HSN summaries aggregate on.
- **Alternatives considered**: Reuse `TaxConfiguration.gstin` as the single filing identity (rejected —
  cannot represent multi-state; the brief explicitly calls out the branch model). Key the period by
  workspace only (rejected — a workspace ≠ a GSTIN; multi-GSTIN filers would collide). Model period
  as a free string (rejected — needs structured `(fy, period, frequency)` for QRMP and annual logic).

## R3. GSTR-1 derivation — aggregating finalized invoices into return sections

- **Decision**: GSTR-1 is **auto-prepared by a `Gstr1Aggregator`** that reads all finalized,
  non-cancelled invoices (and credit/debit notes) for `(gstin, period)` and classifies each into the
  statutory sections off the **already-modelled invoice fields**, never re-computing tax:
  - **B2B** (invoice-wise) — buyer `customerGst` present and registered ⇒ one record per invoice with
    rate-wise taxable/CGST/SGST/IGST/CESS from `taxInfos`/`InvoiceItem.taxInfos`.
  - **B2CL** (B2C-Large) — unregistered buyer, **inter-state** (`placeOfSupply` state ≠
    `sellerPlaceOfSupply` state) **and** invoice value > the statutory threshold (₹1,00,000 from the
    current notification; configurable) ⇒ invoice-wise.
  - **B2CS** (B2C-Small) — all other B2C ⇒ **aggregated to (place-of-supply state, rate)** summary rows
    (not invoice-wise).
  - **CDNR/CDNUR** — credit/debit notes (registered/unregistered), invoice-wise.
  - **EXP** — exports / zero-rated (from the spec 026 `gstTreatment = ZERO_RATED` / SEZ marker), with/
    without payment of IGST.
  - **NIL** — nil-rated/exempt/non-GST aggregated (from `gstTreatment`).
  - **HSN summary** — every line rolled up by `(hsn, uqc, rate)` → quantity, taxable, tax.
  - **DOCS** — document series summary from `series`/`sequenceNumber` (issued / cancelled / net), which
    the invoice module already maintains gap-free.
  The aggregator runs on the **immutable tax audit snapshot** (spec 026 R8) when present, falling back to
  the live `taxInfos` for legacy invoices, and produces a `GstReturnSnapshot` (the section-structured
  return JSON) that is the input to both export and (Phase 2) filing.
- **Rationale**: The invoice already carries everything GSTR-1 needs — registered-vs-unregistered is
  `customerGst` presence, intra-vs-inter-state is the `placeOfSupply`/`sellerPlaceOfSupply` compare that
  drives CGST+SGST-vs-IGST, rate-wise tax is in `taxInfos`, document series is `series`/`sequenceNumber`.
  GSTR-1 is therefore a **pure aggregation**, not a parallel data-entry form. Deriving from the
  immutable snapshot guarantees the return reflects rates **as of issue** (a backdated rate change must
  never alter a filed period). The B2CL/B2CS split and the HSN/document summaries are exactly the places
  where individual invoices *collapse* into summary rows — they aggregate in the `Gstr1Aggregator`, not
  on the invoice.
- **Alternatives considered**: A separate GSTR-1 data-entry model (rejected — duplicates the invoice;
  defeats "auto-prepared from finalized invoices"). Aggregate from the `payment` ledger (rejected — the
  ledger nets cash movement, not rate-wise tax breakup; it's the wrong grain). Re-run the tax calculator
  at return time (rejected — non-deterministic vs the issued document; backdated-rate hazard — use the
  snapshot).

## R4. GSTR-3B derivation — summary built on GSTR-1 + ITC/inward data

- **Decision**: GSTR-3B (the monthly/quarterly self-assessed summary) is derived by a
  `Gstr3bAggregator` that composes **outward tax liability** from the same finalized-invoice aggregation
  as GSTR-1 (table 3.1 outward supplies, 3.2 inter-state to unregistered/composition/UIN, 3.1.1
  e-commerce u/s 9(5) when applicable), the **RCM inward liability** from spec 026's `rcmApplicable`
  flag + self-assessed postings, and **eligible ITC** (table 4) from the purchase/ITC source (R5) net of
  ITC reversals (spec 026 R6 `ItcEntry`). Net cash payable = output tax + RCM − eligible ITC, with a
  read-only late-fee/interest **estimate** (R10) when filed after the due date. 3B is **not**
  independently re-aggregated from invoices for the outward side — it reuses the GSTR-1 snapshot totals
  so the two returns reconcile by construction.
- **Rationale**: 3B liability must tie to GSTR-1 (mismatch between them is the single most common G
  notice trigger). Building 3B *on top of* the GSTR-1 snapshot totals — rather than re-deriving from
  invoices — makes them consistent by design and turns "3.1 ⟷ GSTR-1" reconciliation into an identity
  check. The ITC side genuinely needs purchase data, which the platform lacks first-class today (R5), so
  3B's ITC table is **phase-gated**: outward + RCM in P1, ITC in P2 once a purchase register exists.
- **Alternatives considered**: Independently aggregate 3B from invoices (rejected — risks 3B≠GSTR-1
  drift). Treat 3B as pure manual entry (rejected — the brief wants it derived; manual entry is only an
  override fallback). Block 3B until full ITC exists (rejected — outward 3B has standalone value;
  phase-gate the ITC table).

## R5. The purchase-register / 2A-2B gap — honest treatment

- **Decision**: Ampairs has **no first-class purchase/vendor module today** — per spec 013 R6,
  purchases, purchase-returns and supplier notes are recorded as **`AdjustmentVoucher`s** in the
  `payment` ledger, which capture the money movement but **not** the rate-wise tax, supplier GSTIN,
  invoice number or HSN that ITC matching needs. Therefore:
  - **Phase 1**: GSTR-2A/2B reconciliation and the 3B ITC table are **explicitly deferred**. The
    `gstr` module defines a minimal **`PurchaseRegisterEntry`** read-model (supplier GSTIN, supplier
    invoice no/date, taxable, CGST/SGST/IGST/CESS, ITC eligibility) as the documented *future source*,
    populated in Phase 1 only by **manual CSV/Excel import** (a purchase register the accountant already
    maintains in Tally/Excel). No vendor master is built.
  - **Phase 2**: pull supplier-reported inward invoices from GSTN (2A is the live feed, 2B the static
    monthly statement) via the GSP, and run the **ITC reconciliation engine** (R9) matching
    `PurchaseRegisterEntry` ⟷ 2B records into **matched / mismatched / missing-in-2B / missing-in-books**
    buckets, feeding eligible-ITC into 3B.
  - **Phase 3+ / cross-spec**: a first-class purchase/vendor billing module (a clean future addition
    that can reuse this register shape and the spec 013 ledger) replaces the CSV import as the source.
- **Rationale**: Being honest about the gap is the only defensible plan — claiming 2A/2B reconciliation
  "for free" would be false because the books side (the purchase register) does not exist as structured,
  taxed data. A thin import-fed `PurchaseRegisterEntry` lets the matching engine and 2B pull be built and
  demonstrated in Phase 2 without first building a vendor module, and gives a forward-compatible shape
  the eventual purchase module slots into. This mirrors spec 013's own honesty about purchases-as-
  adjustments and spec 026's phase-gating of ITC.
- **Alternatives considered**: Build a full vendor/purchase module now (rejected — large scope creep far
  beyond return filing; spec 013/026 already chose to defer it). Derive ITC from `AdjustmentVoucher`s
  (rejected — they lack supplier GSTIN/invoice-no/rate; un-matchable against 2B). Skip 2A/2B entirely
  (rejected — explicitly in the brief; deferred-but-designed is the right posture).

## R6. GSP/ASP filing abstraction vs export-first MVP

- **Decision**: **Export-first MVP, GSP API second** — the same provider-abstraction posture spec 015
  took for the IRP. Phase 1 ships **no live GSTN API filing**: the `gstr` module generates a
  **GSTN-portal-compatible artifact** — the offline-tool **JSON** (the schema the GST Offline Utility /
  portal accepts for GSTR-1 and 3B) and an **Excel** export of the same — which the user uploads to the
  GST portal manually. Phase 2 introduces a **`GstnFilingProvider`** port (`authenticate`,
  `saveGstr1`, `submitGstr1`, `fileGstr1`, `getReturnStatus`, `get2A`, `get2B`, `getFiledReturn`) with
  pluggable per-GSP implementations (e.g. `ClearTaxGspProvider`, `MasterIndiaGspProvider`,
  `GstnSandboxProvider`), selected per-workspace by a `GstnProviderResolver`; credentials (GSP
  client id/secret, the GSTN API username, the GSTIN-scoped session) come from environment + an
  encrypted per-GSTIN credential row, never from source. GSTN/GSP is the source of truth for
  filing status and the ARN; the provider is transport.
- **Rationale**: Export-first lands real compliance value (a correctly-prepared GSTR-1/3B the
  accountant files in minutes) **without** the GSP onboarding, sandbox certification and credential
  custody that API filing requires — the highest-value, lowest-risk first slice. The provider port then
  mirrors spec 015's `EInvoiceProvider` exactly (many GSPs over one GSTN), so the abstraction,
  per-workspace resolver and encrypted-credential pattern are reused rather than reinvented, and a GSP
  can be swapped on price/uptime without touching call sites.
- **Alternatives considered**: GSP API filing from day one (rejected — long GSP certification lead
  time blocks all value; export-first ships now). Hardcode one GSP (rejected — vendor lock-in, no
  fallback). Portal screen-scraping/RPA (rejected — brittle, against portal terms). PDF-only export
  (rejected — the portal ingests JSON/Excel, not PDF; PDF is a human artifact only).

## R7. Filing authentication (EVC/OTP) & ARN tracking

- **Decision**: Phase-2 filing follows the GSTN auth model: a **two-step submit-then-file** with
  **EVC (Electronic Verification Code)** or **DSC** authentication. The `gstr` module models a
  `GstFilingAttempt` (status `INITIATED → SUBMITTED → EVC_REQUESTED → FILED → ACKNOWLEDGED` /
  `FAILED`), drives the EVC OTP request to GSTN (which sends the OTP to the authorized signatory's
  registered mobile/email — Ampairs never sees credentials), accepts the OTP back from the user via an
  **online command** endpoint, and on success persists the returned **ARN (Acknowledgement Reference
  Number)** and filed-date on the `GstReturnPeriod`. DSC signing is out of scope (server-side DSC custody
  is a non-starter; EVC covers the SMB case). Filing and the 2A/2B pull run through a **backend queue +
  exponential-backoff retry worker** (a `@Scheduled` poller over `INITIATED`/`FAILED`), identical in
  shape to spec 015's `EInvoiceQueueWorker`.
- **Rationale**: GSTN filing is intrinsically an online, multi-step, externally-authenticated
  operation — the OTP goes to the signatory, not the app, so the only correct model is "backend
  orchestrates, user supplies the EVC, ARN comes back." Queue + retry is mandatory because GSTN has
  maintenance windows and rate limits. EVC over DSC keeps credential custody off our servers (rule
  10-security). ARN is the legal proof of filing and the natural terminal state of the period lifecycle
  (R8).
- **Alternatives considered**: Store the signatory's GST portal password and auto-OTP (rejected —
  credential custody risk; against G terms). DSC on the server (rejected — hardware-token/PFX custody
  is unsafe and impractical for SMBs). Synchronous in-request filing (rejected — blocks on a slow/maint-
  windowed external system; must be queued).

## R8. Idempotency, period-locking & immutability of a filed return

- **Decision**: Three guards. **(1) Idempotent period** — a unique constraint on
  `GstReturnPeriod(owner_id, gstin, return_type, financial_year, period)`; re-preparing a period
  regenerates its `GstReturnSnapshot` only while `status ∈ {NOT_STARTED, PREPARED, RECONCILED}`.
  **(2) Period locking** — once a period reaches `FILED`/`ACKNOWLEDGED`, it is **immutable**: the snapshot
  is frozen, and the *source data is locked* — finalizing/cancelling an invoice whose `invoiceDate`
  falls in a filed period is **blocked or routed to the next open period** (the `gstr` module exposes a
  `isPeriodLocked(gstin, date)` query the `invoice` finalize path consults via the public service).
  **(3) Filing idempotency** — a `GstFilingAttempt` carries a stable client request-id so a retried file
  call after a lost ack does not double-file; before re-filing, the worker calls
  `getReturnStatus` and treats an already-FILED period as success (storing the existing ARN).
- **Rationale**: A filed G return is a legal submission — it must never silently change because someone
  edited a back-dated invoice, and it must never be double-filed on a network retry (the classic GSP
  integration bug, mirrored from spec 015 R5's duplicate-IRN handling). Period-locking is the mechanism
  that makes the immutable-snapshot guarantee real against the offline-first invoice flow: an offline
  device that finalizes a late invoice into an already-filed month must be funneled to the open period,
  not allowed to mutate filed history.
- **Alternatives considered**: Recompute the return on demand from current invoices (rejected — a filed
  return would drift as invoices change; non-auditable). Trust local state for double-file protection
  (rejected — a lost ack leaves us unsure; must reconcile via `getReturnStatus`). Allow editing a filed
  period and re-file (rejected — that's an *amendment* in the **next** period's GSTR-1, a distinct flow,
  not an in-place edit).

## R9. Reconciliation engine & mismatch taxonomy

- **Decision**: A single **`ReconciliationEngine`** runs two reconciliation jobs producing typed
  mismatch reports, never auto-mutating source data (it flags, the user resolves):
  - **Invoice ⟷ GSTR-1** (intra-platform self-check): every finalized invoice must land in exactly one
    GSTR-1 section; flags invoices **missing a mandatory field for filing** (R11), document-series gaps,
    rate-wise totals that don't foot, and CDN references to a non-existent original.
  - **Books ⟷ 2A/2B** (ITC, Phase 2): match `PurchaseRegisterEntry` ⟷ GSTN 2B on
    `(supplier_gstin, invoice_no, invoice_date, taxable, tax)` with tolerance, classifying into
    **`MATCHED`**, **`MISMATCH_VALUE`** (matched key, differing amount within/over tolerance),
    **`MISMATCH_GSTIN`**, **`MISSING_IN_2B`** (in books, supplier hasn't filed — ITC at risk under
    Rule 36(4)/2B-only credit), **`MISSING_IN_BOOKS`** (in 2B, not in books — possible un-booked
    purchase), and **`PROBABLE_MATCH`** (fuzzy invoice-no). Each bucket aggregates eligible vs at-risk
    ITC for the 3B table.
  Money compares use `BigDecimal` scale 4 with a ±₹1 (and ±%) tolerance; the rupee-rounded portal totals
  use R12.
- **Rationale**: Reconciliation is the feature's analytical core, and a precise **mismatch taxonomy** is
  what makes it actionable — "missing in 2B" (chase the supplier) is a fundamentally different action
  than "value mismatch" (correct a typo) or "missing in books" (book the purchase). Keeping the engine
  **flag-only** (no auto-mutation) preserves the immutability and audit guarantees and matches the spec
  013 posture of never silently rewriting posted data. Building the invoice⟷GSTR-1 self-check in
  Phase 1 (no external dependency) delivers data-quality value immediately and de-risks the harder 2A/2B
  matching.
- **Alternatives considered**: Auto-accept GSTN 2B as truth and overwrite books (rejected — destroys
  the user's records and audit). A single matched/unmatched boolean (rejected — too coarse to drive the
  right corrective action). Fuzzy-only matching (rejected — exact-key first, fuzzy as a labelled
  `PROBABLE_MATCH` fallback keeps precision auditable).

## R10. Data-quality preconditions — filing gate

- **Decision**: A return cannot advance to `PREPARED → RECONCILED` (and certainly not `FILED`) while any
  invoice in the period fails a **filing-readiness validation**: missing/invalid `customerGst` on a B2B
  invoice, missing **HSN/SAC** on any line (mandatory by turnover slab), missing/invalid `placeOfSupply`
  or `sellerPlaceOfSupply`, missing `sellerGst`, a tax breakup that doesn't reconcile to the line totals,
  or a document-series gap. These surface as a **blocking `ReturnReadinessReport`** (errors vs warnings),
  computed by the same `ReconciliationEngine`. Late-fee/interest figures are **informational only**
  (an estimate based on the due date and `Instant.now()`), never auto-paid or treated as a liability the
  module manages.
- **Rationale**: GSTN rejects returns with malformed GSTINs, missing HSN or bad place-of-supply with
  cryptic errors — catching them *before* export/filing is the single biggest UX win and the reason
  reconciliation exists. Making readiness a hard gate to `PREPARED` enforces "garbage in can't be filed."
  Keeping late-fee/interest informational avoids the module overreaching into a tax-advice/liability role
  it shouldn't own.
- **Alternatives considered**: Let GSTN reject and surface its error (rejected — poor UX, wastes a GSP
  call, cryptic codes). Auto-fix data (rejected — silently editing a finalized tax invoice is wrong; the
  user must correct the source). Treat late fee as a managed liability (rejected — out of scope; the
  `payment` ledger owns money; this module only estimates).

## R11. Offline boundary — prepare locally(ish), file online; mobile read-only

- **Decision**: This is **primarily a backend + web concern**. Return preparation/aggregation,
  reconciliation and export generation are **server-computed** (they need the whole period's invoices and
  the immutable snapshots, and run heavy aggregation/joins better suited to the backend). **Filing and
  2A/2B pull are online-only GSTN/GSP operations** (backend queue + retry). The **mobile app is a
  read-only status/summary surface**: a small `feature/gstr` (or a panel in an existing module) **pulls**
  `GstReturnPeriod` status (`NOT_STARTED…ACKNOWLEDGED` + ARN) and headline GSTR-1/3B totals via a
  **pull-only `SyncDelegate`** on the canonical `/sync` contract, and can trigger a *prepare* or *file*
  as an **online command** — it never authors return data or files offline. Period status is the only
  thing a field user needs on a phone ("June GSTR-1: FILED, ARN AA27…").
- **Rationale**: A return is an aggregate over a whole period that the backend owns; computing it on a
  single device that may hold only a subset of invoices would be wrong. Filing inherently needs
  connectivity and the signatory's EVC. So the correct division is exactly spec 015's: server authors the
  compliance artifact, the app surfaces and triggers it. Pull-only mobile fits the existing sync engine
  with zero client-authored push.
- **Alternatives considered**: Compute the return on-device (rejected — a device may not hold every
  invoice; aggregation belongs server-side; would drift from the authoritative books). Full mobile
  return-editing (rejected — preparation is automated from invoices; editing a return on a phone is the
  wrong surface — web is). No mobile surface at all (rejected — owners want filing status on their phone).

## R12. Money & rounding — returns round to the rupee

- **Decision**: Internally the module computes on **`BigDecimal` scale 4** (consistent with `payment`/
  spec 026 ledger money) read from invoice `taxInfos`/snapshots; the **portal-facing return totals are
  rounded to the nearest rupee** (`HALF_UP`, scale 0) per GSTN return rules, with rounding applied
  **once at the section-total boundary** (not per invoice) so section sums foot to the rounded header.
  Reconciliation tolerance is ±₹1 to absorb rounding between books and 2B. The invoice's legacy `Double`
  totals are converted to exact `BigDecimal` **once** at aggregation time.
- **Rationale**: GST returns are filed in whole rupees; rounding per-invoice then summing produces
  totals that don't tie to the rounded portal expectation, while rounding once at the section total does.
  `BigDecimal` throughout avoids the `Double` accumulation drift that would make a return fail GSTN
  arithmetic validation — the same single-conversion discipline spec 013 R5 and spec 015 R9 adopted. The
  ±₹1 reconciliation tolerance is the standard accommodation for rupee-rounding differences between books
  and supplier-reported 2B.
- **Alternatives considered**: Round per invoice (rejected — section totals won't foot to the rupee-
  rounded header). Compute in `Double` (rejected — precision/validation failures on a many-line period).
  No tolerance in reconciliation (rejected — rupee rounding alone would flag thousands of false
  mismatches).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Module placement | New `gstr` bounded context; reads invoice/tax/einvoice via services + events (R1) |
| Return-period & multi-GSTIN | `GstReturnPeriod` keyed `(gstin, type, fy, period)` + first-class `GstinRegistration` branch model (R2) |
| GSTR-1 derivation | `Gstr1Aggregator` over finalized invoices/snapshots; B2B invoice-wise, B2CL/B2CS/HSN/DOCS summaries (R3) |
| GSTR-3B derivation | `Gstr3bAggregator` reuses GSTR-1 totals + RCM + ITC (ITC phase-gated) so 3B⟷GSTR-1 tie (R4) |
| Purchase-register / 2A-2B gap | No vendor module today; `PurchaseRegisterEntry` via CSV import (P1), GSTN 2B pull + matching (P2), real module later (R5) |
| GSP/ASP vs export | Export-first JSON/Excel MVP (P1); `GstnFilingProvider` port + per-workspace resolver (P2), mirrors spec 015 (R6) |
| Filing auth & ARN | EVC/OTP two-step submit→file; `GstFilingAttempt` lifecycle; ARN persisted; queue + retry (R7) |
| Idempotency / locking / immutability | Unique period key; frozen snapshot + source-period lock at FILED; `getReturnStatus` pre-check (R8) |
| Reconciliation & mismatch taxonomy | `ReconciliationEngine`: invoice⟷GSTR-1 (P1) + books⟷2B buckets MATCHED/MISMATCH/MISSING (P2), flag-only (R9) |
| Data-quality preconditions | Blocking `ReturnReadinessReport` gates PREPARED; missing GSTIN/HSN/PoS block filing; late-fee informational (R10) |
| Offline boundary | Backend/web-centric; filing & 2A/2B online-only; mobile pull-only status/summary + online commands (R11) |
| Money / rounding | `BigDecimal` scale 4 internal; portal totals rupee-rounded HALF_UP at section boundary; ±₹1 tolerance (R12) |
