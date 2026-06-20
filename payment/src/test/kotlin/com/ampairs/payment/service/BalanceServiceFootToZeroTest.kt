package com.ampairs.payment.service

import com.ampairs.customer.domain.service.CustomerService
import com.ampairs.payment.domain.enums.Direction
import com.ampairs.payment.domain.enums.EntryType
import com.ampairs.payment.domain.enums.SourceType
import com.ampairs.payment.domain.model.LedgerEntry
import com.ampairs.payment.domain.model.PartyBalance
import com.ampairs.payment.repository.LedgerEntryRepository
import com.ampairs.payment.repository.PartyBalanceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

/**
 * The master invariant test (SC-002/006, FR-022): for every party,
 * `openingSigned + ΣDr − ΣCr == cachedClosingBalance`, and `Σ all party balances ==
 * Σ receivable − Σ payable`, with zero rounding drift across a mixed transaction set
 * (sales, multi-bill receipts, advances, returns, write-offs, edits, backdated entries).
 *
 * The repository aggregation queries are reproduced faithfully in-memory so the engine's
 * arithmetic is exercised without a database.
 */
@ExtendWith(MockitoExtension::class)
class BalanceServiceFootToZeroTest {

    @Mock private lateinit var ledgerRepo: LedgerEntryRepository
    @Mock private lateinit var balanceRepo: PartyBalanceRepository
    @Mock private lateinit var customerService: CustomerService

    private lateinit var service: BalanceService

    // In-memory state shared by the stubbed repositories.
    private val entries = mutableListOf<LedgerEntry>()
    private val balances = mutableMapOf<String, PartyBalance>()

    @BeforeEach
    fun setup() {
        service = BalanceService(ledgerRepo, balanceRepo, customerService)

        whenever(ledgerRepo.signedSumForParty(any())).thenAnswer { inv ->
            val party = inv.getArgument<String>(0)
            entries.filter { it.partyUid == party && it.active }
                .fold(BigDecimal.ZERO) { acc, e -> acc.add(e.signedAmount()) }
        }
        whenever(ledgerRepo.totalDebitForParty(any())).thenAnswer { inv ->
            val party = inv.getArgument<String>(0)
            entries.filter { it.partyUid == party && it.active && it.direction == Direction.DR }
                .fold(BigDecimal.ZERO) { acc, e -> acc.add(e.amount) }
        }
        whenever(ledgerRepo.totalCreditForParty(any())).thenAnswer { inv ->
            val party = inv.getArgument<String>(0)
            entries.filter { it.partyUid == party && it.active && it.direction == Direction.CR }
                .fold(BigDecimal.ZERO) { acc, e -> acc.add(e.amount) }
        }
        whenever(ledgerRepo.findByUid(any())).thenAnswer { inv ->
            val uid = inv.getArgument<String>(0)
            entries.firstOrNull { it.uid == uid }
        }
        whenever(ledgerRepo.save(any<LedgerEntry>())).thenAnswer { inv ->
            val e = inv.getArgument<LedgerEntry>(0)
            entries.removeAll { it.uid == e.uid && e.uid.isNotBlank() }
            entries.add(e)
            e
        }
        whenever(balanceRepo.findByPartyUid(any())).thenAnswer { inv -> balances[inv.getArgument<String>(0)] }
        whenever(balanceRepo.save(any<PartyBalance>())).thenAnswer { inv ->
            val b = inv.getArgument<PartyBalance>(0)
            balances[b.partyUid] = b
            b
        }
        whenever(customerService.getCustomerByUid(any())).thenReturn(null)
    }

    private fun seedOpening(party: String, amount: String, direction: Direction) {
        balances[party] = PartyBalance().apply {
            partyUid = party
            openingBalance = BigDecimal(amount)
            openingDirection = direction
            openingAsOf = Instant.parse("2026-04-01T00:00:00Z")
        }
    }

    private fun post(
        party: String,
        uid: String,
        type: EntryType,
        amount: String,
        date: String,
        direction: Direction = type.naturalDirection(),
    ) {
        val e = LedgerEntry().apply {
            this.uid = uid
            partyUid = party
            entryType = type
            this.direction = direction
            this.amount = BigDecimal(amount)
            sourceType = SourceType.MANUAL
            entryDate = Instant.parse(date)
            active = true
        }
        // Use save() through repo so the in-memory store is the single source.
        ledgerRepo.save(e)
        service.recompute(party)
    }

