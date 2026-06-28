# Feature Specification: Bank Reconciliation

**Feature Branch**: `claude/bank-reconciliation-spec-jipccu` (spec dir `024-bank-reconciliation`)
**Created**: 2026-06-28
**Status**: Draft
**Input**: User description: "Bank reconciliation — import bank statements and auto-match the bank's credits and debits against the payments and receipts the business has already recorded, so the owner can see which bank transactions correspond to which recorded payment, deal with the exceptions (bank charges, interest, bounced cheques, unrecorded receipts), and trust that the books agree with the bank for each account."

## User Scenarios & Testing *(mandatory)*

A business owner records receipts and payments against parties day to day, but the only authoritative
record of money actually moving is the bank statement. Reconciliation is the act of proving the two
agree: every credit on the statement should correspond to a recorded receipt, every debit to a recorded
payment, and any difference must be explainable (a bank charge, interest, an uncleared cheque, a bounce,
or something simply not yet entered in the books). This feature lets the owner upload a statement, have
the obvious matches found automatically, and resolve only the exceptions.

### User Story 1 - Import a bank statement for an account (Priority: P1)

The owner exports a statement from their bank (a spreadsheet/CSV download is the everyday case) and
uploads it into the app against the relevant bank account. The system reads each line — date, amount,
whether it is a credit or debit, the narration/description, and any reference number — and presents the
imported transactions, ready to be matched.

**Why this priority**: Nothing in this feature is possible until bank transactions are in the system.
Import is the foundation of every later step and the first thing the owner does each cycle.

**Independent Test**: With a configured bank account, upload a statement file; confirm every line is
read with the correct date, amount, credit/debit direction and narration, and the count of imported
transactions matches the file.

**Acceptance Scenarios**:

1. **Given** a bank account is set up, **When** the owner uploads a statement file for it, **Then** each
   transaction line appears with its date, amount, credit/debit direction and description, and a summary
   shows how many lines were imported.
2. **Given** a statement was already imported, **When** the owner re-uploads a file whose date range
   overlaps the previous one, **Then** the already-known transactions are recognised and not duplicated,
   and the summary reports how many were newly added versus skipped.
3. **Given** a statement file that cannot be read (wrong layout, corrupt, or empty), **When** the owner
   uploads it, **Then** the import is rejected with a clear message and no partial/garbled data is saved.
4. **Given** a statement that includes a running balance column, **When** it is imported, **Then** the
   system verifies the line-to-line balance continuity and warns if the file appears truncated or out of
   order.

---

### User Story 2 - Auto-match bank transactions to recorded payments and review the rest (Priority: P1)

