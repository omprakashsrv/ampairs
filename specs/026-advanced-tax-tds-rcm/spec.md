# Feature Specification: Advanced Indian Tax (TDS / TCS / RCM / Composition / ITC)

**Feature Branch**: `026-advanced-tax-tds-rcm`  
**Created**: 2026-06-28  
**Status**: Draft  
**Input**: User description: "specs/026-advanced-tax-tds-rcm"

## Overview

Ampairs already calculates GST (CGST/SGST/IGST/CESS) on invoice lines for Indian businesses. Real
Tier-2/3 wholesalers and B2B sellers, however, routinely hit forms of indirect and withholding tax
that GST alone cannot express:

- **TDS** — income tax the *buyer* deducts at settlement and pays to the government, so the seller
  receives less than the invoice value.
- **TCS** — tax the *seller* collects on top of the sale value (above a threshold) and remits.
- **RCM (Reverse Charge)** — for certain supplies the *buyer*, not the seller, is liable to pay GST;
  the seller's bill collects no output GST.
- **Composition scheme** — small dealers who pay a flat turnover tax cannot charge GST and must issue
  a "bill of supply" instead of a tax invoice.
- **Nil-rated / Exempt / Zero-rated / Non-GST** supplies — legally distinct ways a line can carry no
  GST, each with different downstream credit and reporting consequences.
- **ITC (Input Tax Credit) and ITC reversal** — credit available on purchases, and the rules that
  claw part of it back.

This feature extends Ampairs so a business can correctly record, calculate, settle, and audit each of
these — while preserving the product's hard guarantee that tax amounts are computed identically
**offline on the device** and **online on the server** (an invoice created on a plane must foot to the
same numbers the backend would produce).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record buyer-deducted TDS at settlement (Priority: P1)

