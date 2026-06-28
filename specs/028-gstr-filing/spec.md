# Feature Specification: GST Return Filing & Reconciliation (GSTR)

**Feature Branch**: `028-gstr-filing`
**Created**: 2026-06-28
**Status**: Draft
**Input**: User description: "specs/028-gstr-filing"

## Overview

Indian businesses on the platform already capture everything a GST return needs — finalized tax
invoices, credit/debit notes, place-of-supply, customer GSTINs, rate-wise tax and document series —
yet today they still re-key all of it into the GST portal (or pay an accountant to) every month. This
feature closes that gap: it **auto-prepares the statutory GST returns (GSTR-1 outward supplies and
GSTR-3B summary) directly from finalized invoices**, **checks the data for the errors the portal would
reject**, lets the business **export a portal-ready file or file electronically**, and (later)
**reconciles purchases against what suppliers reported to GST (2A/2B)** so input-tax credit is not
lost. A single business may hold several GSTINs (one per state); each is tracked and filed
independently.

## Clarifications

### Session 2026-06-28

- Q: A finalized invoice whose invoice date falls inside an already-FILED period — block it or route it? → A: Allow the finalize (the invoice keeps its real date) and include it in the **next open** return period's GSTR-1 as a later-period document/amendment; the filed return is never altered.
- Q: Who is authorized to electronically FILE a GST return (the legal EVC/ARN submission)? → A: Workspace **owner/admin role only** (via existing RBAC); preparation/export may be broader, but the file action and credential setup are admin-restricted.
- Q: For QRMP (quarterly) GSTIN filers, is the optional monthly invoice furnishing (IFF) in scope? → A: No — quarterly filers prepare/file **one GSTR-1 per quarter**; monthly IFF is deferred to a later phase.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Auto-prepare a month's GSTR-1 and get a portal-ready file (Priority: P1)

A business owner (or their accountant) opens the GST filing screen at month-end, picks the GSTIN and
the tax period (e.g. June 2026), and asks the system to prepare GSTR-1. The system reads every
finalized, non-cancelled invoice and credit/debit note in that period and organizes them into the
statutory GSTR-1 sections — invoice-wise business-to-business (B2B) supplies, large inter-state
consumer sales (B2CL), summarized small consumer sales (B2CS), credit/debit notes, exports/nil-rated,
an HSN-code summary and a document-series summary. The owner reviews the headline totals and downloads
a file they can upload to the GST portal (or hand to their accountant), instead of re-keying invoices.

**Why this priority**: This is the core compliance value and the minimum viable product. Even on its
own — with no electronic filing and no purchase reconciliation — it eliminates the single most painful,
error-prone monthly task for a GST-registered business, using data the platform already holds.

**Independent Test**: Finalize a representative mix of invoices (registered/unregistered buyers,
intra- and inter-state, multiple tax rates, a credit note) in a period, run "prepare GSTR-1", and
confirm every invoice lands in exactly one correct section, the rate-wise and HSN totals foot to the
source invoices, and a portal-ready export downloads.

**Acceptance Scenarios**:

1. **Given** a period with finalized B2B and B2C invoices across multiple rates, **When** the user
   prepares GSTR-1, **Then** each invoice appears in exactly one section with correct rate-wise
   taxable value and tax, and the section totals equal the sum of the underlying finalized invoices.
2. **Given** an unregistered inter-state invoice above the large-value threshold, **When** GSTR-1 is
   prepared, **Then** it is classified as B2CL (invoice-wise); an unregistered invoice below the
   threshold is instead aggregated into the B2CS summary by state and rate.
3. **Given** a prepared GSTR-1, **When** the user exports it, **Then** a GST-portal-compatible file is
   produced whose section structure and field names match what the portal accepts.
4. **Given** a credit note issued in the period, **When** GSTR-1 is prepared, **Then** it appears in
   the credit/debit-note section and reduces the relevant totals.
5. **Given** the tax rate on a product changed after an invoice was issued, **When** GSTR-1 is
   prepared, **Then** the return reflects the rate as it was at the time of issue, not the current
   rate.

---

### User Story 2 - Auto-prepare GSTR-3B that ties to GSTR-1 (Priority: P2)

