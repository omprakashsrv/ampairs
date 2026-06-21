---
description: "Task list for Payment & Collection (Party Ledger)"
---

# Tasks: Payment & Collection (Party Ledger)

**Input**: Design documents from `/specs/013-payment-collection/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included only for **critical ledger logic** — backend foot-to-zero, statement, aging,
allocation, edit/cancel reversal, bounce/reversal; mobile recompute parity + offline convergence
(`feature/payment` tests). Mandated by Success Criteria SC-002/005/006/007 and the constitution's
testing gates. Not full TDD across all surfaces.

**Two repos**:
- Backend `ampairs/` — module path `payment/src/main/kotlin/com/ampairs/payment/` (abbreviated `BE:`).
- Mobile `ampairs-app/` — module path `feature/payment/src/` (abbreviated `MB:`).

> ## Implementation status — 2026-06-19 (commit `007c3ee` backend; `ampairs-app` branch)
> Implemented by background agents; **compilation not yet verified** (CI on PR #143 is the gate).
> - **Backend — DONE (files present):** T001, T002, T005, T008, T009, T010, T011, T012, T013, T014,
>   T020(BE), T021, T022, T023, T024, T024a(BE), T025, T026, T032, T033, T036, T037, T040, T041,
>   T044, T045, T046, T049, T050, T052, T055(BE docs), T056. (T053 perf: not separately verified.)
> - **Mobile — PARTIAL:** DONE T003, T004, T006, T007, T015, T016, T017, T018, T019, T020(MB),
>   T027, T028; partial T029 (RecordPaymentViewModel only). **TODO:** T030 (+ other screens),
>   T031 (Routes/entry provider/ModuleRegistry), T034, T035 (customer-list badge), T038, T042, T043,
>   T047(UI), T048, T051, T021a, T054, T054a. Mobile agent hit session limit mid-UI.
> - **Next:** verify backend CI; finish mobile UI/navigation + mobile tests; run 3-target compile gate.

## Format: `[ID] [P?] [Story] Description`
- **[P]** = parallelizable (different files, no dependency). **[Story]** = US1…US6.

---

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 BE: Scaffold `payment` module — create `payment/build.gradle.kts` (mirror `invoice/build.gradle.kts`: deps on `core`, `sequence`, Spring Data JPA, Flyway) and package dirs `domain/{model,enums,dto}`, `repository`, `service`, `controller`, `config`, `event`.
- [ ] T002 BE: Wire module — add `include("payment")` to `settings.gradle.kts`; `implementation(project(":payment"))` to `ampairs_service/build.gradle.kts`; add `"payment"` to `migrationModules`.
- [ ] T003 [P] MB: Scaffold `feature/payment` — create `feature/payment/build.gradle.kts` (mirror `feature/invoice`), commonMain/androidMain/iosMain/desktopMain source sets, package dirs `data/{api,db,repository}`, `domain`, `di`, `sync`, `ui`.
- [ ] T004 [P] MB: Wire module — add `:feature:payment` to `settings.gradle.kts`; add `paymentUrl(path)` helper to `data/common/.../ApiUrlBuilder.kt`.

---

## Phase 2: Foundational (Blocking Prerequisites — the ledger engine)

**⚠️ CRITICAL**: Every user story depends on this phase (the `LedgerEntry`/`PartyBalance` core, sync
plumbing, and money type). No story work begins until this completes.

- [ ] T005 [P] BE: Define enums in `domain/enums/` — `EntryType`, `Direction`, `PaymentDirection`, `PaymentMode`, `ClearanceStatus`, `AdjustmentType` (per data-model.md), with the `EntryType → Direction` natural-direction mapping.
- [ ] T006 [P] MB: Mirror the same enums in `feature/payment/src/commonMain/.../domain/` (`@Serializable`).
- [ ] T007 [P] MB: Implement `Money` value type (`Long` minor units) + helpers in `domain/Money.kt` — half-up rounding, `fromDouble`/`toDouble`, `fromDecimalString`/`toDecimalString` for the API boundary.
- [ ] T008 BE: Create entities `PartyBalance` and `LedgerEntry` in `domain/model/` extending `OwnableBaseDomain` (`BigDecimal` money, `Instant` dates, `@NamedEntityGraph` where needed) per data-model.md.
- [ ] T009 BE: Flyway migration `V1.0.X__create_payment_module_tables.sql` in **both** `payment/src/main/resources/db/migration/mysql/` and `.../postgresql/` — all 5 tables (`party_balance`, `ledger_entry`, `payment_voucher`, `payment_allocation`, `adjustment_voucher`), `DECIMAL(19,4)`, `TIMESTAMP(TZ)`, `owner_id` + unique/index per data-model.md. Run `./gradlew :ampairs_service:flywayInfo` first to pick the real next version.
- [ ] T010 BE: Request/Response DTOs + converters (`asResponse()`/`toEntity()`) for `LedgerEntry` and `PartyBalance` in `domain/dto/` with validation annotations.
- [ ] T011 BE: `LedgerEntryRepository` + `PartyBalanceRepository` (Spring Data; `findUpdatedAfter`/`findAllForSync` sync-feed queries incl. soft-deleted; signed-sum aggregation query for recompute).
- [ ] T012 BE: `BalanceService` — `recompute(partyUid)` (= openingSigned + Σ signed active entries), tie-out check, `postEntry()`/`reverseEntry()` helpers, upsert of cached `PartyBalance`. Core of FR-002/022.
- [ ] T013 BE: `LedgerEntry` + `PartyBalance` `/sync` service + controller (`bulkUpsert` UID-keyed; backfill deterministic `LDG_<sourceUid>`; recompute affected balance on push). Endpoints per contracts/payment-sync.md §3,§4.
- [ ] T014 BE: `POST /payment/v1/parties/{uid}/recompute-balance` endpoint returning tie-out (contracts/payment-actions.md).
- [ ] T015 [P] MB: Room entities + DAOs `LedgerEntryEntity`, `PartyBalanceEntity` + `PaymentRoomDatabase` in `data/db/` (money as `Long`, `synced`/`active` flags).
- [ ] T016 [P] MB: Add `LEDGER_ENTRY`, `PARTY_BALANCE`, `PAYMENT_VOUCHER`, `PAYMENT_ALLOCATION`, `ADJUSTMENT` to `data/sync/.../SyncEntity.kt`.
- [ ] T017 MB: Platform DI — `PaymentModule.{android,ios,desktop}.kt` (`@ContributesTo(WorkspaceScope::class)`, `@SingleIn(WorkspaceScope::class)` DB via `WorkspaceAwareDatabaseFactory`, register with `WorkspaceClosableRegistry`, explicit reified type) + `PaymentModule.kt` for DAOs.
- [ ] T018 MB: `LedgerEntryRepository` + `PartyBalanceRepository` (local-only: `markPendingPush`), local recompute (signed sum, identical formula to BE) in `domain/`, and `LedgerEntrySyncDelegate` + `PartyBalanceSyncDelegate` (`@SyncEntityKey`, `dependsOn` order per contracts) in `sync/`.
- [ ] T019 MB: `PaymentApi` + `PaymentApiImpl` (Ktor) for ledger-entry & party-balance `/sync` using `ApiUrlBuilder.paymentUrl`.
- [ ] T020 BE+MB: Invoice integration — BE publish `InvoiceFinalizedEvent` from the `invoice` module on `INVOICED`; BE `@EventListener` in `payment` posts/updates `LDG_<invoice.uid>` (SALES_INVOICE, DR, `totalCost`), reverses on cancel; MB write the same deterministic ledger entry in the invoice-finalize Room transaction. Drafts never post (FR-013/014).
- [ ] T021 [P] BE: **Test** — `BalanceService` foot-to-zero invariant over a mixed set (sales, multi-bill receipts, advances, returns, edits, backdated entries): `Σ party balances == Σ receivable − Σ payable`, zero drift (SC-002/006).
- [ ] T021a [P] MB: **Test** (`feature/payment` commonTest) — local recompute parity: the on-device signed-sum (`Money` minor units) over the same fixture equals the backend `recompute()` result exactly (no drift); covers the mobile leg of SC-002/006 and the constitution mobile-test gate.

**Checkpoint**: Ledger engine live — balances compute, sync, and reconcile. Stories can begin.

---

## Phase 3: User Story 1 — Record a collection against a party (Priority: P1) 🎯 MVP

**Goal**: Record a payment (any mode) for a party; balance and affected bills update immediately.
**Independent Test**: With a party owing money, record a receipt of any mode/amount → balance drops by
exactly that amount; allocate to bills → those bills' outstanding drop; overpayment → on-account advance.

- [ ] T022 BE: `PaymentVoucher` DTOs + converters in `domain/dto/`; `PaymentVoucherRepository` (`@EntityGraph("PaymentVoucher.withAllocations")`, sync-feed queries).
- [ ] T023 [US1] BE: `PaymentVoucherService` — `bulkUpsert`; on finalize post `PAYMENT_IN`(CR)/`PAYMENT_OUT`(DR) `LDG_<voucher.uid>` via `BalanceService`; default `clearanceStatus`; assign `voucher_no` from `SequenceNumberProvider` (series `RCP`/`PAY`).
- [ ] T024 [US1] BE: `PaymentAllocationService` + `PaymentAllocationRepository` — validate `Σ amount ≤ voucher.total`, recompute `unallocatedAmount` (does NOT touch balance — FR-010/011).
- [ ] T024a [US1] BE+MB: **Edit / cancel a payment voucher (FR-012).** BE: editing a voucher updates/replaces its `LDG_<voucher.uid>` and re-validates allocations; soft-deleting (`active=false`) reverses the ledger entry, drops its allocations, and recomputes — restoring the affected bills' outstanding (no hard delete — FR-023). MB: mirror in `PaymentVoucherRepository` (edit re-posts local entry, soft-delete reverses) so the on-device balance/bills stay consistent offline.
- [ ] T025 [US1] BE: `PaymentController` `/payment/v1/vouchers/sync` + `/payment/v1/allocations/sync` (contracts/payment-sync.md §1,§2); tenant context at controller level.
- [ ] T026 [US1] BE: `PaymentSettingDefinitions : SettingDefinitionProvider` in `config/` — `enabledPaymentModes`, `defaultPaymentMode`, `chequeRequiresClearance`, `allowOnAccountReceipts`, `enforceCreditLimit`, `agingBuckets` (gated by module `payment-collection`).
- [ ] T027 [P] [US1] MB: Room entities/DAOs `PaymentVoucherEntity`, `PaymentAllocationEntity`; `PaymentVoucherRepository` + `PaymentAllocationRepository` (local-only, posts local ledger entry in same txn, `markPendingPush`).
- [ ] T028 [US1] MB: `PaymentVoucherSyncDelegate` + `PaymentAllocationSyncDelegate` (`dependsOn` order) and extend `PaymentApi(Impl)` for both `/sync` resources.
- [ ] T029 [US1] MB: `RecordPaymentViewModel` (assisted, party-keyed; generate uid `UidGenerator.generateUid("RCP"/"PAY")`; load `enabledPaymentModes`; FIFO/manual allocation; on-account remainder).
- [ ] T030 [US1] MB: `RecordPaymentScreen` — party picker, amount, mode picker with **mode-specific fields** (cheque no/bank/date; UTR/ref for UPI/NEFT/RTGS/IMPS/net-banking/card), date, allocation list. Strings via `stringResource`, money via `formatMoney`.
- [ ] T031 [US1] MB: Route `Route.Payment` + sub-route for record-payment, entry provider in `shared/`, and `ModuleRegistry` mapping `"payment-collection" → Route.Payment`.
- [ ] T032 [US1] BE: **Test** — voucher posting + allocation: receipt reduces balance by total; `Σ allocations ≤ total`; over-allocation rejected (VALIDATION_ERROR); on-account remainder retained; **editing a voucher re-posts correctly and soft-deleting it reverses the entry and restores the bills' outstanding** (FR-012/023).

**Checkpoint**: A collection can be recorded (online & offline) and the party balance reflects it. MVP demoable.

---

## Phase 4: User Story 2 — Party running balance & statement (Priority: P1)

**Goal**: Show live closing balance (receivable/payable) and a chronological statement with running
balance; share/print it.
**Independent Test**: Party with opening + ≥1 sale + ≥1 receipt → statement lines in date order, last
running balance == headline closing balance; customer list shows the balance badge.

- [ ] T033 [US2] BE: `StatementService` — build ordered lines (opening first), debit/credit/running-balance columns; `GET /payment/v1/parties/{uid}/statement?from&to` (contracts/payment-actions.md). Last running balance MUST equal closing.
- [ ] T034 [P] [US2] MB: `PartyStatementViewModel` (reactive on local ledger DAO + party balance) + `PartyStatementScreen` (running balance per line; share/print via the invoice HTML/PDF pipeline, passing `currencySymbol` param).
- [ ] T035 [P] [US2] MB: Customer-list balance badge + customer-detail balance — read `PartyBalance` (signed → "₹X Dr/Cr") in the `customer` feature's list/detail (inject ledger read; no hardcoded currency).
- [ ] T036 [US2] BE: **Test** — statement running balance of last line == `recompute(partyUid)` for a mixed party (SC-002).

**Checkpoint**: US1 + US2 give the full record→see-balance loop (the user's core ask).

---

## Phase 5: User Story 3 — Opening balances at cutover (Priority: P2)

**Goal**: Set/edit a party's opening balance (to receive / to pay) as of a date; closing recomputes.
**Independent Test**: New party, set opening ₹5,000 to receive → closing reads ₹5,000 receivable;
statement's first line is the opening on the chosen date.

- [ ] T037 [US3] BE: Opening-balance handling in `PartyBalanceService` — accept opening fields via `/payment/v1/party-balances/sync` push; on change, recompute closing (opening folded as `openingSigned`, not a ledger entry per data-model). Validate `openingBalance ≥ 0` + direction.
- [ ] T038 [US3] MB: Opening-balance edit in `PartyBalanceRepository` (local-only) + `OpeningBalanceViewModel`/section in party setup UI (amount + to-receive/to-pay toggle + as-of date); reflects in badge/statement immediately.
- [ ] T039 [US3] BE: **Test** — opening-only party closing equals signed opening; editing opening recomputes correctly.

**Checkpoint**: Established businesses can onboard real balances.

---

## Phase 6: User Story 4 — Outstanding, due dates & aging (Priority: P2)

**Goal**: Show unpaid bills, overdue flags (from credit days), aging buckets, credit-limit warning.
**Independent Test**: Invoices of varying ages + party credit period → each bill in correct bucket;
exceeding credit limit raises a warning; dashboard shows totals + aging.

- [ ] T040 [US4] BE: `OutstandingService` — open bills = `bill.total − Σ allocations`; due date = `invoiceDate + customer.creditDays` (read customer via its public service interface); `GET /payment/v1/parties/{uid}/open-bills`.
- [ ] T041 [US4] BE: `AgingService` — bucket receivables by `agingBuckets` setting; totals receivable/payable; parties over `creditLimit`; `GET /payment/v1/aging?as_of` (contracts/payment-actions.md).
- [ ] T042 [P] [US4] MB: `CollectionsDashboardViewModel` + `CollectionsDashboardScreen` — total receivable/payable, aging summary, today's collection.
- [ ] T043 [P] [US4] MB: `OpenBillsViewModel` + `OpenBillsScreen` (per party) with aging chips + overdue flag; credit-limit warning surfaced on party/record-payment.
- [ ] T044 [US4] BE: **Test** — aging classification of each open bill matches due date + configured buckets (SC-007).

**Checkpoint**: Owners can prioritise collections by aging.

---

## Phase 7: User Story 5 — Returns & adjustments (Priority: P3)

**Goal**: Record sales return/credit note, purchase & purchase-return, settlement discount, write-off;
balance updates with correct direction.
**Independent Test**: Party owes ₹3,000; record ₹500 sales return → balance ₹2,500, distinct ledger line.

- [ ] T045 [US5] BE: `AdjustmentVoucher` DTOs + converters + `AdjustmentVoucherRepository`; `AdjustmentService` maps `adjustmentType → EntryType+Direction` and posts `LDG_<adjustment.uid>` via `BalanceService`; `voucher_no` from sequence (`CRN`/`DBN`/`ADJ`).
- [ ] T046 [US5] BE: `/payment/v1/adjustments/sync` controller (contracts/payment-sync.md §5).
- [ ] T047 [P] [US5] MB: `AdjustmentEntity`/DAO + `AdjustmentRepository` (local-only, posts ledger entry) + `AdjustmentSyncDelegate` + `PaymentApi` extension.
- [ ] T048 [US5] MB: `AdjustmentViewModel` + screen (type picker: credit/debit note, purchase, purchase-return, write-off; amount; optional linked bill).
- [ ] T049 [US5] BE: **Test** — each adjustment type posts the correct direction; sales return reduces receivable; write-off zeroes a debt; both retained in statement.

**Checkpoint**: Non-payment movements (purchases/returns) reflect in balances.

---

## Phase 8: User Story 6 — Cheque / online realisation (Priority: P3)

**Goal**: Pending → cleared/bounced lifecycle; bounce restores outstanding via reversal (audit kept).
**Independent Test**: Pending cheque receipt reduces balance; mark bounced → balance restored, original
+ reversal both visible; mark cleared → counts as realised.

- [ ] T050 [US6] BE: Clearance handling in `PaymentVoucherService` + endpoints `POST /payment/v1/vouchers/{uid}/bounce` and `/clear` (contracts/payment-actions.md) — bounce posts contra `LedgerEntry` (`reversalOf = LDG_<voucher.uid>`), soft-state transitions; no-op if terminal.
- [ ] T051 [US6] MB: Clearance UI — pending indicator on receipts, mark-cleared / mark-bounced actions, `ClearanceViewModel`; bounce restores local balance + writes reversal entry.
- [ ] T052 [US6] BE: **Test** — bounce restores party balance to pre-receipt value; original + reversal both `active`; tie-out holds; clearing an already-terminal voucher is rejected.

**Checkpoint**: Realisation tracking accurate; cleared vs pending vs bounced distinguished.

---

## Phase 9: Polish & Cross-Cutting

- [ ] T053 [P] BE: Performance — verify indexes on `(owner_id, party_uid, entry_date)` and aging queries; `@EntityGraph` coverage; avoid N+1 in statement/open-bills.
- [ ] T054 [P] MB: Compile gate — `androidApp:compileDebugKotlinAndroid`, `shared:compileKotlinIosSimulatorArm64`, `desktopApp:compileKotlin`; verify workspace-switch isolation (no stale balances).
- [ ] T054a MB: **Test** — offline convergence (SC-005) + run `./gradlew :feature:payment:check`: record a voucher/adjustment while offline (local balance updates immediately) → push/pull sync → on-device `cachedClosingBalance` equals the server `PartyBalance` with zero discrepancy.
- [ ] T055 [P] Docs — add `payment/CLAUDE.md` (module overview) and `docs/modules/payment.md`; update `ModuleRegistry`/navigation notes.
- [ ] T056 (Optional, R12) BE: Mirror `PartyBalance.cachedClosingBalance` → `Customer.outstandingAmount` via the same event path (pending confirmation of the open decision).
- [ ] T057 Run `quickstart.md` scenarios A–D end-to-end (backend + mobile) as acceptance validation.

---

## Dependencies & Execution Order

- **Phase 1 (Setup)** → **Phase 2 (Foundational, blocking)** → user stories.
- **US1 (P1)**, **US2 (P1)**: after Phase 2. US2 reads the ledger US1/foundational produce; both are
  independently testable (US2 works off opening + invoice-derived entries even before US1).
- **US3 (P2)**, **US4 (P2)**: after Phase 2. US4's open-bills/aging build on US1 allocations but degrade
  gracefully (bills show full outstanding if no allocations yet).
- **US5 (P3)**, **US6 (P3)**: after Phase 2; US6 extends US1's voucher.
- **Phase 9 (Polish)**: after the desired stories.

### Within a story: DTO/entity → repository → service → controller (BE); Room/DAO → repo → delegate →
ViewModel → screen (MB). Tests after the logic they cover.

---

## Parallel Opportunities

- Setup: T003/T004 (mobile) ∥ T001/T002 (backend).
- Foundational: T005 ∥ T006 ∥ T007; T015 ∥ T016; T021 ∥ later T013/T014 review.
- After Phase 2, with capacity: US1, US2, US3, US4 can proceed in parallel (different files); US5/US6
  follow. Backend and mobile halves of each story are largely parallel (separate repos).

### Parallel example — Foundational
```
T005 BE enums   |  T006 MB enums  |  T007 MB Money type
then T008 BE entities → T009 migration → T010 DTOs → T011 repos → T012 BalanceService
in parallel with  T015 MB Room  |  T016 SyncEntity
```

---

## Implementation Strategy

- **MVP** = Phase 1 + Phase 2 + **US1 + US2** (record a collection and see the live balance/statement) —
  this is the user's core ask. Stop and validate (quickstart Scenario A), demo.
- **Increment 2**: US3 (opening balances) + US4 (aging) → makes it usable for an established distributor.
- **Increment 3**: US5 (returns/adjustments) + US6 (cheque realisation).
- **Two-person split**: one on backend `payment` module, one on mobile `feature/payment`, syncing on the
  `/sync` contracts in `contracts/`.

## Notes
- `[P]` = different files, no dependency. `[Story]` traces to spec user stories.
- Commit after each task/logical group; never hard-delete posted ledger rows (reverse instead).
- Every commonMain change → run the 3-target compile gate (T054).
- Tie-out (`opening + ΣDr − ΣCr = closing`) is the master assertion — keep T021's test green throughout.
