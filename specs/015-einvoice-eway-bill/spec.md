# Feature Specification: GST E-Invoicing (IRN) & E-Way Bill

**Feature Branch**: `015-einvoice-eway-bill`
**Created**: 2026-06-28
**Status**: Draft
**Input**: User description: "specs/015-einvoice-eway-bill"

## Overview

Indian GST law requires eligible businesses to register every B2B (and certain other) tax
invoice on the Government's Invoice Registration Portal (IRP) **before** it is shared with the
buyer. Registration returns an **Invoice Reference Number (IRN)**, a digitally **signed QR code**,
and an **acknowledgement number/date**; the invoice is not legally valid for input-tax-credit
purposes until it carries these. Separately, when taxable goods move above a value threshold, the
business must generate an **E-Way Bill (EWB)** carrying transporter, vehicle, distance and validity
details.

Today the product produces GST-compliant invoices but stops short of the statutory portal
registration, forcing users to re-key invoice data into the Government portal manually. This feature
closes that gap: finalized invoices are automatically registered to obtain an IRN and QR code, an
e-way bill can be generated for goods movement, and both artifacts are displayed on the invoice and
embedded in the printed/PDF document — including for field staff working offline.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automatic e-invoice (IRN) registration on finalize (Priority: P1)

A billing user in an e-invoicing-eligible business finalizes a B2B tax invoice. Without any extra
data entry, the invoice is registered with the Government IRP and comes back stamped with an IRN, a
signed QR code, and an acknowledgement number/date. The user sees the IRN and QR on the invoice and
can print/share a legally compliant e-invoice.

**Why this priority**: IRN registration is the legally mandatory core of e-invoicing — without it
the rest of the feature (e-way bill, cancellation) has no anchor. It directly removes the manual
double-entry that businesses do today and is the minimum viable, independently valuable slice.

**Independent Test**: Enable e-invoicing for a workspace, finalize an eligible B2B invoice, and
verify that an IRN, signed QR code, and acknowledgement number appear on the invoice and on its
printed output — with no manual portal interaction.

**Acceptance Scenarios**:

1. **Given** a workspace with e-invoicing enabled and a finalized eligible B2B invoice, **When** the
   system registers it with the IRP, **Then** the invoice shows an IRN, a scannable signed QR code,
   an acknowledgement number, and an acknowledgement date, and its status reads "Generated".
2. **Given** an invoice that has just been finalized while the Government portal is unreachable,
   **When** the user views it, **Then** the e-invoice status reads "Pending" and the system
   automatically completes registration once connectivity to the portal is restored — without the
   user re-submitting.
3. **Given** registration is retried after a network timeout where the portal had already issued an
   IRN, **When** the retry runs, **Then** the system stores the original IRN once (no second IRN, no
   error shown to the user).
4. **Given** an invoice that is missing data the portal requires (e.g. buyer GSTIN or an item HSN
   code), **When** registration is attempted, **Then** the e-invoice status reads "Failed" with a
   human-readable reason, and the invoice itself remains finalized and valid locally.
5. **Given** a generated e-invoice, **When** the user prints or exports the invoice as a PDF while
   offline, **Then** the IRN and signed QR code are embedded in the document without needing
   connectivity.

---

### User Story 2 - Generate an E-Way Bill for goods movement (Priority: P2)

A user dispatching goods above the e-way-bill value threshold records the transporter, transport
mode, vehicle number, and distance for an invoice and generates an e-way bill. The resulting e-way
bill number and validity period are shown on the invoice and printed alongside it. The e-way bill can
be generated together with the e-invoice or later, when the vehicle is actually assigned.

**Why this priority**: Required only when goods physically move above the threshold, so it serves a
large but narrower set of invoices than IRN registration. It depends on transport details that are
often known only at dispatch time, so it is a distinct step after P1.

**Independent Test**: For an invoice above the e-way-bill threshold, enter transporter/vehicle/
distance details, generate the e-way bill, and confirm the e-way bill number and validity date appear
on the invoice and its print output.

**Acceptance Scenarios**:

1. **Given** a finalized invoice above the e-way-bill value threshold with transport details
   entered, **When** the user generates an e-way bill, **Then** the invoice shows an e-way bill
   number, generation date, and validity date.
2. **Given** an invoice that already has an IRN, **When** the user generates the e-way bill, **Then**
   the system reuses the registered invoice data so transport details are the only new input
   required.
3. **Given** an invoice below the e-way-bill value threshold or with no goods movement, **When** the
   user views it, **Then** e-way bill generation is not required and is presented as optional.
