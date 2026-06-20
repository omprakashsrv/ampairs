# payment module

Payment & Collection — a **subsidiary party ledger** (spec `013-payment-collection`). On top of
invoices/orders, the owner manages money owed per party: an opening balance, movements from sales /
returns / adjustments, payments in many modes, and an always-correct **closing balance** (receivable
or payable). A party is an existing `customer` (referenced by `customer.uid`).

## Core model

Receivable-positive sign convention. A single signed `LedgerEntry` per movement is the sole driver
of the balance:

```
signedAmount = if (direction == DR) +amount else -amount
closing      = openingSigned + Σ(active LedgerEntry.signedAmount)
```

The opening balance is a `PartyBalance` attribute (`openingBalance`, `openingDirection`,
`openingAsOf`) folded in as `openingSigned` — never a persisted ledger row. `cachedClosingBalance`
is denormalized and fully recomputable; `BalanceService.recompute(partyUid)` reproduces it exactly
and `recomputeWithTieOut` asserts `opening + ΣDr − ΣCr == closing`.

## REST endpoints

Base path `/payment/v1/**`. All return `ApiResponse<T>`; workspace-scoped via `X-Workspace-ID`.

### Canonical `/sync` resources (pull `GET` + push `POST`, UID-keyed bulk upsert, includes soft-deleted)

| Resource | Path |
|----------|------|
| Payment vouchers | `/payment/v1/vouchers/sync` |
| Payment allocations | `/payment/v1/allocations/sync` |
| Ledger entries | `/payment/v1/ledger-entries/sync` |
| Party balances (push = opening only) | `/payment/v1/party-balances/sync` |
| Adjustments | `/payment/v1/adjustments/sync` |

### Action endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/payment/v1/parties/{uid}/statement?from&to` | Running-balance statement (last line == closing) |
| GET | `/payment/v1/parties/{uid}/open-bills` | Outstanding per bill + due date + aging bucket |
| GET | `/payment/v1/aging?as_of` | Totals receivable/payable + aging buckets + over-credit-limit |
| POST | `/payment/v1/parties/{uid}/recompute-balance` | Tie-out integrity guard |
| POST | `/payment/v1/vouchers/{uid}/bounce` | Status BOUNCED + contra reversal entry |
| POST | `/payment/v1/vouchers/{uid}/clear` | Status CLEARED |

## Key entities

- **PartyBalance** — one per party; opening fields + cached signed closing balance.
- **LedgerEntry** — the posting. `amount ≥ 0`; sign from `direction`. Deterministic uid
  `LDG_<sourceUid>` for document-derived rows. Corrections via edit (same uid) or reversal
  (`reversalOf`) + soft-delete — never a hard delete.
- **PaymentVoucher** — receipt (RECEIVED) / payment-out (PAID); posts `PAYMENT_IN` CR / `PAYMENT_OUT`
  DR (`LDG_<voucher.uid>`); mode + instrument details; `clearanceStatus`
  (PENDING→CLEARED/BOUNCED/CANCELLED).
- **PaymentAllocation** — receipt↔bill matching; drives open-bills/aging only, never the balance;
  Σ per voucher ≤ voucher total.
- **AdjustmentVoucher** — non-payment movement; `adjustmentType → EntryType + Direction`.

## Lifecycle & integrity

- **Invoice → ledger**: `invoice` publishes `InvoiceFinalizedEvent` (reaches INVOICED) /
  `InvoiceCancelledEvent` (leaves INVOICED). `InvoiceLedgerListener` posts/reverses
  `LDG_<invoice.uid>` (SALES_INVOICE, DR, `totalCost`). Drafts never post.
- **Edit / cancel a voucher**: re-posts/updates its ledger entry; soft-delete reverses it and drops
  its allocations' effect (audit retained).
- **Bounce**: posts a contra entry restoring the dues; original + reversal both remain visible.
- **Allocations** are orthogonal to the balance (open-bills/aging only).

## Cross-module (public service interfaces only)

`CustomerService` (credit days/limit; mirror `cachedClosingBalance → Customer.outstandingAmount`),
`InvoiceService` (finalized invoices for open-bills), `SettingService`
(`PaymentSettingDefinitions` keys, gated by installed module `payment-collection`),
`SequenceCounterService` (gap-free voucher numbers: RCP/PAY/CRN/DBN/ADJ).

## Settings (`payment` module, gated by `payment-collection`)

`enabled_payment_modes`, `default_payment_mode`, `cheque_requires_clearance`,
`allow_on_account_receipts`, `enforce_credit_limit`, `aging_buckets`.

## Migrations

`V1.0.93` in both `mysql/` and `postgresql/` — `party_balance`, `ledger_entry`, `payment_voucher`,
`payment_allocation`, `adjustment_voucher`; money `DECIMAL(19,4)`, timestamps `TIMESTAMP`/`TIMESTAMPTZ`,
`owner_id` + the unique/indexes per the data model.
