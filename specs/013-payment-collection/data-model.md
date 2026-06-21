# Phase 1 Data Model — Payment & Collection (Party Ledger)

Backend entities extend `OwnableBaseDomain` → inherit `id`, `uid`, `ownerId` (`@TenantId`), `refId`,
`createdAt`, `updatedAt` (`Instant`). Money = `BigDecimal` ⇄ `DECIMAL(19,4)`. Mobile mirrors each as a
Room entity with money stored as `Long` minor units (paise) and `Instant` as ISO-8601 string + epoch
millis, plus `synced`/`active` sync flags. All entities are workspace-scoped and syncable.

A **party** in Phase 1 is an existing `customer` (referenced by `customer.uid`); no new party table.

---

## Enumerations

```
EntryType        = SALES_INVOICE | SALES_RETURN | CREDIT_NOTE |
                   PAYMENT_IN | PAYMENT_OUT | PURCHASE_BILL | PURCHASE_RETURN | DEBIT_NOTE |
                   DISCOUNT_ALLOWED | WRITE_OFF | ROUND_OFF | ADJUSTMENT
Direction        = DR | CR                  # DR ⇒ +amount (receivable), CR ⇒ −amount
PaymentDirection = RECEIVED | PAID          # money in vs money out
PaymentMode      = CASH | CHEQUE | UPI | NEFT | RTGS | IMPS | NET_BANKING | CARD |
                   BANK_TRANSFER | WALLET | OTHER
ClearanceStatus  = PENDING | CLEARED | BOUNCED | CANCELLED
AdjustmentType   = SALES_RETURN | CREDIT_NOTE | PURCHASE_BILL |
                   PURCHASE_RETURN | DEBIT_NOTE | DISCOUNT_ALLOWED | WRITE_OFF | ADJUSTMENT
AllocationTarget = INVOICE | PURCHASE | LEDGER_ENTRY   # Phase 1 uses INVOICE only; PURCHASE/LEDGER_ENTRY are Phase 2
```

> **Opening balance is NOT a `LedgerEntry`.** It is an attribute of `PartyBalance`
> (`openingBalance` + `openingDirection` + `openingAsOf`) and is folded into the balance as
> `openingSigned` by `recompute()`. The party statement renders it as a **synthetic first line** (label
> "Opening Balance"), but no `OPENING_BALANCE` row is ever persisted in `ledger_entry`. This is why
> `OPENING_BALANCE` does not appear in `EntryType`/`AdjustmentType`.

Sign mapping (single source of truth for balance math):
`signedAmount = if (direction == DR) +amount else -amount`. Each `EntryType` has a fixed natural
`Direction` (see `spec.md` §1.2 / `research.md` R2); store both for safety and validate consistency.

---

## Entity: PartyBalance  *(one row per party)*

| Field | Type | Rules |
|---|---|---|
| `partyUid` | String(40) | = `customer.uid`; **unique per `(owner_id, party_uid)`** |
| `openingBalance` | BigDecimal(19,4) | ≥ 0 |
| `openingDirection` | Direction | DR = "to receive", CR = "to pay" |
| `openingAsOf` | Instant | cutover date; defaults to creation |
| `cachedClosingBalance` | BigDecimal(19,4) | **signed**; denormalized; = `recompute(partyUid)` |
| `lastComputedAt` | Instant | set on each recompute |
| `active` | Boolean | soft-delete for sync |

- **Invariant**: `cachedClosingBalance = openingSigned + Σ(active LedgerEntry.signedAmount where partyUid)`.
- **Derivation**: never edited directly except `openingBalance`/`openingDirection`/`openingAsOf`; closing
  is always recomputed.
- Relationships: 1‑to‑many → `LedgerEntry` (by `partyUid`).

## Entity: LedgerEntry  *(the posting — sole driver of balance)*

| Field | Type | Rules |
|---|---|---|
| `partyUid` | String(40) | indexed; required |
| `entryDate` | Instant | statement ordering key |
| `entryType` | EntryType | |
| `direction` | Direction | must match `entryType`'s natural direction |
| `amount` | BigDecimal(19,4) | **≥ 0** (never store negatives) |
| `sourceType` | enum INVOICE/PAYMENT/ADJUSTMENT/PURCHASE/MANUAL | provenance |
| `sourceUid` | String(64) | reference to source doc (no FK) |
| `voucherNo` | String(64) | human-readable (from sequence) |
| `narration` | String(500) | optional |
| `reversalOf` | String(64)? | uid of the entry this reverses |
| `reversed` | Boolean | true once a reversal exists for it |
| `active` | Boolean | soft-delete |

- **Deterministic uid for document-derived entries**: `LDG_<sourceUid>` (1:1 with the invoice/voucher)
  so concurrent clients cannot duplicate; adjustment/manual entries get a normal generated uid.
- **Indexes**: unique(`uid`); (`owner_id`,`party_uid`,`entry_date`); (`owner_id`,`source_type`,`source_uid`).
- **No hard delete**: corrections via edit (same uid) or reversal (`reversalOf`) + soft-delete.

## Entity: PaymentVoucher  *(money movement header)*