4. **Given** a vehicle change during transit on an active e-way bill, **When** the user updates the
   vehicle details, **Then** the e-way bill reflects the new vehicle without a new e-way bill number
   being issued.

---

### User Story 3 - Cancel an e-invoice or e-way bill within the allowed window (Priority: P3)

A user who registered an invoice in error (duplicate, wrong data) cancels the e-invoice within the
statutory window by selecting a valid reason. Similarly, an e-way bill generated in error can be
cancelled within its window. After the window closes, the system clearly explains that cancellation
is no longer possible and points to the alternative remedy.

**Why this priority**: Corrections are important for compliance hygiene but are exception-path
operations that build on P1/P2 already existing. They are time-boxed by law and lower volume.

**Independent Test**: Generate an IRN, cancel it within the allowed window with a valid reason, and
confirm the e-invoice is marked cancelled and can no longer be presented as valid; then attempt a
cancellation outside the window and confirm it is blocked with a clear message.

**Acceptance Scenarios**:

1. **Given** an e-invoice generated less than 24 hours ago, **When** the user cancels it with a valid
   reason, **Then** the e-invoice is marked "Cancelled" and the invoice can no longer be printed as a
   valid e-invoice.
2. **Given** an e-invoice generated more than 24 hours ago, **When** the user attempts to cancel it,
   **Then** the action is blocked with a message explaining the window has closed and that a credit
   note is the remaining remedy.
3. **Given** an e-way bill generated less than 24 hours ago and not yet verified in transit, **When**
   the user cancels it with a valid reason, **Then** the e-way bill is marked "Cancelled".

---

### User Story 4 - Configure e-invoicing applicability per business (Priority: P3)

An administrator turns e-invoicing and e-way-bill generation on or off for their workspace and sets
the e-way-bill value threshold, because applicability depends on the business's turnover and
category and the statutory thresholds change over time. The system only attempts portal registration
for workspaces that have enabled it.

**Why this priority**: Necessary to avoid attempting registration for businesses that are exempt or
below the turnover threshold, but it is a one-time setup concern rather than a daily workflow.

**Independent Test**: With e-invoicing disabled for a workspace, finalize an invoice and confirm no
registration is attempted; enable it and confirm subsequent finalized invoices are registered.

**Acceptance Scenarios**:

1. **Given** a workspace with e-invoicing disabled, **When** an invoice is finalized, **Then** no
   IRN registration is attempted and the invoice carries no e-invoice status.
2. **Given** an administrator enables e-invoicing and sets the e-way-bill threshold, **When** a
   subsequent eligible invoice is finalized, **Then** registration is attempted automatically.
3. **Given** e-invoicing is enabled but an individual invoice is not eligible (e.g. a B2C/consumer
   sale below threshold), **When** it is finalized, **Then** the system skips registration for that
   invoice without raising an error.

---

### Edge Cases

- **Portal unavailable at finalize**: Finalize always succeeds; the e-invoice waits in "Pending" and
  is completed automatically when the portal is reachable again.
- **Lost acknowledgement on retry**: If the portal had already issued an IRN but the response was
  lost, the system reconciles and stores the existing IRN exactly once rather than creating a
  duplicate or surfacing a failure.
- **Missing required data**: Invoices missing buyer GSTIN, HSN codes, valid PIN codes, or with
  totals that do not reconcile within the portal's tolerance are marked "Failed" with a clear reason
  and do not block local invoice use.
- **Rounding/total mismatch**: When summed line amounts differ from the invoice header within
  permitted tolerance, the registered payload reconciles via a round-off adjustment so totals foot
  exactly.
- **Cancellation past the window**: Blocked with guidance toward a credit note (a separate, future
  flow), not silently failed.
- **Cancelled e-invoice reprint**: A cancelled e-invoice cannot be presented or printed as a valid
  e-invoice.
- **E-way bill below threshold / no movement**: Not required; offered as optional, never forced with
  fabricated transport data.
- **E-way bill validity expiry**: An expired e-way bill is shown as expired; extension is a separate
  action where allowed.
- **Offline field user**: Sees current synced status (Pending/Generated/Failed) and can print a
  previously generated, synced e-invoice's QR offline; cannot itself create or cancel compliance
  artifacts while offline.
- **Workspace switch / multi-tenant isolation**: A user only ever sees the e-invoice and e-way-bill
  artifacts belonging to the active workspace.

## Requirements *(mandatory)*

### Functional Requirements

