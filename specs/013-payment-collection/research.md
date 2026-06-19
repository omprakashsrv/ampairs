# Phase 0 Research — Payment & Collection (Party Ledger)

All Technical Context unknowns are resolved below. Each item: **Decision · Rationale · Alternatives
considered**. These supersede the inline assumptions in `spec.md` with concrete technical choices.

---

## R1. Ledger model — subsidiary ledger vs full double-entry GL

- **Decision**: Subsidiary **party ledger** — one *signed* posting (`LedgerEntry`) per movement, from
  the business's books. No chart of accounts / journal–GL split in Phase 1.
- **Rationale**: The feature's job is "what does each party owe / are we owed." A single signed running
  balance per party answers that directly and is what comparable SMB tools (Vyapar, Khatabook, Tally
  party ledger) expose. A full GL can be layered later by mapping entry types to accounts without
  reshaping the party-ledger data.
- **Alternatives considered**: Full double-entry GL (rejected for Phase 1 — large surface, needs a chart
  of accounts and contra postings the user didn't ask for); invoice-embedded `amountPaid`/`balanceDue`
  fields (rejected — cannot express advances, on-account, opening balances, or one receipt across many
  bills).

## R2. Sign convention

- **Decision**: **Receivable positive.** Party balance is signed: `> 0` ⇒ party owes us (Dr / "to
  receive"), `< 0` ⇒ we owe party (Cr / "to pay"). Store `direction` (DR/CR) + positive `amount` on each
  entry; signed contribution = `DR ⇒ +amount`, `CR ⇒ −amount`.
- **Rationale**: One convention everywhere removes ambiguity; a single signed `PartyBalance` row
  naturally handles a party that is both customer and supplier (contra). Customers (debtors) are the
  common case and read as positive, which is intuitive for a collection app.
- **Alternatives considered**: Separate receivable/payable columns (rejected — duplicates state, breaks
  on contra parties); magnitude-only with a type flag (rejected — arithmetic and tie-out get error-prone).

## R3. Balance computation — derived vs stored

- **Decision**: **Derived and cached.** `closing = openingSigned + Σ active signed LedgerEntry`. Cache on
  `PartyBalance.cachedClosingBalance`, recomputed on every posting and **fully recomputable** from scratch
  (recompute service + `POST .../recompute-balance` endpoint).
- **Rationale**: The total is order-independent (pure sum), so backdated/offline/out-of-order entries
  never corrupt it — they just trigger a recompute. The cache keeps list/detail views fast. Invariant
  `opening + ΣDr − ΣCr = closing` is the module's regression guard (SC-002/006).
- **Alternatives considered**: Stored running balance per row as source of truth (rejected — backdated
  inserts force expensive re-sequencing and are fragile offline). Pure on-the-fly compute with no cache
  (rejected — customer-list balance badges would be too slow at scale).

## R4. Posting authority across offline clients (the crux)

- **Decision**: **The side that authors a source document also posts its `LedgerEntry`**, in the same
  local transaction. `LedgerEntry` is a first-class **synced entity**. The backend recomputes
  `PartyBalance` authoritatively and **backfills** a missing entry if an older/lean client omits it.
  Document-derived entries use a **deterministic uid** (e.g. `LDG_<sourceUid>`) so no two clients can
  create a duplicate.
- **Rationale**: Only model that yields correct **offline** balances (recording a sale then a receipt
  against it while offline must update the balance immediately). It fits the existing offline-sync
  architecture — `LedgerEntry` is just another `SyncEntity` with `dependsOn`. Because balance is a pure
  sum of data rows, the *posting rule* (what entry a doc creates) runs once per document; the *balance
  formula* is identical on every platform, so there's no logic drift.
- **Alternatives considered**: Backend-only posting with clients pulling entries (rejected — a freshly
  created offline invoice/receipt wouldn't affect the on-device balance until the next sync). Each client
  recomputing the whole ledger from documents (rejected — two posting implementations → drift; harder to
  audit than persisted entries).

## R5. Money representation & rounding

- **Decision**: Backend **`BigDecimal`, scale 4, `DECIMAL(19,4)`**; mobile **`Long` minor units (paise)**
  via a `Money` value type. Round **half-up to 2 dp** at settlement; an explicit `ROUND_OFF` ledger line
  absorbs residue so every voucher foots. Convert an invoice's existing `Double` total to exact money
  **once** at posting time.
- **Rationale**: `Double` cannot foot to zero across many add/subtract operations — a non-reconciling
  ledger is worthless (SC-006). Integer minor units are exact and KMP-safe in `commonMain` (no stdlib
  `BigDecimal` on Native). Single conversion from the legacy `Double` total avoids accumulation error.
- **Alternatives considered**: Reuse `Double` like invoices/orders (rejected — precision). Adopt a KMP
  `BigDecimal` library (`ionspin/bignum`) on mobile (viable; rejected for Phase 1 to avoid a new
  dependency — minor units cover fixed-scale currency exactly). Scale 2 in DB (rejected — keep 4 for
  unit-price/tax headroom and future use).

## R6. Purchases & returns scope (Phase 1)

- **Decision**: **Sales** come from finalized invoices. **Purchases, purchase-returns, sales-returns/
  credit notes, settlement discounts and write-offs** are entered as **`AdjustmentVoucher`s** that post a
  signed `LedgerEntry`. No first-class vendor/purchase module in Phase 1.
- **Rationale**: The app has no supplier/vendor, purchase-billing, or credit-note module today.
  Adjustment vouchers satisfy the brief's "purchases and returns" effect on the party balance now;
  first-class purchase/vendor billing is a clean Phase 2 addition that can reuse the same ledger.
- **Alternatives considered**: Build vendor + purchase modules now (rejected — large scope creep beyond
  the collection ask). Ignore purchases/returns (rejected — explicitly in the brief).

## R7. Cheque / online realisation

- **Decision**: Payments carry `clearanceStatus` ∈ {`PENDING`,`CLEARED`,`BOUNCED`,`CANCELLED`}. Default
  `CLEARED` for instantly-settled modes (cash, UPI/IMPS when configured); `PENDING` for cheque (and other
  modes per the `chequeRequiresClearance` workspace setting). A **bounce posts a reversal** entry
  (`reversalOf`), restoring the receivable; both rows are retained.
- **Rationale**: Cheques/transfers aren't instant; treating a deposited cheque as realised overstates
  collection, and a bounce must restore the dues without erasing history (audit, FR-021/023).
- **Alternatives considered**: Treat every receipt as instantly cleared (rejected — wrong for cheques).
  Delete a bounced receipt (rejected — destroys the audit trail). Full bank reconciliation now (deferred
  to Phase 2).

## R8. Allocation vs balance (separation of concerns)

- **Decision**: `PaymentAllocation` (receipt ↔ bill) drives **only** open-bills and **aging**. It never
  changes the party total. The party total is the sum of `LedgerEntry` rows; a receipt's `LedgerEntry`
  exists regardless of whether it's allocated.
- **Rationale**: Conflating matching with the balance is the classic way these ledgers stop footing.
  Keeping them orthogonal means an unallocated (on-account) receipt still correctly reduces the balance.
- **Alternatives considered**: Derive balance from per-bill outstanding (rejected — can't represent
  opening balances or on-account advances; breaks the moment a payment isn't bill-linked).

## R9. Invoice → ledger integration (module boundary)

- **Decision**: The `invoice` module publishes an **`InvoiceFinalizedEvent`** (Spring
  `ApplicationEventPublisher`) when an invoice reaches `INVOICED`; the `payment` module listens and posts
  the `SALES_INVOICE` entry. On mobile, the invoice-authoring client writes the deterministic ledger
  entry in the same Room transaction as finalize. **DRAFT/NEW invoices never post** (FR-013).
- **Rationale**: Keeps modules decoupled (Principle IX) — payment depends on an event/public contract,
  not on invoice repositories. Mirrors how the codebase already does cross-module work.
- **Alternatives considered**: Payment module reading invoice tables directly (rejected — violates module
  boundaries). Synchronous service call from invoice into payment on save (rejected — couples write paths;
  event is cleaner and async-friendly).

## R10. Voucher numbering

- **Decision**: Use the existing `sequence` module / `SequenceNumberProvider` for gap-free,
  human-readable numbers per series (`RCP` receipts, `PAY` payments-out, `CRN`/`DBN` notes, `ADJ`
  adjustments), with the same offline local-fallback pattern invoices/orders already use.
- **Rationale**: Reuses a proven, audited numbering mechanism (FR-024); consistent with invoice series.
- **Alternatives considered**: Per-device random/UID-only numbering (rejected — not gap-free/auditable).

## R11. Settings

- **Decision**: Reuse the `setting` module (`StoreSetting`) via a `PaymentSettingDefinitions`
  provider, gated by installed module `payment-collection`: `enabledPaymentModes`, `defaultPaymentMode`,
  `chequeRequiresClearance`, `allowOnAccountReceipts`, `enforceCreditLimit`, `agingBuckets`.
- **Rationale**: Matches how invoice/order expose toggles; no new settings infrastructure (FR-027).
- **Alternatives considered**: Hardcode modes/buckets (rejected — workspaces differ). New settings store
  (rejected — duplicates `StoreSetting`).

## R12. Relationship to existing `Customer.outstandingAmount`

- **Decision**: `PartyBalance` (payment module) is authoritative for the closing balance. Optionally
  mirror `cachedClosingBalance` back to `Customer.outstandingAmount` via the same event path for backward
  compatibility, but do not read it as truth. (Open item flagged for confirmation.)
- **Rationale**: Keeps the ledger self-contained and module-bounded; `outstandingAmount` currently isn't
  transactionally maintained, so it can't be trusted as-is.
- **Alternatives considered**: Add balance fields to `Customer` (rejected — crosses module boundary,
  bloats the customer context). Drop `outstandingAmount` immediately (deferred — avoid breaking any
  current readers until confirmed).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Ledger model | Subsidiary party ledger (R1) |
| Sign convention | Receivable-positive, signed (R2) |
| Balance source of truth | Derived + cached, recomputable (R3) |
| Offline posting authority | Document-author posts `LedgerEntry`; backend reconciles (R4) |
| Money type / rounding | `DECIMAL(19,4)` / minor units, half-up, round-off line (R5) |
| Purchases/returns scope | Adjustment vouchers in Phase 1 (R6) |
| Cheque realisation | `clearanceStatus` + reversal on bounce (R7) |
| Allocation vs balance | Orthogonal; allocation = aging only (R8) |
| Invoice integration | `InvoiceFinalizedEvent` + same-txn mobile posting (R9) |
| Numbering | `SequenceNumberProvider`, per series (R10) |
| Settings | `StoreSetting` + `PaymentSettingDefinitions` (R11) |
| Customer.outstandingAmount | PartyBalance authoritative; optional mirror (R12) |
