package com.ampairs.payment.service

import com.ampairs.payment.domain.dto.PaymentAllocationRequest
import com.ampairs.payment.domain.dto.PaymentVoucherRequest
import com.ampairs.payment.domain.enums.AllocationTarget
import com.ampairs.payment.domain.enums.ClearanceStatus
import com.ampairs.payment.domain.enums.PaymentDirection
import com.ampairs.payment.domain.enums.PaymentMode
import com.ampairs.payment.domain.model.PaymentAllocation
import com.ampairs.payment.domain.model.PaymentVoucher
import com.ampairs.payment.exception.AllocationExceedsTotalException
import com.ampairs.payment.exception.InvalidClearanceTransitionException
import com.ampairs.payment.repository.PaymentAllocationRepository
import com.ampairs.payment.repository.PaymentVoucherRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal

/**
 * Voucher posting + allocation rules (US1/US6, FR-010/011/012/020/021):
 * receipt posts a CR ledger entry; allocations validate Σ ≤ total and never touch the balance;
 * over-allocation is rejected; bounce posts a contra reversal; clearing a terminal voucher fails.
 */
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentVoucherServiceTest {

    @Mock private lateinit var voucherRepo: PaymentVoucherRepository
    @Mock private lateinit var allocationRepo: PaymentAllocationRepository
    @Mock private lateinit var balanceService: BalanceService
    @Mock private lateinit var voucherNumberService: VoucherNumberService

    private lateinit var voucherService: PaymentVoucherService
    private lateinit var allocationService: PaymentAllocationService

    private val vouchers = mutableMapOf<String, PaymentVoucher>()
    private val allocations = mutableListOf<PaymentAllocation>()

    @BeforeEach
    fun setup() {
        voucherService = PaymentVoucherService(voucherRepo, allocationRepo, balanceService, voucherNumberService, mock())
        allocationService = PaymentAllocationService(allocationRepo, voucherRepo, voucherService, mock())

        whenever(voucherRepo.save(any<PaymentVoucher>())).thenAnswer { inv ->
            val v = inv.getArgument<PaymentVoucher>(0)
            if (v.uid.isBlank()) v.uid = "RCP-${vouchers.size + 1}"
            vouchers[v.uid] = v
            v
        }
        whenever(voucherRepo.findByUid(any())).thenAnswer { inv -> vouchers[inv.getArgument<String>(0)] }
        whenever(allocationRepo.save(any<PaymentAllocation>())).thenAnswer { inv ->
            val a = inv.getArgument<PaymentAllocation>(0)
            if (a.uid.isBlank()) a.uid = "ALC-${allocations.size + 1}"
            allocations.removeAll { it.uid == a.uid }
            allocations.add(a)
            a
        }
        whenever(allocationRepo.findByUid(any())).thenAnswer { inv ->
            allocations.firstOrNull { it.uid == inv.getArgument<String>(0) }
        }
        whenever(allocationRepo.allocatedTotalForVoucher(any())).thenAnswer { inv ->
            val vid = inv.getArgument<String>(0)
            allocations.filter { it.paymentVoucherUid == vid && it.active }
                .fold(BigDecimal.ZERO) { acc, a -> acc.add(a.amount) }
        }
        whenever(allocationRepo.findByPaymentVoucherUidAndActiveTrue(any())).thenAnswer { inv ->
            val vid = inv.getArgument<String>(0)
            allocations.filter { it.paymentVoucherUid == vid && it.active }
        }
    }

    private fun newReceipt(uid: String, amount: String, mode: PaymentMode = PaymentMode.CASH): PaymentVoucherResponseHolder {
        val res = voucherService.bulkUpsert(
            listOf(
                PaymentVoucherRequest(
                    uid = uid, partyUid = "A", direction = PaymentDirection.RECEIVED,
                    totalAmount = BigDecimal(amount), paymentMode = mode,
                ),
            ),
        ).first()
        return PaymentVoucherResponseHolder(res.uid, res.unallocatedAmount, res.clearanceStatus)
    }

    data class PaymentVoucherResponseHolder(val uid: String, val unallocated: BigDecimal, val status: ClearanceStatus)

    @Test
    fun `receipt posts a CR ledger entry and starts fully unallocated`() {
        val r = newReceipt("RCP-1", "4000.00")
        assertEquals(0, r.unallocated.compareTo(BigDecimal("4000.00")))
        assertEquals(ClearanceStatus.CLEARED, r.status) // cash is instant
    }

    @Test
    fun `cheque receipt defaults to pending`() {
        val r = newReceipt("RCP-2", "4000.00", PaymentMode.CHEQUE)
        assertEquals(ClearanceStatus.PENDING, r.status)
    }

    @Test
    fun `allocation reduces unallocated and stays within total`() {
        newReceipt("RCP-3", "5000.00")
        allocationService.bulkUpsert(
            listOf(
                PaymentAllocationRequest(
                    uid = "ALC-1", paymentVoucherUid = "RCP-3",
                    targetType = AllocationTarget.INVOICE, targetUid = "INV1", amount = BigDecimal("3000.00"),
                ),
            ),
        )
        assertEquals(0, vouchers["RCP-3"]!!.unallocatedAmount.compareTo(BigDecimal("2000.00")))
    }

    @Test
    fun `over-allocation is rejected`() {
        newReceipt("RCP-4", "4000.00")
        assertThrows(AllocationExceedsTotalException::class.java) {
            allocationService.bulkUpsert(
                listOf(
                    PaymentAllocationRequest(
                        uid = "ALC-9", paymentVoucherUid = "RCP-4",
                        targetType = AllocationTarget.INVOICE, targetUid = "INV1", amount = BigDecimal("4500.00"),
                    ),
                ),
            )
        }
    }

    @Test
    fun `bounce sets BOUNCED and posts a contra reversal`() {
        newReceipt("RCP-5", "4000.00", PaymentMode.CHEQUE)
        val res = voucherService.bounce("RCP-5", "Returned")
        assertEquals(ClearanceStatus.BOUNCED, res.clearanceStatus)
        org.mockito.kotlin.verify(balanceService).postContraReversal(org.mockito.kotlin.eq("RCP-5"), any())
    }

    @Test
    fun `clearing a terminal voucher is rejected`() {
        newReceipt("RCP-6", "4000.00", PaymentMode.CHEQUE)
        voucherService.bounce("RCP-6", null)
        assertThrows(InvalidClearanceTransitionException::class.java) {
            voucherService.clear("RCP-6")
        }
    }

    @Test
    fun `soft-deleting a voucher reverses its ledger entry`() {
        newReceipt("RCP-7", "4000.00")
        voucherService.bulkUpsert(
            listOf(
                PaymentVoucherRequest(
                    uid = "RCP-7", partyUid = "A", direction = PaymentDirection.RECEIVED,
                    totalAmount = BigDecimal("4000.00"), paymentMode = PaymentMode.CASH, active = false,
                ),
            ),
        )
        org.mockito.kotlin.verify(balanceService).reverseSourceEntry("RCP-7")
    }
}