A wholesaler raises a ₹5,00,000 invoice (plus GST) to a corporate buyer. Under section 194Q the buyer
will deduct 0.1% TDS on the taxable value and pay the wholesaler the balance. The wholesaler needs the
system to show the gross invoice unchanged, compute the TDS deduction, and reflect that the *net amount
expected* (and the party's outstanding balance) is reduced by the TDS — so the books reconcile when the
short payment arrives.

**Why this priority**: TDS is the single most common "why is my receivable short?" reconciliation pain
for B2B sellers, and it is the foundation (settlement-time withholding posted to the party ledger) that
TCS reuses. It delivers standalone value even with nothing else in this feature built.

**Independent Test**: Create an invoice for a TDS-applicable party/section, settle it, and verify the
invoice total is unchanged, the withholding amount equals base × section rate, and the party's
outstanding balance and expected cash are reduced by exactly that amount, with the ledger footing to
zero.

**Acceptance Scenarios**:

1. **Given** a buyer marked as TDS-deducting under a configured section with a rate and threshold,
   **When** an invoice's cumulative taxable value to that buyer crosses the section threshold,
   **Then** the system computes a TDS amount on the taxable base and records it as a deduction that
   lowers the net receivable, leaving the gross invoice value and GST unchanged.
2. **Given** a TDS deduction has been recorded against an invoice, **When** the user views the party
   balance and the invoice, **Then** the gross dues, the TDS withheld, and the net amount expected are
   each shown separately and reconcile (gross − TDS = net).
3. **Given** the cumulative value to a buyer is still **below** the section threshold, **When** the
   invoice is settled, **Then** no TDS is deducted.

---

### User Story 2 - Immutable tax audit snapshot at finalize (Priority: P1)

Months after issuing an invoice, an accountant is asked "why was this IGST and not CGST+SGST, and what
rate was used?" Tax rules and rates change over time, so the business needs every finalized document to
permanently record the exact tax inputs and outputs that applied *as of issue* — and never silently
re-compute them when rules later change.

**Why this priority**: A finalized invoice is a legal record; backdated rate changes must not alter
history. The snapshot is also the source for return reconciliation, and it is cheap to add alongside
the P1 withholding flow.

**Independent Test**: Finalize an invoice, change the underlying tax rule/rate, re-open the invoice, and
confirm the displayed components, rates, place-of-supply decision, and treatment match the original
issue-time values, not the new rule.

**Acceptance Scenarios**:

1. **Given** an invoice is finalized, **When** the system records it, **Then** an immutable snapshot
   captures the resolved rule, source/destination state, intra-vs-inter-state decision, each tax
   component (name, rate, taxable amount, tax amount), the line treatment, the reverse-charge flag, any
   withholding section and amount, and the effective date.
2. **Given** a finalized invoice with a snapshot, **When** the governing tax rule or rate is later
   changed, **Then** the finalized invoice continues to show its original snapshot values and is never
   recomputed.

---

### User Story 3 - Reverse-charge (RCM) invoices (Priority: P2)

A seller supplies a service that falls under reverse charge. The seller's invoice must state that GST is
payable by the recipient under RCM and must collect **no** output GST, while still showing the correct
tax category (CGST+SGST vs IGST per place of supply) for reference. The recipient, in turn, must be able
to self-account for that GST.

**Why this priority**: RCM changes *who is liable* and is required for compliant billing of notified
goods/services, but it builds on the same place-of-supply calculation already shipped, so it follows the
P1 settlement plumbing.

**Independent Test**: Mark an invoice (or line) as reverse-charge, finalize it, and verify the supplier
document collects zero output GST, carries the reverse-charge declaration, and that the place-of-supply
tax category is still computed and displayed for reference.

**Acceptance Scenarios**:

1. **Given** an invoice or line flagged as reverse-charge applicable, **When** it is finalized,
   **Then** the document collects no output GST from the buyer and displays a "tax payable under reverse
   charge by recipient" declaration.
2. **Given** a reverse-charge invoice, **When** the tax category is determined, **Then** the
   intra-vs-inter-state (CGST+SGST vs IGST) decision is still computed using the normal place-of-supply
   logic and shown for reference — the rate is unchanged, only liability shifts.
3. **Given** a reverse-charge transaction, **When** the recipient records it on their books, **Then**
   the GST is self-assessed as a liability with an offsetting input credit rather than collected from
   the supplier.

---

### User Story 4 - Composition-scheme bill of supply (Priority: P2)

A small dealer enrolled in the composition scheme pays a flat percentage of turnover and is legally
barred from charging GST. When this dealer issues a sale document it must be a **bill of supply** with no
GST line and the statutory "composition taxable person, not eligible to collect tax" declaration. The
flat composition tax is the dealer's own periodic liability, not charged to the customer.

**Why this priority**: Composition dealers cannot legally issue a tax invoice; getting the document shape
wrong is a direct compliance risk. It is a distinct mode rather than a tweak to standard invoicing.

**Independent Test**: Switch a workspace to composition mode, create a sale, and verify the resulting
document is a bill of supply with no per-line GST components and the required declaration, and that no
GST is charged to the customer.

**Acceptance Scenarios**:

1. **Given** a workspace configured as a composition-scheme taxpayer, **When** it issues a sale
   document, **Then** the document is a bill of supply with no GST components, no tax breakup, and the
   mandatory composition declaration.
2. **Given** composition mode is active, **When** the dealer reviews liabilities, **Then** the flat
   composition tax on turnover is tracked as the dealer's own periodic liability and is never added to a
   customer's bill.

---

### User Story 5 - Nil / exempt / zero-rated / non-GST line treatment (Priority: P2)

A seller deals in a mix of taxable goods, nil-rated grains, exempt services, exported (zero-rated)
goods, and non-GST items like petrol. "0% tax" alone is ambiguous: exempt vs zero-rated differ on
whether input credit is available, and each is reported differently. The seller needs to mark each
line's GST treatment so the tax breakup, credit eligibility, and reporting category are correct.

**Why this priority**: Without an explicit treatment, distinct legal categories collapse into a
look-alike "0%", corrupting credit eligibility and return reporting. It is orthogonal to the rate and
applies per line.

**Independent Test**: Create lines with each treatment and verify nil/exempt/non-GST lines carry no GST
components, zero-rated lines carry 0% with credit eligibility flagged, and each line's reporting category
reflects its treatment.

**Acceptance Scenarios**:

1. **Given** a line marked nil-rated, exempt, or non-GST, **When** the invoice is calculated, **Then**
   the line carries no GST components.
2. **Given** a line marked zero-rated (export/SEZ), **When** the invoice is calculated, **Then** the
   line carries 0% GST but is flagged as input-credit eligible, distinct from an exempt line.
3. **Given** lines with different treatments on one invoice, **When** the document is reviewed, **Then**
   each line's treatment is recorded so credit eligibility and reporting category can be derived.

---

### User Story 6 - TCS collection on applicable sales (Priority: P2)

A seller of goods crosses the section 206C(1H) threshold with a buyer and must collect a small
percentage of TCS on top of the sale value and remit it. The system needs to add the TCS as an
additional amount the buyer owes and track the corresponding remittance liability — the mirror image of
TDS.

**Why this priority**: TCS reuses the exact settlement-time mechanism built for TDS (opposite sign), so
it is low marginal cost once P1 lands, and is required for sellers over the turnover threshold.

**Independent Test**: Configure a TCS section, settle an above-threshold sale, and verify the buyer's
amount due increases by sale value × TCS rate and a matching remittance liability is tracked.

**Acceptance Scenarios**:

1. **Given** a buyer/section where cumulative sale value crosses the TCS threshold, **When** a sale is
   settled, **Then** the system adds a TCS amount to the receivable and records a liability to remit it.
2. **Given** TCS has been collected, **When** the user reviews the transaction, **Then** the TCS amount
   is shown separately from GST and from the sale value, never folded into the GST breakup.

---

### User Story 7 - ITC tracking, reversal, and return reconciliation (Priority: P3)

A business accumulates input tax credit on purchases and occasionally must reverse part of it (for
exempt-supply apportionment, non-payment within 180 days, or blocked credits). Period-end, the accountant
needs a credit ledger plus return-shaped summaries to reconcile what GST was charged, what credit is
available, and what was reversed.

**Why this priority**: ITC depends on first-class purchase billing that is still maturing, so it is
scaffolded last; it never alters output tax on a sale.

**Independent Test**: Record input credits and a reversal, then generate a return-shaped summary and
verify available credit, reversed credit, and net are reported separately and never change any sale's
output GST.

**Acceptance Scenarios**:

1. **Given** input credits and reversal events are recorded, **When** the credit ledger is viewed,
   **Then** available credit, reversals, and net credit are tracked as distinct, auditable entries.
2. **Given** finalized invoices with tax snapshots over a period, **When** a return-shaped summary is
   generated, **Then** it aggregates the snapshots into output-tax and credit summaries without
   recomputing or altering any individual invoice.
3. **Given** an ITC reversal is recorded, **When** any sale invoice is reviewed, **Then** its output GST
   is unchanged — reversal affects only the credit ledger.

---

### Edge Cases

- **Threshold crossing mid-relationship**: TDS/TCS apply only once cumulative value with a party crosses
  the statutory threshold within the financial year — partial periods and the crossing transaction must
  be handled correctly.
- **Offline determinism**: A document created offline must compute the same withholding, GST category,
  and treatment amounts as the server; on reconnection the amounts must match, not be overwritten.
- **Rounding**: Withholding and collection amounts that affect the money ledger must foot exactly (no
  cents drift), and GST line amounts must round consistently so a voucher balances to zero.
- **RCM with composition / treatment conflicts**: A composition (bill-of-supply) document must never
  carry a reverse-charge GST line or any GST component; conflicting flags resolve to the legally correct
  document shape.
- **Mixed treatments on one invoice**: Taxable, exempt, zero-rated, and non-GST lines can coexist; totals
  and reporting categories must remain correct per line.
- **Withholding timing**: TDS may be applied at invoice time or at the time of actual payment; both
  timings must reconcile to the same party balance.
- **Backdated rule change**: Changing a rate or rule must never alter a previously finalized document's
  recorded tax.
- **Below threshold**: When a party/section is configured but the threshold is not met, no withholding
  is applied.

## Requirements *(mandatory)*

### Functional Requirements

**TDS / TCS (withholding & collection at settlement)**

- **FR-001**: System MUST allow a business to configure withholding sections (TDS and TCS) with a
  section code, rate, threshold, and the party types to which they apply.
- **FR-002**: System MUST compute a TDS deduction on the applicable taxable base when a buyer is subject
  to a TDS section and the configured threshold is met, leaving the gross invoice value and GST
  unchanged.
- **FR-003**: System MUST reduce the net amount expected and the party's outstanding balance by the TDS
  amount, and present gross dues, TDS withheld, and net expected as separate, reconciling figures.
- **FR-004**: System MUST compute a TCS collection on applicable sales above the configured threshold,
  adding it to the buyer's amount due and recording a corresponding remittance liability.
- **FR-005**: System MUST keep TDS and TCS amounts entirely separate from the GST breakup — they MUST
  never appear as GST components.
- **FR-006**: System MUST apply withholding only when the relevant cumulative threshold for the party and
  section is met, and apply none otherwise.
- **FR-007**: System MUST reconcile withholding applied at invoice time and withholding applied at
  payment time to the same party balance.

**RCM (reverse charge)**

- **FR-008**: System MUST allow an invoice (and individual line) to be marked as reverse-charge
  applicable.
- **FR-009**: A reverse-charge supplier document MUST collect no output GST from the buyer and MUST carry
  the statutory reverse-charge declaration.
- **FR-010**: System MUST still determine the place-of-supply tax category (intra-state CGST+SGST vs
  inter-state IGST) for a reverse-charge transaction for reference, without changing the rate.
- **FR-011**: System MUST record a reverse-charge transaction on the recipient's books as self-assessed
  GST liability with an offsetting input credit, rather than as GST collected from the supplier.

**Composition scheme**

- **FR-012**: System MUST allow a workspace to be configured as a composition-scheme taxpayer with its
  flat composition rate.
- **FR-013**: A composition workspace's sale documents MUST be bills of supply: no per-line GST
  components, no GST breakup, and the mandatory composition declaration.
- **FR-014**: System MUST track the flat composition tax on turnover as the dealer's own periodic
  liability and MUST never add it to a customer's bill.

**GST treatment**

- **FR-015**: System MUST allow each line to carry a GST treatment of taxable, nil-rated, exempt,
  zero-rated, or non-GST.
- **FR-016**: System MUST produce no GST components for nil-rated, exempt, and non-GST lines, and 0% GST
  with input-credit eligibility flagged for zero-rated lines (distinct from exempt).
- **FR-017**: System MUST record each line's treatment so credit eligibility and reporting category can
  be derived.

**ITC (input tax credit)**

- **FR-018**: System MUST maintain an input-credit ledger that records available credits and reversal
  events (e.g. exempt-supply apportionment, non-payment within 180 days, blocked credits) as distinct,
  auditable entries.
- **FR-019**: ITC and ITC reversal MUST never alter the output GST charged on any sale.
- **FR-020**: System MUST generate return-shaped summaries that aggregate finalized invoice tax snapshots
  into output-tax and credit summaries without recomputing or altering individual invoices.

**Audit & determinism (cross-cutting)**

- **FR-021**: System MUST capture an immutable tax audit snapshot when a document is finalized, recording
  the resolved rule, source/destination state, intra-vs-inter-state decision, each component (name, rate,
  taxable amount, tax amount), the line treatment, the reverse-charge flag, any withholding section and
  amount, and the effective date.
- **FR-022**: System MUST never recompute or alter a finalized document's tax when underlying rules or
  rates later change.
- **FR-023**: System MUST compute every amount-affecting tax decision (GST category, treatment,
  withholding, reverse-charge document shape, composition suppression) identically offline on the device
  and online on the server, so the two produce the same amounts for the same inputs.
- **FR-024**: System MUST ensure withholding and collection amounts that affect the money ledger foot
  exactly with no rounding drift, and that GST line amounts round consistently so vouchers balance to
  zero.
- **FR-025**: System MUST make the new tax definitions (withholding sections/config, composition config,
  treatment overrides) available offline on the device so calculations work without connectivity, and
  keep device and server copies in sync.

### Key Entities *(include if data involved)*

- **Withholding Section (TDS/TCS)**: A statutory section (code, rate, threshold, applicable party types)
  that drives a deduction (TDS) or collection (TCS) at settlement. Distinct from GST components.
- **Workspace Withholding Config**: A business's enablement and defaults for TDS/TCS (which sections
  apply, default section), per workspace.
