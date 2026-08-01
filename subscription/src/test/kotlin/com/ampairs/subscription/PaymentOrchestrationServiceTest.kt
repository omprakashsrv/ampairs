package com.ampairs.subscription

import com.ampairs.subscription.domain.dto.InitiatePurchaseRequest
import com.ampairs.subscription.domain.dto.SubscriptionResponse
import com.ampairs.subscription.domain.dto.VerifyPurchaseRequest
import com.ampairs.subscription.domain.model.BillingCycle
import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.model.PaymentStatus
import com.ampairs.subscription.domain.model.Subscription
import com.ampairs.subscription.domain.model.SubscriptionPlanDefinition
import com.ampairs.subscription.domain.model.SubscriptionStatus
import com.ampairs.subscription.domain.repository.SubscriptionPlanRepository
import com.ampairs.subscription.domain.repository.SubscriptionRepository
import com.ampairs.subscription.domain.service.BillingService
import com.ampairs.subscription.domain.service.PaymentOrchestrationService
import com.ampairs.subscription.domain.service.PaymentProviderService
import com.ampairs.subscription.domain.service.ProviderSubscriptionResult
import com.ampairs.subscription.domain.service.ProviderSubscriptionStatus
import com.ampairs.subscription.domain.service.PurchaseVerificationResult
import com.ampairs.subscription.domain.service.SubscriptionService
import com.ampairs.subscription.exception.SubscriptionException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentOrchestrationServiceTest {

    @Mock private lateinit var subscriptionService: SubscriptionService
    @Mock private lateinit var billingService: BillingService
    @Mock private lateinit var subscriptionRepository: SubscriptionRepository
    @Mock private lateinit var planRepository: SubscriptionPlanRepository

    private lateinit var service: PaymentOrchestrationService

    /** Configurable fake provider so we avoid suspend-stub mocking. */
    private class FakeProvider(
        override val provider: PaymentProvider,
        private val createResult: ProviderSubscriptionResult,
        private val verifyResult: PurchaseVerificationResult
    ) : PaymentProviderService {
        override suspend fun verifyPurchase(request: VerifyPurchaseRequest) = verifyResult
        override suspend fun createSubscription(
            workspaceId: String, planCode: String, billingCycle: BillingCycle, currency: String, customerId: String?
        ) = createResult
        override suspend fun cancelSubscription(externalSubscriptionId: String, immediate: Boolean) = true
        override suspend fun pauseSubscription(externalSubscriptionId: String) = true
        override suspend fun resumeSubscription(externalSubscriptionId: String) = true
        override suspend fun changePlan(
            externalSubscriptionId: String, newPlanCode: String, billingCycle: BillingCycle, immediate: Boolean
        ) = createResult
        override suspend fun getSubscriptionStatus(externalSubscriptionId: String) =
            ProviderSubscriptionStatus(active = true, status = "active", currentPeriodStart = null, currentPeriodEnd = null)
        override fun verifyWebhookSignature(payload: String, signature: String) = true
    }

    @BeforeEach
    fun setUp() {
        service = PaymentOrchestrationService(subscriptionService, billingService, subscriptionRepository, planRepository)
    }

    private fun plan() = SubscriptionPlanDefinition().apply {
        planCode = "PRO"; monthlyPriceInr = BigDecimal("1000"); monthlyPriceUsd = BigDecimal("12")
    }

    private fun subResponse() = SubscriptionResponse(
        uid = "SUB-1", workspaceId = "WS-1", planCode = "PRO", plan = null,
        status = SubscriptionStatus.ACTIVE, billingCycle = BillingCycle.MONTHLY,
        paymentProvider = null, currency = "INR", currentPeriodStart = null, currentPeriodEnd = null,
        trialEndsAt = null, cancelAtPeriodEnd = false, cancelledAt = null, nextBillingAmount = null,
        lastPaymentStatus = null, lastPaymentAt = null, isFree = false, daysRemaining = 0,
        activeAddons = emptyList(), createdAt = null, updatedAt = null
    )

    private fun successCreate() = ProviderSubscriptionResult(
        success = true, externalSubscriptionId = "ext-sub", externalCustomerId = "cust",
        checkoutUrl = "https://checkout", clientSecret = "cs", orderId = "order_1",
        currentPeriodStart = null, currentPeriodEnd = null
    )

    @Test
    fun `getProviderService throws when provider not registered`() {
        assertThrows(SubscriptionException.ProviderError::class.java) {
            service.getProviderService(PaymentProvider.STRIPE)
        }
    }

    @Test
    fun `initiatePurchase returns checkout response for razorpay`() {
        service.registerProvider(FakeProvider(PaymentProvider.RAZORPAY, successCreate(),
            PurchaseVerificationResult(false, null, null, null, null, null, null, null)))
        whenever(planRepository.findByPlanCode("PRO")).thenReturn(plan())
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(null)

        val resp = runBlocking {
            service.initiatePurchase("WS-1", InitiatePurchaseRequest("PRO", BillingCycle.MONTHLY, PaymentProvider.RAZORPAY), PaymentProvider.RAZORPAY)
        }

        assertEquals("https://checkout", resp.checkoutUrl)
        assertEquals("order_1", resp.razorpayOrderId)
    }

    @Test
    fun `initiatePurchase throws when plan missing`() {
        whenever(planRepository.findByPlanCode("PRO")).thenReturn(null)
        assertThrows(SubscriptionException.PlanNotFoundException::class.java) {
            runBlocking {
                service.initiatePurchase("WS-1", InitiatePurchaseRequest("PRO", BillingCycle.MONTHLY, PaymentProvider.RAZORPAY), PaymentProvider.RAZORPAY)
            }
        }
    }

    @Test
    fun `initiatePurchase throws when provider create fails`() {
        service.registerProvider(FakeProvider(PaymentProvider.STRIPE,
            ProviderSubscriptionResult(false, null, null, null, null, null, null, null, "denied"),
            PurchaseVerificationResult(false, null, null, null, null, null, null, null)))
        whenever(planRepository.findByPlanCode("PRO")).thenReturn(plan())
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(null)

        assertThrows(SubscriptionException.PaymentFailed::class.java) {
            runBlocking {
                service.initiatePurchase("WS-1", InitiatePurchaseRequest("PRO", BillingCycle.MONTHLY, PaymentProvider.STRIPE), PaymentProvider.STRIPE)
            }
        }
    }

    @Test
    fun `verifyPurchase activates subscription and records transaction`() {
        service.registerProvider(FakeProvider(PaymentProvider.GOOGLE_PLAY,
            successCreate(),
            PurchaseVerificationResult(
                valid = true, planCode = "PRO", billingCycle = BillingCycle.MONTHLY,
                externalSubscriptionId = "ext", externalCustomerId = "cust", orderId = "o1",
                purchaseTime = null, expiryTime = null
            )))
        whenever(planRepository.findByPlanCode("PRO")).thenReturn(plan())
        whenever(subscriptionService.activateSubscription(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(Subscription().apply { uid = "SUB-1"; currency = "USD" })
        whenever(subscriptionService.getSubscription("WS-1")).thenReturn(subResponse())

        val resp = runBlocking {
            service.verifyPurchase("WS-1", VerifyPurchaseRequest(PaymentProvider.GOOGLE_PLAY, "token", "prod", "o1"))
        }

        assertEquals("SUB-1", resp.uid)
        verify(billingService).recordTransaction(
            eq("SUB-1"), eq("WS-1"), eq(PaymentProvider.GOOGLE_PLAY), anyOrNull(), anyOrNull(), any(), any(),
            eq(PaymentStatus.SUCCEEDED), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
        )
    }

    @Test
    fun `verifyPurchase throws for invalid token`() {
        service.registerProvider(FakeProvider(PaymentProvider.APP_STORE,
            successCreate(),
            PurchaseVerificationResult(false, null, null, null, null, null, null, null)))

        assertThrows(SubscriptionException.InvalidPurchaseToken::class.java) {
            runBlocking {
                service.verifyPurchase("WS-1", VerifyPurchaseRequest(PaymentProvider.APP_STORE, "bad", "prod"))
            }
        }
    }

    @Test
    fun `handleRenewal renews and records when subscription known`() {
        val sub = Subscription().apply { uid = "SUB-1"; workspaceId = "WS-1"; externalSubscriptionId = "ext" }
        whenever(subscriptionRepository.findByExternalSubscriptionId("ext")).thenReturn(sub)

        service.handleRenewal(PaymentProvider.RAZORPAY, "ext", "o1", BigDecimal("100"), "INR", null, null)

        verify(subscriptionService).renewSubscription("WS-1")
        verify(billingService).recordTransaction(
            eq("SUB-1"), eq("WS-1"), eq(PaymentProvider.RAZORPAY), anyOrNull(), anyOrNull(), any(), any(),
            eq(PaymentStatus.SUCCEEDED), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
        )
    }

    @Test
    fun `handleRenewal ignores unknown subscription`() {
        whenever(subscriptionRepository.findByExternalSubscriptionId("nope")).thenReturn(null)
        service.handleRenewal(PaymentProvider.STRIPE, "nope", null, BigDecimal.ZERO, "USD", null, null)
        verify(subscriptionService, never()).renewSubscription(any())
    }

    @Test
    fun `handlePaymentFailure delegates when known`() {
        val sub = Subscription().apply { workspaceId = "WS-1"; externalSubscriptionId = "ext" }
        whenever(subscriptionRepository.findByExternalSubscriptionId("ext")).thenReturn(sub)

        service.handlePaymentFailure(PaymentProvider.STRIPE, "ext", "code", "reason")

        verify(subscriptionService).handlePaymentFailure("WS-1", "reason")
    }

    @Test
    fun `handleCancellation delegates when known`() {
        val sub = Subscription().apply { workspaceId = "WS-1"; externalSubscriptionId = "ext" }
        whenever(subscriptionRepository.findByExternalSubscriptionId("ext")).thenReturn(sub)

        service.handleCancellation(PaymentProvider.RAZORPAY, "ext", true)

        verify(subscriptionService).cancelSubscription(eq("WS-1"), eq(true), any())
    }

    @Test
    fun `handleCancellation ignores unknown subscription`() {
        whenever(subscriptionRepository.findByExternalSubscriptionId("nope")).thenReturn(null)
        service.handleCancellation(PaymentProvider.STRIPE, "nope", false)
        verify(subscriptionService, never()).cancelSubscription(any(), any(), any())
    }
}