| Field | Type | Rules |
|---|---|---|
| `partyUid` | String(40) | required |
| `voucherNo` | String(64) | sequence series `RCP` (in) / `PAY` (out) |
| `voucherDate` | Instant | |
| `direction` | PaymentDirection | RECEIVED / PAID |
| `totalAmount` | BigDecimal(19,4) | > 0 |
| `paymentMode` | PaymentMode | must be in workspace `enabledPaymentModes` |
| `referenceNumber` | String(100)? | UTR / txn id / cheque no (required for non-cash per setting) |
| `instrumentDate` | Instant? | cheque date |
| `bankName` | String(120)? | |
| `clearanceStatus` | ClearanceStatus | default CLEARED (instant modes) / PENDING (cheque) |
| `unallocatedAmount` | BigDecimal(19,4) | = `totalAmount − Σ active allocations`; ≥ 0 |
| `narration` | String(500)? | |
| `active` | Boolean | soft-delete |

- **Posts** exactly one `LedgerEntry` (`PAYMENT_IN` CR when RECEIVED / `PAYMENT_OUT` DR when PAID),
  uid `LDG_<voucher.uid>`, amount = `totalAmount`, `entryDate = voucherDate`.
- `@NamedEntityGraph("PaymentVoucher.withAllocations")` → allocations.
- State machine on `clearanceStatus`:
  `PENDING → CLEARED` (realised) · `PENDING → BOUNCED` (post reversal entry, restore dues) ·
  `PENDING|CLEARED → CANCELLED` (soft-delete + reverse). `BOUNCED`/`CANCELLED` are terminal.

## Entity: PaymentAllocation  *(matching — aging/open-bills only)*

| Field | Type | Rules |
|---|---|---|
| `paymentVoucherUid` | String(64) | required |
| `targetType` | AllocationTarget | INVOICE (Phase 1) / PURCHASE / LEDGER_ENTRY |
| `targetUid` | String(64) | the bill being settled |
| `amount` | BigDecimal(19,4) | > 0; Σ per voucher ≤ `voucher.totalAmount` |
| `active` | Boolean | soft-delete |

- **Does NOT affect the party balance.** Drives bill outstanding (`bill.total − Σ allocations`) and aging.
- Index: (`owner_id`,`target_type`,`target_uid`), (`owner_id`,`payment_voucher_uid`).

## Entity: AdjustmentVoucher  *(non-payment movements)*

| Field | Type | Rules |
|---|---|---|
| `partyUid` | String(40) | required |
| `voucherNo` | String(64) | sequence series `CRN`/`DBN`/`ADJ` |
| `voucherDate` | Instant | |
| `adjustmentType` | AdjustmentType | maps to an `EntryType` + `Direction` |
| `amount` | BigDecimal(19,4) | > 0 |
| `narration` | String(500)? | |
| `sourceRef` | String(64)? | optional linked bill |
| `active` | Boolean | soft-delete |

- **Posts** one `LedgerEntry` of the mapped type (e.g. `SALES_RETURN → CR`, `PURCHASE_BILL → CR`,
  `PURCHASE_RETURN/DEBIT_NOTE → DR`, `WRITE_OFF → CR`), uid `LDG_<adjustment.uid>`.

---

## Relationships (textual ER)

```
customer (1) ─────< PartyBalance (1 per party)
customer.uid ──────< LedgerEntry (many)          [partyUid]
PaymentVoucher (1) ─< PaymentAllocation (many)    [paymentVoucherUid]
PaymentVoucher (1) ─1 LedgerEntry                 [LDG_<voucher.uid>]
AdjustmentVoucher (1)1 LedgerEntry                [LDG_<adjustment.uid>]
invoice (1) ────────1 LedgerEntry (when finalized)[LDG_<invoice.uid>, via InvoiceFinalizedEvent]
PaymentAllocation.targetUid ──→ invoice.uid       [no FK; aging only]
```

## Validation rules (from FRs)

- `amount > 0` on all vouchers; `LedgerEntry.amount ≥ 0` (FR-022 math integrity).
- Σ allocations per voucher ≤ `voucher.totalAmount`; remainder = `unallocatedAmount` (FR-010/011).
- `paymentMode ∈ enabledPaymentModes`; reference required for non-cash when configured (FR-008/009/027).
- Only finalized invoices post `SALES_INVOICE`; drafts excluded (FR-013).
- Bounce/cancel produce reversals; nothing posted is hard-deleted (FR-021/023).
- Recompute endpoint must reproduce `cachedClosingBalance` exactly (FR-022; SC-002/006).

## State transitions

**PaymentVoucher.clearanceStatus**
```
                 ┌─────────► CLEARED ──► (CANCELLED, reverses)
PENDING ─────────┤
                 ├─────────► BOUNCED   (terminal; posts reversal, restores dues)
                 └─────────► CANCELLED (terminal; soft-delete + reverse if it had posted)
CASH/instant mode: created directly in CLEARED.
```

**Invoice → LedgerEntry (provenance)**
```
Invoice DRAFT/NEW  → (no ledger entry)
Invoice INVOICED   → upsert LDG_<invoice.uid> (SALES_INVOICE, DR, amount = totalCost)
Invoice edited     → update same entry (amount/date)
Invoice cancelled  → reverse / soft-delete the entry (audit retained)
```
