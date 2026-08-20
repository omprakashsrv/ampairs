# 029 — Ecom Buyer Account Statement (store-workspace access to invoices & payments)

**Status:** Draft · **Modules:** `ecom`, `core`, `payment` (+ optional `invoice`), mobile `feature/ecom`
**Related:** `OrderEcomService`, `EcomCustomerService`, spec 013 (payment-collection)

---

## 1. Summary

Storefront buyers can already see **their orders** from a store's workspace
(`GET /v1/ecom/account/orders`). This spec extends the buyer account with **their money position** —
outstanding balance, open bills, and a running account statement (invoices + payments interleaved) —
by **reading through** the payment module's existing party-ledger services, not by mirroring data.

The buyer never gets workspace membership. Access is granted the same way order access is: the
storefront **slug** resolves the tenant, and the buyer's login is resolved to a **linked CRM
customer** (`partyUid`) which keys every read.

### Why statement-centric (not an invoice/payment clone of the orders design)

Orders live in ecom as a **buyer-keyed projection** (`EcomOrder.customer_id`) because the buyer
*creates* them. Invoices and payments are **workspace documents keyed by the CRM customer**
(`Invoice.customerId`, payment `partyUid`) and mutate constantly (status, allocations, balance).
Projecting them into ecom would mean an event pipeline that re-syncs on every downstream change —
all cost, stale risk, no benefit. Instead we reuse what the payment module already computes
per party:

- `StatementService.buildStatement(partyUid, from, to)` → running ledger with debit/credit/balance
- `OutstandingService.openBills(partyUid, asOf)` → per-bill outstanding + due date + aging
- `PartyBalanceRepository.findByPartyUid(partyUid)` → current signed closing balance

A distributor buyer's real question — *"what do I owe, and what are my recent bills & payments?"* —
is exactly what these produce.

---

## 2. Goals / Non-goals

**Goals**
- Buyer-facing, slug-scoped endpoints for: current outstanding, open bills (+aging), and a
  date-ranged account statement.
- Read-through the existing `payment` services via a new **`core` interface** (mirrors
  `OrderEcomService`), so `ecom` depends only on `core`.
- **Buyer-safe DTOs** — never reuse owner/management DTOs; expose only what the buyer may see.
- Server-side resolution of the buyer→CRM-customer link; a client-sent `customer_id` is only
  honored if the login is genuinely linked to it.

**Non-goals (this pass)**
- No new persistence / projection / eventing. Read-only over existing tables.
- No "ecom-originated documents only" filter — the party ledger is account-level by design
  (see §7, Data-scope decision).
- Invoice **PDF** download is Phase 2 (§9).
- Making the buyer a workspace member or exposing `/payment/v1/**` / `/invoice/v1/**` to them.

---

## 3. User stories

- **US1 — Outstanding at a glance.** As a linked buyer, I open my account and see my current
  outstanding balance and the bills that are still open, with due dates and how overdue they are.
- **US2 — Statement.** As a linked buyer, I view a date-ranged statement of my invoices and payments
  with a running balance, so I can reconcile against my own books.
- **US3 — Multiple accounts.** As a buyer linked to more than one CRM account (owner/manager/worker),
  I pick which account's statement to view (same picker as checkout's "ordering for").
- **US4 — Unlinked buyer.** As a buyer not linked to any account, I'm told to link my account (or
  contact the owner) rather than shown someone else's data.

---

## 4. Functional requirements

- **FR-001** Buyer endpoints live under `/v1/ecom/account/**`, `@PreAuthorize("isAuthenticated()")`,
  with **no** `X-Workspace-ID` (public surface; exempted in `SessionUserFilter`). The
  `storefront_slug` query param resolves the tenant via
  `storefrontService.getPublishedStorefrontBySlug(slug).ownerId`, set with `TenantContextHolder` in a
  `try/finally` (identical to the existing order endpoints).
- **FR-002** The controller resolves the CRM customer with
  `EcomCustomerService.resolveLinkedCustomerId(userId, requestedCustomerId)`:
  - returns the requested account **only** if the login is actually linked to it;
  - else the login's default (else first) linked account;
  - else `null` → respond **403** with a `NOT_LINKED` error code (US4). Restricted contacts
    (`active = false`) are treated as unlinked.
