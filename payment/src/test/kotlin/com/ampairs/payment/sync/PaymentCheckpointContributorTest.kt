package com.ampairs.payment.sync

import com.ampairs.payment.repository.AdjustmentVoucherRepository
import com.ampairs.payment.repository.LedgerEntryRepository
import com.ampairs.payment.repository.PartyBalanceRepository
import com.ampairs.payment.repository.PaymentAllocationRepository
import com.ampairs.payment.repository.PaymentVoucherRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentCheckpointContributorTest {

    @Mock private lateinit var paymentVoucherRepository: PaymentVoucherRepository
    @Mock private lateinit var paymentAllocationRepository: PaymentAllocationRepository
    @Mock private lateinit var ledgerEntryRepository: LedgerEntryRepository
    @Mock private lateinit var partyBalanceRepository: PartyBalanceRepository
    @Mock private lateinit var adjustmentVoucherRepository: AdjustmentVoucherRepository

    private fun contributor() = PaymentCheckpointContributor(
        paymentVoucherRepository,
        paymentAllocationRepository,
        ledgerEntryRepository,
        partyBalanceRepository,
        adjustmentVoucherRepository,
    )

    @Test
    fun `checkpoints reports all five payment entity codes`() {
        val voucherAt = Instant.parse("2026-06-01T10:00:00Z")
        val ledgerAt = Instant.parse("2026-06-02T10:00:00Z")
        whenever(paymentVoucherRepository.findMaxUpdatedAt()).thenReturn(voucherAt)
        whenever(paymentAllocationRepository.findMaxUpdatedAt()).thenReturn(null)
        whenever(ledgerEntryRepository.findMaxUpdatedAt()).thenReturn(ledgerAt)
        whenever(partyBalanceRepository.findMaxUpdatedAt()).thenReturn(null)
        whenever(adjustmentVoucherRepository.findMaxUpdatedAt()).thenReturn(null)

        val checkpoints = contributor().checkpoints()

        assertEquals(
            setOf("payment_voucher", "payment_allocation", "ledger_entry", "party_balance", "adjustment"),
            checkpoints.keys,
        )
        assertEquals(voucherAt, checkpoints["payment_voucher"])
        assertEquals(ledgerAt, checkpoints["ledger_entry"])
        assertNull(checkpoints["payment_allocation"])
        assertNull(checkpoints["party_balance"])
        assertNull(checkpoints["adjustment"])
    }
}
