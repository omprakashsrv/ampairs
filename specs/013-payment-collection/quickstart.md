# Quickstart — Payment & Collection (Party Ledger)

How to exercise the feature once implemented. Two surfaces: backend API and the mobile app. Everything
is workspace-scoped — send a valid JWT + `X-Workspace-ID` on API calls.

## Prerequisites
- A workspace with at least one customer (party).
- Backend running (`./gradlew :ampairs_service:bootRun`); migrations applied
  (`./gradlew :ampairs_service:flywayInfo`).
- Module `payment-collection` enabled for the workspace (settings); `enabledPaymentModes` configured.

## Scenario A — Opening balance → sale → collection → statement (the core loop)

1. **Set an opening balance** (US3): push a `PartyBalance` with `opening_balance=2000`,
   `opening_direction=DR`, `opening_as_of` = cutover date.
   `POST /payment/v1/party-balances/sync`.
   → party closing balance reads **₹2,000 to receive**.
2. **Finalize an invoice** of ₹3,000 for the party (existing invoice flow → status `INVOICED`).
   → the payment module posts `LDG_<invoice.uid>` (SALES_INVOICE, DR 3000) via `InvoiceFinalizedEvent`.
   → closing balance = **₹5,000 to receive**.
3. **Record a collection** (US1): create a `PaymentVoucher`
   `{direction:RECEIVED, total_amount:4000, payment_mode:CASH}` → `POST /payment/v1/vouchers/sync`.
   Optionally allocate ₹3,000 to the invoice and ₹1,000 on-account → `POST /payment/v1/allocations/sync`.
   → closing balance = **₹1,000 to receive**; invoice shows settled; ₹1,000 advance is on-account.
4. **View the statement** (US2): `GET /payment/v1/parties/{uid}/statement?from&to`.
   → lines: opening 2000 → +3000 (running 5000) → −4000 (running 1000). `closing_balance == 1000`,
   matching the last `running_balance`.
5. **Verify integrity**: `POST /payment/v1/parties/{uid}/recompute-balance` → `tie_out_ok: true`.

## Scenario B — Cheque that bounces (US6)
1. Record a cheque receipt ₹4,000 → `clearance_status` defaults `PENDING`; balance reflects it.
2. `POST /payment/v1/vouchers/{uid}/bounce` → status `BOUNCED`, a reversal `LedgerEntry` is posted,
   the ₹4,000 returns to outstanding; original + reversal both visible in the statement.

## Scenario C — Sales return (US5)
1. Party owes ₹3,000. Create an `AdjustmentVoucher` `{adjustment_type:SALES_RETURN, amount:500}` →
   `POST /payment/v1/adjustments/sync` → posts CR 500 → balance **₹2,500**.

## Scenario D — Aging & outstanding (US4)
1. `GET /payment/v1/parties/{uid}/open-bills` → each unpaid bill with `outstanding`, `days_overdue`,
   `aging_bucket`.
2. `GET /payment/v1/aging?as_of=` → totals + buckets + parties over credit limit.

## Mobile (offline-first)
1. Open a customer → balance badge + **Statement** screen.
2. **Record payment** screen → pick party, amount, mode (mode-specific fields appear for cheque/online),
   allocate to bills or leave on-account. Works **offline**: balance updates immediately (local
   `LedgerEntry` written in the same transaction), pushes on reconnect.
3. **Collections dashboard** → total receivable/payable + aging summary.
4. Switch workspaces → balances are isolated per workspace (no stale data).

## Test gates
- Backend: `./gradlew :payment:test` — include the **foot-to-zero** test across a mixed transaction set
  (sales, multi-bill receipts, advances, returns, bounced cheques, edits, backdated entries) →
  `Σ party balances == Σ receivable − Σ payable`, zero drift (SC-006).
- Mobile: `./gradlew :feature:payment:check` and compile all three targets:
  `androidApp:compileDebugKotlinAndroid`, `shared:compileKotlinIosSimulatorArm64`,
  `desktopApp:compileKotlin`.
