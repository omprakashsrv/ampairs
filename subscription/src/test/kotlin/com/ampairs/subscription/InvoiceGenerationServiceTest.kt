package com.ampairs.subscription

import com.ampairs.subscription.domain.model.BillingMode
import com.ampairs.subscription.domain.model.BillingPreferences
import com.ampairs.subscription.domain.model.Invoice
import com.ampairs.subscription.domain.model.InvoiceStatus
import com.ampairs.subscription.domain.model.Subscription
import com.ampairs.subscription.domain.model.SubscriptionPlanDefinition
import com.ampairs.subscription.domain.repository.BillingPreferencesRepository
import com.ampairs.subscription.domain.repository.SubscriptionInvoiceRepository
import com.ampairs.subscription.domain.repository.SubscriptionRepository
import com.ampairs.subscription.domain.service.InvoiceGenerationService
import com.ampairs.subscription.domain.service.InvoicePaymentService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceGenerationServiceTest {

    @Mock private lateinit var subscriptionRepository: SubscriptionRepository
    @Mock private lateinit var invoiceRepository: SubscriptionInvoiceRepository
    @Mock private lateinit var billingPreferencesRepository: BillingPreferencesRepository
    @Mock private lateinit var invoicePaymentService: InvoicePaymentService

    private lateinit var service: InvoiceGenerationService

    @BeforeEach
    fun setUp() {
        service = InvoiceGenerationService(
            subscriptionRepository, invoiceRepository, billingPreferencesRepository, invoicePaymentService
        )
        whenever(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }
        whenever(billingPreferencesRepository.save(any<BillingPreferences>())).thenAnswer { it.arguments[0] }
    }

    private fun subscription() = Subscription().apply {
        uid = "SUB-1"
        id = 7
        workspaceId = "WS-1"
        planCode = "PRO"
        nextBillingAmount = BigDecimal("1000")
        plan = SubscriptionPlanDefinition().apply { planCode = "PRO" }
    }

    private val periodStart = Instant.now().minus(40, ChronoUnit.DAYS)
    private val periodEnd = Instant.now().minus(10, ChronoUnit.DAYS)

    @Test
    fun `generateInvoiceManually creates pending invoice with line item and India GST`() {
        val prefs = BillingPreferences().apply {
            workspaceId = "WS-1"; billingCurrency = "INR"; billingCountry = "IN"; gracePeriodDays = 15
        }
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1")).thenReturn(prefs)
        whenever(invoiceRepository.findByWorkspaceIdAndBillingPeriod(any(), any(), any())).thenReturn(emptyList())

        val invoice = service.generateInvoiceManually(subscription(), periodStart, periodEnd, "manual note")

        assertEquals("WS-1", invoice.workspaceId)
        assertEquals(InvoiceStatus.PENDING, invoice.status)
        assertEquals("INR", invoice.currency)
        assertEquals("manual note", invoice.notes)
        assertEquals(1, invoice.lineItems.size)
        // subtotal 1000, GST 18% -> tax 180.0000
        assertEquals(0, BigDecimal("1000").compareTo(invoice.subtotal))
        assertEquals(0, BigDecimal("180.0000").compareTo(invoice.taxAmount))
        // total = 1000 + 180 - 0
        assertEquals(0, BigDecimal("1180.0000").compareTo(invoice.totalAmount))
        assertTrue(invoice.invoiceNumber.startsWith("INV-"))
    }

    @Test
    fun `generateInvoiceManually applies no tax for non-India country`() {
        val prefs = BillingPreferences().apply {
            workspaceId = "WS-1"; billingCurrency = "USD"; billingCountry = "US"; gracePeriodDays = 10
        }
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1")).thenReturn(prefs)
        whenever(invoiceRepository.findByWorkspaceIdAndBillingPeriod(any(), any(), any())).thenReturn(emptyList())

        val invoice = service.generateInvoiceManually(subscription(), periodStart, periodEnd, null)

        assertEquals(0, BigDecimal("0.0000").compareTo(invoice.taxAmount))
        assertEquals(0, BigDecimal("1000").compareTo(invoice.totalAmount))
    }

    @Test
    fun `generateInvoiceManually returns existing invoice for same period`() {
        val prefs = BillingPreferences().apply { workspaceId = "WS-1"; gracePeriodDays = 15 }
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1")).thenReturn(prefs)
        val existing = Invoice().apply { invoiceNumber = "INV-EXISTING" }
        whenever(invoiceRepository.findByWorkspaceIdAndBillingPeriod(any(), any(), any())).thenReturn(listOf(existing))

        val invoice = service.generateInvoiceManually(subscription(), periodStart, periodEnd, null)

        assertSame(existing, invoice)
    }

    @Test
    fun `generateInvoiceManually creates default billing prefs when missing`() {
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1")).thenReturn(null)
        whenever(invoiceRepository.findByWorkspaceIdAndBillingPeriod(any(), any(), any())).thenReturn(emptyList())

        val invoice = service.generateInvoiceManually(subscription(), periodStart, periodEnd, null)

        // default prefs are POSTPAID/INR
        assertEquals("INR", invoice.currency)
        org.mockito.kotlin.verify(billingPreferencesRepository).save(any())
    }

    @Test
    fun `generateMonthlyInvoices only bills postpaid subscriptions`() {
        val postpaidSub = subscription()
        whenever(subscriptionRepository.findByStatus(any())).thenReturn(listOf(postpaidSub))
        val prefs = BillingPreferences().apply {
            workspaceId = "WS-1"; billingMode = BillingMode.POSTPAID; billingCurrency = "INR"; gracePeriodDays = 15
        }
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1")).thenReturn(prefs)
        whenever(invoiceRepository.findByWorkspaceIdAndBillingPeriod(any(), any(), any())).thenReturn(emptyList())

        service.generateMonthlyInvoices()

        // a payment link is sent (no auto-payment configured)
        org.mockito.kotlin.verify(invoicePaymentService).generatePaymentLink(any())
    }

    @Test
    fun `generateMonthlyInvoices skips prepaid subscriptions`() {
        whenever(subscriptionRepository.findByStatus(any())).thenReturn(listOf(subscription()))
        whenever(billingPreferencesRepository.findByWorkspaceId("WS-1"))
            .thenReturn(BillingPreferences().apply { workspaceId = "WS-1"; billingMode = BillingMode.PREPAID })

        service.generateMonthlyInvoices()

        org.mockito.kotlin.verify(invoiceRepository, org.mockito.kotlin.never()).save(any())
    }
}
