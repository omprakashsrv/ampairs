package com.ampairs.analytics.service

import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.model.KpiDailySummary
import com.ampairs.analytics.repository.KpiDailySummaryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Unit tests for the coarse SALES roll-up. No DB — the repository and timezone provider are mocked.
 * Testcontainers-backed bucketing/idempotence tests (T015/T016/T016a) are deferred to CI (no Docker
 * in the build sandbox).
 */
class KpiRollupServiceTest {

    private val repo = mock<KpiDailySummaryRepository>()

    private fun serviceInZone(zone: ZoneId): KpiRollupService {
        val tz = mock<BusinessTimeZoneProvider> { on { currentZone() } doReturn zone }
        return KpiRollupService(repo, tz)
    }

    @Test
    fun `buckets a late-night sale into the business day, not UTC (R7)`() {
        // 2026-06-30T18:30:00Z == 2026-07-01 00:00 in Asia/Kolkata (+05:30)
        val service = serviceInZone(ZoneId.of("Asia/Kolkata"))
        val epoch = Instant.parse("2026-06-30T18:30:00Z").toEpochMilli()

        assertEquals(LocalDate.of(2026, 7, 1), service.businessDateOf(epoch))
    }

    @Test
    fun `same instant buckets to June in UTC`() {
        val service = serviceInZone(ZoneOffset.UTC)
        val epoch = Instant.parse("2026-06-30T18:30:00Z").toEpochMilli()

        assertEquals(LocalDate.of(2026, 6, 30), service.businessDateOf(epoch))
    }

    @Test
    fun `finalized invoice increments the SALES bucket gross and count`() {
        val service = serviceInZone(ZoneOffset.UTC)
        whenever(
            repo.findByMetricGroupAndBusinessDateAndDimProductIdAndDimCustomerId(
                eq(MetricGroup.SALES), any(), eq(""), eq(""),
            ),
        ).thenReturn(null)
        whenever(repo.save(any<KpiDailySummary>())).thenAnswer { it.arguments[0] }

        val epoch = Instant.parse("2026-06-15T10:00:00Z").toEpochMilli()
        service.applyInvoiceFinalized(1500.0, epoch)

        val captor = argumentCaptor<KpiDailySummary>()
        verify(repo).save(captor.capture())
        val saved = captor.firstValue
        assertEquals(MetricGroup.SALES, saved.metricGroup)
        assertEquals(LocalDate.of(2026, 6, 15), saved.businessDate)
        assertEquals(0, BigDecimal.valueOf(1500.0).compareTo(saved.grossAmount))
        assertEquals(1, saved.docCount)
    }
}
