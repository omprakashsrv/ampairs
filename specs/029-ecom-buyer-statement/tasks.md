---
description: "Task list for 029 — Ecom Buyer Account (invoices, statement, order↔invoice linking)"
---

# Tasks: Ecom Buyer Account — invoices, statement & order↔invoice linking

**Input**: Design documents from `/specs/029-ecom-buyer-statement/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: INCLUDED — the spec's Testing section (§10) explicitly requires unit + integration tests.

**Organization**: Grouped by user story. Backend is the deliverable; the mobile app (spec Phase 3) is
an optional follow-on phase at the end.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1–US8 from spec.md (Setup/Foundational/Polish carry no story label)

## Path Conventions
Backend module roots at repo root: `core/`, `invoice/`, `payment/`, `ecom/` under
`src/main/kotlin/com/ampairs/{module}/…` and `src/test/kotlin/com/ampairs/{module}/…`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm the ground the read-through builds on. No new module, no migration.

- [x] T001 Verify build baseline compiles: `./gradlew :core:compileKotlin :invoice:compileKotlin :payment:compileKotlin :ecom:compileKotlin`
- [x] T002 Confirm existing dependency edges exist (no build.gradle change expected): `invoice`→`core`, `payment`→`core`, `ecom`→`core` in each module's `build.gradle.kts`; note if any is missing
- [x] T003 Confirm the buyer surface exemption: `ecom` `CustomerAccountController` order endpoints run without `X-Workspace-ID` and set tenant from `storefront_slug` (reference for the new endpoints) in `ecom/src/main/kotlin/com/ampairs/ecom/controller/CustomerAccountController.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The two `core` interfaces + buyer-safe DTOs and the shared party-resolution helper that
EVERY user story depends on.

**⚠️ CRITICAL**: No user story phase can begin until this phase is complete.

- [x] T004 [P] Create buyer invoice DTOs `BuyerInvoiceSummary`, `BuyerInvoiceDetail`, `BuyerInvoiceLine` in `core/src/main/kotlin/com/ampairs/core/service/InvoiceEcomService.kt` (data classes; `Instant`/`BigDecimal`; `status: String`; `orderRefId: String?`)
- [x] T005 [P] Declare interface `InvoiceEcomService` with `listBuyerInvoices(partyUid, pageable): Page<BuyerInvoiceSummary>`, `getBuyerInvoice(invoiceUid, partyUid): BuyerInvoiceDetail?`, `listInvoicesForOrder(orderRefId, partyUid): List<BuyerInvoiceSummary>` in the same `core/.../service/InvoiceEcomService.kt`
- [x] T006 [P] Create buyer ledger DTOs `BuyerOutstandingResponse`, `BuyerOpenBill`, `BuyerAgingBucket`, `BuyerStatementResponse`, `BuyerStatementLine` in `core/src/main/kotlin/com/ampairs/core/service/PartyLedgerEcomService.kt`
- [x] T007 [P] Declare interface `PartyLedgerEcomService` with `outstanding(partyUid, asOf): BuyerOutstandingResponse`, `statement(partyUid, from, to): BuyerStatementResponse` in the same `core/.../service/PartyLedgerEcomService.kt`
- [x] T008 Add a private `resolveParty(authentication, storefrontSlug, customerId?)` helper in `ecom/.../controller/CustomerAccountController.kt`: set tenant from `storefrontService.getPublishedStorefrontBySlug(slug).ownerId` (try/finally), resolve `EcomCustomerService.resolveLinkedCustomerId(userId, customerId)`, throw `NOT_LINKED`→403 when null (US8). Reuse across all new endpoints.
- [x] T009 [P] Unit test the party-resolution/exemption contract stub in `ecom/src/test/kotlin/com/ampairs/ecom/controller/CustomerAccountResolveTest.kt` (linked → partyUid; unlinked → 403 NOT_LINKED; restricted `active=false` contact → unlinked)

**Checkpoint**: Interfaces + DTOs compile; resolve helper enforces the link gate.

---

## Phase 3: User Story 1 — Invoice list (Priority P1) 🎯 MVP

**Goal**: A linked buyer sees a paginated, newest-first list of their finalized invoices.
**Independent test**: `GET /v1/ecom/account/invoices?storefront_slug=…` returns the linked customer's
finalized invoices only (no drafts), `PageResponse`-wrapped, each with `order_ref` or null.