- **Composition Config**: A workspace-level mode marking the business as a composition taxpayer, with its
  flat turnover rate; switches sale documents to bills of supply.
- **GST Treatment**: A per-line classification (taxable / nil-rated / exempt / zero-rated / non-GST) that
  shapes the tax components and credit eligibility for that line.
- **Reverse-Charge Flag**: A transaction/line attribute indicating GST liability shifts to the recipient;
  changes posting and document shape, not the rate.
- **Tax Audit Snapshot**: An immutable record captured at finalize of the exact tax inputs and outputs
  for a document; the source of truth for historical display and return reconciliation.
- **Input-Credit (ITC) Ledger Entry**: A signed, auditable record of input credit available or reversed;
  decoupled from output-tax calculation on sales.
- **Withholding Posting**: The settlement-time money movement (deduction for TDS, collection for TCS) that
  adjusts the party balance and the remittance liability, footing exactly in the money ledger.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A business can finalize a TDS-applicable invoice and the net amount expected equals gross
  plus GST minus the section's TDS, with the party balance matching to the rupee in 100% of cases.
- **SC-002**: For any finalized document, the same inputs produce identical tax and withholding amounts
  whether computed offline on a device or online on the server (zero discrepancy on a representative test
  matrix covering all treatments, RCM, composition, TDS, and TCS).