After preparing GSTR-1, the user prepares the GSTR-3B summary for the same GSTIN and period. The system
builds the outward-supply liability of 3B **from the GSTR-1 totals it already computed** (plus
reverse-charge liability), so the two returns reconcile by construction and the user is not exposed to
the most common cause of GST notices — a mismatch between GSTR-1 and GSTR-3B. Input-tax-credit figures
appear once purchase data is available (a later phase); until then the outward and reverse-charge
sections are complete and the credit section is clearly marked as pending.

**Why this priority**: GSTR-3B is filed alongside GSTR-1 every period, and a 3B↔GSTR-1 mismatch is the
top trigger for tax notices. Deriving 3B from the GSTR-1 totals makes the two consistent automatically.

**Independent Test**: Prepare GSTR-1 for a period, then prepare GSTR-3B and confirm the outward-supply
liability in 3B equals the GSTR-1 outward totals to the rupee, and reverse-charge entries are included.

**Acceptance Scenarios**:

1. **Given** a prepared GSTR-1, **When** GSTR-3B is prepared, **Then** the 3B outward-supply tax equals
   the GSTR-1 outward tax totals (rounded to the rupee).
2. **Given** invoices marked reverse-charge, **When** 3B is prepared, **Then** the reverse-charge
   liability is reflected in the 3B summary.
3. **Given** purchase/credit data is not yet available, **When** 3B is prepared, **Then** the
   input-tax-credit section is shown as pending rather than silently zero.

---

### User Story 3 - Catch filing-blocking data errors before filing (Priority: P2)

When the user prepares a return, the system runs a filing-readiness check and shows a clear report of
problems that would cause the GST portal to reject the return — a missing or malformed customer GSTIN
on a B2B invoice, a missing HSN/SAC code on a line, a missing or invalid place of supply, a tax breakup
that does not add up to the line total, or a gap in the document series. Errors block the return from
being marked ready (and from being filed); warnings are surfaced but do not block. The user fixes the
underlying invoices and re-prepares. The system never silently edits a finalized invoice.

**Why this priority**: Catching these errors up front is the biggest usability win — the portal rejects
malformed returns with cryptic errors, and fixing them after a failed upload is far costlier than
before. It also protects the integrity of finalized tax documents by never auto-editing them.

**Independent Test**: Introduce a B2B invoice with a missing customer GSTIN and a line with no HSN code,
prepare the return, and confirm the readiness report lists both as blocking errors and prevents the
return from advancing to "ready".

**Acceptance Scenarios**:

1. **Given** a B2B invoice missing a valid customer GSTIN, **When** the user prepares the return,
   **Then** the readiness report flags it as a blocking error and the return cannot be marked ready.
2. **Given** an invoice line with no HSN/SAC code, **When** the readiness check runs, **Then** it is
   reported as a blocking error.
3. **Given** all blocking errors are resolved, **When** the user re-prepares, **Then** the readiness
   report is clear and the return advances to "ready".
4. **Given** a return filed after its due date, **When** prepared, **Then** an estimated late
   fee/interest figure is shown for information only and is never charged or treated as a managed
   liability.

---

### User Story 4 - File for each GSTIN of a multi-state business independently (Priority: P3)

A business operating in several states registers each of its GSTINs (with its state, legal/trade name,
registration type and monthly/quarterly filing frequency). Returns are then prepared, tracked and filed
**per GSTIN per period** — the Maharashtra GSTIN and the Karnataka GSTIN each have their own return
status, totals and acknowledgement, and a sale is attributed to the GSTIN of the state from which it was
billed.

**Why this priority**: Multi-state ("branch") businesses are a core target. Without per-GSTIN tracking,
their returns would collide; with it, each registration files on its own schedule.

**Independent Test**: Register two GSTINs in different states, finalize invoices billed from each, and
confirm each GSTIN's GSTR-1 contains only its own state's invoices and carries independent status.

**Acceptance Scenarios**:

1. **Given** two registered GSTINs in different states, **When** returns are prepared, **Then** each
   GSTIN's return contains only the invoices billed from that GSTIN's state.