#### Applicability & configuration

- **FR-001**: The system MUST let an administrator enable or disable e-invoicing and e-way-bill
  generation per workspace, and MUST NOT attempt portal registration for a workspace that has not
  enabled it.
- **FR-002**: The system MUST let an administrator configure the e-way-bill value threshold (default
  ₹50,000) per workspace, since the threshold is set by regulation and can change.
- **FR-003**: The system MUST skip registration for invoices that are not eligible for e-invoicing
  (e.g. consumer/B2C or sub-threshold documents) without raising an error.

#### E-invoice (IRN) registration

- **FR-004**: When an eligible invoice is finalized in an e-invoicing-enabled workspace, the system
  MUST automatically initiate IRN registration with the Government portal without requiring extra
  data entry from the user.
- **FR-005**: Invoice finalization MUST NOT be blocked by, or wait on, the outcome of portal
  registration; finalization MUST continue to work while offline or while the portal is unavailable.
- **FR-006**: The system MUST build the registration request from the invoice's existing data
  (parties, GSTINs, place of supply, line items, HSN codes, CGST/SGST/IGST amounts, totals) and MUST
  validate required fields and total reconciliation before submission, surfacing a clear reason when
  validation fails.
- **FR-007**: On successful registration the system MUST persist and display the IRN, the signed QR
  code, the acknowledgement number, and the acknowledgement date against the invoice.
- **FR-008**: The system MUST guarantee at most one IRN per invoice document, including across
  retries and concurrent attempts, and MUST treat the portal's "duplicate request — existing IRN"
  response as success by storing the originally issued IRN.
- **FR-009**: When registration cannot complete (portal down, transient error), the system MUST
  retain the request and retry automatically with backoff until it succeeds or is determined to be
  permanently invalid; the user MUST NOT need to manually resubmit transient failures.
- **FR-010**: The system MUST expose the e-invoice status for each invoice as one of Pending,
  Generated, Failed, or Cancelled, with a human-readable reason for Failed states.
- **FR-011**: The system MUST allow a user to manually trigger registration (or retry a failed
  registration) for an invoice while online.

#### E-way bill

- **FR-012**: The system MUST let a user generate an e-way bill for an invoice by capturing
  transporter identity/name, transport mode, vehicle number, transport distance, and transport
  document reference, and MUST persist the returned e-way bill number, generation date, and validity
  date.
- **FR-013**: When an invoice already has an IRN, e-way-bill generation MUST reuse the registered
  invoice data so that transport details are the only additional input required.
- **FR-014**: The system MUST support generating the e-way bill either together with the e-invoice or
  as a standalone later step (e.g. when the vehicle is assigned at dispatch).
- **FR-015**: The system MUST support updating the vehicle/transport (Part-B) details of an active
  e-way bill without issuing a new e-way bill number, and MUST support extending validity where the
  regulation permits.
- **FR-016**: The system MUST present e-way-bill generation as optional for invoices below the value
  threshold or without goods movement, and MUST NOT force fabricated transport data.

#### Cancellation & lifecycle

- **FR-017**: The system MUST allow cancellation of an e-invoice only within the statutory window
  (24 hours of acknowledgement) and only with a valid reason code, and MUST block and clearly explain
  attempts made after the window, pointing to the credit-note remedy.
- **FR-018**: The system MUST allow cancellation of an e-way bill only within its statutory window
  (24 hours of generation) and not after the goods have been verified in transit.
- **FR-019**: Once an e-invoice is cancelled, the system MUST prevent the invoice from being
  presented or printed as a valid e-invoice.

#### Display & print

- **FR-020**: The system MUST display the IRN, signed QR code, acknowledgement number, e-invoice
  status, and (when present) the e-way-bill number and validity on the invoice detail view.
- **FR-021**: The printed/PDF invoice MUST embed the signed QR code, IRN, and (when present) the
  e-way-bill number, and this MUST be possible offline once the artifacts have synced to the device.
- **FR-022**: On mobile/field clients the e-invoice and e-way-bill artifacts MUST be display-only
  (read-only): the device renders the synced QR/IRN/EWB but never authors compliance state offline;
  generation and cancellation are online actions only.

#### Security, isolation & audit

- **FR-023**: Government portal/service-provider credentials MUST be stored securely server-side
  (encrypted, never in source or shipped to client devices) and resolved per workspace.
- **FR-024**: Signed legal artifacts (the signed invoice payload and signed QR) MUST be retained for
  audit and reprint, and MUST NOT be exposed in list/summary responses — only on the single-document
  detail view.
