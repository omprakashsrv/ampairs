package com.ampairs.subscription

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.subscription.controller.BillingController
import com.ampairs.subscription.controller.DeviceController
import com.ampairs.subscription.controller.SubscriptionController
import com.ampairs.subscription.domain.dto.CancelSubscriptionRequest
import com.ampairs.subscription.domain.dto.ChangePlanRequest
import com.ampairs.subscription.domain.dto.ChangePlanResponse
import com.ampairs.subscription.domain.dto.DeviceRegistrationResponse
import com.ampairs.subscription.domain.dto.InitiatePurchaseRequest
import com.ampairs.subscription.domain.dto.InitiatePurchaseResponse
import com.ampairs.subscription.domain.dto.PauseSubscriptionRequest
import com.ampairs.subscription.domain.dto.PaymentMethodResponse
import com.ampairs.subscription.domain.dto.PaymentTransactionResponse
import com.ampairs.subscription.domain.dto.PlanResponse
import com.ampairs.subscription.domain.dto.RefreshDeviceTokenRequest
import com.ampairs.subscription.domain.dto.RegisterDeviceRequest
import com.ampairs.subscription.domain.dto.SetDefaultPaymentMethodRequest
import com.ampairs.subscription.domain.dto.SubscriptionResponse
import com.ampairs.subscription.domain.dto.UsageResponse
import com.ampairs.subscription.domain.dto.VerifyPurchaseRequest
import com.ampairs.subscription.domain.model.BillingCycle
import com.ampairs.subscription.domain.model.DevicePlatform
import com.ampairs.subscription.domain.model.PaymentMethodType
import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.model.SubscriptionAccessMode
import com.ampairs.subscription.domain.model.SubscriptionStatus
import com.ampairs.subscription.domain.service.BillingService
import com.ampairs.subscription.domain.service.DeviceRegistrationService
import com.ampairs.subscription.domain.service.LimitCheckResult
import com.ampairs.subscription.domain.service.PaymentOrchestrationService
import com.ampairs.subscription.domain.service.SubscriptionService
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import org.mockito.quality.Strictness
import org.springframework.data.domain.PageImpl
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ControllersTest {

    @Mock private lateinit var subscriptionService: SubscriptionService
    @Mock private lateinit var orchestrationService: PaymentOrchestrationService
    @Mock private lateinit var deviceService: DeviceRegistrationService
    @Mock private lateinit var billingService: BillingService

    @BeforeEach
    fun setUp() {
        TenantContextHolder.setCurrentTenant("WS-1")
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clearTenantContext()
    }

    private fun subResponse() = SubscriptionResponse(
        uid = "SUB-1", workspaceId = "WS-1", planCode = "PRO", plan = null,
        status = SubscriptionStatus.ACTIVE, billingCycle = BillingCycle.MONTHLY,
        paymentProvider = null, currency = "INR", currentPeriodStart = null, currentPeriodEnd = null,
        trialEndsAt = null, cancelAtPeriodEnd = false, cancelledAt = null, nextBillingAmount = null,
        lastPaymentStatus = null, lastPaymentAt = null, isFree = false, daysRemaining = 0,
        activeAddons = emptyList(), createdAt = null, updatedAt = null
    )

    private fun deviceResponse() = DeviceRegistrationResponse(
        uid = "DEV-1", deviceId = "D1", deviceName = "Pixel", platform = DevicePlatform.ANDROID,
        deviceModel = null, osVersion = null, appVersion = null, tokenExpiresAt = Instant.now(),
        lastSyncAt = null, lastActivityAt = null, isActive = true,
        accessMode = SubscriptionAccessMode.FULL_ACCESS, createdAt = null
    )

    // ---------- BillingController ----------

    @Test
    fun `billing getPaymentHistory wraps page`() {
        val controller = BillingController(billingService)
        whenever(billingService.getPaymentHistory(eq("WS-1"), any(), any()))
            .thenReturn(PageImpl(emptyList<PaymentTransactionResponse>()))

        val resp = controller.getPaymentHistory(0, 20)

        assertTrue(resp.success)
        assertNotNull(resp.data)
    }

    @Test
    fun `billing getPaymentMethods and default`() {
        val controller = BillingController(billingService)
        whenever(billingService.getPaymentMethods("WS-1")).thenReturn(emptyList())
        whenever(billingService.getDefaultPaymentMethod("WS-1")).thenReturn(null)

        assertTrue(controller.getPaymentMethods().success)
        assertTrue(controller.getDefaultPaymentMethod().success)
    }

    @Test
    fun `billing setDefault and remove`() {
        val controller = BillingController(billingService)
        val pm = PaymentMethodResponse(
            uid = "PM-1", paymentProvider = PaymentProvider.STRIPE, type = PaymentMethodType.CREDIT_CARD,
            last4 = "4242", brand = "VISA", expMonth = 12, expYear = 2030,
            cardholderName = "John", upiId = null, bankName = null,
            isDefault = true, isExpired = false, displayName = "VISA **** 4242", createdAt = null
        )
        whenever(billingService.setDefaultPaymentMethod("WS-1", "PM-1")).thenReturn(pm)

        assertEquals("PM-1", controller.setDefaultPaymentMethod(SetDefaultPaymentMethodRequest("PM-1")).data!!.uid)
        assertEquals(true, controller.removePaymentMethod("PM-1").data!!["removed"])
    }

    // ---------- DeviceController ----------

    @Test
    fun `device register updates activity and returns response`() {
        val controller = DeviceController(deviceService)
        val req = RegisterDeviceRequest(deviceId = "D1", platform = DevicePlatform.ANDROID)
        whenever(deviceService.registerDevice(eq("WS-1"), eq("USR-1"), any())).thenReturn(deviceResponse())
        val servletRequest = mock<HttpServletRequest> {
            on { getHeader("X-Forwarded-For") } doReturn null
            on { remoteAddr } doReturn "1.2.3.4"
        }

        val resp = controller.registerDevice(req, "USR-1", servletRequest)

        assertTrue(resp.success)
        verify(deviceService).updateActivity("WS-1", "D1", "1.2.3.4")
    }

    @Test
    fun `device refresh, list, get, access-mode, deactivate, push-token`() {
        val controller = DeviceController(deviceService)
        whenever(deviceService.refreshDeviceToken("WS-1", "D1", "1.0")).thenReturn(deviceResponse())
        whenever(deviceService.getDevices("WS-1")).thenReturn(listOf(deviceResponse()))
        whenever(deviceService.getDevice("WS-1", "D1")).thenReturn(deviceResponse())
        whenever(deviceService.getAccessMode("WS-1", "D1")).thenReturn(SubscriptionAccessMode.FULL_ACCESS)
        whenever(deviceService.getUserDevices("USR-1")).thenReturn(listOf(deviceResponse()))

        assertTrue(controller.refreshDeviceToken(RefreshDeviceTokenRequest("D1", "1.0")).success)
        assertEquals(1, controller.getDevices().data!!.size)
        assertEquals("DEV-1", controller.getDevice("D1").data!!.uid)
        assertEquals(SubscriptionAccessMode.FULL_ACCESS, controller.getAccessMode("D1").data!!["access_mode"])
        assertEquals(true, controller.deactivateDevice("DEV-1", "lost").data!!["deactivated"])
        assertEquals(1, controller.getUserDevices("USR-1").data!!.size)
        assertEquals(true, controller.updatePushToken("D1", "tok", "FCM").data!!["updated"])
    }

    // ---------- SubscriptionController ----------

    private fun subController() =
        SubscriptionController(subscriptionService, orchestrationService, deviceService, billingService)

    @Test
    fun `subscription plans endpoints`() {
        val controller = subController()
        whenever(subscriptionService.getAvailablePlans()).thenReturn(emptyList())
        val planResp = mock<PlanResponse>()
        whenever(subscriptionService.getPlanByCode("PRO")).thenReturn(planResp)

        assertTrue(controller.getPlans().success)
        assertEquals(planResp, controller.getPlan("PRO").data)
    }

    @Test
    fun `subscription current, change, cancel, pause, resume, trial`() {
        val controller = subController()
        whenever(subscriptionService.getSubscription("WS-1")).thenReturn(subResponse())
        whenever(subscriptionService.changePlan(eq("WS-1"), eq("PRO"), eq(BillingCycle.MONTHLY), eq(false)))
            .thenReturn(ChangePlanResponse(subResponse(), null, Instant.now(), true, emptyList()))
        whenever(subscriptionService.cancelSubscription(eq("WS-1"), any(), any())).thenReturn(subResponse())
        whenever(subscriptionService.pauseSubscription(eq("WS-1"), any(), any())).thenReturn(subResponse())
        whenever(subscriptionService.resumeSubscription("WS-1")).thenReturn(subResponse())
        whenever(subscriptionService.startTrial(eq("WS-1"), eq("PRO"), eq(14))).thenReturn(subResponse())

        assertEquals("SUB-1", controller.getCurrentSubscription().data!!.uid)
        assertTrue(controller.changePlan(ChangePlanRequest("PRO", BillingCycle.MONTHLY, false)).data!!.isImmediate)
        assertTrue(controller.cancelSubscription(CancelSubscriptionRequest(true, "x", null)).success)
        assertTrue(controller.pauseSubscription(PauseSubscriptionRequest(30, "y")).success)
        assertTrue(controller.resumeSubscription().success)
        assertTrue(controller.startTrial("PRO", 14).success)
    }

    @Test
    fun `subscription usage, limits, features`() {
        val controller = subController()
        val usage = mock<UsageResponse>()
        whenever(subscriptionService.getUsage("WS-1")).thenReturn(usage)
        whenever(subscriptionService.checkLimit("WS-1", "CUSTOMER", 5))
            .thenReturn(LimitCheckResult(true, 10, 5, 5))
        whenever(subscriptionService.hasFeature("WS-1", "API_ACCESS")).thenReturn(true)

        assertEquals(usage, controller.getUsage().data)
        assertTrue(controller.checkLimit("CUSTOMER", 5).data!!.allowed)
        assertEquals(true, controller.hasFeature("API_ACCESS").data!!["available"])
    }

    @Test
    fun `subscription purchase initiate and verify use orchestration`() {
        val controller = subController()
        val initResp = InitiatePurchaseResponse(
            checkoutUrl = "https://pay", checkoutSessionId = "cs", provider = PaymentProvider.STRIPE,
            subscriptionId = null, razorpayOrderId = null, razorpaySubscriptionId = null,
            stripeClientSecret = "secret", amount = BigDecimal("100"), currency = "INR"
        )
        val req = InitiatePurchaseRequest("PRO", BillingCycle.MONTHLY, PaymentProvider.STRIPE)
        wheneverBlocking { orchestrationService.initiatePurchase(eq("WS-1"), any(), eq(PaymentProvider.STRIPE)) }
            .thenReturn(initResp)
        val vreq = VerifyPurchaseRequest(PaymentProvider.GOOGLE_PLAY, "token", "prod")
        wheneverBlocking { orchestrationService.verifyPurchase(eq("WS-1"), any()) }.thenReturn(subResponse())

        assertEquals("https://pay", controller.initiatePurchase(req, PaymentProvider.STRIPE).data!!.checkoutUrl)
        assertEquals("SUB-1", controller.verifyPurchase(vreq).data!!.uid)
    }

    @Test
    fun `subscription payment-history and payment-methods aliases`() {
        val controller = subController()
        whenever(billingService.getPaymentHistory(eq("WS-1"), any(), any()))
            .thenReturn(PageImpl(emptyList<PaymentTransactionResponse>()))
        whenever(billingService.getPaymentMethods("WS-1")).thenReturn(emptyList())
        val pm = PaymentMethodResponse(
            uid = "PM-1", paymentProvider = PaymentProvider.STRIPE, type = PaymentMethodType.UPI,
            last4 = null, brand = null, expMonth = null, expYear = null,
            cardholderName = null, upiId = "a@upi", bankName = null,
            isDefault = true, isExpired = false, displayName = "UPI", createdAt = null
        )
        whenever(billingService.setDefaultPaymentMethod("WS-1", "PM-1")).thenReturn(pm)

        assertTrue(controller.getPaymentHistory(0, 20).success)
        assertTrue(controller.getPaymentMethods().success)
        assertEquals("PM-1", controller.setDefaultPaymentMethod("PM-1").data!!.uid)
        assertEquals(true, controller.removePaymentMethod("PM-1").data!!["removed"])
    }
}
