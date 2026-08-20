# 029 — Ecom Buyer Account: invoices, statement & order↔invoice linking (store-workspace access)

**Status:** Draft · **Modules:** `ecom`, `core`, `invoice`, `payment`, mobile `feature/ecom`
**Related:** `OrderEcomService`, `EcomCustomerService`, spec 013 (payment-collection)

---

## 1. Summary

Storefront buyers can already see **their orders** from a store's workspace
(`GET /v1/ecom/account/orders`). This spec extends the buyer account with:

1. **Their invoices** — a paginated list of the linked CRM customer's invoices, plus single-invoice
   detail (§5).
2. **Order ↔ invoice linking** — from an order the buyer can see the invoice(s) it produced, and from
   an invoice they can see the originating order (§5A). The link already exists in the data model —
   `EcomOrder.managementOrderRef` == workspace `Order.uid` == `Invoice.orderRefId` — so this is a
   read/join, no new columns.
3. **Their money position** — current outstanding balance, open bills (+aging), and a running account
   statement (invoices + payments interleaved), by **reading through** the payment module's existing
   party-ledger services rather than mirroring data.

The buyer never gets workspace membership. Access is granted the same way order access is: the
storefront **slug** resolves the tenant, and the buyer's login is resolved to a **linked CRM
customer** (`partyUid` == `Invoice.customerId`) which keys every read.

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
- Buyer-facing, slug-scoped endpoints for: **invoice list + detail**, **order↔invoice links**,
  current outstanding, open bills (+aging), and a date-ranged account statement.
- Read-through the existing `invoice` and `payment` services via new **`core` interfaces** (mirroring
  `OrderEcomService`), so `ecom` depends only on `core`.
- **Buyer-safe DTOs** — never reuse owner/management DTOs; expose only what the buyer may see.
- Server-side resolution of the buyer→CRM-customer link; a client-sent `customer_id` is only
  honored if the login is genuinely linked to it. Every invoice/order read is re-checked against the
  resolved party (`invoice.customerId == partyUid`, `order.customerId == partyUid`).

**Non-goals (this pass)**
- No new persistence / projection / eventing. Read-only + joins over existing tables.
- No "ecom-originated documents only" filter — the party ledger and invoice list are account-level by
  design (see §7, Data-scope decision).
- Invoice **PDF** download is Phase 2 (§9).
- Making the buyer a workspace member or exposing `/payment/v1/**` / `/invoice/v1/**` to them.

---

## 3. User stories

- **US1 — Invoice list.** As a linked buyer, I open my account and see a paginated list of my invoices
  (number, date, status, total, amount due), newest first.
- **US2 — Invoice detail.** As a linked buyer, I open one invoice to see its line items and totals.
- **US3 — Order → invoice.** As a linked buyer viewing an order, I see the invoice(s) raised for it
  and can jump to them.
- **US4 — Invoice → order.** As a linked buyer viewing an invoice, I see which order it was raised
  from and can jump back.
- **US5 — Outstanding at a glance.** As a linked buyer, I see my current outstanding balance and the
  bills still open, with due dates and how overdue they are.
- **US6 — Statement.** As a linked buyer, I view a date-ranged statement of my invoices and payments
  with a running balance, so I can reconcile against my own books.
- **US7 — Multiple accounts.** As a buyer linked to more than one CRM account (owner/manager/worker),
  I pick which account's invoices/statement to view (same picker as checkout's "ordering for").