- **SC-003**: Reopening a finalized document after a later rate/rule change shows its original tax in 100%
  of cases (no recomputation).
- **SC-004**: A composition workspace can never produce a document that contains a GST component (0
  composition documents carry GST across all generation paths).
- **SC-005**: TDS and TCS amounts never appear inside the GST breakup, and every settlement voucher
  containing a withholding amount foots to zero (no rounding drift).
- **SC-006**: A user can configure a withholding section and apply it to a transaction in under 2 minutes
  without external help.
- **SC-007**: Each of the five GST treatments yields the correct components and credit-eligibility flag,
  with exempt and zero-rated correctly distinguished in 100% of test cases.
- **SC-008**: A period return-shaped summary reconciles to the sum of the underlying finalized invoice
  snapshots with zero variance, and generating it never changes any individual invoice.

## Assumptions

- **Indian tax regime**: This feature targets Indian GST/income-tax rules (TDS sections such as 194Q/194C/194J,
  TCS 206C(1H), composition scheme, RCM notifications). The existing tax strategy is `INDIA_GST`; other
  jurisdictions are out of scope here.
- **Builds on existing GST engine**: The place-of-supply CGST/SGST-vs-IGST calculation already exists and
  is reused unchanged; this feature adds withholding, liability flips, document modes, and treatments
  around it.
