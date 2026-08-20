package com.ampairs.core.service

import java.math.BigDecimal
import java.time.Instant

/**
 * Cross-module bridge (spec 029): read a storefront buyer's money position — current balance, open
 * bills + aging, and a running account statement — from the workspace `payment` party ledger.
 *
 * The interface lives in `core`, the implementation in `payment`, so `ecom` depends only on `core`
 * (same pattern as [OrderEcomService] / [InvoiceEcomService]). Every read is keyed by the resolved
 * CRM party (`partyUid`), resolved by the ecom controller before calling. Requires an active tenant
 * context (set by the controller from the storefront slug). Read-only: delegates to the existing
 * `StatementService` / `OutstandingService` / `AgingService`.
 */
interface PartyLedgerEcomService {

    /** Current balance + open bills (per-bill outstanding, due date, aging) + aging summary for [partyUid]. */
    fun outstanding(partyUid: String, asOf: Instant): BuyerOutstandingResponse

    /**
     * Running-balance statement for [partyUid] over the window [[from], [to]] (null [from] = account
     * opening, null [to] = now). The last line's running balance equals the closing balance.
     */
    fun statement(partyUid: String, from: Instant?, to: Instant?): BuyerStatementResponse
}

data class BuyerOutstandingResponse(
    val currentBalance: BigDecimal,
    val balanceDirection: String,
    val openBills: List<BuyerOpenBill>,
    val aging: List<BuyerAgingBucket>,
)

data class BuyerOpenBill(
    val billNo: String?,
    val billDate: Instant,
    val total: BigDecimal,
    val outstanding: BigDecimal,
    val dueDate: Instant?,
    val daysOverdue: Long,
    val agingBucket: String,
)

data class BuyerAgingBucket(
    val label: String,
    val amount: BigDecimal,
)

data class BuyerStatementResponse(
    val from: Instant?,
    val to: Instant?,
    val openingBalance: BigDecimal,
    val openingDirection: String,
    val lines: List<BuyerStatementLine>,
    val closingBalance: BigDecimal,
    val closingDirection: String,
)

data class BuyerStatementLine(
    val date: Instant,
    val kind: String,
    val reference: String?,
    val narration: String?,
    val debit: BigDecimal,
    val credit: BigDecimal,
    val runningBalance: BigDecimal,
)
