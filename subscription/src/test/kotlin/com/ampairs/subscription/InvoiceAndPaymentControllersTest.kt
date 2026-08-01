package com.ampairs.subscription

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.subscription.controller.PaymentController
import com.ampairs.subscription.controller.SubscriptionBillingPreferencesController
import com.ampairs.subscription.controller.SubscriptionInvoiceController
import com.ampairs.subscription.domain.dto.GenerateInvoiceRequest
import com.ampairs.subscription.domain.dto.InitiatePurchaseRequest
import com.ampairs.subscription.domain.dto.InitiatePurchaseResponse
import com.ampairs.subscription.domain.dto.PayInvoiceRequest
import com.ampairs.subscription.domain.dto.SubscriptionResponse
import com.ampairs.subscription.domain.dto.UpdateBillingPreferencesRequest
import com.ampairs.subscription.domain.model.BillingCycle
import com.ampairs.subscription.domain.model.BillingPreferences
import com.ampairs.subscription.domain.model.Invoice
import com.ampairs.subscription.domain.model.InvoiceStatus
import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.model.Subscription
import com.ampairs.subscription.domain.model.SubscriptionStatus
import com.ampairs.subscription.domain.service.BillingPreferencesService
import com.ampairs.subscription.domain.service.InvoiceGenerationService
import com.ampairs.subscription.domain.service.InvoicePaymentService
import com.ampairs.subscription.domain.service.PaymentOrchestrationService
import com.ampairs.subscription.domain.service.SubscriptionInvoiceQueryService
import com.ampairs.subscription.domain.service.SubscriptionService
import com.ampairs.subscription.exception.SubscriptionException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import org.mockito.quality.Strictness
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceAndPaymentControllersTest {

    @Mock private lateinit var queryService: SubscriptionInvoiceQueryService
    @Mock private lateinit var subscriptionService: SubscriptionService
    @Mock private lateinit var invoiceGenerationService: InvoiceGenerationService
    @Mock private lateinit var invoicePaymentService: InvoicePaymentService
    @Mock private lateinit var billingPreferencesService: BillingPreferencesService
    @Mock private lateinit var orchestrationService: PaymentOrchestrationService

    @BeforeEach
    fun setUp() {
        TenantContextHolder.setCurrentTenant("WS-1")
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clearTenantContext()
    }

    private fun invoiceController() =
        SubscriptionInvoiceController(queryService, subscriptionService, invoiceGenerationService, invoicePaymentService)

    private fun invoice(block: Invoice.() -> Unit = {}) = Invoice().apply {
        uid = "INV-1"; workspaceId = "WS-1"; invoiceNumber = "INV-2025-001"
        status = InvoiceStatus.PENDING; totalAmount = BigDecimal("100"); paidAmount = BigDecimal.ZERO
        block()
    }

    @Test
    fun `getInvoices maps page`() {
        whenever(queryService.getInvoicesForWorkspace(eq("WS-1"), anyOrNull(), any()))
            .thenReturn(PageImpl(listOf(invoice()), PageRequest.of(0, 20), 1))

        val resp = invoiceController().getInvoices(null, PageRequest.of(0, 20))

        assertTrue(resp.success)
        assertEquals(1, resp.data!!.content.size)
    }

    @Test
    fun `getInvoice maps single`() {
        whenever(queryService.getInvoice("INV-1")).thenReturn(invoice())
        assertEquals("INV-2025-001", invoiceController().getInvoice("INV-1").data!!.invoiceNumber)
    }

    @Test
    fun `generateInvoice uses generation service`() {
        whenever(subscriptionService.getSubscriptionById(7L)).thenReturn(Subscription().apply { uid = "SUB-1" })
        whenever(invoiceGenerationService.generateInvoiceManually(any(), any(), any(), any())).thenReturn(invoice())

        val req = GenerateInvoiceRequest(7L, Instant.now(), Instant.now(), "n")
        assertEquals("INV-2025-001", invoiceController().generateInvoice(req).data!!.invoiceNumber)
    }

    @Test
    fun `payInvoice rejects already-paid invoice`() {
        whenever(queryService.getInvoice("INV-1")).thenReturn(invoice {
            totalAmount = BigDecimal("100"); paidAmount = BigDecimal("100")
        })
        assertThrows(SubscriptionException.PaymentFailed::class.java) {
            invoiceController().payInvoice("INV-1", PayInvoiceRequest("INV-1", null, false))
        }
    }

    @Test
    fun `payInvoice generates payment link when not auto-charging`() {
        whenever(queryService.getInvoice("INV-1")).thenReturn(invoice())
        whenever(invoicePaymentService.generatePaymentLink(any())).thenReturn("https://pay")

        val resp = invoiceController().payInvoice("INV-1", PayInvoiceRequest("INV-1", null, false))

        assertEquals("https://pay", resp.data!!.paymentLinkUrl)
        assertFalse(resp.data!!.autoCharged)
    }

    @Test
    fun `payInvoice auto-charges when requested and enabled`() {
        whenever(queryService.getInvoice("INV-1")).thenReturn(invoice { autoPaymentEnabled = true })

        val resp = invoiceController().payInvoice("INV-1", PayInvoiceRequest("INV-1", null, true))

        assertNull(resp.data!!.paymentLinkUrl)
        assertTrue(resp.data!!.autoCharged)
        verify(invoicePaymentService).processAutoPayment(any())
    }

    @Test
    fun `getInvoiceSummary and downloadInvoice`() {
        whenever(queryService.getSummary("WS-1")).thenReturn(
            com.ampairs.subscription.domain.dto.InvoiceSummaryResponse(0, 0, 0, BigDecimal.ZERO, null, null)
        )
        whenever(queryService.getInvoice("INV-1")).thenReturn(invoice())

        assertTrue(invoiceController().getInvoiceSummary().success)
        assertTrue(invoiceController().downloadInvoice("INV-1").data!!.contains("not yet implemented"))
    }

    @Test
    fun `retryPayment falls back to link when auto-charge fails`() {
        whenever(queryService.getInvoice("INV-1")).thenReturn(invoice { autoPaymentEnabled = true; paymentMethodId = 5L })
        whenever(invoicePaymentService.processAutoPayment(any())).thenThrow(SubscriptionException.PaymentFailed("x"))
        whenever(invoicePaymentService.generatePaymentLink(any())).thenReturn("https://pay")

        val resp = invoiceController().retryPayment("INV-1")

        assertEquals("https://pay", resp.data!!.paymentLinkUrl)
    }

    @Test
    fun `retryPayment rejects already-paid invoice`() {
        whenever(queryService.getInvoice("INV-1")).thenReturn(invoice {
            totalAmount = BigDecimal("100"); paidAmount = BigDecimal("100")
        })
        assertThrows(SubscriptionException.PaymentFailed::class.java) {
            invoiceController().retryPayment("INV-1")
        }
    }

    // ---------- BillingPreferencesController ----------

    @Test
    fun `billing-preferences get and update`() {
        val controller = SubscriptionBillingPreferencesController(billingPreferencesService)
        val prefs = BillingPreferences().apply { workspaceId = "WS-1"; billingEmail = "a@b.com" }
        whenever(billingPreferencesService.getBillingPreferences("WS-1")).thenReturn(prefs)
        whenever(billingPreferencesService.updateBillingPreferences(eq("WS-1"), any())).thenReturn(prefs)

        assertEquals("WS-1", controller.getBillingPreferences().data!!.workspaceId)
        assertEquals("a@b.com", controller.updateBillingPreferences(UpdateBillingPreferencesRequest()).data!!.billingEmail)
    }

    // ---------- PaymentController ----------

    private fun subResponse() = SubscriptionResponse(
        uid = "SUB-1", workspaceId = "WS-1", planCode = "PRO", plan = null,
        status = SubscriptionStatus.ACTIVE, billingCycle = BillingCycle.MONTHLY,
        paymentProvider = null, currency = "INR", currentPeriodStart = null, currentPeriodEnd = null,
        trialEndsAt = null, cancelAtPeriodEnd = false, cancelledAt = null, nextBillingAmount = null,
        lastPaymentStatus = null, lastPaymentAt = null, isFree = false, daysRemaining = 0,
        activeAddons = emptyList(), createdAt = null, updatedAt = null
    )

    @Test
    fun `payment initiate, verify and product ids`() {
        val controller = PaymentController(orchestrationService)
        val initResp = InitiatePurchaseResponse(
            "https://pay", "cs", PaymentProvider.RAZORPAY, null, "order_1", "sub_1", null,
            BigDecimal("100"), "INR"
        )
        val req = InitiatePurchaseRequest("PRO", BillingCycle.MONTHLY, PaymentProvider.RAZORPAY)
        wheneverBlocking { orchestrationService.initiatePurchase(eq("WS-1"), any(), eq(PaymentProvider.RAZORPAY)) }
            .thenReturn(initResp)
        wheneverBlocking { orchestrationService.verifyPurchase(eq("WS-1"), any()) }.thenReturn(subResponse())

        assertEquals("https://pay", controller.initiatePurchase(req).data!!.checkoutUrl)
        assertEquals("SUB-1", controller.verifyPurchase(
            com.ampairs.subscription.domain.dto.VerifyPurchaseRequest(PaymentProvider.GOOGLE_PLAY, "t", "p")
        ).data!!.uid)

        val products = controller.getProductIds("PRO", "MONTHLY").data!!
        assertEquals("PRO", products.planCode)
        assertEquals("ampairs_pro_monthly", products.googlePlayMonthly)
    }
}
