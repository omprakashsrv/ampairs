# Feature Specification: Payment & Collection (Party Ledger)

**Feature Branch**: `claude/affectionate-bohr-er32cm` (spec dir `013-payment-collection`)
**Created**: 2026-06-19
**Status**: Draft
**Input**: User description: "Payment and collection module for retail/wholesale customers and business owners. On top of invoices and orders, let the owner/distributor manage payments against their parties — starting from an opening balance and driven by sales, purchases and returns — and always know the current closing balance (credit or debit) per party."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record a collection against a party (Priority: P1)

A shop owner or distributor receives money from a customer against one or more outstanding bills (or
as an advance) and records it, choosing how it was paid (cash, cheque, UPI, bank transfer, etc.). The
party's outstanding balance immediately reflects the receipt.

**Why this priority**: This is the core reason the module exists — without recording collections there
is no way to track what a party still owes. It is the single most frequent daily action.

**Independent Test**: With at least one customer having an outstanding amount, record a payment of any
mode and amount; confirm the party balance decreases by exactly that amount, the receipt appears in the
party's history, and (when applied to a bill) that bill's outstanding decreases.

**Acceptance Scenarios**:

1. **Given** a customer owes ₹10,000, **When** the owner records a ₹4,000 cash receipt, **Then** the
   customer's closing balance becomes ₹6,000 (still receivable) and the receipt is listed in their ledger.
2. **Given** a customer owes ₹10,000 across three invoices, **When** the owner records a ₹10,000 receipt
   and allocates it to the bills, **Then** all three invoices show as fully settled and the balance is ₹0.
3. **Given** a customer owes ₹5,000, **When** the owner records a ₹7,000 receipt, **Then** ₹5,000 settles
   the dues and ₹2,000 is retained as an advance (on-account), making the party balance ₹2,000 in the
   party's favour.
4. **Given** a receipt is recorded by cheque, **When** saving, **Then** the cheque number, bank and date
   can be captured and the receipt is marked as not-yet-cleared.

---

### User Story 2 - See a party's running balance and statement (Priority: P1)

