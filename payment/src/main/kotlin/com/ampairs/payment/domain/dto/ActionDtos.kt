package com.ampairs.payment.domain.dto

import com.ampairs.payment.domain.enums.Direction
import com.ampairs.payment.domain.enums.EntryType
import java.math.BigDecimal
import java.time.Instant

// ── Party statement (US2 / FR-004, FR-005) ──────────────────────────────────────

data class StatementLine(
    val entryDate: Instant,
    val entryType: EntryType,
    val voucherNo: String?,
    val narration: String?,
    val debit: BigDecimal,
    val credit: BigDecimal,
    val runningBalance: BigDecimal,
)

data class PartyStatementResponse(
    val partyUid: String,
    val from: Instant?,
    val to: Instant?,
    val openingBalance: BigDecimal,
    val openingDirection: Direction,
    val lines: List<StatementLine>,
    val closingBalance: BigDecimal,
    val closingDirection: Direction,
)

// ── Open bills (US1/US4 / FR-016) ───────────────────────────────────────────────

data class OpenBillResponse(
    val billUid: String,
    val billNo: String?,
    val billDate: Instant,
    val total: BigDecimal,
    val allocated: BigDecimal,
    val outstanding: BigDecimal,
    val dueDate: Instant?,
    val daysOverdue: Long,
    val agingBucket: String,
)

// ── Aging summary (US4 / FR-017, FR-018) ────────────────────────────────────────

data class AgingBucketAmount(
    val label: String,
    val amount: BigDecimal,
)

data class PartyOverCreditLimit(
    val partyUid: String,
    val balance: BigDecimal,
    val creditLimit: BigDecimal,
)

data class AgingSummaryResponse(
    val asOf: Instant,
    val totalReceivable: BigDecimal,
    val totalPayable: BigDecimal,
    val buckets: List<AgingBucketAmount>,
    val partiesOverCreditLimit: List<PartyOverCreditLimit>,
)

// ── Recompute (FR-022 / SC-002) ─────────────────────────────────────────────────

data class RecomputeResponse(
    val partyUid: String,
    val cachedBefore: BigDecimal,
    val recomputed: BigDecimal,
    val totalDebit: BigDecimal,
    val totalCredit: BigDecimal,
    val openingSigned: BigDecimal,
    val tieOutOk: Boolean,
)

// ── Bounce / clear (US6 / FR-020, FR-021) ───────────────────────────────────────

data class BounceRequest(
    val narration: String? = null,
)
