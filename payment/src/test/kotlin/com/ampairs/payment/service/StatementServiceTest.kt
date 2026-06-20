package com.ampairs.payment.service

import com.ampairs.payment.domain.enums.Direction
import com.ampairs.payment.domain.enums.EntryType
import com.ampairs.payment.domain.model.LedgerEntry
import com.ampairs.payment.domain.model.PartyBalance
import com.ampairs.payment.repository.LedgerEntryRepository
import com.ampairs.payment.repository.PartyBalanceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant

/**
 * Statement (US2, FR-004, SC-002): the last line's running balance equals the recomputed closing,
 * and lines render in date order with debit/credit columns.
 */
@ExtendWith(MockitoExtension::class)
class StatementServiceTest {

    @Mock private lateinit var ledgerRepo: LedgerEntryRepository
    @Mock private lateinit var balanceRepo: PartyBalanceRepository

    private lateinit var service: StatementService

    private fun entry(uid: String, type: EntryType, amount: String, date: String): LedgerEntry =
        LedgerEntry().apply {
            this.uid = uid
            partyUid = "A"
            entryType = type
            direction = type.naturalDirection()
            this.amount = BigDecimal(amount)
            entryDate = Instant.parse(date)
            active = true
        }

    @BeforeEach
    fun setup() {
        service = StatementService(ledgerRepo, balanceRepo)
    }

    @Test
    fun `last running balance equals closing for a mixed party`() {
        val opening = PartyBalance().apply {
            partyUid = "A"
            openingBalance = BigDecimal("2000.00")
            openingDirection = Direction.DR
            openingAsOf = Instant.parse("2026-04-01T00:00:00Z")
        }
        val ledger = listOf(
            entry("LDG_INV1", EntryType.SALES_INVOICE, "3000.00", "2026-04-05T00:00:00Z"),
            entry("LDG_INV2", EntryType.SALES_INVOICE, "1500.00", "2026-04-10T00:00:00Z"),
            entry("LDG_RCP1", EntryType.PAYMENT_IN, "4000.00", "2026-04-20T00:00:00Z"),
        )
        whenever(balanceRepo.findByPartyUid("A")).thenReturn(opening)
        whenever(ledgerRepo.findByPartyUidAndActiveTrueOrderByEntryDateAsc(any())).thenReturn(ledger)

        val statement = service.buildStatement("A", null, null)

        // 2000 + 3000 + 1500 - 4000 = 2500
        assertEquals(0, statement.closingBalance.compareTo(BigDecimal("2500.00")))
        assertEquals(Direction.DR, statement.closingDirection)
        assertEquals(3, statement.lines.size)
        assertEquals(
            0,
            statement.lines.last().runningBalance.compareTo(statement.closingBalance),
            "last running balance must equal closing",
        )
        // First line debit column carries the sale amount.
        assertEquals(0, statement.lines.first().debit.compareTo(BigDecimal("3000.00")))
        // Receipt line uses the credit column.
        assertEquals(0, statement.lines.last().credit.compareTo(BigDecimal("4000.00")))
    }
}
