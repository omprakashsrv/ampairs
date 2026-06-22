package com.ampairs.payment.service

import com.ampairs.payment.domain.dto.PartyStatementResponse
import com.ampairs.payment.domain.dto.StatementLine
import com.ampairs.payment.domain.enums.Direction
import com.ampairs.payment.domain.model.LedgerEntry
import com.ampairs.payment.repository.LedgerEntryRepository
import com.ampairs.payment.repository.PartyBalanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

/**
 * Builds a party statement (US2 / FR-004, FR-005): opening line first, then each active ledger
 * entry in date order with debit/credit/running-balance columns. The last line's running balance
 * equals the recomputed closing balance (signed).
 */
@Service
@Transactional(readOnly = true)
class StatementService(
    private val ledgerEntryRepository: LedgerEntryRepository,
    private val partyBalanceRepository: PartyBalanceRepository,
) {

    fun buildStatement(partyUid: String, from: Instant?, to: Instant?): PartyStatementResponse {
        val balance = partyBalanceRepository.findByPartyUid(partyUid)
        val openingSigned = balance?.openingSigned() ?: BigDecimal.ZERO
        val openingBalance = balance?.openingBalance ?: BigDecimal.ZERO
        val openingDirection = balance?.openingDirection ?: Direction.DR

        // Running balance starts from the opening signed amount. When a `from` filter excludes
        // earlier entries, fold them into the opening so the first shown running balance is correct.
        val all = ledgerEntryRepository.findByPartyUidAndActiveTrueOrderByEntryDateAsc(partyUid)
        var running = openingSigned
        val lines = mutableListOf<StatementLine>()
        for (entry in all) {
            val before = entry.entryDate.isBefore(from ?: Instant.MIN)
            val after = entry.entryDate.isAfter(to ?: Instant.MAX)
            if (before) {
                running = running.add(entry.signedAmount())
                continue
            }
            running = running.add(entry.signedAmount())
            if (after) continue
            lines.add(entry.toLine(running))
        }

        // Closing = running over ALL entries (foots to recompute regardless of the window).
        val closing = all.fold(openingSigned) { acc, e -> acc.add(e.signedAmount()) }
        return PartyStatementResponse(
            partyUid = partyUid,
            from = from,
            to = to,
            openingBalance = openingBalance,
            openingDirection = openingDirection,
            lines = lines,
            closingBalance = closing,
            closingDirection = if (closing.signum() >= 0) Direction.DR else Direction.CR,
        )
    }

    private fun LedgerEntry.toLine(running: BigDecimal): StatementLine {
        val isDebit = direction == Direction.DR
        return StatementLine(
            entryDate = entryDate,
            entryType = entryType,
            voucherNo = voucherNo,
            narration = narration,
            debit = if (isDebit) amount else BigDecimal.ZERO,
            credit = if (isDebit) BigDecimal.ZERO else amount,
            runningBalance = running,
        )
    }
}
