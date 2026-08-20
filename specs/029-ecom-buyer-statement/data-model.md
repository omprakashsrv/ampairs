# Phase 1 Data Model — 029 Ecom Buyer Account

**No schema changes.** This feature is read-only over existing entities plus a join. This document
captures (a) the existing entities/columns it reads, (b) the link topology, and (c) the new
**buyer-safe DTOs** (read models) it returns.

## A. Existing entities read (no modification)

### `Invoice` (module `invoice`)
Relevant columns: `uid`, `invoiceNumber`, `invoiceDate: Instant`, `status: InvoiceStatus`,
`customerId: String?`, `orderRefId: String?`, `totalCost: Double`, items via `@NamedEntityGraph`.
- Read filters: `customerId == partyUid`; `status ∈ finalizedStatuses` (drafts excluded).
- `orderRefId` → the workspace `Order.uid` this invoice was raised from (nullable for non-order invoices).

### `EcomOrder` (module `ecom`)
Relevant columns: `ecomOrderRef` (buyer-facing lookup key), `orderNumber` (display), `customerId`
(CRM customer == `partyUid`), `managementOrderRef` (→ workspace `Order.uid`, nullable until ingest),
`status`.

### `PartyBalance`, `LedgerEntry`, `PaymentAllocation` (module `payment`)
Read via existing services — not directly:
- `StatementService.buildStatement(partyUid, from, to)` → `PartyStatementResponse` (opening + lines + closing).
- `OutstandingService.openBills(partyUid, asOf)` → `List<OpenBillResponse>` (per-bill outstanding, due, aging).
- `AgingService` / `PartyBalanceRepository.findByPartyUid(partyUid)` → current signed closing balance + buckets.

## B. Link topology (the join, no new column)

```
EcomOrder.managementOrderRef ──┐
                               ├──► workspace Order.uid  ◄── the shared key
Invoice.orderRefId ────────────┘

Order → invoices:  Invoice.orderRefId == EcomOrder.managementOrderRef
Invoice → order:   Invoice.orderRefId ──(EcomOrderRepository.findByManagementOrderRef)──► EcomOrder.ecomOrderRef
```

**Validation / edge rules**
- A linked invoice is returned only if `invoice.customerId == partyUid` (never widens visibility).
- A linked order is surfaced only after it passed `getCustomerOrder(partyUid, ecomOrderRef)`.
- `managementOrderRef == null` (ingest pending) → order→invoices returns `[]`, not an error.
- One order may map to **multiple** finalized invoices (partial fulfilment) → list all.
- Invoice with `orderRefId == null` or no matching `EcomOrder` → `orderRef = null` (non-ecom invoice).

## C. New buyer-safe DTOs (in `core.service`, beside the interfaces)

All are Kotlin `data class`; timestamps `Instant`; money `BigDecimal`; enums mapped to strings.
None expose cost/margin, audit columns, tenant/`synced` flags, or other parties' data.

### Invoice DTOs (mapped from `Invoice` in `InvoiceEcomServiceImpl`)

```kotlin
data class BuyerInvoiceSummary(
    val invoiceUid: String,
    val invoiceNumber: String,
    val invoiceDate: Instant,
    val status: String,            // buyer-facing, mapped from InvoiceStatus (finalized only)
    val total: BigDecimal,
    val orderRefId: String?,       // raw workspace order uid; ecom swaps → buyer-facing orderRef
)

data class BuyerInvoiceDetail(
    val invoiceUid: String,
    val invoiceNumber: String,
    val invoiceDate: Instant,
    val status: String,
    val orderRefId: String?,
    val lines: List<BuyerInvoiceLine>,
    val subtotal: BigDecimal,
    val taxTotal: BigDecimal,
    val total: BigDecimal,
)

data class BuyerInvoiceLine(
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,     // display price only — no cost/margin
    val lineTotal: BigDecimal,
)
```

> The controller replaces `orderRefId` with a buyer-facing `orderRef` (from `EcomOrder`) before
> serializing, so the wire DTO the buyer sees carries `order_ref`, not the workspace uid. (Kept as one
> DTO with the controller doing the swap; alternatively a separate wire DTO — see contracts.)

### Ledger DTOs (mapped from payment DTOs in `PartyLedgerEcomServiceImpl`)

```kotlin
data class BuyerOutstandingResponse(
    val currentBalance: BigDecimal,
    val balanceDirection: String,          // "DR" | "CR"
    val openBills: List<BuyerOpenBill>,
    val aging: List<BuyerAgingBucket>,
)
data class BuyerOpenBill(
    val billNo: String?, val billDate: Instant, val total: BigDecimal,
    val outstanding: BigDecimal, val dueDate: Instant?, val daysOverdue: Long, val agingBucket: String,
)
data class BuyerAgingBucket(val label: String, val amount: BigDecimal)

data class BuyerStatementResponse(
    val from: Instant?, val to: Instant?,
    val openingBalance: BigDecimal, val openingDirection: String,
    val lines: List<BuyerStatementLine>,
    val closingBalance: BigDecimal, val closingDirection: String,
)
data class BuyerStatementLine(
    val date: Instant, val kind: String,   // "INVOICE" | "PAYMENT" | "ADJUSTMENT" (from EntryType)
    val reference: String?, val narration: String?,
    val debit: BigDecimal, val credit: BigDecimal, val runningBalance: BigDecimal,
)
```

### Extended existing DTO

`EcomOrderResponse` gains: `val invoices: List<BuyerInvoiceSummary>` (may be empty), populated by the
ecom order-detail path via `InvoiceEcomService.listInvoicesForOrder(managementOrderRef, partyUid)`.

## D. New repository finders (derived queries — no `@Query` needed)

```kotlin
// invoice/repository/InvoiceRepository.kt
fun findByCustomerIdAndStatusIn(customerId: String, statuses: Collection<InvoiceStatus>, pageable: Pageable): Page<Invoice>
fun findByOrderRefIdAndStatusIn(orderRefId: String, statuses: Collection<InvoiceStatus>): List<Invoice>

// ecom/repository/EcomOrderRepository.kt
fun findByManagementOrderRef(managementOrderRef: String): EcomOrder?
```

`finalizedStatuses = {InvoiceStatus.INVOICED}` — the sole finalize boundary the system keys off
(`InvoiceFinalizedEvent`, ledger posting, stock movement, analytics). `DRAFT` and `NEW` are
pre-finalization and never shown to the buyer. Invoice detail lookup uses the existing `findByUid`
+ entity graph, then applies the `customerId == partyUid` and `status ∈ finalizedStatuses` guards.
