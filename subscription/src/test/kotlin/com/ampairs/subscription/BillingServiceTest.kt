package com.ampairs.subscription

import com.ampairs.subscription.domain.model.PaymentMethod
import com.ampairs.subscription.domain.model.PaymentMethodType
import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.model.PaymentStatus
import com.ampairs.subscription.domain.model.PaymentTransaction
import com.ampairs.subscription.domain.repository.PaymentMethodRepository
import com.ampairs.subscription.domain.repository.PaymentTransactionRepository
import com.ampairs.subscription.domain.repository.SubscriptionRepository
import com.ampairs.subscription.domain.service.BillingService
import com.ampairs.subscription.exception.SubscriptionException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingServiceTest {

    @Mock private lateinit var transactionRepository: PaymentTransactionRepository
    @Mock private lateinit var paymentMethodRepository: PaymentMethodRepository
    @Mock private lateinit var subscriptionRepository: SubscriptionRepository

    private lateinit var service: BillingService

    @BeforeEach
    fun setUp() {
        service = BillingService(transactionRepository, paymentMethodRepository, subscriptionRepository)
        whenever(transactionRepository.save(any<PaymentTransaction>())).thenAnswer { it.arguments[0] }
        whenever(paymentMethodRepository.save(any<PaymentMethod>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `recordTransaction sets paidAt for succeeded payment`() {
        val tx = service.recordTransaction(
            subscriptionId = "SUB-1", workspaceId = "WS-1", provider = PaymentProvider.RAZORPAY,
            externalPaymentId = "pay_1", externalInvoiceId = "inv_1",
            amount = BigDecimal("100"), currency = "INR", status = PaymentStatus.SUCCEEDED,
            description = "test", billingPeriodStart = null, billingPeriodEnd = null,
            paymentMethodType = PaymentMethodType.UPI, paymentMethodLast4 = null,
            cardBrand = null, receiptUrl = null
        )

        assertEquals(PaymentStatus.SUCCEEDED, tx.status)
        assertNotNull(tx.paidAt)
        assertEquals(0, BigDecimal("100").compareTo(tx.netAmount))
    }

    @Test
    fun `recordTransaction leaves paidAt null when pending`() {
        val tx = service.recordTransaction(
            subscriptionId = "SUB-1", workspaceId = "WS-1", provider = PaymentProvider.STRIPE,
            externalPaymentId = null, externalInvoiceId = null,
            amount = BigDecimal("50"), currency = "USD", status = PaymentStatus.PENDING,
            description = null, billingPeriodStart = null, billingPeriodEnd = null,
            paymentMethodType = null, paymentMethodLast4 = null, cardBrand = null, receiptUrl = null
        )
        assertNull(tx.paidAt)
    }

    @Test
    fun `updateTransactionStatus throws when not found`() {
        whenever(transactionRepository.findByUid("T-X")).thenReturn(null)
        assertThrows(SubscriptionException.TransactionNotFound::class.java) {
            service.updateTransactionStatus("T-X", PaymentStatus.SUCCEEDED)
        }
    }

    @Test
    fun `updateTransactionStatus updates and sets paidAt`() {
        val tx = PaymentTransaction().apply { status = PaymentStatus.PENDING }
        whenever(transactionRepository.findByUid("T-1")).thenReturn(tx)

        val result = service.updateTransactionStatus("T-1", PaymentStatus.SUCCEEDED)

        assertEquals(PaymentStatus.SUCCEEDED, result.status)
        assertNotNull(result.paidAt)
    }

    @Test
    fun `recordFailedPayment captures failure details`() {
        val tx = PaymentTransaction()
        whenever(transactionRepository.findByUid("T-1")).thenReturn(tx)

        val result = service.recordFailedPayment("T-1", "card_declined", "Insufficient funds")

        assertEquals(PaymentStatus.FAILED, result.status)
        assertEquals("card_declined", result.failureCode)
        assertEquals("Insufficient funds", result.failureReason)
    }

    @Test
    fun `recordRefund captures refund details`() {
        val tx = PaymentTransaction().apply { currency = "INR" }
        whenever(transactionRepository.findByUid("T-1")).thenReturn(tx)

        val result = service.recordRefund("T-1", BigDecimal("25"), "duplicate")

        assertEquals(PaymentStatus.REFUNDED, result.status)
        assertEquals(0, BigDecimal("25").compareTo(result.refundAmount!!))
        assertEquals("duplicate", result.refundReason)
        assertNotNull(result.refundedAt)
    }

    @Test
    fun `getTransactionByExternalId delegates to repository`() {
        val tx = PaymentTransaction()
        whenever(transactionRepository.findByExternalPaymentId("pay_1")).thenReturn(tx)
        assertEquals(tx, service.getTransactionByExternalId("pay_1"))
    }

    @Test
    fun `addPaymentMethod returns existing duplicate without saving`() {
        val existing = PaymentMethod().apply { workspaceId = "WS-1"; externalPaymentMethodId = "ext_1" }
        whenever(paymentMethodRepository.findByExternalPaymentMethodId("ext_1")).thenReturn(existing)

        service.addPaymentMethod(
            "WS-1", PaymentProvider.STRIPE, "ext_1", PaymentMethodType.CREDIT_CARD,
            "4242", "VISA", 12, 2030, "John", null, null, "j@b.com", "fp", false
        )

        verify(paymentMethodRepository, never()).save(any())
    }

    @Test
    fun `addPaymentMethod makes first method default`() {
        whenever(paymentMethodRepository.findByExternalPaymentMethodId("ext_new")).thenReturn(null)
        whenever(paymentMethodRepository.findActiveByWorkspaceId("WS-1")).thenReturn(emptyList())

        val result = service.addPaymentMethod(
            "WS-1", PaymentProvider.RAZORPAY, "ext_new", PaymentMethodType.UPI,
            null, null, null, null, null, "a@upi", null, null, null, false
        )

        assertTrue(result.isDefault)
    }

    @Test
    fun `addPaymentMethod with setAsDefault clears existing defaults`() {
        whenever(paymentMethodRepository.findByExternalPaymentMethodId("ext_2")).thenReturn(null)
        whenever(paymentMethodRepository.findActiveByWorkspaceId("WS-1")).thenReturn(listOf(PaymentMethod()))

        service.addPaymentMethod(
            "WS-1", PaymentProvider.STRIPE, "ext_2", PaymentMethodType.CREDIT_CARD,
            "1111", "MC", 1, 2031, "Jane", null, null, null, null, true
        )

        verify(paymentMethodRepository).clearDefaultByWorkspaceId(eq("WS-1"), any())
    }

    @Test
    fun `getPaymentMethods maps active methods`() {
        whenever(paymentMethodRepository.findActiveByWorkspaceId("WS-1"))
            .thenReturn(listOf(PaymentMethod().apply { type = PaymentMethodType.UPI; upiId = "a@upi" }))
        assertEquals(1, service.getPaymentMethods("WS-1").size)
    }

    @Test
    fun `getDefaultPaymentMethod returns null when none`() {
        whenever(paymentMethodRepository.findDefaultByWorkspaceId("WS-1")).thenReturn(null)
        assertNull(service.getDefaultPaymentMethod("WS-1"))
    }

    @Test
    fun `setDefaultPaymentMethod throws when not found`() {
        whenever(paymentMethodRepository.findByUid("PM-X")).thenReturn(null)
        assertThrows(SubscriptionException.PaymentMethodNotFound::class.java) {
            service.setDefaultPaymentMethod("WS-1", "PM-X")
        }
    }

    @Test
    fun `setDefaultPaymentMethod rejects cross-workspace method`() {
        whenever(paymentMethodRepository.findByUid("PM-1"))
            .thenReturn(PaymentMethod().apply { workspaceId = "WS-OTHER" })
        assertThrows(SubscriptionException.PaymentMethodNotFound::class.java) {
            service.setDefaultPaymentMethod("WS-1", "PM-1")
        }
    }

    @Test
    fun `setDefaultPaymentMethod sets the chosen method`() {
        val pm = PaymentMethod().apply { uid = "PM-1"; workspaceId = "WS-1"; type = PaymentMethodType.UPI; upiId = "a@upi" }
        whenever(paymentMethodRepository.findByUid("PM-1")).thenReturn(pm)

        val result = service.setDefaultPaymentMethod("WS-1", "PM-1")

        assertTrue(result.isDefault)
        verify(paymentMethodRepository).setAsDefault(eq("PM-1"), any())
    }

    @Test
    fun `removePaymentMethod throws when missing`() {
        whenever(paymentMethodRepository.findByUid("PM-X")).thenReturn(null)
        assertThrows(SubscriptionException.PaymentMethodNotFound::class.java) {
            service.removePaymentMethod("WS-1", "PM-X")
        }
    }

    @Test
    fun `removePaymentMethod promotes another default when removing the default`() {
        val toRemove = PaymentMethod().apply { uid = "PM-1"; workspaceId = "WS-1"; isDefault = true }
        val other = PaymentMethod().apply { uid = "PM-2"; workspaceId = "WS-1" }
        whenever(paymentMethodRepository.findByUid("PM-1")).thenReturn(toRemove)
        whenever(paymentMethodRepository.findActiveByWorkspaceId("WS-1")).thenReturn(listOf(toRemove, other))

        val result = service.removePaymentMethod("WS-1", "PM-1")

        assertTrue(result)
        verify(paymentMethodRepository).setAsDefault(eq("PM-2"), any())
        verify(paymentMethodRepository).deactivate(eq("PM-1"), any())
    }
}