- **FR-003** All reads are keyed by the resolved `partyUid` (== CRM customer uid == `Invoice.customerId`
  == payment `partyUid`). The client-sent `customer_id` is never trusted directly.
- **FR-004** `GET /v1/ecom/account/outstanding` → current balance + open bills + aging summary for the
  resolved party.
- **FR-005** `GET /v1/ecom/account/statement?from&to` → running-balance statement for the resolved
  party over the window (defaults: `from` = null → account opening, `to` = now).
- **FR-006** Responses use **new buyer-safe DTOs** defined in `core` alongside the service interface
  (precedent: `EcomCustomerAccount`). They MUST NOT carry internal-only fields (no raw `partyUid` echo
  beyond what the buyer needs, no cost/margin/other-party data, no owner audit columns).
- **FR-007** Every response is `ApiResponse<T>`; exceptions bubble to `GlobalExceptionHandler`
  (no try/catch for business errors). Paginated lists use `PageResponse`.
- **FR-008** Cross-module rule: `ecom` reaches `payment`/`invoice` **only** through the new `core`
  interface(s) — no cross-module repository or service-impl imports.

---

## 5. API design (backend, `/api` prefix global)

| Method & path | Purpose | Query params |
|---|---|---|
| `GET /v1/ecom/account/customers` *(exists)* | Account picker ("which account?") | `storefront_slug` |
| `GET /v1/ecom/account/outstanding` | Current balance + open bills + aging | `storefront_slug`, `customer_id?` |
| `GET /v1/ecom/account/statement` | Running ledger (invoices+payments) | `storefront_slug`, `customer_id?`, `from?`, `to?` |
| `GET /v1/ecom/account/invoices/{invoiceUid}` *(Phase 2)* | Single invoice detail / PDF | `storefront_slug`, `customer_id?` |

**Buyer-safe response DTOs (defined in `core.service`, mapped from payment DTOs):**

```kotlin
data class BuyerOutstandingResponse(
    val currentBalance: BigDecimal,      // signed closing (receivable +)
    val balanceDirection: String,        // "DR" | "CR"
    val openBills: List<BuyerOpenBill>,  // from OutstandingService.openBills
    val aging: List<BuyerAgingBucket>,   // label + amount
)
data class BuyerOpenBill(
    val billNo: String?, val billDate: Instant, val total: BigDecimal,
    val outstanding: BigDecimal, val dueDate: Instant?, val daysOverdue: Long, val agingBucket: String,
    // note: billUid/allocated omitted unless invoice detail (Phase 2) needs the lookup key
)
data class BuyerStatementResponse(
    val from: Instant?, val to: Instant?,
    val openingBalance: BigDecimal, val openingDirection: String,
    val lines: List<BuyerStatementLine>,
    val closingBalance: BigDecimal, val closingDirection: String,
)
data class BuyerStatementLine(
    val date: Instant, val kind: String,        // e.g. "INVOICE" | "PAYMENT" | "ADJUSTMENT" (mapped from EntryType)
    val reference: String?, val narration: String?,
    val debit: BigDecimal, val credit: BigDecimal, val runningBalance: BigDecimal,
)
```