2. **Given** a quarterly-filing-frequency GSTIN, **When** periods are listed, **Then** they are
   presented as quarters; a monthly GSTIN is presented as months.
3. **Given** two GSTINs, **When** one is filed, **Then** the other's status is unaffected.

---

### User Story 5 - File electronically and track the acknowledgement (Priority: P3)

Instead of uploading a file manually, the user files the prepared, reconciled return through the GST
network. The system submits the return, requests an electronic verification code (OTP), which the GST
network sends to the authorized signatory's own registered phone/email; the user enters that code to
confirm filing. On success the system records the official acknowledgement reference number (ARN) and
filed date against the period, and the period becomes locked and immutable. Filing runs as a queued
operation that retries safely and never double-files on a network retry.

**Why this priority**: Direct electronic filing removes the last manual step, but it depends on external
network onboarding and credential handling, so it follows the export-first slices.

**Independent Test**: With electronic filing enabled (against a sandbox), file a prepared return, supply
the verification code, and confirm an acknowledgement reference number is recorded and the period is
locked.

**Acceptance Scenarios**:

1. **Given** a reconciled return, **When** the user initiates electronic filing, **Then** a
   verification code is requested and sent to the authorized signatory (never shown to the app), and
   filing completes only after the user supplies the correct code.
2. **Given** a successful filing, **When** it completes, **Then** the acknowledgement reference number
   and filed date are recorded and the period status becomes filed/acknowledged.
3. **Given** a filing acknowledgement was lost on the network, **When** filing is retried, **Then** the
   system checks the network's status first and does not file the same period twice.
4. **Given** a filed period, **When** someone finalizes a back-dated invoice dated in that period,
   **Then** the invoice finalizes (keeping its real date), is included in the next open period's GSTR-1,
   and the filed return is never altered in place.
5. **Given** a member without the owner/admin role, **When** they attempt to file a return or configure
   electronic-filing credentials, **Then** the action is denied (though they may still prepare, review
   and export the return).

---

### User Story 6 - Reconcile purchases against supplier-reported GST (2A/2B) for input-tax credit (Priority: P3)

The user imports their purchase register (or, later, pulls supplier-reported inward invoices from the
GST network) and the system matches each purchase against what suppliers reported, classifying every
line as matched, value-mismatch, GSTIN-mismatch, missing-in-supplier-data (credit at risk — chase the
supplier), missing-in-books (a purchase not yet recorded) or a probable fuzzy match. The result tells
the user exactly how much input-tax credit is safe to claim and which suppliers to follow up with, and
feeds the credit section of GSTR-3B.

**Why this priority**: Input-tax-credit reconciliation protects real money, but it depends on purchase
data the platform does not yet hold as structured, taxed records, so it is explicitly a later phase.

**Independent Test**: Import a purchase register and a sample supplier-reported statement with one exact
match, one value mismatch and one entry missing from the statement, and confirm each is classified into
the correct bucket with the at-risk credit summarized.

**Acceptance Scenarios**:

1. **Given** a purchase that exactly matches a supplier-reported entry, **When** reconciliation runs,
   **Then** it is classified as matched and its credit counted as eligible.
2. **Given** a purchase whose amount differs from the supplier-reported entry beyond tolerance, **When**
   reconciliation runs, **Then** it is classified as a value mismatch.
3. **Given** a purchase with no corresponding supplier-reported entry, **When** reconciliation runs,
   **Then** it is classified as missing-in-supplier-data and its credit flagged at risk.
4. **Given** a supplier-reported entry with no corresponding purchase, **When** reconciliation runs,
   **Then** it is classified as missing-in-books.

---

### User Story 7 - See filing status on a phone (Priority: P3)

A business owner away from their desk opens the mobile app and sees, per GSTIN and period, the return
status (not started → prepared → reconciled → filed → acknowledged), the headline GSTR-1/3B totals and
the acknowledgement reference number once filed. They can trigger a "prepare" or "file" as an online
command, but the phone never computes or edits a return.

**Why this priority**: Owners want filing status at a glance, but the rich preparation and
reconciliation work belongs on a larger screen, so mobile is a read-only/status surface.

