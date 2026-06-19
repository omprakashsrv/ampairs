# Contract — Payment action endpoints (non-sync, UI-invoked)

All return `ApiResponse<T>`, workspace-scoped (`X-Workspace-ID`).

---

## Party statement  (US2 / FR-004, FR-005)

```
GET /payment/v1/parties/{party_uid}/statement?from={iso}&to={iso}
→ ApiResponse<PartyStatementResponse>
```
```jsonc
{
  "party_uid": "CUS2026...",
  "from": "2026-04-01T00:00:00Z",
  "to":   "2026-06-19T23:59:59Z",
  "opening_balance": 2000.00, "opening_direction": "DR",
  "lines": [
    { "entry_date":"2026-04-05T...","entry_type":"SALES_INVOICE","voucher_no":"INV/0007",
      "narration":"...", "debit":3000.00, "credit":0.00, "running_balance":5000.00 },
    { "entry_date":"2026-04-20T...","entry_type":"PAYMENT_IN","voucher_no":"RCP/0001",
      "narration":"Cheque","debit":0.00,"credit":4000.00,"running_balance":1000.00 }
  ],
  "closing_balance": 2500.00, "closing_direction": "DR"
}
```
`running_balance` of the last line **equals** `closing_balance` (signed). Used for the printable
statement.

## Open bills  (US1/US4 / FR-016)

```
GET /payment/v1/parties/{party_uid}/open-bills
→ ApiResponse<List<OpenBillResponse>>
```
```jsonc
{ "bill_uid":"INV2026...","bill_no":"INV/0007","bill_date":"2026-04-05T...",
  "total":3000.00,"allocated":1000.00,"outstanding":2000.00,
  "due_date":"2026-05-05T...","days_overdue":45,"aging_bucket":"31-60" }
```

## Aging summary  (US4 / FR-017, FR-018)

```
GET /payment/v1/aging?as_of={iso}
→ ApiResponse<AgingSummaryResponse>
```
```jsonc
{
  "as_of":"2026-06-19T...",
  "total_receivable":120000.00, "total_payable":35000.00,
  "buckets":[ {"label":"0-30","amount":40000.00},{"label":"31-60","amount":50000.00},
              {"label":"61-90","amount":20000.00},{"label":"90+","amount":10000.00} ],
  "parties_over_credit_limit":[ {"party_uid":"CUS...","balance":80000.00,"credit_limit":50000.00} ]
}
```

## Recompute party balance  (FR-022 / SC-002)

```
POST /payment/v1/parties/{party_uid}/recompute-balance
→ ApiResponse<RecomputeResponse>
```
```jsonc
{ "party_uid":"CUS2026...","cached_before":2500.00,"recomputed":2500.00,
  "total_debit":5000.00,"total_credit":4500.00,"opening_signed":2000.00,
  "tie_out_ok":true }
```
`tie_out_ok = (opening_signed + total_debit − total_credit == recomputed)`. Used by tests/ops as the
ledger integrity guard.

## Bounce / clear a payment  (US6 / FR-020, FR-021)

```
POST /payment/v1/vouchers/{voucher_uid}/bounce
     body: { "narration": "Cheque returned - insufficient funds" }
→ ApiResponse<PaymentVoucherResponse>     # status BOUNCED; a reversal LedgerEntry is posted

POST /payment/v1/vouchers/{voucher_uid}/clear
→ ApiResponse<PaymentVoucherResponse>     # status CLEARED
```
Bounce posts a contra `LedgerEntry` (`reversalOf = LDG_<voucher.uid>`), restoring the party's
outstanding; both original and reversal remain visible (audit). `clear`/`bounce` are no-ops if the
voucher is already terminal (`BOUNCED`/`CANCELLED`).

---

### Error shape (global handler)
```jsonc
{ "success": false,
  "error": { "code":"VALIDATION_ERROR","message":"Allocated amount exceeds voucher total",
             "validation_errors": { "amount":"sum 4500.00 > total 4000.00" }, "module":"payment" },
  "timestamp":"...","path":"/payment/v1/allocations/sync","trace_id":"..." }
```
