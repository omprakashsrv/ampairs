package com.ampairs.payment.service

import com.ampairs.payment.domain.dto.OpenBillResponse
import com.ampairs.payment.domain.dto.PartyStatementResponse
import com.ampairs.payment.domain.dto.StatementLine
import com.ampairs.payment.domain.enums.Direction
import com.ampairs.payment.domain.enums.EntryType
import com.ampairs.payment.domain.model.PartyBalance
import com.ampairs.payment.repository.PartyBalanceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant

/**
 * Spec 029 — buyer money-position mapping: enum→string, per-party aging derived from the party's own
 * bills, and statement lines mapped with a buyer-facing kind (last line running balance preserved).
 */
@ExtendWith(MockitoExtension::class)
class PartyLedgerEcomServiceImplTest {

    @Mock private lateinit var statementService: StatementService
    @Mock private lateinit var outstandingService: OutstandingService
    @Mock private lateinit var partyBalanceRepository: PartyBalanceRepository

    private lateinit var service: PartyLedgerEcomServiceImpl

    @BeforeEach
    fun setup() {
        service = PartyLedgerEcomServiceImpl(statementService, outstandingService, partyBalanceRepository)
    }

    private fun bill(no: String, outstanding: String, bucket: String) = OpenBillResponse(
        billUid = "B$no", billNo = no, billDate = Instant.parse("2026-08-01T00:00:00Z"),
        total = BigDecimal(outstanding), allocated = BigDecimal.ZERO, outstanding = BigDecimal(outstanding),
        dueDate = Instant.parse("2026-08-31T00:00:00Z"), daysOverdue = 0, agingBucket = bucket,
    )

    @Test
    fun `outstanding with empty ledger yields zero DR balance and no bills`() {
        whenever(outstandingService.openBills(eq("A"), any())).thenReturn(emptyList())
        whenever(partyBalanceRepository.findByPartyUid("A")).thenReturn(null)

        val out = service.outstanding("A", Instant.now())

        assertEquals(BigDecimal.ZERO, out.currentBalance)
        assertEquals("DR", out.balanceDirection)
        assertEquals(0, out.openBills.size)
        assertEquals(0, out.aging.size)
    }

    @Test
    fun `outstanding maps CR balance and groups aging from the party bills`() {
        whenever(outstandingService.openBills(eq("A"), any()))
            .thenReturn(listOf(bill("INV1", "100", "0-30"), bill("INV2", "50", "0-30"), bill("INV3", "20", "31-60")))
        whenever(partyBalanceRepository.findByPartyUid("A"))
            .thenReturn(PartyBalance().apply { partyUid = "A"; cachedClosingBalance = BigDecimal("-70") })

        val out = service.outstanding("A", Instant.now())

        assertEquals(BigDecimal("-70"), out.currentBalance)
        assertEquals("CR", out.balanceDirection)
        assertEquals(3, out.openBills.size)
        val byBucket = out.aging.associate { it.label to it.amount }
        assertEquals(BigDecimal("150"), byBucket["0-30"])
        assertEquals(BigDecimal("20"), byBucket["31-60"])
    }

    @Test
    fun `statement maps kinds, directions and preserves running balance`() {
        val resp = PartyStatementResponse(
            partyUid = "A",
            from = null,
            to = Instant.parse("2026-08-20T00:00:00Z"),
            openingBalance = BigDecimal.ZERO,
            openingDirection = Direction.DR,
            lines = listOf(
                StatementLine(
                    entryDate = Instant.parse("2026-08-15T10:00:00Z"), entryType = EntryType.SALES_INVOICE,
                    voucherNo = "INV-42", narration = null,
                    debit = BigDecimal("9207.50"), credit = BigDecimal.ZERO, runningBalance = BigDecimal("9207.50"),
                ),
                StatementLine(
                    entryDate = Instant.parse("2026-08-18T09:00:00Z"), entryType = EntryType.PAYMENT_IN,
                    voucherNo = "RCP-11", narration = "UPI",
                    debit = BigDecimal.ZERO, credit = BigDecimal("5000.00"), runningBalance = BigDecimal("4207.50"),
                ),
                StatementLine(
                    entryDate = Instant.parse("2026-08-19T09:00:00Z"), entryType = EntryType.CREDIT_NOTE,
                    voucherNo = "CRN-3", narration = null,
                    debit = BigDecimal.ZERO, credit = BigDecimal("100.00"), runningBalance = BigDecimal("4107.50"),
                ),
            ),
            closingBalance = BigDecimal("4107.50"),
            closingDirection = Direction.DR,
        )
        whenever(statementService.buildStatement(eq("A"), anyOrNull(), anyOrNull())).thenReturn(resp)

        val out = service.statement("A", null, null)

        assertEquals("DR", out.openingDirection)
        assertEquals("DR", out.closingDirection)
        assertEquals(listOf("INVOICE", "PAYMENT", "ADJUSTMENT"), out.lines.map { it.kind })
        assertEquals("INV-42", out.lines.first().reference)
        // last line running balance equals the closing balance
        assertEquals(out.closingBalance, out.lines.last().runningBalance)
    }
}