After import, the system compares each bank transaction against the receipts and payments the owner has
already recorded and proposes matches. Transactions it is confident about (e.g. the bank reference/UTR
equals a recorded receipt's reference, or the amount and date line up with exactly one recorded entry)
are matched automatically; everything uncertain is presented as a suggestion the owner can confirm or
reject. The owner can also match transactions by hand.

**Why this priority**: Auto-matching is the time-saving core of reconciliation — it turns hours of
manual ticking into reviewing a short list of exceptions. Without it, importing is just data entry.

**Independent Test**: With recorded receipts/payments and an imported statement containing some lines
that clearly correspond to them, run matching; confirm the obvious lines are matched automatically with
a confidence indication, ambiguous lines appear as suggestions, and the owner can confirm, reject, or
manually link a transaction to a recorded entry.

**Acceptance Scenarios**:

1. **Given** an imported credit whose reference number equals a recorded receipt's reference and amount,
   **When** matching runs, **Then** the two are matched automatically and flagged as a high-confidence
   (exact) match.
2. **Given** an imported credit whose amount and date match exactly one recorded receipt but with no
   shared reference, **When** matching runs, **Then** it is proposed as a high-confidence suggestion for
   the owner to confirm.
3. **Given** an imported credit whose amount matches several recorded receipts, **When** matching runs,
   **Then** the candidates are presented for the owner to choose, and none is auto-confirmed.
4. **Given** a suggested match, **When** the owner rejects it, **Then** the bank transaction returns to
   unmatched and the recorded entry is untouched; **When** the owner confirms it, **Then** the recorded
   entry is marked as bank-reconciled and shows the linked bank transaction.
5. **Given** an unmatched bank transaction, **When** the owner manually links it to a recorded entry,
   **Then** the pair is matched as a manual match and recorded as confirmed by that user.
6. **Given** a confirmed match, **When** the owner later realises it is wrong, **Then** they can undo it,
   returning both the bank transaction and the recorded entry to their pre-match state.

---

### User Story 3 - See the reconciliation status and exceptions for an account (Priority: P2)

For a chosen account and period, the owner sees a reconciliation view that separates the work into clear
buckets: transactions matched, bank transactions with no corresponding entry in the books (e.g. bank
charges, interest, an unrecorded deposit), and recorded entries with no corresponding bank transaction
(e.g. an uncleared cheque still in transit). The view ties out: opening balance plus the net of all
transactions equals the statement's closing balance, so the owner knows the account is fully explained.

**Why this priority**: The value of reconciliation is the exceptions — what is on the bank but not the
books, and vice versa. This view is how the owner closes the period with confidence; it depends on
import and matching existing first, hence P2.

**Independent Test**: For an account with a mix of matched, bank-only and books-only transactions, open
the reconciliation view; confirm the three buckets are correct, each unmatched item is listed with its
details, and the displayed tie-out (opening + net movement = closing) is correct.

**Acceptance Scenarios**:

1. **Given** an account with matched and unmatched transactions, **When** the owner opens the
   reconciliation view for a period, **Then** matched, bank-only, and books-only items are shown in
   separate buckets with counts and totals.
2. **Given** a bank-only transaction that is a genuine charge or unrecorded receipt, **When** the owner
   chooses to record it from that line, **Then** a corresponding receipt/payment is created in the books
   and the bank transaction becomes matched to it.
3. **Given** all bank transactions for a period are matched or accounted for, **When** the owner views
   the reconciliation, **Then** the tie-out shows opening balance + net movement = statement closing
   balance, confirming the account is fully reconciled.
4. **Given** a books-only entry (an uncleared cheque), **When** the owner views the reconciliation,
   **Then** it is listed as informational/in-transit and excluded from the bank side of the tie-out.

---

### User Story 4 - Reconcile bounced / returned payments (Priority: P2)

A receipt the owner previously recorded (e.g. a cheque deposit) is returned by the bank and appears on
the statement as a reversing debit, often flagged in the narration (cheque return / NACH bounce). The
owner matches that debit to the original receipt; the system reverses the receipt's effect so the
party's outstanding amount is correctly restored, and the bank's return-charge debit is recorded as a
bank charge.

**Why this priority**: Bounces materially change what a party owes — leaving them unreconciled
overstates collections. They are a real, recurring reconciliation case but less frequent than ordinary
matching, hence P2.

**Independent Test**: With a previously recorded receipt and an imported reversing debit referencing it,
match them; confirm the original receipt is marked bounced/reversed, the party's outstanding amount is
restored to its pre-receipt value, and any associated return charge is captured separately.

**Acceptance Scenarios**:

1. **Given** a recorded cheque receipt and an imported debit that reverses it (matching amount, narration
   indicating a return), **When** the owner reconciles it as a return, **Then** the original receipt is
   marked bounced and the party's outstanding amount is restored.
2. **Given** the bank also charged a return fee as a separate debit, **When** reconciling, **Then** that
   fee line is recorded as a bank charge and is not treated as a customer payment.

---

### User Story 5 - Manage multiple bank accounts and statement formats (Priority: P3)

The business runs more than one account (e.g. a current account, a UPI/settlement account, an
overdraft). The owner configures each account once and reconciles each independently. Beyond everyday
CSV exports, the owner can also import the standardised bank-statement formats their bank provides
(e.g. MT940) and, where available, pull statements automatically through an account-aggregator
connection, with all formats producing the same reviewable transactions.

**Why this priority**: Multi-account is essential for many businesses, but a single account already
delivers a working product; additional formats and automated retrieval are convenience that broadens
reach rather than enabling the core flow, hence P3.

**Independent Test**: Configure two accounts; import a statement into each; confirm transactions, matches
and reconciliation are scoped per account and never mix. Import a non-CSV format file and confirm it
yields the same canonical transactions as the CSV path.

**Acceptance Scenarios**:

1. **Given** two configured accounts, **When** the owner imports a statement into one, **Then** its
   transactions, matches and reconciliation view belong only to that account.
2. **Given** a statement in a supported standardised format, **When** the owner imports it, **Then** its
   transactions appear identically to a CSV import and are matched the same way.
3. **Given** a recurring CSV layout for a specific bank, **When** the owner imports another file from the
   same bank, **Then** the saved column layout is reused so the owner need not re-describe the columns.

---

### Edge Cases

- **Overlapping re-imports**: re-uploading a file covering dates already imported must add only genuinely
  new transactions and never duplicate existing ones.
- **Ambiguous matches**: when one bank amount/date matches several recorded entries (or one recorded
  entry matches several bank lines), the system must not silently auto-pick — it surfaces the candidates.
- **One-to-many / many-to-one**: a single bank credit may settle several recorded receipts (a batched
  settlement), and several bank lines may correspond to one recorded entry; reconciliation must support
  these groupings.
- **Rounding / fee differences**: amounts that differ by a small tolerance (rounding, a deducted charge)
  should still be matchable, within an allowed tolerance, rather than forced to be exact.
- **Date shift**: a transaction recorded on one date but cleared by the bank a few days later must still
  match within an allowed date window.
- **Wrong auto-match**: a confirmed or auto-match later found to be incorrect must be reversible without
  altering the bank's transaction facts.
- **Unreadable / partial file**: a corrupt, empty, or wrongly-formatted file must be rejected cleanly
  with no partial data persisted.
- **Foreign / unknown lines**: bank charges, interest, taxes, and inter-account transfers that have no
  recorded counterpart must be visible as exceptions, not lost.
- **Account isolation**: transactions and matches must never leak across bank accounts or across
  workspaces.

## Clarifications

### Session 2026-06-28

- Q: Should auto-reconciliation matching use the app's on-device/offline model, or stay a deterministic server-side engine? → A: Server-side only — auto-matching is a deterministic engine run on the server; the app's on-device/offline model is not used for reconciliation.

## Requirements *(mandatory)*

### Functional Requirements

**Bank accounts**

- **FR-001**: The system MUST let the owner configure one or more bank accounts (bank name, masked
  account number, account type, opening balance, currency) and reconcile each independently.
- **FR-002**: Every imported transaction, match, and reconciliation view MUST be scoped to a single bank
  account and to the owner's workspace, with no cross-account or cross-workspace leakage.

**Statement import**

- **FR-003**: The system MUST let the owner upload a bank statement file against a chosen account and
  read each transaction's date, amount, credit/debit direction, narration/description, and any reference
  number (UTR / cheque number / reference) into a reviewable list.
- **FR-004**: The system MUST support importing the everyday spreadsheet/CSV export as a minimum, and
  MUST be able to accommodate additional standardised formats (e.g. MT940) and an account-aggregator
  source that all produce the same canonical transactions.
- **FR-005**: The system MUST treat re-imports idempotently — transactions already present (by their
  bank-provided facts) MUST NOT be duplicated, and the import summary MUST report newly added versus
  skipped counts.
- **FR-006**: The system MUST reject an unreadable, empty, or malformed file with a clear message and
  persist no partial data.
- **FR-007**: Where the statement provides a running balance, the system MUST validate line-to-line
  balance continuity and flag gaps or apparent truncation.
- **FR-008**: Imported transactions MUST be preserved as immutable facts; reconciliation decisions MUST
  be recorded separately and never overwrite what the bank reported.
- **FR-009**: For recurring imports of the same bank's layout, the system SHOULD let a saved column
  mapping be reused so the owner need not re-describe the columns each time.

**Matching**

- **FR-010**: The system MUST automatically compare each imported transaction against the owner's
  recorded receipts and payments and propose matches. This comparison MUST be performed server-side by a
  deterministic matching engine; the app's on-device/offline model MUST NOT be used to generate matches.
- **FR-011**: The system MUST assign each proposed match a confidence level derived from layered signals:
  an exact reference/UTR match is highest; an exact amount and direction within an allowed date window is
  high; a close amount with a narration that resembles the party name is medium.
- **FR-012**: The system MUST auto-confirm only high-confidence matches that have a single unambiguous
  candidate, and MUST present all other transactions as suggestions for manual review.
- **FR-013**: The system MUST let the owner confirm, reject, or manually create a match, and MUST let a
  previously confirmed or auto-created match be undone, returning both sides to their pre-match state.
- **FR-014**: The system MUST support one-to-many and many-to-one matches (one bank line covering several
  recorded entries, and vice versa).
- **FR-015**: The system MUST apply configurable tolerances — an amount tolerance for rounding/fee
  differences and a date window for clearing delay — when proposing amount/date matches.
- **FR-016**: Confirming a match MUST mark the corresponding recorded receipt/payment as bank-reconciled
  and link it to the bank transaction, WITHOUT creating or altering any underlying financial ledger
  entry beyond that reconciliation status.
- **FR-017**: The system MUST record, for each confirmed match, whether it was automatic or manual, who
  confirmed it, and when.

**Reconciliation view & exceptions**

- **FR-018**: The system MUST present, per account and period, three buckets: matched transactions, bank
  transactions with no recorded counterpart (bank-only), and recorded entries with no bank transaction
  (books-only / in-transit), each with counts and totals.
- **FR-019**: The system MUST let the owner record a receipt/payment directly from a bank-only line
  (e.g. a bank charge, interest, or an unrecorded deposit) and have that line become matched to the
  newly created entry.
- **FR-020**: The system MUST display a tie-out for the period: opening balance plus the net of matched
  and accounted-for transactions equals the statement's closing balance, signalling a fully reconciled
  account.

**Returns / bounces**

- **FR-021**: The system MUST let the owner reconcile a statement debit that reverses an earlier receipt
  (cheque return / bounce) against the original receipt, so the original receipt is marked
  bounced/reversed and the affected party's outstanding amount is restored.
- **FR-022**: The system MUST allow an associated return charge to be recorded as a bank charge, distinct
  from any customer payment.

**Configuration**

- **FR-023**: The owner MUST be able to enable the feature per workspace and tune reconciliation
  behaviour: the minimum confidence required for auto-matching, the matching date window, the amount
  tolerance, a default import layout, and the narration patterns that indicate a return/bounce.

### Key Entities *(include if feature involves data)*

- **Bank Account**: A bank account the business holds — bank name, masked account number, identifier
  (e.g. IFSC), account type, opening balance, currency. The unit of reconciliation; everything is scoped
  to one.
- **Statement Import**: A single uploaded statement file for an account — source format, the period it
  covers, and a summary of how many transactions were added versus skipped (deduplicated). Provides the
  audit trail of what was loaded and when.
- **Bank Transaction (Statement Line)**: One immutable line from a statement — value date, transaction
  date, amount, credit/debit direction, narration, parsed reference (UTR/cheque/reference number),
  running balance, the account it belongs to, the import it came from, and its current match status.
- **Match**: The link between one or more bank transactions and one or more recorded receipts/payments —
  amount, confidence level, type (automatic/manual), status (suggested → confirmed → rejected), and who
  matched it and when. Carries the reconciliation decision without altering bank facts or the financial
  ledger.
- **Recorded Receipt / Payment (referenced)**: An existing payment record the business already keeps
  (from the payment & collection feature). Reconciliation links bank transactions to these and marks them
  bank-reconciled on confirmation; it does not create or modify their financial ledger postings (except
  through the existing bounce/reversal flow for returns).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For a typical monthly statement (hundreds to a few thousand lines), the owner can upload it
  and see imported transactions ready for review within seconds.
- **SC-002**: At least 80% of bank transactions that have a corresponding recorded entry are matched
  automatically (without manual intervention) for statements where reference numbers/UTRs are present.
- **SC-003**: Auto-matching produces zero silent incorrect matches in acceptance testing — every
  ambiguous case is surfaced for review rather than auto-confirmed.
- **SC-004**: Re-importing an overlapping statement adds zero duplicate transactions.
- **SC-005**: For any reconciled period, the displayed tie-out (opening balance + net movement = closing
  balance) is exact for every test account.
- **SC-006**: An owner can reconcile a typical monthly statement — reviewing only the exceptions — in
  under 10 minutes, versus manual line-by-line ticking.
- **SC-007**: Reconciling a bounced receipt restores the affected party's outstanding amount to exactly
  its pre-receipt value in 100% of test cases.
- **SC-008**: Transactions, matches and reconciliation views never appear under the wrong account or
  workspace in any test scenario.

## Assumptions

- **Recorded payments already exist**: This feature reconciles against receipts/payments captured by the
  existing payment & collection capability; it does not introduce a new way to record payments (beyond
  the convenience of spawning one from an unmatched bank line).
- **Single source of truth for the ledger**: Reconciliation annotates recorded payments as
  bank-reconciled and routes returns through the existing bounce/reversal flow; it never authors new
  financial ledger entries itself, preserving the books' existing balancing guarantees.
- **Import and matching happen server-side**: Reading statement files and searching the full set of
  recorded payments for candidates is performed centrally; the mobile experience is configuring accounts,
  uploading a file, and reviewing/confirming results. Reconciliation is not expected to work offline on a
  partial copy of the data. Auto-matching uses a deterministic server-side matching engine; the app's
  on-device/offline model is not used for reconciliation.
- **Statement upload is online**: Because statement files are binary/large, uploading a statement is an
  online action, distinct from routine offline-first data entry.
- **Amounts in minor units**: Money is compared in exact minor units (paise) with a configurable
  tolerance for rounding/fee differences; no floating-point comparison is used.
- **Bounce detection from narration**: Returns/bounces are identified primarily from configurable
  narration patterns, since everyday CSV exports rarely carry a structured return code.
- **Default tuning**: Sensible defaults apply out of the box — auto-match requires high confidence, a few
  days' date window, zero amount tolerance — and are adjustable per workspace.
- **Currency**: Each account reconciles in its own currency; cross-currency conversion is out of scope.