- [x] T010 [P] [US1] Add derived finder `findByCustomerIdAndStatusIn(customerId, statuses, pageable): Page<Invoice>` (called with `finalizedStatuses = setOf(InvoiceStatus.INVOICED)`, sorted `invoiceDate` desc) in `invoice/src/main/kotlin/com/ampairs/invoice/repository/InvoiceRepository.kt`
- [x] T011 [US1] Implement `InvoiceEcomServiceImpl.listBuyerInvoices(...)` in `invoice/src/main/kotlin/com/ampairs/invoice/service/InvoiceEcomServiceImpl.kt`: filter to `{INVOICED}` (excludes DRAFT/NEW), map `Invoice`→`BuyerInvoiceSummary` (status→buyer string; `total` only, no `amountDue`; raw `orderRefId` passed through for the controller to swap); `@Service`
- [x] T012 [US1] Add `EcomOrderRepository.findByManagementOrderRef(managementOrderRef): EcomOrder?` in `ecom/src/main/kotlin/com/ampairs/ecom/repository/EcomOrderRepository.kt` (used to swap `orderRefId`→buyer-facing `order_ref`)
- [x] T013 [US1] Add `GET /v1/ecom/account/invoices` to `ecom/.../controller/CustomerAccountController.kt`: `resolveParty(...)`, call `invoiceEcomService.listBuyerInvoices(partyUid, pageable)`, map each item's `orderRefId`→`order_ref` via `findByManagementOrderRef`, return `ApiResponse.success(PageResponse.from(page))`
- [x] T014 [P] [US1] Unit test invoice→DTO mapping + finalized filter in `invoice/src/test/kotlin/com/ampairs/invoice/service/InvoiceEcomServiceImplTest.kt` (drafts excluded; status string; total mapped)
- [x] T015 [US1] Integration test `GET .../invoices` in `ecom/src/test/kotlin/com/ampairs/ecom/controller/BuyerInvoiceListIT.kt` (linked → own finalized invoices; draft absent; `order_ref` set for ecom invoice, null otherwise; unlinked → 403)

**Checkpoint**: MVP — buyer can list their invoices.

---

## Phase 4: User Story 2 — Invoice detail (Priority P1)

**Goal**: A linked buyer opens one invoice to see line items + totals.
**Independent test**: `GET /v1/ecom/account/invoices/{uid}` returns detail for own invoice; another
party's invoice or a draft → 404.

- [x] T016 [US2] Implement `InvoiceEcomServiceImpl.getBuyerInvoice(invoiceUid, partyUid)` in `invoice/.../service/InvoiceEcomServiceImpl.kt`: load via existing `findByUid` + item entity graph; return null if `customerId != partyUid` or draft; map `Invoice`+items → `BuyerInvoiceDetail` (subtotal/taxTotal/total; lines omit cost/margin)
- [x] T017 [US2] Add `GET /v1/ecom/account/invoices/{invoiceUid}` to `ecom/.../controller/CustomerAccountController.kt`: `resolveParty(...)`, call `getBuyerInvoice(...)`, `?: throw NotFound`, swap `orderRefId`→`order_ref`, `ApiResponse.success(...)`
- [x] T018 [P] [US2] Unit test detail mapping + guard in `invoice/src/test/kotlin/com/ampairs/invoice/service/InvoiceEcomServiceImplTest.kt` (wrong party → null; draft → null; lines carry no cost field)
- [x] T019 [US2] Integration test `GET .../invoices/{uid}` in `ecom/src/test/kotlin/com/ampairs/ecom/controller/BuyerInvoiceDetailIT.kt` (own → 200 with lines; other party → 404; draft → 404)

**Checkpoint**: Buyer can open an invoice.

---

## Phase 5: User Stories 3 & 4 — Order ↔ invoice linking (Priority P2)

**Goal**: From an order see its invoice(s) (US3); from an invoice see its originating order (US4).
**Independent test**: `GET .../orders/{ref}/invoices` and order-detail `invoices[]` return the order's
finalized invoices; invoice detail/list carry `order_ref`; empty/multi/pending-ingest handled.

