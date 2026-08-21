package com.ampairs.payment.service

import com.ampairs.core.service.BuyerAgingBucket
import com.ampairs.core.service.BuyerOpenBill
import com.ampairs.core.service.BuyerOutstandingResponse
import com.ampairs.core.service.BuyerStatementLine
import com.ampairs.core.service.BuyerStatementResponse
import com.ampairs.core.service.PartyLedgerEcomService
import com.ampairs.payment.domain.dto.PartyStatementResponse
import com.ampairs.payment.domain.dto.StatementLine
import com.ampairs.payment.domain.enums.Direction
import com.ampairs.payment.domain.enums.EntryType
import com.ampairs.payment.repository.PartyBalanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

/**
 * Spec 029 — buyer-facing read of the workspace party ledger, exposed to `ecom` via the `core`
 * [PartyLedgerEcomService] interface. Delegates to the existing [StatementService] /
 * [OutstandingService] and the cached [PartyBalanceRepository] closing balance; maps to buyer-safe
 * DTOs (enums → strings, no raw `partyUid` echo). Every read is keyed by the resolved [partyUid];
 * requires an active tenant context (set by the ecom controller). Read-only.
 *
 * Aging buckets are derived from the party's own open bills — not from the workspace-wide
 * `AgingService.summary()` — so a buyer only ever sees their own account's ageing.
 */
@Service
@Transactional(readOnly = true)
class PartyLedgerEcomServiceImpl(
    private val statementService: StatementService,
    private val outstandingService: OutstandingService,
    private val partyBalanceRepository: PartyBalanceRepository,
) : PartyLedgerEcomService {

    override fun outstanding(partyUid: String, asOf: Instant): BuyerOutstandingResponse {
        val bills = outstandingService.openBills(partyUid, asOf)
        val closing = partyBalanceRepository.findByPartyUid(partyUid)?.cachedClosingBalance ?: BigDecimal.ZERO
        val aging = bills
            .groupBy { it.agingBucket }
            .map { (label, group) -> BuyerAgingBucket(label, group.fold(BigDecimal.ZERO) { a, b -> a.add(b.outstanding) }) }
        return BuyerOutstandingResponse(
            currentBalance = closing,
            balanceDirection = directionOf(closing),
            openBills = bills.map {
                BuyerOpenBill(
                    billUid = it.billUid,
                    billNo = it.billNo,
                    billDate = it.billDate,
                    total = it.total,
                    outstanding = it.outstanding,
                    dueDate = it.dueDate,
                    daysOverdue = it.daysOverdue,
                    agingBucket = it.agingBucket,
                )
            },
            aging = aging,
        )
    }

    override fun statement(partyUid: String, from: Instant?, to: Instant?): BuyerStatementResponse =
        statementService.buildStatement(partyUid, from, to).toBuyer()

    private fun PartyStatementResponse.toBuyer() = BuyerStatementResponse(
        from = from,
        to = to,
        openingBalance = openingBalance,
        openingDirection = openingDirection.name,
        lines = lines.map { it.toBuyer() },
        closingBalance = closingBalance,
        closingDirection = closingDirection.name,
    )

    private fun StatementLine.toBuyer() = BuyerStatementLine(
        date = entryDate,
        kind = kindOf(entryType),
        reference = voucherNo,
        narration = narration,
        debit = debit,
        credit = credit,
        runningBalance = runningBalance,
    )

    private companion object {
        fun directionOf(signed: BigDecimal): String =
            if (signed.signum() >= 0) Direction.DR.name else Direction.CR.name

        /** Collapse the internal [EntryType] taxonomy to a small buyer-facing kind. */
        fun kindOf(type: EntryType): String = when (type) {
            EntryType.SALES_INVOICE, EntryType.PURCHASE_BILL -> "INVOICE"
            EntryType.PAYMENT_IN, EntryType.PAYMENT_OUT -> "PAYMENT"
            else -> "ADJUSTMENT"
        }
    }
}
