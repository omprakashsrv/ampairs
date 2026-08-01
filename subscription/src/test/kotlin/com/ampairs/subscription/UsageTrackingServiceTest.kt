package com.ampairs.subscription

import com.ampairs.subscription.domain.model.UsageMetric
import com.ampairs.subscription.domain.repository.UsageMetricRepository
import com.ampairs.subscription.domain.service.UsageTrackingService
import com.ampairs.subscription.listener.ResourceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.time.YearMonth
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsageTrackingServiceTest {

    @Mock
    private lateinit var usageMetricRepository: UsageMetricRepository

    private lateinit var service: UsageTrackingService

    private val ym = YearMonth.now(ZoneOffset.UTC)

    @BeforeEach
    fun setUp() {
        service = UsageTrackingService(usageMetricRepository)
        whenever(usageMetricRepository.save(any<UsageMetric>())).thenAnswer { it.arguments[0] }
    }

    private fun currentMetric(block: UsageMetric.() -> Unit = {}): UsageMetric = UsageMetric().apply {
        workspaceId = "WS-1"
        periodYear = ym.year
        periodMonth = ym.monthValue
        block()
    }

    @Test
    fun `incrementCount increments the right resource`() {
        val metric = currentMetric { customerCount = 4 }
        whenever(usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth("WS-1", ym.year, ym.monthValue))
            .thenReturn(metric)

        service.incrementCount("WS-1", ResourceType.CUSTOMER)

        assertEquals(5, metric.customerCount)
    }

    @Test
    fun `incrementCount creates metric when none exists`() {
        whenever(usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth("WS-NEW", ym.year, ym.monthValue))
            .thenReturn(null)

        service.incrementCount("WS-NEW", ResourceType.PRODUCT)

        // saved twice: once during get-or-create, once after increment
        verify(usageMetricRepository, org.mockito.kotlin.atLeast(1)).save(any())
    }

    @Test
    fun `decrementCount never goes below zero`() {
        val metric = currentMetric { invoiceCount = 0 }
        whenever(usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth("WS-1", ym.year, ym.monthValue))
            .thenReturn(metric)

        service.decrementCount("WS-1", ResourceType.INVOICE)

        assertEquals(0, metric.invoiceCount)
    }

    @Test
    fun `decrementCount reduces a positive count`() {
        val metric = currentMetric { orderCount = 3 }
        whenever(usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth("WS-1", ym.year, ym.monthValue))
            .thenReturn(metric)

        service.decrementCount("WS-1", ResourceType.ORDER)

        assertEquals(2, metric.orderCount)
    }

    @Test
    fun `setCount sets the absolute member count`() {
        val metric = currentMetric()
        whenever(usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth("WS-1", ym.year, ym.monthValue))
            .thenReturn(metric)

        service.setCount("WS-1", ResourceType.MEMBER, 42)

        assertEquals(42, metric.memberCount)
    }

    @Test
    fun `updateStorageUsage stores bytes`() {
        val metric = currentMetric()
        whenever(usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth("WS-1", ym.year, ym.monthValue))
            .thenReturn(metric)

        service.updateStorageUsage("WS-1", 2048L)

        assertEquals(2048L, metric.storageUsedBytes)
    }

    @Test
    fun `incrementApiCalls accumulates`() {
        val metric = currentMetric { apiCalls = 10 }
        whenever(usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth("WS-1", ym.year, ym.monthValue))
            .thenReturn(metric)

        service.incrementApiCalls("WS-1", 5)

        assertEquals(15L, metric.apiCalls)
    }

    @Test
    fun `getCount returns zero when no metric exists`() {
        whenever(usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth("WS-NONE", ym.year, ym.monthValue))
            .thenReturn(null)

        assertEquals(0, service.getCount("WS-NONE", ResourceType.DEVICE))
    }

    @Test
    fun `getCount returns the device count`() {
        val metric = currentMetric { deviceCount = 7 }
        whenever(usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth("WS-1", ym.year, ym.monthValue))
            .thenReturn(metric)

        assertEquals(7, service.getCount("WS-1", ResourceType.DEVICE))
    }

    @Test
    fun `resetMonthlyCounters carries forward cumulative and resets monthly`() {
        val previous = ym.minusMonths(1)
        val prevMetric = UsageMetric().apply {
            workspaceId = "WS-1"
            periodYear = previous.year
            periodMonth = previous.monthValue
            customerCount = 100
            productCount = 50
            memberCount = 5
            deviceCount = 3
            storageUsedBytes = 9999
            invoiceCount = 20
            orderCount = 30
            apiCalls = 1000
        }
        whenever(usageMetricRepository.findByPeriodYearAndPeriodMonth(previous.year, previous.monthValue))
            .thenReturn(listOf(prevMetric))
        whenever(usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth("WS-1", ym.year, ym.monthValue))
            .thenReturn(null)

        val processed = service.resetMonthlyCounters()

        assertEquals(1, processed)
        val captor = ArgumentCaptor.forClass(UsageMetric::class.java)
        verify(usageMetricRepository).save(captor.capture())
        val saved = captor.value
        assertEquals(100, saved.customerCount)
        assertEquals(50, saved.productCount)
        assertEquals(9999, saved.storageUsedBytes)
        assertEquals(0, saved.invoiceCount)
        assertEquals(0, saved.orderCount)
        assertEquals(0L, saved.apiCalls)
    }

    @Test
    fun `resetMonthlyCounters skips workspaces that already have current metric`() {
        val previous = ym.minusMonths(1)
        val prevMetric = UsageMetric().apply { workspaceId = "WS-1"; periodYear = previous.year; periodMonth = previous.monthValue }
        whenever(usageMetricRepository.findByPeriodYearAndPeriodMonth(previous.year, previous.monthValue))
            .thenReturn(listOf(prevMetric))
        whenever(usageMetricRepository.findByWorkspaceIdAndPeriodYearAndPeriodMonth("WS-1", ym.year, ym.monthValue))
            .thenReturn(currentMetric())

        val processed = service.resetMonthlyCounters()

        assertEquals(0, processed)
    }

    @Test
    fun `deleteOldUsageMetrics removes records and returns count`() {
        val old = listOf(UsageMetric(), UsageMetric())
        whenever(
            usageMetricRepository.findByPeriodYearLessThanOrPeriodYearAndPeriodMonthLessThan(any(), any(), any())
        ).thenReturn(old)

        val deleted = service.deleteOldUsageMetrics(12)

        assertEquals(2, deleted)
        verify(usageMetricRepository).deleteAll(old)
    }
}
