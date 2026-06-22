package com.ampairs.subscription

import com.ampairs.subscription.domain.service.SubscriptionDowngradeService
import com.ampairs.subscription.domain.service.UsageTrackingService
import com.ampairs.subscription.scheduler.SubscriptionScheduledJobs
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionScheduledJobsTest {

    @Mock private lateinit var downgradeService: SubscriptionDowngradeService
    @Mock private lateinit var usageTrackingService: UsageTrackingService

    private lateinit var jobs: SubscriptionScheduledJobs

    @BeforeEach
    fun setUp() {
        jobs = SubscriptionScheduledJobs(downgradeService, usageTrackingService)
    }

    @Test
    fun `processSubscriptionDowngrades delegates to downgrade service`() {
        whenever(downgradeService.processSubscriptionDowngrades()).thenReturn(3)
        jobs.processSubscriptionDowngrades()
        verify(downgradeService).processSubscriptionDowngrades()
    }

    @Test
    fun `processSubscriptionDowngrades swallows exceptions`() {
        whenever(downgradeService.processSubscriptionDowngrades()).thenThrow(RuntimeException("boom"))
        jobs.processSubscriptionDowngrades() // should not throw
    }

    @Test
    fun `resetMonthlyUsageCounters delegates to usage service`() {
        whenever(usageTrackingService.resetMonthlyCounters()).thenReturn(5)
        jobs.resetMonthlyUsageCounters()
        verify(usageTrackingService).resetMonthlyCounters()
    }

    @Test
    fun `resetMonthlyUsageCounters swallows exceptions`() {
        whenever(usageTrackingService.resetMonthlyCounters()).thenThrow(RuntimeException("boom"))
        jobs.resetMonthlyUsageCounters()
    }

    @Test
    fun `sendRenewalReminders runs without error`() {
        jobs.sendRenewalReminders()
    }

    @Test
    fun `expireTrials runs without error`() {
        jobs.expireTrials()
    }

    @Test
    fun `cleanupOldUsageMetrics delegates to usage service`() {
        whenever(usageTrackingService.deleteOldUsageMetrics(12)).thenReturn(8)
        jobs.cleanupOldUsageMetrics()
        verify(usageTrackingService).deleteOldUsageMetrics(12)
    }

    @Test
    fun `cleanupOldUsageMetrics swallows exceptions`() {
        whenever(usageTrackingService.deleteOldUsageMetrics(12)).thenThrow(RuntimeException("boom"))
        jobs.cleanupOldUsageMetrics()
    }
}
