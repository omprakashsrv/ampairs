# payment module

Payment & Collection — a **subsidiary party ledger** (spec 013). Opening balance + sales/returns/
purchases + payments → a signed running closing balance per party (a party = an existing `customer`).

## Sign convention
Receivable positive. `signedAmount = if (direction == DR) +amount else -amount`.
`closing = openingSigned + Σ active LedgerEntry.signedAmount`. Opening balance is a `PartyBalance`
attribute (NOT a ledger entry). `BalanceService` is the sole owner of the foot-to-zero invariant
(`opening + ΣDr − ΣCr == closing`).

## Key entities
- `PartyBalance` — one per party; opening fields + cached signed `cachedClosingBalance` (recomputable)
- `LedgerEntry` — the posting, sole driver of the balance; deterministic uid `LDG_<sourceUid>` for
  document-derived rows; soft-delete + reversal, never hard-delete
- `PaymentVoucher` — money movement (RECEIVED/PAID); posts `PAYMENT_IN` CR / `PAYMENT_OUT` DR;
  `clearanceStatus` lifecycle (PENDING→CLEARED/BOUNCED/CANCELLED)
- `PaymentAllocation` — receipt↔bill matching; drives open-bills/aging only, **never** the balance
- `AdjustmentVoucher` — returns/notes/write-offs; maps `adjustmentType → EntryType + Direction`

## Base path
`/payment/v1/**`

## Sync resources (canonical `/sync` contract)
`vouchers`, `allocations`, `ledger-entries`, `party-balances`, `adjustments`
(each `GET` + `POST .../sync`).

## Action endpoints
- `GET /parties/{uid}/statement?from&to` — running-balance statement (last line == closing)
- `GET /parties/{uid}/open-bills` — outstanding = bill total − Σ allocations; due date + aging
- `GET /aging?as_of` — totals + aging buckets + parties over credit limit
- `POST /parties/{uid}/recompute-balance` — tie-out guard (FR-022 / SC-002)
- `POST /vouchers/{uid}/bounce` · `POST /vouchers/{uid}/clear`

## Invoice integration
The `invoice` module publishes `InvoiceFinalizedEvent` (INVOICED) / `InvoiceCancelledEvent`
(leaves INVOICED). `InvoiceLedgerListener` posts/reverses `LDG_<invoice.uid>` (SALES_INVOICE, DR,
totalCost). Drafts never post (FR-013/014).

## Cross-module
- `CustomerService` — read credit days/limit; mirror `cachedClosingBalance → Customer.outstandingAmount` (R12)
- `InvoiceService` — read finalized invoices for open-bills
- `SettingService` — `aging_buckets` etc. (settings declared by `PaymentSettingDefinitions`, gated by
  installed module `payment-collection`)
- `SequenceCounterService` — gap-free voucher numbers (series RCP/PAY/CRN/DBN/ADJ)

## Voucher numbering
Client-supplied `voucher_no` is honored as-is (offline); a blank number is filled from the server
counter via `VoucherNumberService`.

## Migrations
`V1.0.93` (both `mysql/` and `postgresql/`) — `party_balance`, `ledger_entry`, `payment_voucher`,
`payment_allocation`, `adjustment_voucher`; money `DECIMAL(19,4)`, timestamps `TIMESTAMP(TZ)`.

## Full docs
`docs/modules/payment.md` · spec `specs/013-payment-collection/`