- **Settlement-time withholding via the money ledger**: TDS/TCS are modeled as settlement-time ledger
  movements that adjust party balances, not as invoice-total reductions — the invoice legally shows gross
  plus GST.
- **Statutory rates/thresholds are configured, not hardcoded**: Section rates and thresholds are set up as
  data so they can change without an app release; the snapshot preserves the values used at issue.
- **Phased delivery**: TDS/TCS + audit snapshot first (P1), then RCM + composition + treatments (P2), then
  ITC ledger + return reconciliation (P3). Full ITC depth deepens as first-class purchase billing matures.
- **Offline-first is mandatory**: All amount-affecting logic runs on the device against synced
  definitions; the server is the authority for sync tracking but must reproduce identical amounts.
- **Money precision**: Ledger-affecting amounts are exact; GST line amounts use consistent half-up
  rounding with a round-off so vouchers foot.

## Out of Scope

- Filing returns directly with the GST portal (GSTR-1/3B e-filing) — see related spec 028; this feature
  provides return-*shaped* summaries, not portal submission.
- E-invoice / e-way bill generation (spec 015).
- First-class purchase-bill capture (ITC here is scaffolded against existing adjustment vouchers and
  deepens when purchase billing lands).
- Non-Indian tax jurisdictions.
- Web (Angular) UI for these flows in this phase — mobile/offline calculation and backend definitions
  come first.
