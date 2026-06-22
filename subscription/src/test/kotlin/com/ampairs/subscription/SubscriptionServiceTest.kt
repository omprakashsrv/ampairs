package com.ampairs.subscription

import com.ampairs.subscription.domain.model.BillingCycle
import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.model.PaymentStatus
import com.ampairs.subscription.domain.model.Subscription
import com.ampairs.subscription.domain.model.SubscriptionPlanDefinition
import com.ampairs.subscription.domain.model.SubscriptionStatus
import com.ampairs.subscription.domain.model.UsageMetric
import com.ampairs.subscription.domain.repository.SubscriptionAddonRepository
import com.ampairs.subscription.domain.repository.SubscriptionPlanRepository
import com.ampairs.subscription.domain.repository.SubscriptionRepository
import com.ampairs.subscription.domain.repository.UsageMetricRepository
import com.ampairs.subscription.domain.service.SubscriptionDowngradeService
import com.ampairs.subscription.domain.service.SubscriptionService
import com.ampairs.subscription.exception.SubscriptionException
import com.ampairs.workspace.model.Workspace
import com.ampairs.workspace.repository.WorkspaceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionServiceTest {

    @Mock private lateinit var subscriptionRepository: SubscriptionRepository
    @Mock private lateinit var planRepository: SubscriptionPlanRepository
    @Mock private lateinit var addonRepository: SubscriptionAddonRepository
    @Mock private lateinit var usageMetricRepository: UsageMetricRepository
    @Mock private lateinit var workspaceRepository: WorkspaceRepository
    @Mock private lateinit var downgradeService: SubscriptionDowngradeService

    private lateinit var service: SubscriptionService

    @BeforeEach
    fun setUp() {
        service = SubscriptionService(
            subscriptionRepository, planRepository, addonRepository,
            usageMetricRepository, workspaceRepository, downgradeService
        )
        whenever(subscriptionRepository.save(any<Subscription>())).thenAnswer { it.arguments[0] }
        whenever(addonRepository.findActiveBySubscriptionId(any())).thenReturn(emptyList())
    }

    private fun plan(code: String = "PRO", block: SubscriptionPlanDefinition.() -> Unit = {}) =
        SubscriptionPlanDefinition().apply {
            planCode = code
            displayName = code
            monthlyPriceInr = BigDecimal("1000")
            monthlyPriceUsd = BigDecimal("12")
            maxCustomers = 100
            maxProducts = 100
            maxInvoicesPerMonth = 50
            maxMembersPerWorkspace = 10
            maxDevices = 5
            maxStorageGb = 10
            block()
        }

    private fun sub(block: Subscription.() -> Unit = {}) = Subscription().apply {
        uid = "SUB-1"
        workspaceId = "WS-1"
        planCode = "PRO"
        status = SubscriptionStatus.ACTIVE
        isFree = false
        block()
    }

    @Test
    fun `getAvailablePlans maps active plans`() {
        whenever(planRepository.findAllActivePlansOrdered()).thenReturn(listOf(plan("FREE"), plan("PRO")))

        val result = service.getAvailablePlans()

        assertEquals(2, result.size)
        assertEquals("FREE", result.first().planCode)
    }

    @Test
    fun `getPlanByCode returns plan or throws`() {
        whenever(planRepository.findByPlanCode("PRO")).thenReturn(plan("PRO"))
        assertEquals("PRO", service.getPlanByCode("PRO").planCode)

        whenever(planRepository.findByPlanCode("NOPE")).thenReturn(null)
        assertThrows(SubscriptionException.PlanNotFoundException::class.java) { service.getPlanByCode("NOPE") }
    }

    @Test
    fun `getSubscriptionById returns or throws NotFound`() {
        whenever(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub()))
        assertEquals("SUB-1", service.getSubscriptionById(1L).uid)

        whenever(subscriptionRepository.findById(2L)).thenReturn(Optional.empty())
        assertThrows(SubscriptionException.NotFound::class.java) { service.getSubscriptionById(2L) }
    }

    @Test
    fun `getSubscription returns existing`() {
        val s = sub { plan = plan("PRO") }
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1")).thenReturn(s)

        val resp = service.getSubscription("WS-1")

        assertEquals("WS-1", resp.workspaceId)
        assertEquals("PRO", resp.planCode)
    }

    @Test
    fun `getSubscription auto-creates FREE when none exists`() {
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-NEW"))
            .thenReturn(null) // first call
            .thenReturn(sub { workspaceId = "WS-NEW"; planCode = "FREE"; isFree = true; plan = plan("FREE") })
        whenever(planRepository.findByPlanCode("FREE")).thenReturn(plan("FREE"))

        val resp = service.getSubscription("WS-NEW")

        assertEquals("FREE", resp.planCode)
        verify(subscriptionRepository).save(any())
    }

    @Test
    fun `createFreeSubscription throws when FREE plan missing`() {
        whenever(planRepository.findByPlanCode("FREE")).thenReturn(null)
        assertThrows(SubscriptionException.PlanNotFoundException::class.java) {
            service.createFreeSubscription("WS-1")
        }
    }

    @Test
    fun `getOrCreateSubscription returns existing without creating`() {
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(sub())
        val result = service.getOrCreateSubscription("WS-1")
        assertEquals("SUB-1", result.uid)
        verify(planRepository, never()).findByPlanCode(any())
    }

    @Test
    fun `startTrial throws when trial already used`() {
        whenever(planRepository.findByPlanCode("PRO")).thenReturn(plan("PRO"))
        whenever(subscriptionRepository.findByWorkspaceId("WS-1"))
            .thenReturn(sub { trialEndsAt = Instant.now() })

        assertThrows(SubscriptionException.TrialAlreadyUsed::class.java) {
            service.startTrial("WS-1", "PRO", 14)
        }
    }

    @Test
    fun `startTrial throws when already on active paid plan`() {
        whenever(planRepository.findByPlanCode("PRO")).thenReturn(plan("PRO"))
        whenever(subscriptionRepository.findByWorkspaceId("WS-1"))
            .thenReturn(sub { isFree = false; status = SubscriptionStatus.ACTIVE; trialEndsAt = null })

        assertThrows(SubscriptionException.AlreadyHasActivePlan::class.java) {
            service.startTrial("WS-1", "PRO", 14)
        }
    }

    @Test
    fun `startTrial creates trialing subscription`() {
        whenever(planRepository.findByPlanCode("PRO")).thenReturn(plan("PRO"))
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(null)

        val resp = service.startTrial("WS-1", "PRO", 14)

        assertEquals(SubscriptionStatus.TRIALING, resp.status)
        assertFalse(resp.isFree)
    }

    @Test
    fun `activateSubscription sets ACTIVE and computes next billing`() {
        whenever(planRepository.findByPlanCode("PRO")).thenReturn(plan("PRO"))
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(null)
        whenever(workspaceRepository.findByUid("WS-1")).thenReturn(Optional.empty())

        val result = service.activateSubscription(
            "WS-1", "PRO", BillingCycle.MONTHLY, PaymentProvider.RAZORPAY,
            "ext-sub", "ext-cust", "INR"
        )

        assertEquals(SubscriptionStatus.ACTIVE, result.status)
        assertEquals(PaymentStatus.SUCCEEDED, result.lastPaymentStatus)
        assertEquals(0, BigDecimal("1000.00").compareTo(result.nextBillingAmount!!))
    }

    @Test
    fun `activateSubscription applies workspace-count and cycle discount when owner known`() {
        whenever(planRepository.findByPlanCode("PRO")).thenReturn(plan("PRO") {
            multiWorkspaceMinCount = 2
            multiWorkspaceDiscountPercent = 10
        })
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(null)
        val ws = Workspace().apply { createdBy = "USR-1" }
        whenever(workspaceRepository.findByUid("WS-1")).thenReturn(Optional.of(ws))
        whenever(workspaceRepository.countByCreatedByAndActiveTrue("USR-1")).thenReturn(3)

        val result = service.activateSubscription(
            "WS-1", "PRO", BillingCycle.ANNUAL, PaymentProvider.STRIPE, null, null, "INR"
        )

        // base 1000, -10% multi = 900, annual *12 *0.8 = 8640.00
        assertEquals(0, BigDecimal("8640.00").compareTo(result.nextBillingAmount!!))
    }

    @Test
    fun `changePlan throws when subscription not found`() {
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-X")).thenReturn(null)
        assertThrows(SubscriptionException.SubscriptionNotFoundException::class.java) {
            service.changePlan("WS-X", "PRO", BillingCycle.MONTHLY, true)
        }
    }

    @Test
    fun `changePlan computes proration and usage warnings`() {
        val current = sub {
            isFree = false
            plan = plan("STARTER")
            currency = "INR"
            billingCycle = BillingCycle.MONTHLY
            nextBillingAmount = BigDecimal("300")
            currentPeriodEnd = Instant.now().plus(Duration.ofDays(15))
        }
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1")).thenReturn(current)
        whenever(planRepository.findByPlanCode("PRO")).thenReturn(plan("PRO") { maxCustomers = 1 })
        whenever(workspaceRepository.findByUid("WS-1")).thenReturn(Optional.empty())
        // usage to trigger a warning
        whenever(usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth(any(), any(), any()))
            .thenReturn(UsageMetric().apply { workspaceId = "WS-1"; customerCount = 50 })

        val resp = service.changePlan("WS-1", "PRO", BillingCycle.MONTHLY, true)

        assertTrue(resp.isImmediate)
        assertEquals("PRO", resp.subscription.planCode)
        assertTrue(resp.usageWarnings.any { it.contains("Customer count") })
    }

    @Test
    fun `cancelSubscription immediate downgrades to FREE`() {
        val current = sub { plan = plan("PRO") }
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1")).thenReturn(current)

        service.cancelSubscription("WS-1", immediate = true, reason = "too pricey")

        verify(downgradeService).handleUserCancellation("WS-1", "too pricey")
        verify(addonRepository).cancelAllBySubscriptionId(org.mockito.kotlin.eq("SUB-1"), any())
    }

    @Test
    fun `cancelSubscription scheduled marks cancelAtPeriodEnd`() {
        val current = sub { plan = plan("PRO") }
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1")).thenReturn(current)

        val resp = service.cancelSubscription("WS-1", immediate = false, reason = "later")

        assertTrue(resp.cancelAtPeriodEnd)
        verify(downgradeService, never()).handleUserCancellation(any(), any())
    }

    @Test
    fun `pauseSubscription rejects free subscription`() {
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1")).thenReturn(sub { isFree = true })
        assertThrows(SubscriptionException.OperationNotAllowed::class.java) {
            service.pauseSubscription("WS-1", 30, null)
        }
    }

    @Test
    fun `pauseSubscription pauses paid subscription`() {
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1")).thenReturn(sub { isFree = false; plan = plan() })
        val resp = service.pauseSubscription("WS-1", 30, "vacation")
        assertEquals(SubscriptionStatus.PAUSED, resp.status)
    }

    @Test
    fun `resumeSubscription rejects non-paused`() {
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1")).thenReturn(sub { status = SubscriptionStatus.ACTIVE })
        assertThrows(SubscriptionException.OperationNotAllowed::class.java) {
            service.resumeSubscription("WS-1")
        }
    }

    @Test
    fun `resumeSubscription reactivates paused`() {
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1"))
            .thenReturn(sub { status = SubscriptionStatus.PAUSED; plan = plan() })
        val resp = service.resumeSubscription("WS-1")
        assertEquals(SubscriptionStatus.ACTIVE, resp.status)
    }

    @Test
    fun `renewSubscription extends period and resets failures`() {
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1"))
            .thenReturn(sub { failedPaymentCount = 2; plan = plan() })
        val result = service.renewSubscription("WS-1")
        assertEquals(SubscriptionStatus.ACTIVE, result.status)
        assertEquals(0, result.failedPaymentCount)
    }

    @Test
    fun `handlePaymentFailure moves ACTIVE to PAST_DUE then EXPIRED after 3`() {
        val s = sub { status = SubscriptionStatus.ACTIVE; failedPaymentCount = 0 }
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(s)

        val first = service.handlePaymentFailure("WS-1", "card declined")
        assertEquals(SubscriptionStatus.PAST_DUE, first.status)
        assertEquals(1, first.failedPaymentCount)

        // bump to 2 already; simulate 3rd
        s.failedPaymentCount = 2
        val third = service.handlePaymentFailure("WS-1", "again")
        assertEquals(SubscriptionStatus.EXPIRED, third.status)
    }

    @Test
    fun `checkLimit returns unlimited for -1`() {
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1"))
            .thenReturn(sub { plan = plan { maxCustomers = -1 } })

        val result = service.checkLimit("WS-1", "CUSTOMER", 9999)
        assertTrue(result.isUnlimited)
        assertTrue(result.allowed)
    }

    @Test
    fun `checkLimit flags exceeded and warning`() {
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1"))
            .thenReturn(sub { plan = plan { maxCustomers = 10 } })

        val exceeded = service.checkLimit("WS-1", "CUSTOMER", 10)
        assertFalse(exceeded.allowed)
        assertTrue(exceeded.exceeded)

        val warning = service.checkLimit("WS-1", "CUSTOMER", 9)
        assertTrue(warning.allowed)
        assertTrue(warning.warning)
    }

    @Test
    fun `checkLimit uses defaults when no plan`() {
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1")).thenReturn(null)
        val result = service.checkLimit("WS-1", "DEVICE", 1)
        assertEquals(2, result.limit) // default device limit
    }

    @Test
    fun `hasFeature returns false without subscription`() {
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1")).thenReturn(null)
        assertFalse(service.hasFeature("WS-1", "API_ACCESS"))
    }

    @Test
    fun `hasFeature checks plan flags and modules`() {
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1"))
            .thenReturn(sub { plan = plan { apiAccessEnabled = true; availableModules = "[\"CUSTOMER\"]" } })

        assertTrue(service.hasFeature("WS-1", "API_ACCESS"))
        assertFalse(service.hasFeature("WS-1", "SSO"))
        assertTrue(service.hasFeature("WS-1", "customer"))
    }

    @Test
    fun `getUsage falls back to current period metric`() {
        whenever(subscriptionRepository.findWithPlanByWorkspaceId("WS-1")).thenReturn(sub { plan = plan() })
        whenever(usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth(any(), any(), any()))
            .thenReturn(null)

        val usage = service.getUsage("WS-1")
        assertEquals("WS-1", usage.workspaceId)
    }
}