- [x] T020 [P] [US3] Add derived finder `findByOrderRefIdAndStatusIn(orderRefId, statuses): List<Invoice>` in `invoice/.../repository/InvoiceRepository.kt`
- [x] T021 [US3] Implement `InvoiceEcomServiceImpl.listInvoicesForOrder(orderRefId, partyUid)` in `invoice/.../service/InvoiceEcomServiceImpl.kt`: finalized invoices where `orderRefId` matches AND `customerId == partyUid`; map → `BuyerInvoiceSummary`
- [x] T022 [US3] Add `GET /v1/ecom/account/orders/{ecomOrderRef}/invoices` to `ecom/.../controller/CustomerAccountController.kt`: resolve party, `getCustomerOrder(partyUid, ecomOrderRef)` (ownership re-check), read `managementOrderRef` (null → `[]`), call `listInvoicesForOrder(managementOrderRef, partyUid)`, swap `order_ref`
- [x] T023 [US3] Extend order detail: add `invoices: List<BuyerInvoiceSummary>` to `ecom/src/main/kotlin/com/ampairs/ecom/domain/dto/EcomOrderResponse.kt` and populate it in the order-detail path (`EcomOrderService`/controller) via `listInvoicesForOrder(order.managementOrderRef, partyUid)` — empty when none
- [x] T024 [US4] Ensure invoice→order reverse link surfaces `order_ref` on both `BuyerInvoiceSummary` and `BuyerInvoiceDetail` wire output (controller swap already added in T013/T017); add a small `resolveOrderRef(orderRefId): String?` controller helper wrapping `findByManagementOrderRef(...)?.ecomOrderRef` in `ecom/.../controller/CustomerAccountController.kt`
- [x] T025 [P] [US3] Unit test `listInvoicesForOrder` in `invoice/src/test/kotlin/com/ampairs/invoice/service/InvoiceEcomServiceImplTest.kt` (multiple invoices for one order; wrong-party filtered out)
- [x] T026 [US3] Integration test order↔invoice in `ecom/src/test/kotlin/com/ampairs/ecom/controller/OrderInvoiceLinkIT.kt` (order with one/multiple/zero invoices; `management_order_ref` null → `[]`; order-detail `invoices[]` matches the dedicated endpoint; non-ecom invoice → `order_ref` null)

**Checkpoint**: Both link directions resolve.

---

## Phase 6: User Story 5 — Outstanding + open bills + aging (Priority P2)

**Goal**: A linked buyer sees current balance, open bills with due dates, and aging.
**Independent test**: `GET .../outstanding` matches what the owner sees for that customer in `payment`.

- [x] T027 [US5] Implement `PartyLedgerEcomServiceImpl.outstanding(partyUid, asOf)` in `payment/src/main/kotlin/com/ampairs/payment/service/PartyLedgerEcomServiceImpl.kt`: delegate to `OutstandingService.openBills`, `AgingService`/`PartyBalanceRepository.findByPartyUid`; map → `BuyerOutstandingResponse` (direction→"DR"/"CR"); `@Service`
- [x] T028 [US5] Add `GET /v1/ecom/account/outstanding` to `ecom/.../controller/CustomerAccountController.kt`: resolve party, call `partyLedgerEcomService.outstanding(partyUid, Instant.now())`, `ApiResponse.success(...)`
- [x] T029 [P] [US5] Unit test outstanding mapping in `payment/src/test/kotlin/com/ampairs/payment/service/PartyLedgerEcomServiceImplTest.kt` (empty ledger → zero balance; DR/CR direction string; bucket labels preserved)
- [x] T030 [US5] Integration test `GET .../outstanding` in `ecom/src/test/kotlin/com/ampairs/ecom/controller/BuyerOutstandingIT.kt` (linked → own bills+aging; unlinked → 403)

**Checkpoint**: Buyer sees what they owe.

---

## Phase 7: User Story 6 — Running statement (Priority P2)

**Goal**: A linked buyer views a date-ranged statement (invoices + payments) with a running balance.
**Independent test**: `GET .../statement` — last line's running balance == closing balance.

- [x] T031 [US6] Implement `PartyLedgerEcomServiceImpl.statement(partyUid, from, to)` in `payment/.../service/PartyLedgerEcomServiceImpl.kt`: delegate to `StatementService.buildStatement`; map `PartyStatementResponse`→`BuyerStatementResponse` (EntryType→`kind`; `voucherNo`→`reference`; drop `partyUid`)
- [x] T032 [US6] Add `GET /v1/ecom/account/statement` (params `from?`,`to?`) to `ecom/.../controller/CustomerAccountController.kt`: resolve party, default `from`=null/`to`=now, call `partyLedgerEcomService.statement(...)`, `ApiResponse.success(...)`
- [x] T033 [P] [US6] Unit test statement mapping in `payment/src/test/kotlin/com/ampairs/payment/service/PartyLedgerEcomServiceImplTest.kt` (kind mapping; opening/closing signs; last-line == closing)
- [x] T034 [US6] Integration test `GET .../statement` in `ecom/src/test/kotlin/com/ampairs/ecom/controller/BuyerStatementIT.kt` (interleaved invoice/payment lines; running balance foots to closing; window filter honored)

**Checkpoint**: Buyer can reconcile.

---

## Phase 8: User Story 7 — Multiple accounts (Priority P3)

**Goal**: A buyer linked to >1 CRM account picks which account's data to view via `customer_id`.
**Independent test**: valid `customer_id` → that account; a `customer_id` the login isn't linked to →
never returns that account's data.

