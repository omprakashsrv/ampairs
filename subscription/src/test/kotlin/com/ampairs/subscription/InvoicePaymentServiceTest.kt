package com.ampairs.subscription

import com.ampairs.subscription.domain.model.Invoice
import com.ampairs.subscription.domain.model.InvoiceStatus
import com.ampairs.subscription.domain.model.PaymentMethod
import com.ampairs.subscription.domain.model.PaymentMethodType
import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.model.PaymentStatus
import com.ampairs.subscription.domain.model.PaymentTransaction
import com.ampairs.subscription.domain.repository.PaymentMethodRepository
import com.ampairs.subscription.domain.repository.PaymentTransactionRepository
import com.ampairs.subscription.domain.repository.SubscriptionInvoiceRepository
import com.ampairs.subscription.domain.service.InvoicePaymentService
import com.ampairs.subscription.exception.SubscriptionException
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.YearMonth
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoicePaymentServiceTest {

    @Mock
    private lateinit var invoiceRepository: SubscriptionInvoiceRepository

    @Mock
    private lateinit var paymentMethodRepository: PaymentMethodRepository

    @Mock
    private lateinit var paymentTransactionRepository: PaymentTransactionRepository

    private lateinit var service: InvoicePaymentService

    @BeforeEach
    fun setUp() {
        service = InvoicePaymentService(invoiceRepository, paymentMethodRepository, paymentTransactionRepository)
        whenever(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }
        whenever(paymentTransactionRepository.save(any<PaymentTransaction>())).thenAnswer { it.arguments[0] }
    }

    private fun invoice(block: Invoice.() -> Unit = {}): Invoice = Invoice().apply {
        uid = "INV-1"
        workspaceId = "WS-1"
        invoiceNumber = "INV-2025-001"
        subscriptionId = 7
        currency = "INR"
        totalAmount = BigDecimal("500.00")
        block()
    }

    @Test
    fun `generatePaymentLink returns Razorpay link for INR`() {
        val inv = invoice { currency = "INR" }

        val link = service.generatePaymentLink(inv)

        assertTrue(link.contains("razorpay"))
        assertEquals(link, inv.paymentLinkUrl)
    }

    @Test
    fun `generatePaymentLink returns Stripe link for non-INR`() {
        val inv = invoice { currency = "USD" }

        val link = service.generatePaymentLink(inv)

        assertTrue(link.contains("stripe"))
    }

    @Test
    fun `processAutoPayment fails when auto-payment not enabled`() {
        val inv = invoice { autoPaymentEnabled = false }

        assertThrows(SubscriptionException.PaymentFailed::class.java) {
            service.processAutoPayment(inv)
        }
    }

    @Test
    fun `processAutoPayment fails when payment method missing in repo`() {
        val inv = invoice { autoPaymentEnabled = true; paymentMethodId = 55L }
        whenever(paymentMethodRepository.findById(55L)).thenReturn(Optional.empty())

        assertThrows(SubscriptionException.PaymentMethodNotFound::class.java) {
            service.processAutoPayment(inv)
        }
    }

    @Test
    fun `processAutoPayment fails when payment method not verified`() {
        val inv = invoice { autoPaymentEnabled = true; paymentMethodId = 55L }
        val pm = PaymentMethod().apply { active = false }
        whenever(paymentMethodRepository.findById(55L)).thenReturn(Optional.of(pm))

        assertThrows(SubscriptionException.PaymentFailed::class.java) {
            service.processAutoPayment(inv)
        }
    }

    @Test
    fun `processAutoPayment fails for unimplemented Razorpay auto-charge`() {
        val inv = invoice { autoPaymentEnabled = true; paymentMethodId = 55L }
        val pm = PaymentMethod().apply {
            active = true
            type = PaymentMethodType.UPI
            paymentProvider = PaymentProvider.RAZORPAY
        }
        whenever(paymentMethodRepository.findById(55L)).thenReturn(Optional.of(pm))

        // chargeRazorpayPaymentMethod throws PaymentFailed (not implemented)
        assertThrows(SubscriptionException.PaymentFailed::class.java) {
            service.processAutoPayment(inv)
        }
    }

    @Test
    fun `processAutoPayment fails for unsupported provider`() {
        val inv = invoice { autoPaymentEnabled = true; paymentMethodId = 55L }
        val pm = PaymentMethod().apply {
            active = true
            type = PaymentMethodType.WALLET
            paymentProvider = PaymentProvider.GOOGLE_PLAY
        }
        whenever(paymentMethodRepository.findById(55L)).thenReturn(Optional.of(pm))

        assertThrows(SubscriptionException.PaymentFailed::class.java) {
            service.processAutoPayment(inv)
        }
    }

    @Test
    fun `processManualPayment fails when provider verification returns false`() {
        val inv = invoice()

        // Razorpay/Stripe verification stubs return false → PaymentFailed
        assertThrows(SubscriptionException.PaymentFailed::class.java) {
            service.processManualPayment(inv, "pay_123", PaymentProvider.RAZORPAY)
        }
    }

    @Test
    fun `processManualPayment fails for unsupported provider`() {
        val inv = invoice()

        assertThrows(SubscriptionException.PaymentFailed::class.java) {
            service.processManualPayment(inv, "pay_123", PaymentProvider.APP_STORE)
        }
    }

    @Test
    fun `card payment method is verified when active and not expired`() {
        val pm = PaymentMethod().apply {
            active = true
            type = PaymentMethodType.CREDIT_CARD
            expYear = YearMonth.now().year + 1
            expMonth = 12
        }
        assertTrue(pm.isVerified())
    }
}
