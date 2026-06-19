# Payment & Collection Module — Detailed Plan

**Status:** Draft for review · **Scope:** Backend (`ampairs`) + Mobile (`ampairs-app`) · **Spec dir:** `specs/013-payment-collection`

A module to manage money owed between a business and its parties (customers/suppliers):
opening balance → sales, purchases, returns, payments → **running closing balance per party**,
with a proper **subsidiary party ledger**, multi-mode payment vouchers (cash / cheque / UPI /
NEFT / RTGS / IMPS / net-banking / card), bill-wise allocation, and aging.

---

## 0. Decisions baked into this plan (override any of these)

These resolve the open accounting questions raised in review. They are recommendations, not locked.

| # | Decision | Choice (Phase 1) | Rationale |
|---|---|---|---|
| D1 | Ledger model | **Subsidiary party ledger** (single posting per document, from the business's books). NOT a full double-entry GL with chart of accounts. | Matches Vyapar/Khatabook-class SMB needs; double-entry GL can layer on later without reshaping data. |
| D2 | Sign convention | **Dr-positive = receivable.** A party balance is a *signed* amount: `> 0` ⇒ party owes us (debtor), `< 0` ⇒ we owe party (creditor). | One convention everywhere; handles a party that is both customer & supplier (contra). |
| D3 | Purchases & returns | **Sales** come from finalized invoices. **Purchases / returns / write-offs** enter as explicit **adjustment vouchers** (credit/debit notes) in Phase 1 — no vendor module yet. First-class purchase module is Phase 2. | The app has no vendor/purchase/credit-note module today; adjustment vouchers cover the ledger need now. |
| D4 | Cheque / online realisation | Capture `paymentMode` + reference + a **clearance status** (`PENDING/CLEARED/BOUNCED/CANCELLED`) in Phase 1. Full bank reconciliation is Phase 2. | Cheques/online transfers are not instantaneous; a bounce must reverse, not vanish. |
| D5 | Period locking / FY | Out of Phase 1. Opening balance captured **as of a cutover date**. FY close & period lock in Phase 2. | Keeps Phase 1 shippable; locking is additive. |
| D6 | Money type | Backend **`BigDecimal` / `DECIMAL(19,4)`**; mobile **`Long` minor units (paise)** with a `Money` value type. | `Double` cannot foot to zero; a ledger that doesn't reconcile is worthless. |
| D7 | Posting authority | **The client/side that authors a source document also posts its ledger entry**, in the same local transaction. `LedgerEntry` is a synced entity. Backend recomputes the cached balance authoritatively and backfills any missing entry. | Only model that yields correct **offline** balances; fits the existing offline-sync architecture (just another `SyncEntity`). Balance = pure sum, so no posting-logic drift. |
| D8 | Closing balance | **Derived** = `opening + Σ(signed ledger entries)`, **cached** on `PartyBalance`, always recomputable from scratch. | Order-independent total; backdated/offline entries just trigger recompute. |
| D9 | Module name | Backend module `payment` (base path `/payment/v1/**`); mobile `feature/payment`; `SyncEntity` values `PAYMENT_VOUCHER`, `LEDGER_ENTRY`, `PAYMENT_ALLOCATION`, `PARTY_BALANCE`. | "Payment/collection" is the user-facing concept. |

---

## 1. Accounting model

### 1.1 The party ledger (source of truth for balance)

Every financial event affecting a party produces **exactly one `LedgerEntry`** — a signed posting.
The party's balance is the running sum of these entries on top of an opening balance. The ledger
entry is the **only** thing that affects the balance. *Allocation/knock-off (which receipt pays
which bill) is a separate concern used for aging/open-bills — it never changes the total.*

### 1.2 Entry types → balance effect (D2: Dr-positive = receivable)

| `entryType` | Source | Direction | Effect on party balance |
|---|---|---|---|
| `OPENING_BALANCE` (to receive) | Party setup | DR | + |
| `OPENING_BALANCE` (to pay) | Party setup | CR | − |
| `SALES_INVOICE` | Finalized invoice | DR | + |
| `SALES_RETURN` / `CREDIT_NOTE` | Adjustment voucher | CR | − |
| `PAYMENT_IN` (receipt) | Payment voucher (RECEIVED) | CR | − |
| `PURCHASE_BILL` | Adjustment voucher (P1) / purchase (P2) | CR | − |
| `PURCHASE_RETURN` / `DEBIT_NOTE` | Adjustment voucher | DR | + |
| `PAYMENT_OUT` | Payment voucher (PAID) | DR | + |
| `DISCOUNT_ALLOWED` | At settlement | CR | − |
| `WRITE_OFF` (bad debt) | Adjustment voucher | CR | − |
| `ROUND_OFF` | At settlement | DR or CR | ± |
| `INTEREST` (overdue) | Phase 3 | DR | + |

Store **both** an explicit `direction` (DR/CR) and a positive `amount`; the signed contribution is
derived (`DR ⇒ +amount`, `CR ⇒ −amount`). Never store a negative amount.

### 1.3 Posting rules (what entry a document creates)

- **Finalized invoice** (`status = INVOICED` only — **DRAFT/NEW never post**) → one `SALES_INVOICE` DR
  entry, `amount = invoice.totalCost`, `entryDate = invoiceDate`, `sourceType=INVOICE`,
  `sourceUid=invoice.uid`. Ledger-entry uid is **deterministic** from the source (`LDG_<invoice.uid>`)
  so no client can create a duplicate.
- **Invoice edited** → update the linked entry's amount/date (same uid).
- **Invoice cancelled / deleted** → **reverse** (post a contra entry `reversalOf=<uid>`, or soft-delete
  the entry with `active=false`); never silently drop a posted entry.
- **Payment voucher finalized** → one `PAYMENT_IN`/`PAYMENT_OUT` entry for the voucher total.
- **Cheque bounced** → reverse the payment entry (contra), restoring the receivable; keep both rows
  for audit.
- **Adjustment voucher** (credit/debit note, write-off, opening) → the corresponding entry.

### 1.4 Balance formula & tie-out (D8)

```
closingBalance(party) = openingBalance(signed)
                      + Σ active LedgerEntry.signedAmount  (entryType-driven sign)
```

Invariant that must always hold (the regression test for the whole module):

```
opening + Σ DR − Σ CR = closing            (per party, and summed across all parties)
```

`PartyBalance.cachedClosingBalance` is a denormalization, recomputed on every posting and
**fully recomputable** from `LedgerEntry` rows (recompute job + on-demand endpoint).

### 1.5 Allocation (aging / open bills only)

A `PaymentAllocation` matches part of a payment voucher to a specific invoice/bill. The unallocated
remainder is **on account** (advance). Allocation drives:
- **Open bills** list (invoice outstanding = invoice total − Σ allocations against it),
- **Aging buckets** (0–30 / 31–60 / 61–90 / 90+ based on `dueDate = invoiceDate + creditDays`),
- credit-limit breach warnings (`creditLimit` already on Customer/CustomerType).

It does **not** affect the party total balance.

---

## 2. Backend data model (`/payment` module)

All entities extend `OwnableBaseDomain` (→ `uid`, `ownerId` `@TenantId`, `refId`, `createdAt`,
`updatedAt` as `Instant`). All money is `BigDecimal` ⇄ `DECIMAL(19,4)`. All are syncable via the
canonical `/sync` contract.

### 2.1 `PartyBalance` (one row per party)
| field | type | notes |
|---|---|---|
| `partyUid` | String(40) | = `customer.uid`; unique per `(owner_id, party_uid)` |
| `openingBalance` | BigDecimal | always ≥ 0 |
| `openingDirection` | enum DR/CR | "to receive" / "to pay" |
| `openingAsOf` | Instant | cutover date |
| `cachedClosingBalance` | BigDecimal | signed (Dr-positive); denormalized |
| `lastComputedAt` | Instant | |
| `active` | Boolean | soft-delete for sync |

### 2.2 `LedgerEntry` (the posting — balance source of truth)
| field | type | notes |
|---|---|---|
| `partyUid` | String(40) | indexed |
| `entryDate` | Instant | ordering key for statement |
| `entryType` | enum | §1.2 |
| `direction` | enum DR/CR | |
| `amount` | BigDecimal | ≥ 0 |
| `sourceType` | enum | INVOICE / PAYMENT / ADJUSTMENT / RETURN / MANUAL / PURCHASE |
| `sourceUid` | String(64) | doc reference (no FK) |
| `voucherNo` | String(64) | human-readable, from `SequenceNumberProvider` |
| `narration` | String(500) | |
| `reversalOf` | String(64)? | uid of the entry this reverses |
| `reversed` | Boolean | true once a reversal exists |
| `active` | Boolean | soft-delete |

Indexes: `(owner_id, party_uid, entry_date)`, unique `(uid)`, `(owner_id, source_type, source_uid)`.

### 2.3 `PaymentVoucher` (money movement header)
| field | type | notes |
|---|---|---|
| `partyUid` | String(40) | |
| `voucherNo` | String(64) | sequence: `RCP`/`PAY` series |
| `voucherDate` | Instant | |
| `direction` | enum RECEIVED/PAID | |
| `totalAmount` | BigDecimal | |
| `paymentMode` | enum | CASH, CHEQUE, UPI, NEFT, RTGS, IMPS, NET_BANKING, CARD, BANK_TRANSFER, WALLET, OTHER |
| `referenceNumber` | String(100)? | UTR / txn id / cheque no |
| `instrumentDate` | Instant? | cheque date |
| `bankName` | String(120)? | |
| `clearanceStatus` | enum | PENDING/CLEARED/BOUNCED/CANCELLED (default CLEARED for cash & instant modes; PENDING for cheque) |
| `unallocatedAmount` | BigDecimal | cached = total − Σ allocations |
| `narration` | String(500)? | |
| `active` | Boolean | soft-delete |

### 2.4 `PaymentAllocation` (knock-off, aging only)
| field | type | notes |
|---|---|---|
| `paymentVoucherUid` | String(64) | |
| `targetType` | enum | INVOICE / PURCHASE / LEDGER_ENTRY |
| `targetUid` | String(64) | the bill being settled |
| `amount` | BigDecimal | Σ ≤ voucher total |
| `active` | Boolean | |

### 2.5 `AdjustmentVoucher` (credit/debit note, write-off, opening) — Phase 1 light
Header that posts a `LedgerEntry`. Fields: `partyUid`, `voucherNo` (series `CRN`/`DBN`/`ADJ`),
`voucherDate`, `adjustmentType` (CREDIT_NOTE/DEBIT_NOTE/WRITE_OFF/OPENING/PURCHASE_BILL/PURCHASE_RETURN),
`amount`, `narration`, `sourceRef`, `active`.

### 2.6 Flyway
- `payment/src/main/resources/db/migration/{mysql,postgresql}/V1.0.93__create_payment_module_tables.sql`
  (check `./gradlew :ampairs_service:flywayInfo` for the real next number — write **both** vendors).
- `DECIMAL(19,4)` for money, `TIMESTAMP`/`TIMESTAMPTZ` for instants, `owner_id` indexed on every table.

### 2.7 Module wiring
- `settings.gradle.kts`: `include("payment")`
- `ampairs_service/build.gradle.kts`: `implementation(project(":payment"))` + add `"payment"` to `migrationModules`
- Standard package layout `com.ampairs.payment.{domain,repository,service,controller,config,sync}`.

---

## 3. Backend API (canonical `/sync` + actions)

### 3.1 Sync endpoints (one per syncable entity)
```
GET/POST  /payment/v1/vouchers/sync        → PaymentVoucher
GET/POST  /payment/v1/allocations/sync     → PaymentAllocation
GET/POST  /payment/v1/ledger-entries/sync  → LedgerEntry
GET/POST  /payment/v1/party-balances/sync  → PartyBalance   (pull-authoritative; see §4)
GET/POST  /payment/v1/adjustments/sync     → AdjustmentVoucher
```
All follow the contract: snake_case params (`last_sync`, `page`, `size`, `sort_by`, `sort_dir`),
pull feed includes soft-deleted rows, push = UID-keyed bulk upsert, wrapped in
`ApiResponse<PageResponse<T>>` / `ApiResponse<List<T>>`.

### 3.2 Action endpoints (non-sync, UI-invoked)
```
GET  /payment/v1/parties/{uid}/statement?from=&to=     → ledger statement (printable)
GET  /payment/v1/parties/{uid}/open-bills              → unsettled invoices + outstanding
GET  /payment/v1/aging?as_of=                          → aging buckets across parties
POST /payment/v1/parties/{uid}/recompute-balance       → force balance recompute (returns tie-out)
POST /payment/v1/vouchers/{uid}/bounce                 → cheque bounce → reversal entry
```

### 3.3 Server-side posting & recompute
On `bulkUpsert` of a `PaymentVoucher`/`AdjustmentVoucher`/finalized invoice:
1. Upsert the doc.
2. Ensure its `LedgerEntry` exists (backfill with deterministic uid if the client didn't send one).
3. Recompute `PartyBalance.cachedClosingBalance` and assert the §1.4 invariant.
Invoice→ledger posting is driven by an `InvoiceFinalizedEvent` (Spring `ApplicationEventPublisher`)
consumed in the payment module, so invoice and payment modules stay decoupled.

---

## 4. Offline-sync strategy on mobile (the crux — D7/D8)

Mirror the invoice/order feature exactly: local-only repository + `SyncDelegate` owning the API.

- **New `SyncEntity`** values: `PARTY_BALANCE`, `LEDGER_ENTRY`, `PAYMENT_VOUCHER`, `PAYMENT_ALLOCATION`,
  `ADJUSTMENT` — added to `data/sync/.../SyncEntity.kt`.
- **Dependency order** (`dependsOn` on the delegates): `CUSTOMER` → `INVOICE`/`ORDER` →
  `LEDGER_ENTRY` → `PAYMENT_VOUCHER`/`ADJUSTMENT` → `PAYMENT_ALLOCATION` → `PARTY_BALANCE`.
- **Posting on the client:** when the user finalizes an invoice or saves a payment voucher, the
  same Room transaction also writes the `LedgerEntry` (deterministic uid for invoice-derived ones)
  and marks both `synced=0` + `markPendingPush(...)`. → balance updates **instantly offline**.
- **Balance computation:** `cachedClosingBalance` recomputed locally from local `LedgerEntry` rows
  on every write (pure signed sum — identical formula to backend, so no drift). `PartyBalance` is
  **pull-authoritative**: on pull, the server's value wins (it's the reconciled truth) but the local
  recompute keeps the UI live between syncs.
- **Money:** ledger amounts stored as `Long` minor units; a `Money` value type does the arithmetic;
  convert to/from decimal strings at the API boundary. Converting an invoice's `Double` total to
  minor units happens **once** at posting (half-up rounding) — no accumulation, so no float drift.
- **Repository = local-only** (inject `Dao` + `SyncStateDao`, never the `Api`); all server traffic in
  the delegates. Deletes are soft (`active=false, synced=0`).

### 4.1 Mobile module layout (`feature/payment`)
```
feature/payment/src/commonMain/kotlin/com/ampairs/payment/
├── data/api/         PaymentApi(+Impl), uses ApiUrlBuilder.paymentUrl("v1/...")
├── data/db/          Room: PaymentVoucherEntity, LedgerEntryEntity, PaymentAllocationEntity,
│                     PartyBalanceEntity, AdjustmentEntity + DAOs + PaymentRoomDatabase
├── data/repository/  PaymentRepository, LedgerRepository (local-only)
├── domain/           Money, PartyLedger, Voucher, enums, posting rules (shared, pure)
├── di/               PaymentModule.{android,ios,desktop}.kt (@SingleIn(WorkspaceScope::class))
├── sync/             PaymentVoucherSyncDelegate, LedgerEntrySyncDelegate, ... (+ @SyncEntityKey)
└── ui/               screens + ViewModels
```
- Workspace-scoped DB (Metro `@ContributesTo(WorkspaceScope::class)`, registered with
  `WorkspaceClosableRegistry`, explicit reified `createDatabase<PaymentRoomDatabase>()`).
- UID in ViewModel: `UidGenerator.generateUid("RCP"/"PAY"/"LDG"/...)`.
- `settings.gradle.kts`: `:feature:payment`; add a top-level `Route.Payment` + sub-routes + entry
  provider in `shared/`; register module code in `ModuleRegistry` (`"payment-collection" → Route.Payment`).
- `ApiUrlBuilder.paymentUrl(...)` helper added in `data/common`.

---

## 5. Money & precision (D6)
- **Backend:** `BigDecimal`, scale 4, `DECIMAL(19,4)`; round half-up to 2dp at settlement; explicit
  `ROUND_OFF` ledger line absorbs residue so a voucher always foots.
- **Mobile:** `Long` minor units (paise) for all ledger math; `Money` value type; display via existing
  `formatMoney(amount, LocalAppLocale.current)` (convert minor→Double only for formatting).
- **Never** do ledger arithmetic in `Double`. Existing invoice/order `Double` totals are inputs only,
  converted once at posting.

---

## 6. Settings (reuse `setting` module / `StoreSetting`)
Per-workspace toggles, gated by installed module (`requiresModule = "payment-collection"`):
- `enabledPaymentModes` (JSON list) — which modes appear in the picker.
- `defaultPaymentMode` (ENUM).
- `chequeRequiresClearance` (BOOLEAN) — if false, cheques post as CLEARED immediately.
- `allowOnAccountReceipts` (BOOLEAN) — permit advances/unallocated.
- `enforceCreditLimit` (BOOLEAN) — block/warn on limit breach.
- `agingBuckets` (JSON) — bucket boundaries.
Definitions live in code: a `PaymentSettingDefinitions : SettingDefinitionProvider` in the payment module.

---

## 7. Mobile UI / UX (Phase 1)
- **Party balance** surfaced on customer list (badge: "₹X Dr/Cr") and customer detail.
- **Party ledger / statement screen** — chronological entries, running balance, share/print PDF
  (reuse the invoice HTML/PDF pipeline; pass `currencySymbol` as a param per cmp-practices §12).
- **Record payment** screen — party picker, amount, mode (mode-specific fields: cheque no/bank/date,
  UTR/ref), date, optional bill allocation (auto-FIFO or manual), on-account remainder.
- **Collections dashboard** — total receivable, total payable, aging summary, today's collection.
- **Open bills / outstanding** list with aging chips.
- MVI + Metro per existing patterns; `collectAsStateWithLifecycle`; all strings via `stringResource`;
  no hardcoded currency; iOS uses `Dispatchers.Default`.

---

## 8. Audit, reconciliation, edge cases
- **No hard deletes of posted entries** — cancel/edit posts a reversal; soft-delete only.
- **Gapless voucher numbering** per series via `SequenceNumberProvider` (offline fallback like invoices).
- **Backdated/offline entries** reorder the statement — balance is a sum (order-independent); the
  statement re-sorts by `(entryDate, createdAt)`.
- **Tie-out endpoint/job** asserts `opening + ΣDr − ΣCr = closing` per party (regression guard).
- **Contra parties** (both customer & supplier) handled by signed balance; one `PartyBalance` row.
- **TDS** (India B2B): a receipt short of the invoice can carry a `TDS` adjustment line so the bill
  still knocks off fully — flagged for Phase 2 detail.
- **Concurrency:** per-entity `Mutex` in `CentralSyncService` already serializes pushes; backend
  recompute is transactional.

---

## 9. Phasing

**Phase 1 — Receivables / customer collection (MVP)**
Backend `payment` module (PartyBalance, LedgerEntry, PaymentVoucher, PaymentAllocation,
AdjustmentVoucher) + `/sync` + statement/aging/open-bills/recompute endpoints + invoice-finalized
event posting. Mobile `feature/payment` (offline-first, all delegates, balance on customer
list/detail, record-payment, statement PDF, dashboard). Payment modes + cheque clearance status.
Adjustment vouchers cover purchases/returns/opening/write-off. `BigDecimal`/minor-units throughout.

**Phase 2 — Payables & realisation**
First-class purchase/vendor + debit-note modules; payment-out flows; cheque realisation + cash/bank
accounts + daybook/cashbook + bank reconciliation; period/FY locking + opening carry-forward; GST on
advances; TDS handling.

**Phase 3 — Reporting & automation**
Outstanding/collection reports, payment reminders (notification module), interest on overdue,
multi-currency, partial-payment auto-allocation strategies.

---

## 10. High-level task breakdown (Phase 1)

**Backend**
1. Scaffold `payment` module + wiring (settings.gradle, ampairs_service, migrationModules).
2. Entities + Flyway (mysql + postgresql), `DECIMAL(19,4)`.
3. DTOs (Request/Response) + mappers; `ApiResponse`/`PageResponse` wrappers.
4. Repositories (`@EntityGraph`, derived queries, `findUpdatedAfter`/`findAllForSync`).
5. Services: bulkUpsert, posting, balance recompute + tie-out, aging, statement, open-bills.
6. `InvoiceFinalizedEvent` publisher (invoice module) + consumer (payment module).
7. Controllers (sync + actions), tenant context at controller level.
8. `PaymentSettingDefinitions`.
9. Tests incl. the foot-to-zero invariant.

**Mobile**
1. `SyncEntity` additions + `ApiUrlBuilder.paymentUrl`.
2. `feature/payment` scaffold + settings.gradle + Metro platform modules (WorkspaceScope DB).
3. Room entities/DAOs/DB; `Money` value type (minor units); pure posting rules in `domain`.
4. Repositories (local-only) + `markPendingPush`.
5. SyncDelegates (push/pull/event) with `dependsOn` ordering; deterministic ledger-entry uids.
6. API interface/impl.
7. ViewModels + screens (dashboard, record payment, statement, open bills); customer-list balance badge.
8. Routes + entry provider + `ModuleRegistry` registration.
9. Compile all 3 targets (`androidApp`, `shared:compileKotlinIosSimulatorArm64`, `desktopApp`).

---

## 11. Open items to confirm before build
1. Confirm D1 (subsidiary ledger vs full double-entry) and D3 (purchases as adjustment vouchers vs
   first-class now).
2. Confirm module/route name `payment` vs `collection`.
3. Should `Customer.outstandingAmount` (backend, already exists) be kept in sync with
   `PartyBalance.cachedClosingBalance`, or deprecated in favor of the new field?
4. Mobile money representation: `Long` minor units (this plan) vs adopting a KMP `BigDecimal`
   library (`ionspin/bignum`). Minor units chosen for KMP-safety + exactness.
5. Phase 1 PDF statement: reuse invoice HTML pipeline or a new template?