- [ ] T035 [US7] Integration test multi-account resolution across all endpoints in `ecom/src/test/kotlin/com/ampairs/ecom/controller/BuyerMultiAccountIT.kt` (linked `customer_id` → its data; not-linked `customer_id` → default account, never the requested; picker `getCustomers` unchanged)

**Checkpoint**: Account picker drives every read.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Security/isolation hardening, docs, and gate.

- [ ] T036 [P] Tenant-isolation integration test in `ecom/src/test/kotlin/com/ampairs/ecom/controller/BuyerTenantIsolationIT.kt` (same buyer, two storefronts/workspaces → each returns only that workspace's invoices/ledger)
- [x] T037 [P] Verify no cross-module leakage: `ecom` imports only `com.ampairs.core.service.*` for these calls (no `invoice`/`payment` impl or repo imports) — grep check noted in PR description
- [x] T038 [P] Confirm `NO_MIGRATION_NEEDED.md` carries the spec-029 entry (already added) and no `db/migration` files were introduced
- [ ] T039 Run the full gate: `./gradlew :core:test :invoice:test :payment:test :ecom:test` then `./gradlew testAll` (Docker up); fix failures
- [x] T040 [P] Update `docs/modules/ecom.md` (buyer account section) with the new endpoints and the order↔invoice link; keep `spec.md` §12 open questions in sync if any resolved

---

## Phase 10: Mobile app (spec Phase 3 — OPTIONAL, separate repo `ampairs-app`)

**Purpose**: Buyer UI in `feature/ecom`. Optional follow-on; can ship after backend. Compile all three
targets after commonMain changes.

- [x] T041 [P] Add `getInvoices/getInvoice/getOrderInvoices/getOutstanding/getStatement` to `ampairs-app/feature/ecom/src/commonMain/kotlin/com/ampairs/ecom/data/api/EcomApi.kt`
- [x] T042 [P] Add `BuyerInvoiceRepository` + `StatementRepository` (live reads; optional cache) under `ampairs-app/feature/ecom/src/commonMain/.../data/repository/`
- [x] T043 Build `InvoiceListScreen` + `InvoiceDetailScreen` (with originating-order link) + VMs `@ContributesIntoMap(WorkspaceScope::class)` under `ampairs-app/feature/ecom/.../ui/account/`; money via `formatMoney(amount, LocalAppLocale.current)`, dates via `formatDate(..., locale)`
- [x] T044 Add `invoices` section to the existing order-detail screen; `AccountStatementScreen` + VM; wire into `AccountScreen`; reuse the account picker
- [ ] T045 Compile all targets: `./gradlew androidApp:compileDebugKotlinAndroid shared:compileKotlinIosSimulatorArm64 desktopApp:compileKotlin`

---

## Dependencies & Story Completion Order

- **Setup (P1)** → **Foundational (P2)** block everything.
- **US1 (Phase 3)** is the MVP; **US2 (Phase 4)** depends only on Foundational.
- **US3/US4 (Phase 5)** depend on Foundational + the `InvoiceEcomService` impl file from US1 (T011) and the `findByManagementOrderRef` finder (T012).
- **US5 (Phase 6)** and **US6 (Phase 7)** depend only on Foundational (the `PartyLedgerEcomService` interface/DTOs) — independent of the invoice stories; can run in parallel with Phases 3–5.
- **US7 (Phase 8)** depends on the endpoints existing (Phases 3–7).
- **Polish (Phase 9)** last. **Mobile (Phase 10)** after backend endpoints exist.

```
Setup → Foundational ┬→ US1 → US2 ┐
                     │            ├→ US3/US4 ┐
                     ├→ US5 ──────┤          ├→ US7 → Polish → (Mobile)
                     └→ US6 ──────┘──────────┘
```

## Parallel Execution Examples

- **Foundational**: T004–T007 in parallel (two separate `core` files), then T008/T009.
- **After Foundational**, two tracks in parallel:
  - Invoice track: US1 → US2 → US3/US4 (`invoice` + `ecom`)
  - Ledger track: US5 ∥ US6 (`payment` + `ecom`)
- Within a story, `[P]` unit tests (T014, T018, T025, T029, T033) run alongside impl once their target file exists.

## Implementation Strategy

- **MVP = Phase 1 + Phase 2 + Phase 3 (US1)** — buyer invoice list end-to-end.
- **Increment 2**: US2 (detail) + US3/US4 (linking) — the full invoice experience (spec Phase 1a).
- **Increment 3**: US5 + US6 (money position — spec Phase 1b), then US7 + Polish.
- **Increment 4 (optional)**: Mobile (Phase 10).
- Each phase is independently testable and shippable; no phase requires a later phase to be useful.