**Independent Test**: After a return is prepared and filed on the server, open the mobile app and
confirm the period shows the correct status, totals and acknowledgement reference number.

**Acceptance Scenarios**:

1. **Given** a prepared return on the server, **When** the user opens the mobile app, **Then** the
   period status and headline totals are shown read-only.
2. **Given** a filed return, **When** viewed on mobile, **Then** the acknowledgement reference number
   is displayed.
3. **Given** the user is on mobile, **When** they trigger "prepare", **Then** it runs as an online
   command on the server and the device does not compute the return locally.

---

### Edge Cases

- **Empty / nil period**: a GSTIN with no sales in a period must still be preparable as a NIL return.
- **Late invoice into a filed period**: finalizing a back-dated invoice whose date falls in an
  already-filed period must still succeed (the invoice keeps its real date) and be reported in the next
  open period's GSTR-1 — never altering filed history.
- **Re-preparing after edits**: re-preparing a not-yet-filed period must regenerate its totals from the
  current invoices; re-preparing a filed period must be refused.
- **Rounding**: section totals must foot to the rupee-rounded header (rounding applied once at the
  section total, not per invoice), and books↔supplier reconciliation must tolerate ±₹1 rounding
  differences so rupee rounding alone does not create thousands of false mismatches.
- **Mixed data sources**: invoices that predate the immutable tax snapshot must still aggregate
  correctly by falling back to their stored tax breakup.
- **External outage / maintenance window**: electronic filing and supplier-data pulls must queue and
  retry rather than fail outright when the GST network is unavailable.
- **Document-series gap**: a missing sequence number in an invoice series must be reported (it indicates
  a skipped or deleted document) and reflected in the document-series summary.
- **Corrections to a filed return**: a correction to an already-filed period is an amendment reported in
  a *later* period's return, never an in-place edit of the filed return.

## Requirements *(mandatory)*

### Functional Requirements

**Return preparation (GSTR-1 / GSTR-3B)**

- **FR-001**: System MUST auto-prepare a period's GSTR-1 by aggregating all finalized, non-cancelled
  invoices and credit/debit notes for a given GSTIN and tax period, without re-computing tax.
- **FR-002**: System MUST classify each source document into exactly one GSTR-1 section: invoice-wise
  B2B; B2CL (unregistered, inter-state, above the large-value threshold); B2CS (other B2C, summarized by
  state and rate); credit/debit notes (registered and unregistered); exports / zero-rated; nil-rated /
  exempt / non-GST; plus an HSN summary and a document-series summary.
- **FR-003**: System MUST determine registered-vs-unregistered from the presence of a customer GSTIN and
  intra-vs-inter-state from comparing buyer place-of-supply against seller place-of-supply.
- **FR-004**: System MUST treat the large-value threshold that splits B2CL from B2CS as configurable.
- **FR-005**: System MUST base each prepared return on the tax breakup as of invoice issue, so a later
  tax-rate change never alters an already-prepared or filed period.
- **FR-006**: System MUST auto-prepare GSTR-3B deriving its outward-supply liability from the GSTR-1
  totals (plus reverse-charge liability) so that GSTR-3B and GSTR-1 reconcile to the rupee.
- **FR-007**: System MUST present the GSTR-3B input-tax-credit section as pending until purchase data is
  available, rather than reporting it as zero.
- **FR-008**: System MUST support a NIL return for a period with no qualifying activity.

**Readiness & reconciliation**

- **FR-009**: System MUST produce a filing-readiness report identifying invoices that would cause the
  return to be rejected — missing/invalid customer GSTIN on B2B, missing HSN/SAC, missing/invalid place
  of supply, missing seller GSTIN, a tax breakup that does not reconcile to line totals, and
  document-series gaps — separating blocking errors from non-blocking warnings.
- **FR-010**: System MUST prevent a return from advancing to "ready" (and from being filed) while any
  blocking readiness error exists.
- **FR-011**: System MUST NOT auto-edit any finalized invoice to resolve a readiness problem; the user
  corrects the source document and re-prepares.
- **FR-012**: System MUST run an invoice↔GSTR-1 self-check confirming every finalized invoice lands in
  exactly one section and that rate-wise totals foot.