The owner opens a customer and sees the current closing balance (clearly marked as "to receive" or "to
pay") plus a chronological statement: opening balance, every sale, return, and payment, with a running
balance after each line. The statement can be shared/printed.

**Why this priority**: Knowing the live closing balance per party is the explicit goal of the feature.
The statement is what owners send to parties to confirm dues — a primary trust/collection tool.

**Independent Test**: For a party with an opening balance, at least one sale and one payment, open the
statement; verify entries appear in date order, each shows a running balance, and the final running
balance equals the displayed closing balance.

**Acceptance Scenarios**:

1. **Given** a party with opening balance ₹2,000 to receive, two invoices (₹3,000, ₹1,500) and one
   receipt (₹4,000), **When** the owner opens the statement, **Then** the running balance after the last
   line and the headline closing balance both read ₹2,500 to receive.
2. **Given** any party, **When** the owner views the customer list, **Then** each customer shows their
   current balance with a clear receivable/payable indicator.
3. **Given** a party statement, **When** the owner chooses to share it, **Then** a printable statement
   for a chosen date range is produced.

---

### User Story 3 - Set opening balances at cutover (Priority: P2)

When a business starts using the app (or at the start of a period), the owner enters each party's
existing balance carried over from their old books, marked as "to receive" or "to pay", as of a chosen
date, so the app's balances match reality from day one.

**Why this priority**: Without opening balances the closing balance is wrong for every existing party.
Required for adoption by an established business, but the module is still demonstrable without it for a
brand-new business starting at zero.

**Independent Test**: Set an opening balance of ₹5,000 "to receive" for a new party with no other
activity; confirm the closing balance reads ₹5,000 receivable and the statement's first line is the
opening balance on the chosen date.

**Acceptance Scenarios**:

1. **Given** a party with no activity, **When** the owner sets an opening balance of ₹5,000 to pay,
   **Then** the closing balance reads ₹5,000 payable (in the party's favour).
2. **Given** a party with an existing opening balance, **When** the owner edits it, **Then** the closing
   balance and statement recompute consistently.

---

### User Story 4 - Track outstanding, due dates and aging (Priority: P2)

The owner sees which bills are unpaid, how overdue they are (based on each party's credit period), and a
summary of total receivables grouped into aging buckets, with a warning when a party exceeds its credit
limit.

**Why this priority**: Aging turns raw balances into actionable collection priorities — the reason a
distributor wants this module. Builds directly on US1/US2 data.

**Independent Test**: With invoices of varying ages and a party credit period, open the outstanding view;
verify each unpaid bill is placed in the correct aging bucket and that exceeding the credit limit raises
a visible warning.

**Acceptance Scenarios**:

1. **Given** an invoice older than the party's credit period, **When** the owner opens outstanding bills,
   **Then** that invoice is flagged overdue and counted in the appropriate aging bucket.
2. **Given** a party whose receivable exceeds its credit limit, **When** creating a new sale or viewing
   the party, **Then** a credit-limit warning is shown.
3. **Given** the collections dashboard, **When** opened, **Then** it shows total receivable, total
   payable, and the aging summary.

---

### User Story 5 - Record returns and adjustments (Priority: P3)

The owner records a sales return / credit note, a purchase or purchase-return adjustment, a discount
given at settlement, or a write-off of a bad debt, and the party balance updates accordingly.

**Why this priority**: Real balances need non-payment movements (the brief names "returns" and
"purchases"). Lower priority because the most common daily flow (sales + receipts) is covered by P1.

**Independent Test**: For a party owing ₹3,000, record a ₹500 sales-return credit; confirm the balance
becomes ₹2,500 and the credit appears as a distinct line in the statement.

**Acceptance Scenarios**:

1. **Given** a customer owes ₹3,000, **When** the owner records a ₹500 sales return, **Then** the balance
   becomes ₹2,500 and the return is a distinct ledger line.
2. **Given** a customer owes ₹3,000, **When** the owner writes off ₹3,000 as bad debt, **Then** the
   balance becomes ₹0 and the write-off is recorded for audit.
3. **Given** a party that supplies goods, **When** the owner records a purchase adjustment of ₹2,000,
   **Then** the party balance moves ₹2,000 in the party's favour (payable).

---

### User Story 6 - Cheque / online realisation (Priority: P3)

A receipt taken by cheque or online transfer is initially "pending"; the owner later marks it cleared or
bounced. A bounced receipt restores the party's outstanding without erasing the record.

**Why this priority**: Cheques and transfers are not instant; treating them as cleared overstates
collection. Lower priority because cash collections (settled instantly) cover the MVP.

**Independent Test**: Record a cheque receipt (party balance shows the receipt as pending), then mark it
bounced; confirm the party's receivable returns to its pre-receipt value and both the original receipt
and its reversal remain visible.

**Acceptance Scenarios**:

1. **Given** a pending cheque receipt of ₹4,000, **When** the owner marks it cleared, **Then** it counts
   as realised collection.
2. **Given** a pending cheque receipt of ₹4,000 that reduced the balance, **When** the owner marks it
   bounced, **Then** the ₹4,000 is added back to the party's outstanding and a reversal entry is recorded.

---

### Edge Cases

- **Advance / overpayment**: a receipt larger than total dues leaves an on-account credit; later sales
  draw it down.
- **One payment, many bills**: a single receipt settles several invoices; allocation across them must sum
  to no more than the receipt amount.
- **Editing or cancelling a posted invoice**: the party balance must adjust; a cancelled invoice must not
  silently disappear from the audit trail (it is reversed, not erased).
- **Draft documents**: only finalized invoices affect the balance; drafts never do.
- **Backdated / out-of-order entries**: a receipt or sale dated in the past re-sequences the statement but
  the closing balance stays correct.
- **Offline entry**: collections recorded without connectivity update the balance immediately and
  reconcile when connectivity returns; if the same record is changed on two devices, the resolution is
  deterministic and the balance still foots.
- **Party that is both customer and supplier**: a single net balance per party, correctly signed.
- **Deleting a receipt**: removing a receipt restores the previously settled outstanding and is auditable.
- **Rounding**: settlements always foot exactly — no residual paise drift across many transactions.

## Requirements *(mandatory)*

### Functional Requirements

**Party balance & ledger**
- **FR-001**: System MUST maintain a single running balance per party, expressed as a signed amount with a
  clear "to receive" (receivable) vs "to pay" (payable) indicator.
- **FR-002**: System MUST derive the closing balance as the opening balance plus the net of all recorded
  movements (sales, returns, purchases, payments, adjustments) for that party.
- **FR-003**: System MUST allow an opening balance per party, marked "to receive" or "to pay", effective
  from a chosen date, and MUST treat it as the first line of the party statement.
- **FR-004**: System MUST present, per party, a chronological statement of every movement with a running
  balance after each line, and the final running balance MUST equal the displayed closing balance.
- **FR-005**: System MUST allow the party statement to be produced for a chosen date range in a
  shareable/printable form.
- **FR-006**: System MUST show each party's current balance and receivable/payable indicator in the
  customer list and on the customer detail view.

**Recording collections / payments**
- **FR-007**: Users MUST be able to record a payment received from, or paid to, a party, with an amount,
  date, and payment mode.
- **FR-008**: System MUST support these payment modes at minimum: cash, cheque, UPI, NEFT, RTGS, IMPS,
  net banking, card, and generic bank transfer.
- **FR-009**: System MUST capture a reference for non-cash modes (e.g. transaction/UTR number) and, for
  cheques, the cheque number, bank name, and cheque date.
- **FR-010**: Users MUST be able to allocate a single payment across one or more outstanding bills, and
  the total allocated MUST NOT exceed the payment amount.
- **FR-011**: System MUST retain any unallocated portion of a payment as an on-account advance that
  reduces (or, for advances, becomes) the party's balance and can be applied to future bills.
- **FR-012**: Recording, editing, or removing a payment MUST update the party balance and affected bills'
  outstanding consistently.

**Sales, returns & adjustments**
- **FR-013**: System MUST increase a party's receivable when a sale (finalized invoice) is recorded, using
  the invoice total, and MUST NOT count draft or non-finalized invoices.
- **FR-014**: System MUST keep the party balance consistent when a finalized invoice is later edited or
  cancelled, without erasing the original record (cancellations are reversed for audit).
- **FR-015**: Users MUST be able to record sales returns / credit notes, purchase and purchase-return
  adjustments, settlement discounts, and bad-debt write-offs, each updating the balance with the correct
  direction.

**Outstanding & aging**
- **FR-016**: System MUST compute each bill's outstanding as its total minus amounts allocated to it.
- **FR-017**: System MUST derive a due date per bill from the party's credit period and flag overdue bills.
- **FR-018**: System MUST group outstanding receivables into aging buckets and present a collections
  summary (total receivable, total payable, aging breakdown).
- **FR-019**: System MUST warn when a party's receivable exceeds its configured credit limit.

**Cheque / online realisation**
- **FR-020**: System MUST allow a payment to carry a realisation status (pending, cleared, bounced,
  cancelled), defaulting to cleared for instantly-settled modes (e.g. cash) and pending for cheques.
- **FR-021**: Marking a payment bounced MUST restore the party's outstanding and record a reversal,
  keeping both the original and the reversal visible.

**Integrity, audit & offline**
- **FR-022**: System MUST ensure that for every party, opening balance plus total debits minus total
  credits always equals the closing balance (the ledger always foots).
- **FR-023**: System MUST NOT permanently erase posted financial movements; corrections are made by edit
  or reversal with an auditable trail.
- **FR-024**: System MUST assign each payment/voucher a human-readable, sequential, gap-free number.
- **FR-025**: System MUST allow collections and balance viewing to work offline, updating balances
  immediately and reconciling deterministically when connectivity returns.
- **FR-026**: System MUST scope all party balances, payments and ledger data to the active workspace and
  isolate it from other workspaces.
- **FR-027**: System MUST let a workspace configure which payment modes are available and related
  collection options (e.g. whether cheques require a clearance step, whether advances are allowed,
  whether credit limits are enforced, and aging bucket boundaries).

### Key Entities *(include if feature involves data)*

- **Party balance**: the per-party position — opening balance and direction, effective date, and the
  current signed closing balance. One per party (a party is an existing customer).
- **Ledger entry**: a single dated movement against a party (opening, sale, return, payment, adjustment),
  with a direction and amount; the running balance is the cumulative sum of these. The sole driver of the
  closing balance.
- **Payment / receipt**: a money movement with a party, date, amount, mode, mode-specific reference
  (cheque/bank/UTR details), realisation status, and any unallocated (on-account) remainder.
- **Allocation**: a link recording how much of a payment is applied to a specific outstanding bill; used
  for outstanding and aging, not for the party total.
- **Adjustment / note**: a non-payment movement — sales return / credit note, purchase or
  purchase-return, settlement discount, or write-off — that posts a ledger entry.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An owner can record a collection against a party and see the updated balance in under 30
  seconds, in 3 or fewer steps.
- **SC-002**: For any party, the displayed closing balance equals opening plus debits minus credits in
  100% of cases (no unreconciled balances), verified across a large transaction set.
- **SC-003**: An owner can produce and share a party statement for a chosen date range in under 1 minute.
- **SC-004**: At least 95% of collection entries are recordable without leaving the customer/collection
  flow (no need to navigate to invoices first), including allocation to bills and advances.
- **SC-005**: Collections recorded while offline are reflected in the on-device balance immediately
  (perceived instant) and converge to the same balance on all devices after reconnect with zero
  discrepancy.
- **SC-006**: Across a stress set of mixed transactions (sales, multi-bill receipts, returns, advances,
  bounced cheques, edits, backdated entries), the total of all party balances equals total receivables
  minus total payables with zero rounding drift.
- **SC-007**: Aging classification of every outstanding bill matches its due date and the configured
  buckets in 100% of sampled cases.

## Assumptions

- **Subsidiary party ledger** (one signed posting per movement, from the business's books), not a full
  double-entry general ledger with a chart of accounts. A full GL can be layered later.
- **Sign convention**: receivable is positive ("to receive"); payable is negative ("to pay"). A party may
  net to either side.
- **Parties are existing customers.** A dedicated supplier/vendor concept does not yet exist; until it
  does, purchases and purchase-returns are entered as adjustments. A first-class purchase/vendor capability
  is a later phase.
- **Money is handled at exact precision** (no floating-point drift); amounts settle to two decimal places
  for the workspace currency with explicit rounding.
- **Closing balance is derived and cached**; it is always fully recomputable from the recorded movements.
- **Whoever records a document records its corresponding ledger movement**, so balances are correct even
  when entries originate offline on different devices.
- **Standard workspace multi-tenancy and authentication** apply (workspace-scoped, header-identified), as
  for existing modules.

## Out of Scope (this feature / deferred to later phases)

- Full double-entry general ledger and chart of accounts.
- First-class supplier/vendor and purchase-billing modules (beyond adjustment entries).
- Bank reconciliation against bank statements, and cash/bank account balances / daybook.
- Period and financial-year locking and opening-balance carry-forward.
- GST treatment of advances and TDS handling.
- Interest on overdue balances, automated payment reminders, and multi-currency party balances.