Mapping notes: `EntryType`/`Direction` enums are **not** serialized raw — map to a small buyer-facing
`kind` string. `voucherNo` → `reference`. Drop `partyUid` from line/statement payloads (it's the
workspace's internal id; the buyer already knows which of *their* accounts they picked).

---

## 6. Cross-module wiring

**New `core` interface (impl in `payment`)** — mirrors `OrderEcomService`/`EcomCustomerService`:

```kotlin
// core/.../service/PartyLedgerEcomService.kt
interface PartyLedgerEcomService {
    fun outstanding(partyUid: String, asOf: Instant): BuyerOutstandingResponse
    fun statement(partyUid: String, from: Instant?, to: Instant?): BuyerStatementResponse
}
```

- Impl `PartyLedgerEcomServiceImpl` in `payment` delegates to the existing `StatementService`,
  `OutstandingService`, `AgingService`/`PartyBalanceRepository` and maps to the buyer DTOs.
- Requires an active tenant context (set by the ecom controller) — same contract as the other
  ecom-facing services.
- **Phase 2:** `InvoiceEcomService` in `core` (impl in `invoice`) for `getBuyerInvoice(uid)` /
  PDF, guarded so `invoice.customerId == resolvedPartyUid`.

Dependency direction stays clean: `ecom → core ← payment` (payment already depends on `core`,
`invoice`, `customer`; ecom already depends on `core`).

---

## 7. Security & data scope

- **Link is the gate.** A buyer only ever reads the party they're linked to; the link is re-resolved
  server-side per request via `EcomCustomerService`. No membership, no `X-Workspace-ID`, no RBAC role.
- **Ownership re-check** mirrors `getCustomerOrder` (which throws `AccessDeniedException` when
  `order.customerId != customerId`). Here the equivalent is: only a `partyUid` returned by
  `resolveLinkedCustomerId` is ever queried.
- **Tenant isolation.** All reads run inside `setCurrentTenant(storefront.ownerId)`; `@TenantId`
  auto-filtering keeps the query within the storefront's workspace.

**Data-scope decision (previously flagged).** The party ledger is **account-level**: a buyer sees the
linked CRM customer's *entire* position in that workspace — including counter/offline/other-channel
invoices and payments, not only ecom-originated ones. This is the intended semantic for an "account
statement" (it's the same number the owner would read to that customer). An "ecom-originated only"
view is **out of scope**: the party ledger has no per-channel dimension, so it would require a
separate filter/derivation and a different (bill-list) surface. Recommend shipping the full-account
statement; revisit channel-filtering only if a concrete requirement appears.

---

## 8. Mobile app (`ampairs-app`, `feature/ecom`) — optional, can follow backend

Ecom is a **live/pull** surface, not central-sync (see `EcomOrderRepository` — checkout is a live
call, `pushPendingToServer` is a no-op). The statement follows the same shape:

- **`EcomApi`**: `getOutstanding(slug, customerId?)`, `getStatement(slug, customerId?, from?, to?)`.
- **`StatementRepository`** (UI-invoked live read; optional lightweight cache like `EcomOrderRepository`).
- **UI** under `ecom/ui/account/`: `AccountStatementScreen` + `StatementViewModel`
  (`@ContributesIntoMap(WorkspaceScope::class)`), reachable from `AccountScreen`; reuse the account
  picker already backing `getCustomers`.
- Money via `formatMoney(amount, LocalAppLocale.current)`; dates via `formatDate(..., locale)`.
- Compile all three targets after commonMain changes.

---

## 9. Phasing

1. **Phase 1 (backend, this spec's core):** `PartyLedgerEcomService` (core+payment), buyer DTOs,
   `outstanding` + `statement` endpoints on `CustomerAccountController`, tests.
2. **Phase 2 (backend):** `InvoiceEcomService` + `GET .../invoices/{uid}` detail & PDF.
3. **Phase 3 (app):** `EcomApi`/repository/UI in `feature/ecom`.

---

## 10. Testing

- **Unit:** payment→buyer DTO mapping (enum→kind, direction strings, opening/closing signs, empty
  ledger → zero balance).
- **Integration (ecom):**
  - linked buyer → 200 with own party's statement/outstanding;
  - unlinked buyer → 403 `NOT_LINKED`;
  - buyer sends a `customer_id` they're **not** linked to → resolves to their own default (never the
    requested one) or 403, never the other account's data;
  - multi-account buyer → correct account per `customer_id`;
  - tenant isolation: same buyer, two storefronts/workspaces → each returns only that workspace's ledger;
  - restricted contact (`active=false`) → treated as unlinked.
- Reuse existing `payment` statement/outstanding tests for the underlying math (unchanged).

---

## 11. Migrations

**None.** Read-only over existing `payment`/`invoice` tables. Record in `NO_MIGRATION_NEEDED.md`.

---

## 12. Open questions

- **OQ-1** Statement window defaults & max range (cap to e.g. 12 months to bound ledger scans)?
- **OQ-2** Should `outstanding` include the aging *summary buckets* (from `AgingService`) or just
  per-bill buckets? (Spec assumes buckets included; cheap.)
- **OQ-3** Phase 2 invoice PDF: reuse the workspace invoice PDF renderer, or a buyer-branded variant?
- **OQ-4** Do we surface payment *receipts* the buyer can download, or is the statement line enough
  for Phase 1? (Assumed: statement line only.)