- **FR-013**: System MUST (in a later phase) reconcile the purchase register against supplier-reported
  GST data, classifying each line as matched, value-mismatch, GSTIN-mismatch, missing-in-supplier-data,
  missing-in-books or probable-match, and summarize eligible-vs-at-risk input-tax credit.
- **FR-014**: System MUST treat reconciliation as flag-only — it never auto-mutates the user's source
  records.
- **FR-015**: System MAY accept a purchase register via file import as the books side of reconciliation
  until a first-class purchase source exists.
- **FR-016**: System MUST present any late-fee/interest figure as an informational estimate only, never
  charging it or managing it as a liability.

**Multi-GSTIN & periods**

- **FR-017**: System MUST let a workspace register multiple GSTINs, each with its state, legal/trade
  name, registration type and filing frequency (monthly or quarterly).
- **FR-018**: System MUST track and file returns per GSTIN per period, with independent status, totals
  and acknowledgement for each GSTIN.
- **FR-019**: System MUST attribute each source document to a GSTIN by the state it was billed from.
- **FR-020**: System MUST represent periods according to the GSTIN's filing frequency (months for
  monthly, quarters for quarterly), and support composition-dealer summary returns and (later) annual
  returns. For quarterly (QRMP) filers the system prepares/files **one GSTR-1 per quarter**; the optional
  monthly invoice furnishing (IFF) within a quarter is out of scope for now.

**Filing, idempotency & immutability**

- **FR-021**: System MUST let the user obtain a GST-portal-compatible export (a structured data file and
  a spreadsheet form) for a prepared GSTR-1 and GSTR-3B.
- **FR-022**: System MUST (in a later phase) support electronic filing through the GST network using
  verification-code (OTP) authentication, where the code is delivered to the authorized signatory and
  never exposed to the application.
- **FR-023**: System MUST record the official acknowledgement reference number and filed date on a
  successfully filed period.
- **FR-024**: System MUST guarantee one return per (GSTIN, return type, financial year, period) and make
  re-preparation regenerate totals only while the period is not yet filed.
- **FR-025**: System MUST freeze a filed period's return (immutable snapshot) and lock its source period
  so a back-dated invoice cannot alter filed history. A finalized invoice whose invoice date falls in an
  already-filed period MUST still finalize (it keeps its real invoice date) and MUST be included in the
  **next open** return period's GSTR-1 (reported as a later-period document/amendment); the filed return
  is never altered in place. Finalizing such an invoice is never blocked or rejected.
- **FR-026**: System MUST be idempotent on filing retries — after a lost acknowledgement it MUST verify
  current status with the GST network before re-filing and never double-file a period.
- **FR-027**: System MUST keep all GST-network credentials and signatory secrets server-side; they MUST
  never be stored in source control or sent to the mobile app.
- **FR-031**: System MUST restrict the electronic-filing action and electronic-filing credential setup
  to the workspace **owner/admin** role (via existing role-based access control). Preparing, reviewing
  and exporting returns MAY be available to other members; the legal file action is admin-only.

**Mobile & access**

- **FR-028**: System MUST expose return status, headline GSTR-1/3B totals and the acknowledgement
  reference number to the mobile app as a read-only/status surface, with no on-device return
  computation.
- **FR-029**: System MUST allow the mobile app to trigger "prepare" and "file" only as online commands
  executed on the server.
- **FR-030**: System MUST isolate all return data per workspace and per GSTIN so one tenant or
  registration can never see another's returns.

### Key Entities *(include if feature involves data)*

- **GSTIN Registration**: a single GST registration the business holds — its 15-character number, the
  state it belongs to, legal/trade name, registration type (regular / composition / SEZ / casual) and
  filing frequency. A business may hold several.
- **Return Period**: the filing unit for one GSTIN, return type, financial year and tax period; carries
  the status lifecycle (not started → prepared → reconciled → filed → acknowledged) and, once filed, the
  acknowledgement reference number and filed date.
- **Return Snapshot**: the computed, section-structured contents of a prepared return (GSTR-1 sections
  or the GSTR-3B summary); immutable once the period is filed.
