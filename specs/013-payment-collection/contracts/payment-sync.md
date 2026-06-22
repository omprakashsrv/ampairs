# Contract — Payment `/sync` endpoints

Five syncable entities, each on the canonical contract. Shapes are illustrative DTOs (snake_case on the
wire). `…` = inherited envelope fields.

---

## 1. Payment vouchers

```
GET  /payment/v1/vouchers/sync?last_sync={iso}&page=0&size=100&sort_by=updatedAt&sort_dir=ASC
     → ApiResponse<PageResponse<PaymentVoucherResponse>>     # includes soft-deleted
POST /payment/v1/vouchers/sync
     body: [ PaymentVoucherRequest, … ]                      # UID-keyed bulk upsert
     → ApiResponse<List<PaymentVoucherResponse>>
```

```jsonc
// PaymentVoucherRequest / Response
{
  "uid": "RCP20260619... ",          // client-generated; blank ⇒ server creates
  "party_uid": "CUS2026...",
  "voucher_no": "RCP/0001",
  "voucher_date": "2026-06-19T10:00:00Z",
  "direction": "RECEIVED",            // RECEIVED | PAID
  "total_amount": 4000.00,            // DECIMAL(19,4)
  "payment_mode": "CHEQUE",           // CASH|CHEQUE|UPI|NEFT|RTGS|IMPS|NET_BANKING|CARD|BANK_TRANSFER|WALLET|OTHER
  "reference_number": "000123",
  "instrument_date": "2026-06-18T00:00:00Z",
  "bank_name": "HDFC",
  "clearance_status": "PENDING",      // PENDING|CLEARED|BOUNCED|CANCELLED
  "unallocated_amount": 0.00,
  "narration": "Part payment",
  "active": true
}
```

## 2. Payment allocations

```
GET  /payment/v1/allocations/sync?...   → ApiResponse<PageResponse<PaymentAllocationResponse>>
POST /payment/v1/allocations/sync       → ApiResponse<List<PaymentAllocationResponse>>
```
```jsonc
{ "uid":"...", "payment_voucher_uid":"RCP2026...", "target_type":"INVOICE",
  "target_uid":"INV2026...", "amount":3000.00, "active":true }
```
Server validates `Σ amount ≤ voucher.total_amount`; recomputes `unallocated_amount`.

## 3. Ledger entries

```
GET  /payment/v1/ledger-entries/sync?...  → ApiResponse<PageResponse<LedgerEntryResponse>>
POST /payment/v1/ledger-entries/sync      → ApiResponse<List<LedgerEntryResponse>>
```
```jsonc
{ "uid":"LDG_INV2026...",            // deterministic for document-derived entries
  "party_uid":"CUS2026...", "entry_date":"2026-06-19T10:00:00Z",
  "entry_type":"SALES_INVOICE", "direction":"DR", "amount":3000.00,
  "source_type":"INVOICE", "source_uid":"INV2026...", "voucher_no":"INV/0007",
  "narration":null, "reversal_of":null, "reversed":false, "active":true }
```
On push the server **recomputes the affected `PartyBalance`** and asserts the foot-to-zero invariant. If
a finalized invoice arrives (via event) without its ledger entry, the server **backfills** it
(`LDG_<invoice.uid>`).

## 4. Party balances  *(pull-authoritative)*

```
GET  /payment/v1/party-balances/sync?...  → ApiResponse<PageResponse<PartyBalanceResponse>>
POST /payment/v1/party-balances/sync      → ApiResponse<List<PartyBalanceResponse>>   # opening fields only
```
```jsonc
{ "uid":"...", "party_uid":"CUS2026...",
  "opening_balance":2000.00, "opening_direction":"DR", "opening_as_of":"2026-04-01T00:00:00Z",
  "cached_closing_balance":2500.00,   // signed; server-authoritative
  "last_computed_at":"2026-06-19T10:00:01Z", "active":true }
```
Push carries **only** opening-balance edits; `cached_closing_balance` is server-computed and wins on pull
(clients recompute locally between syncs for live UI).

## 5. Adjustments

```
GET  /payment/v1/adjustments/sync?...  → ApiResponse<PageResponse<AdjustmentVoucherResponse>>
POST /payment/v1/adjustments/sync      → ApiResponse<List<AdjustmentVoucherResponse>>
```
```jsonc
{ "uid":"CRN2026...", "party_uid":"CUS2026...", "voucher_no":"CRN/0001",
  "voucher_date":"2026-06-19T10:00:00Z", "adjustment_type":"SALES_RETURN",
  "amount":500.00, "narration":"Damaged goods", "source_ref":"INV2026...", "active":true }
```

---

### Sync ordering (mobile delegates `dependsOn`)
`CUSTOMER → INVOICE/ORDER → LEDGER_ENTRY → PAYMENT_VOUCHER/ADJUSTMENT → PAYMENT_ALLOCATION → PARTY_BALANCE`