    @Test
    fun `mixed transaction set foots to zero for every party and in aggregate`() {
        // Party A: opening 2000 DR, +3000 sale, +1500 sale, -4000 receipt → 2500 DR
        seedOpening("A", "2000.00", Direction.DR)
        service.recompute("A")
        post("A", "LDG_INV_A1", EntryType.SALES_INVOICE, "3000.00", "2026-04-05T00:00:00Z")
        post("A", "LDG_INV_A2", EntryType.SALES_INVOICE, "1500.00", "2026-04-10T00:00:00Z")
        post("A", "LDG_RCP_A1", EntryType.PAYMENT_IN, "4000.00", "2026-04-20T00:00:00Z")

        // Party B: no opening, +5000 sale, -7000 receipt (overpay) → -2000 (advance, payable)
        post("B", "LDG_INV_B1", EntryType.SALES_INVOICE, "5000.00", "2026-04-06T00:00:00Z")
        post("B", "LDG_RCP_B1", EntryType.PAYMENT_IN, "7000.00", "2026-04-21T00:00:00Z")

        // Party C: opening 3000 DR, -500 sales return, -3000 write-off, backdated +250 adjustment DR
        seedOpening("C", "3000.00", Direction.DR)
        service.recompute("C")
        post("C", "LDG_CRN_C1", EntryType.SALES_RETURN, "500.00", "2026-04-15T00:00:00Z")
        post("C", "LDG_WRO_C1", EntryType.WRITE_OFF, "3000.00", "2026-05-01T00:00:00Z")
        post("C", "LDG_ADJ_C1", EntryType.ADJUSTMENT, "250.00", "2026-03-20T00:00:00Z", Direction.DR)

        // Edit a sale (same uid) on party A: 1500 → 2000
        post("A", "LDG_INV_A2", EntryType.SALES_INVOICE, "2000.00", "2026-04-10T00:00:00Z")

        // Tie-out per party.
        val a = service.recomputeWithTieOut("A")
        val b = service.recomputeWithTieOut("B")
        val c = service.recomputeWithTieOut("C")
        assertTrue(a.tieOutOk, "Party A must foot")
        assertTrue(b.tieOutOk, "Party B must foot")
        assertTrue(c.tieOutOk, "Party C must foot")

        // Expected closings.
        assertEquals(0, a.recomputed.compareTo(BigDecimal("3000.0000")), "A: 2000+3000+2000-4000")
        assertEquals(0, b.recomputed.compareTo(BigDecimal("-2000.0000")), "B: 5000-7000")
        assertEquals(0, c.recomputed.compareTo(BigDecimal("-250.0000")), "C: 3000-500-3000+250")

        // Aggregate: Σ balances == Σ receivable − Σ payable, zero drift.
        val closings = listOf(a.recomputed, b.recomputed, c.recomputed)
        val sumAll = closings.fold(BigDecimal.ZERO) { acc, x -> acc.add(x) }
        val receivable = closings.filter { it.signum() > 0 }.fold(BigDecimal.ZERO) { acc, x -> acc.add(x) }
        val payable = closings.filter { it.signum() < 0 }.fold(BigDecimal.ZERO) { acc, x -> acc.add(x.abs()) }
        assertEquals(
            0,
            sumAll.setScale(4, RoundingMode.HALF_UP)
                .compareTo(receivable.subtract(payable).setScale(4, RoundingMode.HALF_UP)),
            "Σ balances must equal Σ receivable − Σ payable",
        )
        assertEquals(0, sumAll.compareTo(BigDecimal("750.0000")), "3000 - 2000 - 250")
    }

    @Test
    fun `reversing a source entry restores the prior balance`() {
        // Post via the engine so the entry uid is LDG_<sourceUid> (deterministic).
        service.postSourceEntry(
            partyUid = "X", entryType = EntryType.PAYMENT_IN, amount = BigDecimal("1000.00"),
            sourceType = SourceType.PAYMENT, sourceUid = "RCP_X1", voucherNo = "RCP/1",
            entryDate = Instant.parse("2026-05-01T00:00:00Z"),
        )
        service.postSourceEntry(
            partyUid = "X", entryType = EntryType.SALES_INVOICE, amount = BigDecimal("1000.00"),
            sourceType = SourceType.INVOICE, sourceUid = "INV_X1", voucherNo = "INV/1",
            entryDate = Instant.parse("2026-05-02T00:00:00Z"),
        )
        assertEquals(0, service.recompute("X").compareTo(BigDecimal("0.0000")))

        // Reverse the receipt → balance returns to +1000 (only the sale remains).
        service.reverseSourceEntry("RCP_X1")
        assertEquals(0, service.recompute("X").compareTo(BigDecimal("1000.0000")))
    }
}