- **Filing Readiness Report**: the list of blocking errors and warnings found for a period, with the
  specific invoices and fields at fault.
- **Reconciliation Result**: the classification of each invoice (against GSTR-1) or purchase line
  (against supplier-reported data) into a mismatch bucket, with the eligible/at-risk credit summary.
- **Purchase Register Entry**: a purchase line (supplier GSTIN, supplier invoice number/date, taxable
  value, tax breakup, credit eligibility) used as the books side of input-tax-credit reconciliation.
- **Supplier-Reported Record (2A/2B)**: an inward-supply line as reported to the GST network by the
  supplier, used as the counter-party side of reconciliation.
- **Filing Attempt**: one electronic-filing transaction with its own status (initiated → submitted →
  verification-requested → filed → acknowledged / failed), used to drive the queue, retries and
  idempotency.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can prepare a full month's GSTR-1 for a GSTIN with thousands of invoices and review
  the headline totals in under one minute of interaction, with no manual invoice entry.
- **SC-002**: For any prepared period, 100% of finalized non-cancelled invoices in the period appear in
  exactly one GSTR-1 section, and every section total foots to the underlying invoices (to the rupee).
- **SC-003**: The GSTR-3B outward-supply liability equals the GSTR-1 outward totals to the rupee for
  every prepared period (zero 3B↔GSTR-1 outward drift).
- **SC-004**: 100% of returns that would be rejected by the portal for missing GSTIN, missing HSN, or
  invalid place of supply are caught by the readiness report before export/filing.
- **SC-005**: A prepared return can be exported to a portal-compatible file that the GST portal/offline
  tool accepts without structural/field errors on first upload.
- **SC-006**: No filed period is ever altered after filing, and no period is ever double-filed, across
  repeated network-retry scenarios.
- **SC-007**: A multi-state business can file each of its GSTINs independently, with each GSTIN's return
  containing only invoices billed from that GSTIN's state (zero cross-state leakage).
- **SC-008**: For input-tax-credit reconciliation, every purchase line and every supplier-reported line
  is placed in exactly one mismatch bucket, and rupee-rounding differences within ±₹1 do not create
  false mismatches.
- **SC-009**: A field user can see the current filing status, headline totals and acknowledgement
  reference number for any GSTIN/period on the mobile app, with the device performing no return
  computation.

## Assumptions

- The platform's finalized invoices already carry, or can be derived from, the fields GSTR-1 needs:
  document series and sequence, customer and seller GSTIN, buyer and seller place of supply, rate-wise
  tax breakup, HSN/SAC per line, and totals. (Confirmed against the `invoice` and `tax` modules.)
- Tax *calculation* remains owned by the existing tax capability; this feature only *aggregates,
  reconciles and files* — it does not re-compute per-line GST.
- E-invoice (IRN) data, where present, is read from the existing e-invoice capability and not
  re-generated here.
- The business does not yet have a structured purchase/vendor source, so input-tax-credit
  reconciliation is phase-gated and seeded by file import until such a source exists.
- The first delivery is **export-first** (portal-compatible file), with electronic filing and
  supplier-data pulls as a subsequent phase that requires GST-network onboarding.
- The rich preparation and reconciliation experience is delivered on the web surface; mobile is a
  read-only/status surface. (Web UI is a tracked follow-up to this specification.)
- Returns are filed in whole rupees; internal computation is exact and rounding is applied once at the
  section-total boundary.
- Digital-signature (DSC) based filing is out of scope; verification-code (OTP/EVC) filing covers the
  target small-and-medium-business case.

## Out of Scope

- Re-computing or changing per-line GST (owned by the tax capability).
- Generating e-invoices / IRNs (owned by the e-invoice capability).
- Annual returns (GSTR-9 / 9C) and the GSTR-1 amendment flow — a later phase.
- The optional monthly invoice furnishing (IFF) for QRMP quarterly filers — deferred; quarterly filers
  file one quarter-end GSTR-1.
- A full purchase/vendor billing module — input-tax-credit reconciliation is fed by import until such a
  module exists.
- Server-side digital-signature custody.
- Treating late fee / interest as a managed, payable liability (it is shown as an estimate only).
