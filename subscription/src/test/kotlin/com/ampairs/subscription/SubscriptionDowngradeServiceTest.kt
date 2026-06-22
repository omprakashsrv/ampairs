package com.ampairs.subscription

import com.ampairs.subscription.domain.model.Subscription
import com.ampairs.subscription.domain.model.SubscriptionPlanDefinition
import com.ampairs.subscription.domain.model.SubscriptionStatus
import com.ampairs.subscription.domain.repository.SubscriptionPlanRepository
import com.ampairs.subscription.domain.repository.SubscriptionRepository
import com.ampairs.subscription.domain.service.SubscriptionDowngradeService
import com.ampairs.subscription.exception.SubscriptionException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
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
import java.time.Duration
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionDowngradeServiceTest {

    @Mock private lateinit var subscriptionRepository: SubscriptionRepository
    @Mock private lateinit var planRepository: SubscriptionPlanRepository

    private lateinit var service: SubscriptionDowngradeService

    @BeforeEach
    fun setUp() {
        service = SubscriptionDowngradeService(subscriptionRepository, planRepository)
        whenever(subscriptionRepository.save(any<Subscription>())).thenAnswer { it.arguments[0] }
        whenever(planRepository.findByPlanCode("FREE")).thenReturn(SubscriptionPlanDefinition().apply { planCode = "FREE" })
    }

    private fun paid(block: Subscription.() -> Unit = {}) = Subscription().apply {
        uid = "SUB-1"; workspaceId = "WS-1"; planCode = "PRO"; isFree = false
        status = SubscriptionStatus.ACTIVE
        block()
    }

    @Test
    fun `downgradeToFreePlan converts paid subscription to FREE`() {
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(paid())

        val result = service.downgradeToFreePlan("WS-1", "expired")

        assertEquals("FREE", result.planCode)
        assertTrue(result.isFree)
        assertEquals(SubscriptionStatus.ACTIVE, result.status)
        assertEquals(null, result.currentPeriodEnd)
        assertEquals(null, result.paymentProvider)
    }

    @Test
    fun `downgradeToFreePlan is no-op when already free`() {
        val free = paid { planCode = "FREE"; isFree = true }
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(free)

        val result = service.downgradeToFreePlan("WS-1", "noop")

        assertSame(free, result)
        verify(subscriptionRepository, never()).save(any())
    }

    @Test
    fun `downgradeToFreePlan throws when subscription missing`() {
        whenever(subscriptionRepository.findByWorkspaceId("WS-X")).thenReturn(null)
        assertThrows(SubscriptionException.SubscriptionNotFoundException::class.java) {
            service.downgradeToFreePlan("WS-X", "x")
        }
    }

    @Test
    fun `downgradeToFreePlan throws when FREE plan missing`() {
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(paid())
        whenever(planRepository.findByPlanCode("FREE")).thenReturn(null)
        assertThrows(SubscriptionException.PlanNotFoundException::class.java) {
            service.downgradeToFreePlan("WS-1", "x")
        }
    }

    @Test
    fun `handlePaymentFailure first failure sets PAST_DUE`() {
        val s = paid()
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(s)

        service.handlePaymentFailure("WS-1", 1)

        assertEquals(SubscriptionStatus.PAST_DUE, s.status)
        assertEquals(1, s.failedPaymentCount)
    }

    @Test
    fun `handlePaymentFailure second failure stays past due`() {
        val s = paid { status = SubscriptionStatus.PAST_DUE }
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(s)

        service.handlePaymentFailure("WS-1", 2)

        assertEquals(2, s.failedPaymentCount)
        assertEquals(SubscriptionStatus.PAST_DUE, s.status)
    }

    @Test
    fun `handlePaymentFailure third failure downgrades to FREE`() {
        val s = paid { status = SubscriptionStatus.PAST_DUE }
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(s)

        service.handlePaymentFailure("WS-1", 3)

        assertEquals("FREE", s.planCode)
        assertTrue(s.isFree)
    }

    @Test
    fun `handlePaymentFailure ignores free subscription`() {
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(paid { isFree = true })

        service.handlePaymentFailure("WS-1", 5)

        verify(subscriptionRepository, never()).save(any())
    }

    @Test
    fun `handleSubscriptionExpiry downgrades expired subscription`() {
        val s = paid { currentPeriodEnd = Instant.now().minus(Duration.ofDays(1)) }
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(s)

        service.handleSubscriptionExpiry("WS-1")

        assertEquals("FREE", s.planCode)
    }

    @Test
    fun `handleSubscriptionExpiry skips not-yet-expired subscription`() {
        val s = paid { currentPeriodEnd = Instant.now().plus(Duration.ofDays(5)) }
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(s)

        service.handleSubscriptionExpiry("WS-1")

        verify(subscriptionRepository, never()).save(any())
    }

    @Test
    fun `handleUserCancellation downgrades to FREE`() {
        whenever(subscriptionRepository.findByWorkspaceId("WS-1")).thenReturn(paid())

        service.handleUserCancellation("WS-1", "too expensive")

        verify(subscriptionRepository).save(any())
    }

    @Test
    fun `getSubscriptionsToDowngrade collects past-due, expired and scheduled`() {
        val pastDue = paid { uid = "S1"; status = SubscriptionStatus.PAST_DUE; failedPaymentCount = 3 }
        val expired = paid { uid = "S2"; status = SubscriptionStatus.ACTIVE; currentPeriodEnd = Instant.now().minus(Duration.ofDays(2)); cancelAtPeriodEnd = false }
        val scheduled = paid { uid = "S3"; status = SubscriptionStatus.ACTIVE; currentPeriodEnd = Instant.now().minus(Duration.ofDays(2)); cancelAtPeriodEnd = true }

        whenever(subscriptionRepository.findAllByStatus(SubscriptionStatus.PAST_DUE)).thenReturn(listOf(pastDue))
        whenever(subscriptionRepository.findAllByStatus(SubscriptionStatus.ACTIVE)).thenReturn(listOf(expired, scheduled))

        val result = service.getSubscriptionsToDowngrade()

        assertEquals(3, result.size)
    }

    @Test
    fun `processSubscriptionDowngrades returns 0 when nothing to do`() {
        whenever(subscriptionRepository.findAllByStatus(any())).thenReturn(emptyList())
        assertEquals(0, service.processSubscriptionDowngrades())
    }

    @Test
    fun `processSubscriptionDowngrades downgrades collected subscriptions`() {
        val pastDue = paid { uid = "S1"; workspaceId = "WS-PD"; status = SubscriptionStatus.PAST_DUE; failedPaymentCount = 3 }
        whenever(subscriptionRepository.findAllByStatus(SubscriptionStatus.PAST_DUE)).thenReturn(listOf(pastDue))
        whenever(subscriptionRepository.findAllByStatus(SubscriptionStatus.ACTIVE)).thenReturn(emptyList())
        whenever(subscriptionRepository.findByWorkspaceId("WS-PD")).thenReturn(pastDue)

        val count = service.processSubscriptionDowngrades()

        assertEquals(1, count)
    }
}