- **FR-025**: All e-invoice and e-way-bill data MUST be isolated per workspace (multi-tenant): a user
  sees only the artifacts of the active workspace.
- **FR-026**: The system MUST retain the request and response exchanged with the portal for each
  registration/cancellation for audit and troubleshooting.

### Key Entities *(include if feature involves data)*

- **E-Invoice Document**: The compliance record for one finalized invoice. Holds the IRN, signed QR
  code, acknowledgement number and date, the signed invoice artifact, current status
  (Pending/Generated/Failed/Cancelled), the service provider used, any failure reason, and
  cancellation reason/time. One-to-one with an invoice; never alters the invoice itself.
- **E-Way Bill**: The transport-document record for an invoice. Holds the e-way-bill number,
  generation date, validity-upto date, transporter identity/name, transport mode, vehicle number and
  type, transport distance, transport document reference, and status
  (Generated/Updated/Cancelled/Expired). Linked to an invoice and, when present, to its e-invoice.
- **Registration Job / Queue Item**: A pending unit of work representing an invoice awaiting IRN (or
  e-way-bill) registration, tracking attempt count, next attempt time, and last error — the
  mechanism behind automatic retry.
- **E-Invoicing Configuration**: Per-workspace settings governing whether e-invoicing and e-way-bill
  generation are enabled, the chosen service provider, and the e-way-bill value threshold.
- **Portal Credential**: Per-workspace secret material used to authenticate to the Government portal/
  service provider, stored encrypted and server-side only.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: At least 99% of eligible finalized invoices in an enabled workspace obtain an IRN
  automatically without any manual data re-entry into a Government portal.
- **SC-002**: For an eligible B2B invoice with complete data and a healthy portal connection, the
  user sees the IRN and QR code on the invoice within 10 seconds of finalization.
- **SC-003**: 100% of invoices finalized while offline or during a portal outage complete their IRN
  registration automatically once connectivity is restored, with zero manual resubmissions required.
- **SC-004**: Zero duplicate IRNs are issued for the same invoice across all retry and concurrency
  scenarios.
- **SC-005**: 100% of generated e-invoices can be printed or exported with a valid, scannable QR code
  while the device is offline.
- **SC-006**: 100% of cancellation attempts outside the statutory window are blocked with a clear
  explanatory message, and no e-invoice is ever incorrectly registered as both valid and cancelled.
- **SC-007**: Manual data entry per e-invoice (beyond the existing invoice) is reduced to zero for
  IRN registration and to only transport-specific fields for e-way bills.
- **SC-008**: 100% of failed registrations present a human-readable reason that identifies the
  corrective action (e.g. "buyer GSTIN missing").

## Assumptions

- **A1**: E-invoicing applicability is governed by per-workspace configuration rather than an
  automatic turnover calculation, because the system cannot reliably determine a business's Aggregate
  Annual Turnover and the statutory threshold (₹10Cr → ₹5Cr → ₹2Cr from Oct 2025) changes over time.
  The administrator/setup is responsible for enabling it correctly.
- **A2**: The Government IRP (via an authorized service provider) is the sole authority that mints the
  IRN; IRNs and e-way bills cannot be created offline by construction, so generation is online-only
  with automatic retry.
- **A3**: Credit notes for corrections after the 24-hour cancellation window are out of scope for
  this feature and handled by a separate (future) flow; this feature only points users toward that
  remedy.
- **A4**: The default e-way-bill value threshold is ₹50,000 unless an administrator changes it; the
  monetary amounts and tax structure already captured on the invoice are sufficient to build the
  registration request.
- **A5**: Web (Angular) UI for managing these artifacts is deferred; this feature targets the backend
  registration pipeline plus mobile/field display and print.
- **A6**: A document's place-of-supply and party GSTINs already present on the invoice correctly
  determine intra-state (CGST+SGST) vs inter-state (IGST) treatment for the registration payload.

## Dependencies

- An existing finalized-invoice lifecycle that emits a reliable "invoice finalized" / "invoice
  cancelled" signal the e-invoicing pipeline can react to.
- An authorized connection to the Government IRP/E-Way-Bill system, typically via a GST Suvidha
  Provider (GSP), with valid per-workspace credentials.
- The existing per-workspace settings mechanism to hold applicability/threshold configuration.
- The existing invoice print/PDF path to embed the QR/IRN/EWB.
- The existing offline-first sync mechanism to deliver server-authored artifacts to field devices for
  display and offline printing.