- **US8 — Unlinked buyer.** As a buyer not linked to any account, I'm told to link my account (or
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
- **FR-004** `GET /v1/ecom/account/invoices` → **paginated** list of the resolved party's invoices
  (`invoice.customerId == partyUid`), newest first. Drafts are excluded — a buyer only sees finalized
  invoices (status ∈ finalized set, mirroring `OutstandingService`'s finalized-only rule). Each item
  carries the linked order ref so the client can render "for order …" without a second call.
- **FR-005** `GET /v1/ecom/account/invoices/{invoiceUid}` → single invoice detail (line items + totals)
  for the resolved party. If `invoice.customerId != partyUid` → **404** (never 403 — don't confirm the
  invoice exists in another account). Drafts → 404.
- **FR-006** `GET /v1/ecom/account/orders/{ecomOrderRef}/invoices` → invoices raised for that order.
  Resolves the order via `getCustomerOrder(partyUid, ecomOrderRef)` (existing ownership re-check),
  reads its `managementOrderRef`, and returns invoices where `orderRefId == managementOrderRef`
  (empty list if none yet). The existing order-detail response (`GET .../orders/{ecomOrderRef}`) is
  **extended** with an `invoices` array of the same lightweight refs (single round-trip for US3).
- **FR-007** Invoice-detail and each list item expose the reverse link: `orderRef` (the buyer-facing
  `EcomOrder.ecomOrderRef`/`orderNumber`) resolved from `invoice.orderRefId → EcomOrder.managementOrderRef`,
  or `null` for a non-ecom invoice (US4). The reverse resolution is a single lookup in `ecom` (the
  controller owns both sides), so no new cross-module call is needed for it.
- **FR-008** `GET /v1/ecom/account/outstanding` → current balance + open bills + aging summary for the
  resolved party.
- **FR-009** `GET /v1/ecom/account/statement?from&to` → running-balance statement for the resolved
  party over the window (defaults: `from` = null → account opening, `to` = now).
- **FR-010** Responses use **new buyer-safe DTOs** defined in `core` alongside the service interfaces
  (precedent: `EcomCustomerAccount`). They MUST NOT carry internal-only fields (no cost/margin/
  other-party data, no owner audit columns, no `synced`/tenant flags).
- **FR-011** Every response is `ApiResponse<T>`; exceptions bubble to `GlobalExceptionHandler`
  (no try/catch for business errors). Paginated lists use `PageResponse`.
- **FR-012** Cross-module rule: `ecom` reaches `invoice`/`payment` **only** through the new `core`
  interface(s) — no cross-module repository or service-impl imports.

---

## 5. API design (backend, `/api` prefix global)

| Method & path | Purpose | Query params |
|---|---|---|
| `GET /v1/ecom/account/customers` *(exists)* | Account picker ("which account?") | `storefront_slug` |
| `GET /v1/ecom/account/orders` *(exists)* | Buyer's orders | `storefront_slug`, `customer_id?`, paging |
| `GET /v1/ecom/account/orders/{ecomOrderRef}` *(exists, **extended** with `invoices[]`)* | Order detail + linked invoices | `storefront_slug`, `customer_id?` |
| `GET /v1/ecom/account/orders/{ecomOrderRef}/invoices` | Invoices raised for an order | `storefront_slug`, `customer_id?` |
| `GET /v1/ecom/account/invoices` | Buyer's invoice list (finalized, paginated) | `storefront_slug`, `customer_id?`, `page?`, `size?` |
| `GET /v1/ecom/account/invoices/{invoiceUid}` | Single invoice detail (+ order link) | `storefront_slug`, `customer_id?` |
| `GET /v1/ecom/account/outstanding` | Current balance + open bills + aging | `storefront_slug`, `customer_id?` |
| `GET /v1/ecom/account/statement` | Running ledger (invoices+payments) | `storefront_slug`, `customer_id?`, `from?`, `to?` |
| `GET /v1/ecom/account/invoices/{invoiceUid}/pdf` *(Phase 2)* | Invoice PDF | `storefront_slug`, `customer_id?` |

**Buyer-safe invoice DTOs (defined in `core.service`, mapped from `invoice` DTOs):**

```kotlin
data class BuyerInvoiceSummary(
    val invoiceUid: String, val invoiceNumber: String,
    val invoiceDate: Instant, val status: String,     // buyer-facing status string, not raw enum
    val total: BigDecimal, val amountDue: BigDecimal,  // due = total − Σ active allocations
    val orderRef: String?,                             // buyer-facing EcomOrder ref, null if non-ecom
)
data class BuyerInvoiceDetail(
    val invoiceUid: String, val invoiceNumber: String,
    val invoiceDate: Instant, val status: String,
    val orderRef: String?,
    val lines: List<BuyerInvoiceLine>,
    val subtotal: BigDecimal, val taxTotal: BigDecimal, val total: BigDecimal, val amountDue: BigDecimal,
)
data class BuyerInvoiceLine(
    val description: String, val quantity: BigDecimal,
    val unitPrice: BigDecimal, val lineTotal: BigDecimal,   // display-facing only; no cost/margin
)
```

The order-detail response gains: `val invoices: List<BuyerInvoiceSummary>` (may be empty). The same
`BuyerInvoiceSummary` is reused for the order→invoices list and the invoice list.

**Buyer-safe ledger DTOs (defined in `core.service`, mapped from payment DTOs):**

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
workspace's internal id; the buyer already knows which of *their* accounts they picked). Invoice
`status` is likewise mapped to a buyer-facing string, not the raw `InvoiceStatus` enum.

---

## 5A. Order ↔ invoice linkage (no schema change)

The link is already present across three tables; this feature only reads/joins it:

```
EcomOrder.managementOrderRef ──┐   (set when the ecom order is ingested as a management order)
                               ├──►  == workspace Order.uid
Invoice.orderRefId ────────────┘   (set when the invoice is raised from that order)
```

- **Order → invoices:** `orderRefId == ecomOrder.managementOrderRef`. New repo query
  `InvoiceRepository.findByOrderRefIdAndStatusIn(orderRefId, finalizedStatuses)`.
- **Invoice → order:** `invoice.orderRefId` gives the workspace `Order.uid`; the ecom controller maps
  it back to the buyer-facing ref via `EcomOrderRepository.findByManagementOrderRef(orderRefId)?.ecomOrderRef`
  (add this finder). `null` when the invoice didn't originate from an ecom order.
- **Guard:** a linked invoice is only returned if `invoice.customerId == partyUid`; a linked order is
  only surfaced if it already passed `getCustomerOrder(partyUid, …)`. The join never widens visibility
  beyond the resolved party.

Edge cases: an order with **no invoice yet** → empty `invoices[]`; an order that produced **multiple**
invoices (partial fulfilment) → all finalized ones listed; an ecom order whose `managementOrderRef` is
still null (ingest pending) → empty list, not an error.

---

## 6. Cross-module wiring

Two new `core` interfaces, each mirroring `OrderEcomService`/`EcomCustomerService` (interface in
`core`, impl in the owning module, buyer DTOs in `core` beside the interface):

```kotlin
// core/.../service/InvoiceEcomService.kt   (impl in invoice)
interface InvoiceEcomService {
    fun listBuyerInvoices(partyUid: String, pageable: Pageable): Page<BuyerInvoiceSummary>
    fun getBuyerInvoice(invoiceUid: String, partyUid: String): BuyerInvoiceDetail?   // null → 404
    fun listInvoicesForOrder(orderRefId: String, partyUid: String): List<BuyerInvoiceSummary>
}

// core/.../service/PartyLedgerEcomService.kt   (impl in payment)
interface PartyLedgerEcomService {
    fun outstanding(partyUid: String, asOf: Instant): BuyerOutstandingResponse
    fun statement(partyUid: String, from: Instant?, to: Instant?): BuyerStatementResponse
}
```

- `InvoiceEcomServiceImpl` (in `invoice`) filters by `customerId == partyUid`, excludes drafts, maps
  entities → buyer DTOs, and computes `amountDue` from the payment allocations it can already see via
  the existing `payment`→`invoice` integration **or** leaves `amountDue = total` if allocation lookup
  is out of scope for `invoice` (see OQ-5 — cleaner to compute `amountDue` in the `payment`-backed
  `outstanding` call and keep the invoice list total-only). The `orderRef` reverse-link is filled by
  the **ecom controller**, not the impl (ecom owns the `EcomOrder` side), so `InvoiceEcomService`
  returns `orderRefId` (workspace uid) and ecom swaps it for the buyer-facing ref.
- `PartyLedgerEcomServiceImpl` (in `payment`) delegates to the existing `StatementService`,
  `OutstandingService`, `AgingService`/`PartyBalanceRepository`.
- Both require an active tenant context (set by the ecom controller) — same contract as the other
  ecom-facing services.

Dependency direction stays clean: `ecom → core ← invoice` and `ecom → core ← payment` (invoice and
payment already depend on `core`; ecom already depends on `core`). No new module-to-module edges.

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

- **`EcomApi`**: `getInvoices(slug, customerId?, page)`, `getInvoice(slug, uid, customerId?)`,
  `getOrderInvoices(slug, ecomOrderRef, customerId?)`, `getOutstanding(slug, customerId?)`,
  `getStatement(slug, customerId?, from?, to?)`.
- **Repositories** (UI-invoked live reads; optional lightweight cache like `EcomOrderRepository`):
  `BuyerInvoiceRepository`, `StatementRepository`.
- **UI** under `ecom/ui/account/`: `InvoiceListScreen` + `InvoiceDetailScreen` (with an "originating
  order" link), the existing order-detail screen gains a "Invoices" section from `invoices[]`, plus
  `AccountStatementScreen`. ViewModels `@ContributesIntoMap(WorkspaceScope::class)`, reachable from
  `AccountScreen`; reuse the account picker already backing `getCustomers`.
- Money via `formatMoney(amount, LocalAppLocale.current)`; dates via `formatDate(..., locale)`.
- Compile all three targets after commonMain changes.

---

## 9. Phasing

1. **Phase 1a (backend — invoices & linking):** `InvoiceEcomService` (core+invoice), buyer invoice
   DTOs, `InvoiceRepository.findByOrderRefIdAndStatusIn` + `EcomOrderRepository.findByManagementOrderRef`,
   endpoints `GET .../invoices`, `GET .../invoices/{uid}`, `GET .../orders/{ref}/invoices`, and the
   `invoices[]` addition to order detail. Tests.
2. **Phase 1b (backend — money position):** `PartyLedgerEcomService` (core+payment), buyer ledger DTOs,
   `outstanding` + `statement` endpoints. Tests.
3. **Phase 2 (backend):** invoice **PDF** — `GET .../invoices/{uid}/pdf`.
4. **Phase 3 (app):** `EcomApi`/repository/UI in `feature/ecom` — invoice list + detail with the
   order↔invoice cross-links, then the statement/outstanding screens.

Phases 1a and 1b are independent and can land in either order or in parallel.

---

## 10. Testing

- **Unit:**
  - invoice→buyer DTO mapping (status→string, drafts excluded, line mapping omits cost/margin);
  - payment→buyer DTO mapping (enum→kind, direction strings, opening/closing signs, empty ledger →
    zero balance).
- **Integration (ecom) — invoices & linking:**
  - linked buyer → 200 with own party's invoice list (finalized only; drafts absent);
  - invoice detail for own invoice → 200; invoice belonging to **another** party → 404 (not 403);
  - order → invoices: order with one/multiple/zero invoices returns the right set; order detail carries
    the same `invoices[]`;
  - invoice → order: ecom-originated invoice exposes `orderRef`; non-ecom invoice → `orderRef = null`;
  - `managementOrderRef` still null (ingest pending) → order→invoices returns empty list, not error.
- **Integration (ecom) — money position:**
  - linked buyer → 200 with own party's statement/outstanding;
- **Integration (ecom) — access control (applies to every endpoint):**
  - unlinked buyer → 403 `NOT_LINKED`;
  - buyer sends a `customer_id` they're **not** linked to → resolves to their own default (never the
    requested one) or 403, never the other account's data;
  - multi-account buyer → correct account per `customer_id`;
  - tenant isolation: same buyer, two storefronts/workspaces → each returns only that workspace's
    invoices/ledger;
  - restricted contact (`active=false`) → treated as unlinked.
- Reuse existing `payment` statement/outstanding and `invoice` tests for the underlying math (unchanged).

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
- **OQ-5** `amountDue` on the invoice list: compute it in `invoice` (needs a `payment`-allocation
  read from the invoice module — an extra cross-module edge) or leave the list total-only and let the
  `outstanding` endpoint own per-bill dues? (Spec leans total-only in the list; dues via `outstanding`.)
- **OQ-6** Invoice `status` buyer-facing vocabulary — expose the raw finalized/paid/cancelled states,
  or collapse to a smaller buyer set (e.g. "Raised" / "Paid" / "Part-paid" / "Cancelled")?
- **OQ-7** Should the invoice list be filterable (by date range / paid-vs-open), or is newest-first
  pagination enough for the first cut? (Assumed: pagination only.)
